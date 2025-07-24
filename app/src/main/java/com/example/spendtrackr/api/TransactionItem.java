package com.example.spendtrackr.api;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class TransactionItem implements Serializable {

    @SerializedName("row_index")
    public int rowIndex;

    @SerializedName("Date")
    public String date;

    @SerializedName("Description")
    public String description;

    @SerializedName("Amount")
    public String amount;

    @SerializedName("Type")
    public String type;

    @SerializedName("Account")
    public String account;

    @SerializedName("Friend Split")
    public String friendSplit;

    @SerializedName("Amount Borne")
    public String amountBorne;

    @SerializedName("Notes")
    public String notes;
}
