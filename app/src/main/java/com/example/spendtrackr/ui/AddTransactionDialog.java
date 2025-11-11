package com.example.spendtrackr.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.spendtrackr.R;
import com.example.spendtrackr.api.AddTransactionResponse;
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.api.TransactionItem;
import com.example.spendtrackr.utils.ApiParametersHelper;
import com.example.spendtrackr.utils.CategoryManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionDialog extends DialogFragment {

    public interface OnTransactionAddedListener {
        void onTransactionAdded(TransactionItem newItem);
    }


    private static final String ARG_DATE = "transaction_date";

    private String selectedDate;

    private OnTransactionAddedListener listener;

    public void setOnTransactionAddedListener(OnTransactionAddedListener listener) {
        this.listener = listener;
    }


    public static AddTransactionDialog newInstance(String date) {
        AddTransactionDialog fragment = new AddTransactionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_transaction, null);
        selectedDate = (getArguments() != null && getArguments().getString(ARG_DATE) != null)
                ? getArguments().getString(ARG_DATE)
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());


        TextInputEditText inputAmount = view.findViewById(R.id.inputAmount);
        AutoCompleteTextView categoryDropdown = view.findViewById(R.id.categoryDropdown);
        TextInputEditText inputFriendSplit = view.findViewById(R.id.inputFriendSplit);
        TextInputEditText inputNotes = view.findViewById(R.id.inputNotes);
        TextInputEditText inputAccountNumber = view.findViewById(R.id.transactionAccountNumber);
        AutoCompleteTextView transactionMethod = view.findViewById(R.id.transactionMethod);

        MaterialButton fiftyFiftyButton = view.findViewById(R.id.fiftyFiftyButton);
        MaterialButton clearSplitButton = view.findViewById(R.id.clearButton);
        MaterialButton fullSplitButton = view.findViewById(R.id.fullSplitButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);
        MaterialButton saveButton = view.findViewById(R.id.saveButton);
        MaterialButton deleteButton = view.findViewById(R.id.deleteButton);

        // Hide delete button (add mode)
        deleteButton.setVisibility(View.GONE);

        // Populate category dropdown
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                CategoryManager.getCategoryColorMap().keySet().toArray(new String[0])
        );
        categoryDropdown.setAdapter(categoryAdapter);

        // Populate method dropdown
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"CARD", "ACCOUNT"}
        );
        transactionMethod.setAdapter(methodAdapter);

        transactionMethod.setText("ACCOUNT", false);
        inputAccountNumber.setText("1234");

        // 50:50 logic
        fiftyFiftyButton.setOnClickListener(v -> {
            String amountStr = Objects.requireNonNull(inputAmount.getText()).toString();
            if (!TextUtils.isEmpty(amountStr)) {
                try {
                    double amt = Double.parseDouble(amountStr);
                    inputFriendSplit.setText(String.format("%.2f", amt / 2));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        // Full Split Logic
        fullSplitButton.setOnClickListener(v -> {
            String amountStr = Objects.requireNonNull(inputAmount.getText()).toString();
            inputFriendSplit.setText(amountStr);
        });

        // Clear logic
        clearSplitButton.setOnClickListener(v -> {
            inputFriendSplit.setText("");
        });

        // Cancel logic
        cancelButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> {
            String amountStr = Objects.requireNonNull(inputAmount.getText()).toString().trim();
            String typeStr = categoryDropdown.getText().toString().trim();
            String splitStr = Objects.requireNonNull(inputFriendSplit.getText()).toString().trim();
            String noteStr = Objects.requireNonNull(inputNotes.getText()).toString().trim();
            String methodStr = transactionMethod.getText().toString().trim();
            String accNumStr = Objects.requireNonNull(inputAccountNumber.getText()).toString().trim();

            if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(typeStr)) {
                Toast.makeText(requireContext(), "Amount and Category are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put(ApiParametersHelper.ARG_DATE, selectedDate + "T00:00:00");

            Map<String, String> transactionItem = new HashMap<>();
            transactionItem.put(ApiParametersHelper.FIELD_AMOUNT, amountStr);
            transactionItem.put(ApiParametersHelper.FIELD_TYPE, typeStr);
            transactionItem.put(ApiParametersHelper.FIELD_FRIEND_SPLIT, TextUtils.isEmpty(splitStr) ? "0" : splitStr);
            transactionItem.put(ApiParametersHelper.FIELD_NOTES, noteStr);
            transactionItem.put(ApiParametersHelper.FIELD_ACCOUNT, methodStr + " - " + (TextUtils.isEmpty(accNumStr) ? "1234" : accNumStr));

            body.put(ApiParametersHelper.ARG_TRANSACTION_ITEM, transactionItem);

            Log.d(getTag(), body.toString());

            ApiService apiService = ApiClient.getApiService(requireContext());
            apiService.addTransaction(body).enqueue(new Callback<BaseResponse<AddTransactionResponse>>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse<AddTransactionResponse>> call, @NonNull Response<BaseResponse<AddTransactionResponse>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        // Build new TransactionItem to reflect in RecyclerView
                        TransactionItem newItem = new TransactionItem();
                        newItem.rowIndex = response.body().data.getRowIndex();
                        newItem.amount = amountStr;
                        newItem.type = typeStr;
                        newItem.friendSplit = TextUtils.isEmpty(splitStr) ? "0" : splitStr;
                        newItem.amountBorne = String.valueOf(Double.parseDouble(amountStr) - Double.parseDouble(newItem.friendSplit));
                        newItem.notes = noteStr;
                        newItem.account = methodStr + " - " + (TextUtils.isEmpty(accNumStr) ? "1234" : accNumStr);
                        newItem.date = selectedDate;

                        // Notify listener
                        if (listener != null) {
                            listener.onTransactionAdded(newItem);
                        }
                        Toast.makeText(requireContext(), "Transaction added successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        String msg = response.body() != null ? response.body().message : response.message();
                        Toast.makeText(requireContext(), "Failed to Add Transaction \n" + msg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse<AddTransactionResponse>> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), "Failed to Add Transaction\n Network Error", Toast.LENGTH_LONG).show();
                }
            });
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle("Add Transaction")
                .create();
    }
}
