package com.example.spendtrackr.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TransactionResponse {

    @SerializedName("date")
    public String date;

    @SerializedName("transaction_count")
    public int transactionCount;

    @SerializedName("transactions")
    public List<TransactionItem> transactions;

    @SerializedName("generated_at")
    public String generatedAt;
}
