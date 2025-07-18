package com.example.spendtrackr.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {

    private static final String PREF_NAME = "SpendTrackrPrefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String DEFAULT_URL = "https://example.org";
    private static final String API_KEY = "api_key";
    private static final String DEFAULT_KEY = "key_123";

    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, DEFAULT_URL);
    }

    public static void setBaseUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }

    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(API_KEY, DEFAULT_KEY);
    }

    public static void setApiKey(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(API_KEY, key).apply();
    }
}
