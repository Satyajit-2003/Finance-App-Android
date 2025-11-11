package com.example.spendtrackr.api;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class AddTransactionResponse {

    @SerializedName("transaction_data")
    private Map<String, Object> transactionData;

    @SerializedName("row_index")
    private int rowIndex;

    public Map<String, Object> getTransactionData() {
        return transactionData;
    }

    public int getRowIndex() {
        return rowIndex;
    }
}
