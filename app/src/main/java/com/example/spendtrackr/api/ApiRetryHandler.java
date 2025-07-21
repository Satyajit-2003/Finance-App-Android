package com.example.spendtrackr.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiRetryHandler {

    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    public static <T> void enqueueWithRetry(Call<T> call, int retryCount, Callback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull  Response<T> response) {
                if ((response.code() == 404 || (response.code() >= 500 && response.code() < 600)) && retryCount < MAX_RETRIES) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Clone the call to retry
                        Log.i("ApiRetryHandler", String.format("Status %1s, Retrying.. %2s", response.code(), callback.toString()));
                        enqueueWithRetry(call.clone(), retryCount + 1, callback);
                    }, RETRY_DELAY_MS);
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
}
