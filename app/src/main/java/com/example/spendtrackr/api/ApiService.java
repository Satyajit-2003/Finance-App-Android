package com.example.spendtrackr.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.Map;

public interface ApiService {

    @POST("/api/v1/transactions")
    Call<BaseResponse<Void>> logTransaction(@Body Map<String, Object> body);

    @GET("/api/v1/stats/{monthYear}")
    Call<BaseResponse<StatsResponse>> getStats(@Path("monthYear") String monthYear);

    @GET("health")
    Call<BaseResponse<HealthResponse>> getHealth();

}
