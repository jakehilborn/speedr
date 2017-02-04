package com.jakehilborn.speedr.overpass;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.jakehilborn.speedr.utils.UnitUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OverpassInterceptor implements Interceptor {

    private Server DE; //Primary
    private Server RU; //Primary
    private Server FR; //Secondary

    public OverpassInterceptor() {
        DE = new Server("https://overpass-api.de/api/interpreter");
        RU = new Server("http://overpass.osm.rambler.ru/cgi/interpreter");
        FR = new Server("https://api.openstreetmap.fr/oapi/interpreter");
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String query = request.url().encodedQuery();
        Server overpassServer = chooseServer();
        Crashlytics.log(Log.INFO, OverpassInterceptor.class.getSimpleName(), "Using server" + overpassServer.getBaseUrl());

        request = request.newBuilder()
                .url(overpassServer.getBaseUrl() + "?" + query)
                .addHeader("Connection", "keep-alive")
                .addHeader("DNT", "1")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "currentlydebugging") //replace this with name + version or something
                .build();

        overpassServer.setDelay(System.nanoTime());
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Throwable error) {
            recordError(overpassServer, error.toString());
            throw error;
        }

        //Catches non-200 responses, empty 200 responses, and 200 responses that contain warning/error strings.
        //Requests for coordinates that don't return any data will still have an empty "elements" array.
        if (response.isSuccessful()) {

            //OkHttp response body is stored in a buffer that is consumed upon read. We consume the buffer
            //into a string and then re-insert it into the response so that Gson can deserialize later.
            String bodyString = response.body().string();
            ResponseBody rebuildBody = ResponseBody.create(response.body().contentType(), bodyString);
            response = response.newBuilder().body(rebuildBody).build();

            if (bodyString.contains("\"elements\"")) { //success
                overpassServer.addLatency(response.receivedResponseAtMillis() - response.sentRequestAtMillis());
            } else {
                recordError(overpassServer, "Body: " + bodyString);
            }
        } else {
            recordError(overpassServer, "HTTP code: " + response.code() + ", Body: " + response.body().string());
        }

        return response;
    }

    //Round robin between primary servers DE and RU. However, if either DE or RU is 5 times slower
    //than the other then use the faster of DE,RU and don't retry the slow one for 60s. We also clear
    //the last 5 stored latencies for the slower server. If both DE and RU have been penalized due to
    //slowness/errors then use the FR server.
    private Server chooseServer() {

        if (DE.getDelay() <= System.nanoTime() || RU.getDelay() <= System.nanoTime()) { //Use primary
            if (DE.getLatency() == 0 || RU.getLatency() == 0) {
              //Latencies can't be compared, use return value below.
            } else if (DE.getLatency() / RU.getLatency() >= 5) {
                DE.clearLatencies();
                DE.setDelay(System.nanoTime() + UnitUtils.secondsToNanos(60)); //Don't retry this server for 60 seconds
                return RU;
            } else if (RU.getLatency() / DE.getLatency() >= 5) {
                RU.clearLatencies();
                RU.setDelay(System.nanoTime() + UnitUtils.secondsToNanos(60)); //Don't retry this server for 60 seconds
                return DE;
            }

            return DE.getDelay() <= RU.getDelay() ? DE : RU;
        } else { //Use secondary
            if (DE.getDelay() <= RU.getDelay()) {
                return FR.getDelay() <= DE.getDelay() ? FR : DE;
            } else {
                return FR.getDelay() <= RU.getDelay() ? FR : RU;
            }
        }
    }

    private void recordError(Server overpassServer, String message) {
        overpassServer.setDelay(System.nanoTime() + UnitUtils.secondsToNanos(60)); //Don't retry this server for 60 seconds
        Crashlytics.log(Log.ERROR, OverpassInterceptor.class.getSimpleName(), "Server: " + overpassServer.getBaseUrl() + "Message: \n" + message);
    }
}
