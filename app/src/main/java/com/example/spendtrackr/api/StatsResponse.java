package com.example.spendtrackr.api;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class StatsResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("month_year")
        public String monthYear;

        @SerializedName("total_spend")
        public double totalSpend;

        @SerializedName("transaction_count")
        public int transactionCount;

        @SerializedName("categories")
        public Map<String, CategoryInfo> categories;

    }

    public static class CategoryInfo {
        @SerializedName("amount")
        public double amount;

        @SerializedName("count")
        public int count;
    }
}
