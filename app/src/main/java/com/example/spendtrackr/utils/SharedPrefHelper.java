package com.example.spendtrackr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.spendtrackr.api.ApiClient;

public class SharedPrefHelper {

    private static final String PREF_NAME = "SpendTrackrPrefs";

    // Base URL
    private static final String KEY_BASE_URL = "base_url";
    private static final String DEFAULT_URL = "https://example.org";

    // API Key
    private static final String API_KEY = "api_key";
    private static final String DEFAULT_KEY = "key_123";

    // Notification success
    private static final String KEY_SHOW_SUCCESS_NOTIFICATION = "show_success_notification";
    private static final boolean DEFAULT_SHOW_SUCCESS = true;

    // Notification failure
    private static final String KEY_SHOW_FAILURE_NOTIFICATION = "show_failure_notification";
    private static final boolean DEFAULT_SHOW_FAILURE = true;


    // ======== BASE URL ========
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, DEFAULT_URL);
    }

    public static void setBaseUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String existingUrl = prefs.getString(KEY_BASE_URL, DEFAULT_URL);

        if (!url.equals(existingUrl)) {
            prefs.edit().putString(KEY_BASE_URL, url).apply();
            ApiClient.rebuildService(context);
        }
    }

    // ======== API KEY ========
    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(API_KEY, DEFAULT_KEY);
    }

    public static void setApiKey(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String existingKey = prefs.getString(API_KEY, DEFAULT_KEY);

        if (!key.equals(existingKey)){
            prefs.edit().putString(API_KEY, key).apply();
            ApiClient.rebuildService(context);
        }
    }

    // ======== SUCCESS NOTIFICATION ========
    public static boolean getShowSuccessNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_SUCCESS_NOTIFICATION, DEFAULT_SHOW_SUCCESS);
    }

    public static void setShowSuccessNotification(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOW_SUCCESS_NOTIFICATION, enabled).apply();
    }

    // ======== FAILURE NOTIFICATION ========
    public static boolean getShowFailureNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_FAILURE_NOTIFICATION, DEFAULT_SHOW_FAILURE);
    }

    public static void setShowFailureNotification(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOW_FAILURE_NOTIFICATION, enabled).apply();
    }
}
