package com.example.spendtrackr.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.spendtrackr.api.AddTransactionResponse;
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.ApiRetryHandler;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.utils.ApiParametersHelper;
import com.example.spendtrackr.utils.NotificationHelper;
import com.example.spendtrackr.utils.SharedPrefHelper;
import com.example.spendtrackr.sms.parser.SmsModels;
import com.example.spendtrackr.sms.parser.SmsEngine;
import com.example.spendtrackr.sms.parser.ValidationRules;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null && pdus.length > 0) {
                    String format = bundle.getString("format");
                    StringBuilder fullMessage = new StringBuilder();
                    Date timestamp = null;

                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                        if (sms != null) {
                            fullMessage.append(sms.getMessageBody());
                            // Use timestamp from first SMS part
                            if (timestamp == null) {
                                timestamp = new Date(sms.getTimestampMillis());
                            }
                        }
                    }

                    if (timestamp == null) {
                        timestamp = new Date(); // fallback
                    }

                    String messageBody = fullMessage.toString().trim();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    String isoDate = sdf.format(timestamp);

                    // Parse and send to API
                    parseAndSendToApi(context, messageBody, isoDate);

                    // Single API call with full message
                    // sendToApi(context, messageBody, isoDate);
                }
            }
        }
    }


    // Deprecated function, to be removed
    private void sendToApi(Context context, String messageBody, String isoDate) {
        ApiService apiService = ApiClient.getApiService(context);

        Map<String, Object> body = new HashMap<>();
        body.put("text", messageBody);
        body.put("date", isoDate);

        ApiRetryHandler.enqueueWithRetry(apiService.logTransaction(body), 0, new Callback<BaseResponse<AddTransactionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<AddTransactionResponse>> call, @NonNull Response<BaseResponse<AddTransactionResponse>> response) {
                String apiMessage = response.body() != null ? response.body().message : response.message();
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "API Status Code: " + response.code() + ", " + apiMessage);
                    if (response.code() == 201){
                        if (SharedPrefHelper.getShowSuccessNotification(context)) {
                            NotificationHelper.showNotification(context, "SMS Log Successful - " + response.code(), apiMessage);
                        }
                    } else {
                        if (SharedPrefHelper.getShowFailureNotification(context)) {
                            NotificationHelper.showNotification(context, "SMS not transaction - " + response.code(), apiMessage);
                        }
                    }
                } else {
                    if (SharedPrefHelper.getShowErrorNotification(context)) {
                        NotificationHelper.showNotification(context, "API Error - " + response.code(), apiMessage);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<AddTransactionResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed: " + t.getMessage());
                if (SharedPrefHelper.getShowErrorNotification(context)) {
                    NotificationHelper.showNotification(context, "API Failure logTransaction", t.getMessage());
                }
            }
        });


    }

    private void parseAndSendToApi(Context context, String messageBody, String isoDate) {
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(messageBody);
        Log.d(TAG, "Parsed Info: " + info);

        ValidationRules.ValidationResult validation = ValidationRules.validate(info, messageBody);

        if (!validation.valid) {
            Log.d(TAG, "SMS is not a valid transaction: " + validation.reason);
            if (SharedPrefHelper.getShowFailureNotification(context)) {
                NotificationHelper.showNotification(context, "SMS not transaction", validation.reason);
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put(ApiParametersHelper.ARG_DATE, isoDate);

        Map<String, String> transactionItem = new HashMap<>();
        transactionItem.put(ApiParametersHelper.FIELD_AMOUNT, info.transaction.amount);
        transactionItem.put(ApiParametersHelper.FIELD_MERCHANT, info.transaction.merchant);
        transactionItem.put(ApiParametersHelper.FIELD_ACCOUNT,
                info.account.type + " - " + info.account.number);

        body.put(ApiParametersHelper.ARG_TRANSACTION_ITEM, transactionItem);
        Log.d(TAG, "Sending parsed transaction: " + body);

        ApiService apiService = ApiClient.getApiService(context);
        ApiRetryHandler.enqueueWithRetry(apiService.addTransaction(body), 0,
        new Callback<BaseResponse<AddTransactionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<AddTransactionResponse>> call,
                                   @NonNull Response<BaseResponse<AddTransactionResponse>> response) {
                String apiMessage = response.body() != null ? response.body().message : response.message();
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Log.i(TAG, "Transaction logged successfully: " + apiMessage);
                    if (SharedPrefHelper.getShowSuccessNotification(context)) {
                        NotificationHelper.showNotification(context, "Transaction Logged", apiMessage);
                    }
                } else {
                    Log.w(TAG, "Transaction log failed: " + apiMessage);
                    if (SharedPrefHelper.getShowErrorNotification(context)) {
                        NotificationHelper.showNotification(context, "Transaction Log Failed", apiMessage);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<AddTransactionResponse>> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                if (SharedPrefHelper.getShowErrorNotification(context)) {
                    NotificationHelper.showNotification(context, "API Failure addTransaction", t.getMessage());
                }
            }
        });
    }
}
