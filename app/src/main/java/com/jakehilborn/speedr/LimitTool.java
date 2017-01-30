package com.jakehilborn.speedr;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakehilborn.speedr.heremaps.HereMapsManager;
import com.jakehilborn.speedr.heremaps.HereMapsService;
import com.jakehilborn.speedr.heremaps.deserial.HereMapsResponse;
import com.jakehilborn.speedr.heremaps.deserial.Response;
import com.jakehilborn.speedr.overpass.OverpassInterceptor;
import com.jakehilborn.speedr.overpass.OverpassService;
import com.jakehilborn.speedr.overpass.deserial.OverpassManager;
import com.jakehilborn.speedr.overpass.deserial.OverpassResponse;
import com.jakehilborn.speedr.utils.Prefs;
import com.jakehilborn.speedr.utils.UnitUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
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
    private static final String RADIUS = "25"; //meters
    private OverpassService overpassService;
    private Subscription overpassSubscription;
    private OverpassManager overpassManager;

    private HereMapsService hereMapsService;
    private Converter<ResponseBody, HereMapsResponse> hereMapsErrorConverter;
    private Subscription hereMapsSubscription;
    private HereMapsManager hereMapsManager;
    private Toast hereMapsError;

    public LimitTool(StatsCalculator statsCalculator) {
        buildOverpassService();
        overpassManager = new OverpassManager(statsCalculator);

        buildHereMapsService();
        hereMapsManager = new HereMapsManager(statsCalculator);
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

    public void fetchLimit(Context context, Double lat, Double lon) {
        if (Prefs.isUseHereMaps(context)) {
            fetchHereMapsLimit(context, lat, lon);
        } else {
            fetchOverpassLimit(lat, lon);
        }
    }

    private void fetchOverpassLimit(final Double lat, final Double lon) {
        if (overpassSubscription != null) return; //Active request to Overpass has not responded yet

        String data = "[out:json];way(around:" +
                RADIUS + "," + lat + "," + lon +
                ")[\"highway\"][\"maxspeed\"];out;";

        overpassSubscription = overpassService.getLimit(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<OverpassResponse>() {
                    @Override
                    public void onSuccess(OverpassResponse overpassResponse) {
                        overpassSubscription = null;
                        Crashlytics.log(Log.INFO, "LimitTool", "Overpass success");
                        overpassManager.handleResponse(overpassResponse, lat, lon);
                    }

                    @Override
                    public void onError(Throwable error) {
                        overpassSubscription = null;
                        Crashlytics.logException(error);
                    }
                });
    }

    private void fetchHereMapsLimit(final Context context, final Double lat, final Double lon) {
        if (hereMapsSubscription != null) return; //Active request to Here Maps has not responded yet

        final boolean isUseKph = Prefs.isUseKph(context);
        String appId = Prefs.getHereAppId(context);
        String appCode = Prefs.getHereAppCode(context);
        String waypoint = lat + "," + lon;

        hereMapsSubscription = hereMapsService.getLimit(appId, appCode, "roadName", waypoint)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<HereMapsResponse>() {
                    @Override
                    public void onSuccess(HereMapsResponse hereMapsResponse) {
                        hereMapsSubscription = null;
                        Prefs.setPendingHereActivation(context, false);
                        Crashlytics.log(Log.INFO, "LimitTool", "Here maps success");
                        hereMapsManager.handleResponse(hereMapsResponse, lat, lon, isUseKph);
                    }

                    @Override
                    public void onError(Throwable error) {
                        hereMapsSubscription = null;

                        int statusCode = -1;
                        Response result = null;
                        if (error instanceof HttpException) {
                            try {
                                statusCode = ((HttpException) error).code();
                                ResponseBody body = ((HttpException) error).response().errorBody();
                                result = hereMapsErrorConverter.convert(body).getResponse();
                            } catch (IOException ioe) {
                                Crashlytics.logException(ioe);
                            }
                        }

                        String errorString;
                        //New HERE accounts take up to an hour to activate. New creds returns 403, invalid creds returns 401.
                        //If new account show notice on MainActivity instead of showing error as Toast. Then retry the request using Overpass.
                        if (statusCode == HttpURLConnection.HTTP_FORBIDDEN &&
                                System.currentTimeMillis() < Prefs.getTimeOfHereCreds(context) + UnitUtils.secondsToMillis(60 * 60)) {
                            Prefs.setPendingHereActivation(context, true);
                            fetchOverpassLimit(lat, lon);
                            errorString = "Pending HERE Activation";
                            Answers.getInstance().logCustom(new CustomEvent("Pending HERE Activation"));
                        } else if (result != null) {
                            Prefs.setPendingHereActivation(context, false);
                            errorString = result.getType() + " - " + result.getSubtype() + "\n\n" + result.getDetails();
                            if (hereMapsError != null) hereMapsError.cancel(); //Cancel previous toast so they don't queue up
                            hereMapsError = Toast.makeText(context, errorString, Toast.LENGTH_LONG);
                            hereMapsError.show();
                        } else {
                            errorString = "Unknown error occurred with HERE";
                        }

                        Crashlytics.log(Log.ERROR, "HERE error", errorString);
                        Crashlytics.logException(error); //Log exception at the end so Crashlytics includes recent logs in report
                    }
                });
    }

    public void destroy(Context context) {
        if (overpassSubscription != null) overpassSubscription.unsubscribe();
        if (hereMapsSubscription != null) hereMapsSubscription.unsubscribe();
        if (hereMapsError != null) hereMapsError.cancel();
        Prefs.setPendingHereActivation(context, false); //Set to false so the next time MainService is started the pending activation notice does not show
    }
}
