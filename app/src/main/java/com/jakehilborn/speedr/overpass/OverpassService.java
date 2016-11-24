package com.jakehilborn.speedr.overpass;

import com.jakehilborn.speedr.overpass.deserial.OverpassResponse;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Single;

public interface OverpassService {

    @GET("interpreter")
    Single<OverpassResponse> getLimit(@Query("data") String data);
}
