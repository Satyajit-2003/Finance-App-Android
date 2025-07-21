package com.example.spendtrackr.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.ApiRetryHandler;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.utils.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
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
                if (pdus != null) {
                    String format = bundle.getString("format"); // Needed for API 23+
                    for (Object pdu : pdus) {
                        SmsMessage sms;
                        sms = SmsMessage.createFromPdu((byte[]) pdu, format);

                        if (sms == null) return;

                        String messageBody = sms.getMessageBody();
                        Date timestamp = new Date(sms.getTimestampMillis());

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                        String isoDate = sdf.format(timestamp);

                        Log.d(TAG, "Received SMS: " + messageBody + " at " + isoDate);

                        sendToApi(context, messageBody, isoDate);
                    }
                }

            }
        }
    }

    private void sendToApi(Context context, String messageBody, String isoDate) {
        ApiService apiService = ApiClient.getApiService(context);

        Map<String, Object> body = new HashMap<>();
        body.put("text", messageBody);
        body.put("date", isoDate);
        Log.i("SMSReceiver", "Sending to API, " + isoDate + messageBody);

        ApiRetryHandler.enqueueWithRetry(apiService.logTransaction(body), 0, new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Void>> call, @NonNull Response<BaseResponse<Void>> response) {
                String apiMessage = response.body() != null ? response.body().message : response.message();
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "API Error Code: " + response.code());
                    NotificationHelper.showErrorNotification(context, "logTransaction Code: " + response.code(), apiMessage);
                } else {
                    NotificationHelper.showErrorNotification(context, "logTransaction Success", apiMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Void>> call, @NonNull Throwable t) {
                Log.e(TAG, "API Call Failed: " + t.getMessage());
                NotificationHelper.showErrorNotification(context, "API Failure logTransaction", t.getMessage());
            }
        });


    }
}
