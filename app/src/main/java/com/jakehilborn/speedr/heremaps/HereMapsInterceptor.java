package com.jakehilborn.speedr.heremaps;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.jakehilborn.speedr.LimitFetcher;
import com.jakehilborn.speedr.utils.ErrorReporter;
import com.jakehilborn.speedr.utils.UnitUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HereMapsInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        Response response;
        try {
            response = chain.proceed(request);
        } catch (Throwable error) {
            throw error;
        }

        //OkHttp response body is stored in a buffer that is consumed upon read. We consume the buffer
        //into a string and then re-insert it into the response so that Gson can deserialize later.
        String bodyString = response.body().string();
        ResponseBody rebuildBody = ResponseBody.create(response.body().contentType(), bodyString);
        response = response.newBuilder().body(rebuildBody).build();

        return response;
    }
}
