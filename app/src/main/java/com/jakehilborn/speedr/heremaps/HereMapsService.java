package com.jakehilborn.speedr.heremaps;

import com.jakehilborn.speedr.heremaps.deserial.HereMapsResponse;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Single;

public interface HereMapsService {

    @GET("getlinkinfo.json")
    Single<HereMapsResponse> getLimit(
            @Query("app_id") String appId,
            @Query("app_code") String appCode,
            @Query("linkattributes") String linkattributes,
            @Query("waypoint") String waypoint);
}
