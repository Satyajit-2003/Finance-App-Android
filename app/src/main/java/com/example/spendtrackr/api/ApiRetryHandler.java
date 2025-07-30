package com.example.spendtrackr.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiRetryHandler {

    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 3000;
    private static final long INCREMENT_MS = 2000;

    private static final String TAG = "ApiRetryHandler";

    public static <T> void enqueueWithRetry(Call<T> call, int retryCount, Callback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull  Response<T> response) {
                if ((response.code() == 404 || (response.code() >= 500 && response.code() < 600)) && retryCount < MAX_RETRIES) {
                    long retryDelay = BASE_DELAY_MS + (retryCount * INCREMENT_MS);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Clone the call to retry
                        Log.i(TAG, String.format("Status %1s, Retrying in %2s ms.. [%3s]", response.code(), retryDelay, retryCount));
                        enqueueWithRetry(call.clone(), retryCount + 1, callback);
                    }, retryDelay);
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                if (retryCount < MAX_RETRIES && t instanceof IOException) {
                    long retryDelay = BASE_DELAY_MS + (retryCount * INCREMENT_MS);

                    Log.w(TAG, String.format("Failure: %s, Retrying in %d ms... [Retry #%d]",
                            t, retryDelay, retryCount + 1));

                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            enqueueWithRetry(call.clone(), retryCount + 1, callback), retryDelay);
                } else {
                    callback.onFailure(call, t);
                }
            }
        });
    }
}
