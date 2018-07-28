package com.jakehilborn.speedr.heremaps;

import com.jakehilborn.speedr.heremaps.deserial.pde.HerePDEResponse;

import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import rx.Single;

public interface HerePDEService {

    @GET("tile.json")
    Single<HerePDEResponse> getLimits(
            @Query("app_id") String appId,
            @Query("app_code") String appCode,
            @Query("layer") String layer,
            @Query("level") Integer level,
            @Query("tilex") Integer tilex,
            @Query("tiley") Integer tiley,
            @Header("User-Agent") String userAgent);
}
