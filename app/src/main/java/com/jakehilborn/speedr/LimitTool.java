package com.jakehilborn.speedr;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakehilborn.speedr.heremaps.HereMapsService;
import com.jakehilborn.speedr.heremaps.deserial.HereMapsResponse;
import com.jakehilborn.speedr.heremaps.deserial.Response;
import com.jakehilborn.speedr.overpass.OverpassInterceptor;
import com.jakehilborn.speedr.overpass.OverpassService;
import com.jakehilborn.speedr.overpass.deserial.OverpassResponse;
import com.jakehilborn.speedr.utils.Prefs;
import com.jakehilborn.speedr.utils.UnitUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LimitTool {
    private static final String RADIUS = "10"; //meters
    private OverpassService overpassService;
    private Subscription overpassSubscription;

    private HereMapsService hereMapsService;
    private Converter<ResponseBody, HereMapsResponse> hereMapsErrorConverter;
    private Subscription hereMapsSubscription;
    private Toast hereMapsError;

    public LimitTool() {
        buildOverpassService();
        buildHereMapsService();
    }

    private void buildOverpassService() {
        Gson gson = new GsonBuilder().create();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new OverpassInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://dummy.url") //OverpassInterceptor will choose the appropriate Overpass endpoint
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        overpassService = retrofit.create(OverpassService.class);
    }

    private void buildHereMapsService() {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://route.st.nlp.nokia.com/routing/7.2/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        hereMapsService = retrofit.create(HereMapsService.class);
        hereMapsErrorConverter = retrofit.responseBodyConverter(HereMapsResponse.class, new Annotation[0]);
    }

    public void fetchLimit(Context context, Double latitude, Double longitude, final StatsCalculator statsCalculator) {
        if (Prefs.isUseHereMaps(context)) {
            fetchHereMapsLimit(context, latitude, longitude, statsCalculator);
        } else {
            fetchOverpassLimit(latitude, longitude, statsCalculator);
        }
    }

    private void fetchOverpassLimit(Double latitude, Double longitude, final StatsCalculator statsCalculator) {
        if (overpassSubscription != null) return; //Active request to Overpass has not responded yet

        String data = "[out:json];way(around:" +
                RADIUS + "," + latitude + "," + longitude +
                ")[\"highway\"][\"maxspeed\"];out;";

        overpassSubscription = overpassService.getLimit(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<OverpassResponse>() {
                    @Override
                    public void onSuccess(OverpassResponse value) {
                        overpassSubscription = null;
                        Crashlytics.log(Log.INFO, "LimitTool", "Overpass success");
                        if (value.getElements().length >= 1) {
                            Double limit = parseOverpassLimit(value.getElements()[0].getTags().getMaxSpeed());
                            statsCalculator.setLimit(limit);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        overpassSubscription = null;
                        Crashlytics.logException(error);
                    }
                });
    }

    private void fetchHereMapsLimit(final Context context, Double latitude, Double longitude, final StatsCalculator statsCalculator) {
        if (hereMapsSubscription != null) return; //Active request to Here Maps has not responded yet

        final boolean isUseKph = Prefs.isUseKph(context);
        String appId = Prefs.getHereAppId(context);
        String appCode = Prefs.getHereAppCode(context);
        String waypoint = latitude + "," + longitude;

        hereMapsSubscription = hereMapsService.getLimit(appId, appCode, waypoint)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<HereMapsResponse>() {
                    @Override
                    public void onSuccess(HereMapsResponse result) {
                        hereMapsSubscription = null;
                        Crashlytics.log(Log.INFO, "LimitTool", "Here maps success");
                        Double limit = parseHereMapsLimit(result.getResponse().getLink()[0].getSpeedLimit(), isUseKph);
                        statsCalculator.setLimit(limit);
                    }

                    @Override
                    public void onError(Throwable error) {
                        hereMapsSubscription = null;

                        Response result = null;
                        if (error instanceof HttpException) {
                            try {
                                ResponseBody body = ((HttpException) error).response().errorBody();
                                result = hereMapsErrorConverter.convert(body).getResponse();
                            } catch (IOException ioe) {
                                Crashlytics.logException(ioe);
                            }
                        }

                        String toastText;
                        if (result == null) {
                            toastText = "Unknown error occurred with Here Maps";
                        } else {
                            toastText = result.getType() + " - " + result.getSubtype() + "\n\n" + result.getDetails();
                        }

                        Crashlytics.log(Log.ERROR, "Here maps error", toastText);
                        Crashlytics.logException(error);

                        if (hereMapsError != null) hereMapsError.cancel(); //Cancel previous toast so they don't queue up
                        hereMapsError = hereMapsError.makeText(context, toastText, Toast.LENGTH_LONG);
                        hereMapsError.show();
                    }
                });
    }

    private Double parseOverpassLimit(String limit) {
        if (limit == null || limit.isEmpty()) return null;

        //Overpass maxspeed uses whole numbers. Example limit: "35 mph"
        Integer num = Integer.parseInt(limit.replaceAll("[^0-9]", ""));

        if (limit.contains("mph")) {
            return UnitUtils.mphToMs(num);
        } else if (limit.contains("knots")) {
            return UnitUtils.knotsToMs(num);
        } else { //kph if unit is not specified in response
            return UnitUtils.kphToMs(num);
        }
    }

    private Double parseHereMapsLimit(Double limit, boolean isUseKph) {
        //Round Here maps slightly inaccurate speed limit data to nearest 5mph or 5kmh increment
        if (isUseKph) {
            Integer roundedLimit = UnitUtils.msToKphRoundToFive(limit);
            return UnitUtils.kphToMs(roundedLimit);
        } else { //mph
            Integer roundedLimit = UnitUtils.msToMphRoundToFive(limit);
            return UnitUtils.mphToMs(roundedLimit);
        }
    }

    public void destroy() {
        if (overpassSubscription != null) overpassSubscription.unsubscribe();
        if (hereMapsSubscription != null) hereMapsSubscription.unsubscribe();
        if (hereMapsError != null) hereMapsError.cancel();
    }
}
