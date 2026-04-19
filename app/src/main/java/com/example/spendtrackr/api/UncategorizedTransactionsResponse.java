package com.example.spendtrackr.api;


import com.google.gson.annotations.SerializedName;
import java.util.List;


public class UncategorizedTransactionsResponse {
    @SerializedName("month_year")
    private String month_year;

    @SerializedName("uncategorized_dates")
    private List<String> uncategorized_dates;

    @SerializedName("total")
    private int total;

    public List<String> getUncategorizedDates() {
        return uncategorized_dates;
    }

    public String getMonthYear() {
        return month_year;
    }

    public int getTotal() {
        return total;
    }
}
