package com.example.spendtrackr.api;

import com.google.gson.annotations.SerializedName;

public class AuthCheckResponse {

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("version")
    private String version;

    @SerializedName("sheet_url")
    private String sheetUrl;

    public String getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }

    public String getSheetUrl() {
        return sheetUrl;
    }
}
