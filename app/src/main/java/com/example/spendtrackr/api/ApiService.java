package com.example.spendtrackr.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.util.Map;

public interface ApiService {

    @POST("/api/v1/log")
    Call<ResponseBody> logTransaction(@Body Map<String, Object> body);

    @GET("/api/v1/stats")
    Call<StatsResponse> getStats();

    @GET("health")
    Call<HealthResponse> getHealth();

}
