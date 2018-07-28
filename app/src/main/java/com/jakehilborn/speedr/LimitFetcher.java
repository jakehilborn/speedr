package com.jakehilborn.speedr;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakehilborn.speedr.heremaps.HereGeoService;
import com.jakehilborn.speedr.heremaps.HereMapsManager;
import com.jakehilborn.speedr.heremaps.HerePDEService;
import com.jakehilborn.speedr.heremaps.deserial.geo.HereGeoResponse;
import com.jakehilborn.speedr.heremaps.deserial.geo.Response;
import com.jakehilborn.speedr.overpass.OverpassInterceptor;
import com.jakehilborn.speedr.overpass.OverpassManager;
import com.jakehilborn.speedr.overpass.OverpassService;
import com.jakehilborn.speedr.overpass.deserial.OverpassResponse;
import com.jakehilborn.speedr.utils.ErrorReporter;
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

public class LimitFetcher {
    private static final String RADIUS = "25"; //meters
    private OverpassService overpassService;
    private Subscription overpassSubscription;
    private OverpassManager overpassManager;

    private HerePDEService herePDEService;

    private HereGeoService hereGeoService;
    private Converter<ResponseBody, HereGeoResponse> hereMapsErrorConverter;
    private Subscription hereMapsSubscription;
    private HereMapsManager hereMapsManager;
    private Toast hereMapsError;

    public static final String USER_AGENT = "Speedr/" + BuildConfig.VERSION_NAME;

    public LimitFetcher(Context context, StatsCalculator statsCalculator) {
        buildOverpassService();
        overpassManager = new OverpassManager(statsCalculator);

        buildHereMapsService();
        hereMapsManager = new HereMapsManager(context, statsCalculator);
    }

    private void buildOverpassService() {
        Gson gson = new GsonBuilder().create();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new OverpassInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://dummy.url/") //OverpassInterceptor will choose the appropriate Overpass endpoint
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        overpassService = retrofit.create(OverpassService.class);
    }

    private void buildHereMapsService() {
        Retrofit geoRetrofit = new Retrofit.Builder()
                .baseUrl("https://reverse.geocoder.cit.api.here.com/6.2/")
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        hereGeoService = geoRetrofit.create(HereGeoService.class);
        hereMapsErrorConverter = geoRetrofit.responseBodyConverter(HereGeoResponse.class, new Annotation[0]);
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
                        overpassManager.handleResponse(overpassResponse, lat, lon);
                    }

                    @Override
                    public void onError(Throwable error) {
                        overpassSubscription = null;
                        //Error was already logged in OverpassInterceptor
                    }
                });
    }

    private void fetchHereMapsLimit(final Context context, final Double lat, final Double lon) {
        if (hereMapsSubscription != null) return; //Active request to Here Maps has not responded yet

        final boolean isUseKph = Prefs.isUseKph(context);
        String appId = Prefs.getHereAppId(context);
        String appCode = Prefs.getHereAppCode(context);
        String prox = lat + "," + lon + "," + RADIUS;

        hereMapsSubscription = hereGeoService.reverseGeocode(appId, appCode, prox, "retrieveAddresses", "linkInfo", "9", USER_AGENT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<HereGeoResponse>() {
                    @Override
                    public void onSuccess(HereGeoResponse hereGeoResponse) {
                        Prefs.setPendingHereActivation(context, false);
                        hereMapsManager.handleResponse(context, hereGeoResponse, lat, lon, isUseKph);
                        hereMapsSubscription = null;
                    }

                    @Override
                    public void onError(Throwable error) {
                        hereMapsSubscription = null;

                        int statusCode = -1;
                        String type = "";
                        String subType = "";
                        String details = "";
                        if (error instanceof HttpException) {
                            try {
                                ResponseBody body = ((HttpException) error).response().errorBody();
                                Response hereResponse = hereMapsErrorConverter.convert(body).getResponse();

                                statusCode = ((HttpException) error).code();
//                                type = hereResponse.getType();
//                                subType = hereResponse.getSubtype();
//                                details = hereResponse.getDetails();
                            } catch (IOException ioe) {
                                Crashlytics.logException(ioe);
                            }
                        } else {
                            ErrorReporter.logHereError(error);
                            return;
                        }

                        //New HERE accounts take up to an hour to activate. New creds returns 403, invalid creds returns 401.
                        //If new account, show notice on MainActivity instead of showing error as Toast. Then retry the request using Overpass.
                        if (statusCode == HttpURLConnection.HTTP_FORBIDDEN &&
                                System.currentTimeMillis() < Prefs.getTimeOfHereCreds(context) + UnitUtils.secondsToMillis(60 * 60)) {
                            Prefs.setPendingHereActivation(context, true);
                            fetchOverpassLimit(lat, lon);
                            Answers.getInstance().logCustom(new CustomEvent("Pending HERE Activation"));
                            return;
                        }

                        //Show error message to user via Toast
                        if (!type.isEmpty() || !subType.isEmpty() || !details.isEmpty()) {
                            Prefs.setPendingHereActivation(context, false);
                            String toastText = type + " - " + subType + "\n\n" + details;
                            if (hereMapsError != null) hereMapsError.cancel(); //Cancel previous toast so they don't queue up
                            hereMapsError = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
                            hereMapsError.show();
                        }

                        ErrorReporter.logHereError(statusCode, type, subType, details);
                    }
                });
    }

    public void destroy(Context context) {
        Crashlytics.log(Log.INFO, LimitFetcher.class.getSimpleName(), "destroy()");
        if (overpassSubscription != null) overpassSubscription.unsubscribe();
        if (hereMapsSubscription != null) hereMapsSubscription.unsubscribe();
        if (hereMapsError != null) hereMapsError.cancel();
        Prefs.setPendingHereActivation(context, false); //Set to false so the next time MainService is started the pending activation notice does not show
    }
}
