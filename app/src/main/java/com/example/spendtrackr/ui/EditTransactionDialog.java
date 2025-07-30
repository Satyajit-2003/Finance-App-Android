package com.example.spendtrackr.ui;

import android.app.Dialog;
import android.content.Context;
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
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.api.TransactionItem;
import com.example.spendtrackr.utils.CategoryManager;
import com.example.spendtrackr.utils.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditTransactionDialog extends DialogFragment {

    public interface OnTransactionUpdatedListener {
        void onTransactionUpdated(TransactionItem updatedItem);
        void onTransactionDeleted(int rowIndex);
    }

    private static final String ARG_ITEM = "transaction_item";
    private static final String ARG_SHEET_NAME = "sheet_name";

    private static final String FIELD_AMOUNT = "Amount";
    private static final String FIELD_TYPE = "Type";
    private static final String FIELD_FRIEND_SPLIT = "Friend Split";
    private static final String FIELD_NOTES = "Notes";

    private TransactionItem transactionItem;
    private String sheetName;
    private OnTransactionUpdatedListener listener;

    public void setOnTransactionUpdatedListener(OnTransactionUpdatedListener listener) {
        this.listener = listener;
    }

    public static EditTransactionDialog newInstance(TransactionItem item, String sheetName) {
        EditTransactionDialog fragment = new EditTransactionDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ITEM, item);
        args.putString(ARG_SHEET_NAME, sheetName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnTransactionUpdatedListener) {
            listener = (OnTransactionUpdatedListener) context;
        } else if (getParentFragment() instanceof OnTransactionUpdatedListener) {
            listener = (OnTransactionUpdatedListener) getParentFragment();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_transaction, null);
        assert getArguments() != null;
        transactionItem = (TransactionItem) getArguments().getSerializable(ARG_ITEM);
        sheetName = getArguments().getString(ARG_SHEET_NAME);

        TextInputEditText inputAmount = view.findViewById(R.id.inputAmount);
        AutoCompleteTextView categoryDropdown = view.findViewById(R.id.categoryDropdown);
        TextInputEditText inputFriendSplit = view.findViewById(R.id.inputFriendSplit);
        TextInputEditText inputNotes = view.findViewById(R.id.inputNotes);
        MaterialButton buttonDelete = view.findViewById(R.id.deleteButton);
        MaterialButton buttonSave = view.findViewById(R.id.saveButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);
        MaterialButton fiftyFiftyButton = view.findViewById(R.id.fiftyFiftyButton);
        MaterialButton clearSplitButton = view.findViewById(R.id.clearButton);
        MaterialButton fullSplitButton = view.findViewById(R.id.fullSplitButton);


        // Populate dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                CategoryManager.getCategoryColorMap().keySet().toArray(new String[0])
        );
        categoryDropdown.setAdapter(adapter);

        // Pre-fill data
        inputAmount.setText(transactionItem.amount);
        categoryDropdown.setText(transactionItem.type, false);
        inputFriendSplit.setText((Objects.equals(transactionItem.friendSplit, "0")) ? "" : String.valueOf(transactionItem.friendSplit));
        inputNotes.setText(transactionItem.notes);

        // 50:50 logic
        fiftyFiftyButton.setOnClickListener(v -> {
            String amountStr = Objects.requireNonNull(inputAmount.getText()).toString();
            if (!TextUtils.isEmpty(amountStr)) {
                try {
                    double amt = Double.parseDouble(amountStr);
                    inputFriendSplit.setText(String.format("%.2f", amt / 2));
                } catch (NumberFormatException ignored) {}
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

        // Save logic
        buttonSave.setOnClickListener(v -> {
            String amountStrTemp = Objects.requireNonNull(inputAmount.getText()).toString();
            String friendSplitStrTemp = Objects.requireNonNull(inputFriendSplit.getText()).toString();
            double amount, friendSplit;
            final String amountStr, friendSplitStr;

            if (TextUtils.isEmpty((amountStrTemp))){
                amount = 0;
                amountStr = "0";
            } else {
                amount = Double.parseDouble(amountStrTemp);
                amountStr = amountStrTemp;
            }

            if (TextUtils.isEmpty((friendSplitStrTemp))){
                friendSplit = 0;
                friendSplitStr = "0";
            } else {
                friendSplit = Double.parseDouble(friendSplitStrTemp);
                friendSplitStr = friendSplitStrTemp;
            }


            if (friendSplit > amount) {
                Toast.makeText(getContext(), "Friend Split can't exceed Amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> updates = new HashMap<>();
            if (!transactionItem.amount.equals(amountStr)) updates.put(FIELD_AMOUNT, amountStr);
            if (!transactionItem.type.equals(categoryDropdown.getText().toString())) updates.put(FIELD_TYPE, categoryDropdown.getText().toString());
            if (!transactionItem.friendSplit.equals(friendSplitStr)) updates.put(FIELD_FRIEND_SPLIT, friendSplitStr);
            if (!transactionItem.notes.equals(Objects.requireNonNull(inputNotes.getText()).toString())) updates.put(FIELD_NOTES, inputNotes.getText().toString());

            if (updates.isEmpty()) {
                dismiss();
                return;
            }

            ApiService apiService = ApiClient.getApiService(requireContext());
            Map<String, Object> body = new HashMap<>();
            body.put("sheet_name", sheetName);
            body.put("row_index", transactionItem.rowIndex);
            body.put("updates", updates);

            apiService.updateTransaction(body).enqueue(new Callback<BaseResponse<Void>>() {
                @Override
                public void onResponse(@NonNull Call<BaseResponse<Void>> call, @NonNull Response<BaseResponse<Void>> response) {
                    if (response.isSuccessful()) {
                        // Update locally and notify
                        transactionItem.amount = amountStr;
                        transactionItem.type = categoryDropdown.getText().toString();
                        transactionItem.friendSplit = friendSplitStr;
                        transactionItem.amountBorne = String.valueOf(Double.parseDouble(transactionItem.amount) - friendSplit);
                        transactionItem.notes = inputNotes.getText().toString();
                        listener.onTransactionUpdated(transactionItem);
                        dismiss();
                    } else {
                        NotificationHelper.showErrorNotification(requireContext(), "Update Error", response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<BaseResponse<Void>> call, @NonNull Throwable t) {
                    NotificationHelper.showErrorNotification(requireContext(), "Update Failed", t.getMessage());
                }
            });
        });

        // Delete logic
        buttonDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    ApiService apiService = ApiClient.getApiService(requireContext());
                    Map<String, Object> body = new HashMap<>();
                    body.put("sheet_name", sheetName);
                    body.put("row_index", transactionItem.rowIndex);

                    apiService.deleteTransaction(body).enqueue(new Callback<BaseResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<BaseResponse<Void>> call, @NonNull Response<BaseResponse<Void>> response) {
                            if (response.isSuccessful()) {
                                listener.onTransactionDeleted(transactionItem.rowIndex);
                                dismiss();
                            } else {
                                NotificationHelper.showErrorNotification(requireContext(), "Delete Failed", response.message());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<BaseResponse<Void>> call, @NonNull Throwable t) {
                            NotificationHelper.showErrorNotification(requireContext(), "Delete Failed", t.getMessage());
                        }
                    });
                }).show());

        // Cancel logic
        cancelButton.setOnClickListener(v -> dismiss());

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
