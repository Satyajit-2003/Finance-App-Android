package com.example.spendtrackr.api;

import android.content.Context;

import com.example.spendtrackr.utils.SharedPrefHelper;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static ApiService apiService;

    public static ApiService getApiService(Context context) {
        if (apiService == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            String baseUrl = SharedPrefHelper.getBaseUrl(context);
            String apiKey = SharedPrefHelper.getApiKey(context);

            // Add API Key Interceptor
            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();
                Request requestWithHeader = original.newBuilder()
                        .header("X-API-Key", apiKey)
                        .build();
                return chain.proceed(requestWithHeader);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(headerInterceptor)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)  // Attach the client with interceptors
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    public static void rebuildService(Context context) {
        apiService = null;
        getApiService(context);
    }

}

