package com.jakehilborn.speedr.heremaps;

import com.jakehilborn.speedr.heremaps.deserial.geo.HereGeoResponse;

import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import rx.Single;

public interface HereGeoService {

    @GET("reversegeocode.json")
    Single<HereGeoResponse> reverseGeocode(
            @Query("app_id") String appId,
            @Query("app_code") String appCode,
            @Query("prox") String prox,
            @Query("mode") String mode,
            @Query("locationAttributes") String locationAttributes,
            @Query("gen") String gen,
            @Header("User-Agent") String userAgent);
}
