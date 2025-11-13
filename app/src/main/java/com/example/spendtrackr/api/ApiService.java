package com.example.spendtrackr.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.Map;

public interface ApiService {

    @GET("health")
    Call<BaseResponse<HealthResponse>> getHealth();

    @GET("api/v1/check-auth")
    Call<BaseResponse<AuthCheckResponse>> checkAuth();

    @GET("/api/v1/stats")
    Call<BaseResponse<StatsResponse>> getStats(@Query("month_year") String monthYear);

    @POST("/api/v1/log-sms")
    Call<BaseResponse<AddTransactionResponse>> logTransaction(@Body Map<String, Object> body);

    @POST("/api/v1/transactions")
    Call<BaseResponse<AddTransactionResponse>> addTransaction(@Body Map<String, Object> body);

    @GET("/api/v1/transactions")
    Call<BaseResponse<TransactionResponse>> getTransactionsByDate(@Query("date") String date);

    @PATCH("/api/v1/transactions")
    Call<BaseResponse<Void>> updateTransaction(@Body Map<String, Object> body);

    @HTTP(method = "DELETE", path = "/api/v1/transactions", hasBody = true)
    Call<BaseResponse<Void>> deleteTransaction(@Body Map<String, Object> body);


}
