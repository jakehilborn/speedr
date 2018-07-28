package com.jakehilborn.speedr.heremaps;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.GsonBuilder;
import com.jakehilborn.speedr.LimitFetcher;
import com.jakehilborn.speedr.StatsCalculator;
import com.jakehilborn.speedr.heremaps.cache.HereCache;
import com.jakehilborn.speedr.heremaps.deserial.geo.HereGeoResponse;
import com.jakehilborn.speedr.heremaps.deserial.geo.Result;
import com.jakehilborn.speedr.heremaps.deserial.geo.View;
import com.jakehilborn.speedr.heremaps.deserial.pde.GeoNode;
import com.jakehilborn.speedr.heremaps.deserial.pde.HerePDEResponse;
import com.jakehilborn.speedr.heremaps.deserial.pde.Rows;
import com.jakehilborn.speedr.utils.Prefs;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class HereMapsManager {

    private HerePDEService herePDEService;
    private Subscription pdeSubscription;
    private HereCache hereCache;
    private final StatsCalculator statsCalculator;
    private String prevRoadName;
    private boolean prevLimitMissing;

    public HereMapsManager(Context context, StatsCalculator statsCalculator) {
        this.statsCalculator = statsCalculator;
        hereCache = new HereCache(context);

        Retrofit pdeRetrofit = new Retrofit.Builder()
                .baseUrl("https://pde.cit.api.here.com/1/")
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        herePDEService = pdeRetrofit.create(HerePDEService.class);
    }

    public String handleResponse(Context context, HereGeoResponse hereGeoResponse, Double lat, Double lon, boolean isUseKph) {
        Set<GeoNode> nodes = normalizeResponse(hereGeoResponse);

        for (GeoNode node : nodes) {
            Integer limit = hereCache.get(node.getRefId());
            System.out.println(limit);
        }

        PDEArgs args = new PDEArgs(lat, lon, hereGeoResponse.getResponse().getView()[0].getResult()[0].getLocation().getLinkInfo().getFunctionalClass());
        fetchHerePDETile(context, args);

        return null;
    }

    private Set<GeoNode> normalizeResponse(HereGeoResponse hereGeoResponse) {
        Set<GeoNode> set = new HashSet<>();

        try {
            for (View view : hereGeoResponse.getResponse().getView()) {
                for (Result result : view.getResult()) {
                    set.add(new GeoNode(
                            Long.parseLong(result.getLocation().getMapReference().getReferenceId()),
                            result.getLocation().getAddress().getStreet(),
                            result.getLocation().getLinkInfo().getFunctionalClass()
                    ));
                }
            }
        } catch (NullPointerException | NumberFormatException e) {
            //log
        }

        return set;
    }

    private void fetchHerePDETile(final Context context, PDEArgs args) {
        if (pdeSubscription != null) return;

        final String appId = Prefs.getHereAppId(context);
        final String appCode = Prefs.getHereAppCode(context);
        final String layer = args.getLayer();
        final Integer level = args.getLevel();
        final Integer tileX = args.getTileX();
        final Integer tileY = args.getTileY();
        final String userAgent = LimitFetcher.USER_AGENT;

        pdeSubscription = herePDEService.getLimits(appId, appCode, layer, level, tileX, tileY, userAgent)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<HerePDEResponse>() {
                    @Override
                    public void onSuccess(HerePDEResponse herePDEResponse) {
                        pdeSubscription = null;
                        hereCache.putResponse(herePDEResponse, tileX, tileY, level);
                    }

                    @Override
                    public void onError(Throwable error) {
                        pdeSubscription = null;
                        //do some logging
                    }
                });
    }

//    private Double roundLimit(Integer limit, boolean isUseKph) {
//        //Round Here maps slightly inaccurate speed limit data to nearest 5mph or 5kmh increment.
//        if (isUseKph) {
//            Integer roundedKph = UnitUtils.roundToFive(limit);
//            return UnitUtils.kphToMs(roundedKph);
//        } else { //mph
//            Integer mph = UnitUtils.mphto
//            return UnitUtils.mphToMs(roundedLimit);
//        }
//    }

    public void destroy() {
        Crashlytics.log(Log.INFO, HereMapsManager.class.getSimpleName(), "destroy()");
        pdeSubscription.unsubscribe();
        hereCache.close();
    }
}
