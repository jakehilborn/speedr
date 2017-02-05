package com.jakehilborn.speedr.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.stream.MalformedJsonException;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

//Crashlytics groups exceptions together by the line number that logException is called on.
//This inspects the exception and limit provider to log exceptions on different line numbers.
public class ErrorReporter {

    private static final String OVERPASS = "Overpass ";
    private static final String HERE = "HERE ";

    public static void logOverpassError(int statusCode, String baseUrl, String body) {
        Crashlytics.log(Log.ERROR, ErrorReporter.class.getSimpleName(), "Overpass error: " + baseUrl);
        sendHttpError(statusCode, body, false);
    }

    public static void logOverpassError(Throwable error, String baseUrl) {
        Crashlytics.log(Log.ERROR, ErrorReporter.class.getSimpleName(), "Overpass exception: " + baseUrl);
        sendOtherError(error, false);
    }

    public static void logHereError(int statusCode, String type, String subType, String details) {
        Crashlytics.log(Log.ERROR, ErrorReporter.class.getSimpleName(), "HERE error");
        sendHttpError(statusCode, "type: " + type + " subtype: " + subType + " details: " + details, true);
    }

    public static void logHereError(Throwable error) {
        Crashlytics.log(Log.ERROR, ErrorReporter.class.getSimpleName(), "HERE exception");
        sendOtherError(error, true);
    }

    private static void sendHttpError(int statusCode, String body, boolean here) {
        Crashlytics.log(Log.ERROR, ErrorReporter.class.getSimpleName(), statusCode + ": " + body);

        if (statusCode == HttpURLConnection.HTTP_ACCEPTED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_BAD_GATEWAY) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_BAD_METHOD) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_CONFLICT) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_CREATED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_ENTITY_TOO_LARGE) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_GONE) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_LENGTH_REQUIRED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_MULT_CHOICE) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_NOT_AUTHORITATIVE) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_NOT_IMPLEMENTED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_NO_CONTENT) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_OK) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_PARTIAL) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_PAYMENT_REQUIRED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_PRECON_FAILED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_PROXY_AUTH) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_REQ_TOO_LONG) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_RESET) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else if (statusCode == HttpURLConnection.HTTP_UNAVAILABLE) {
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        } else { //some other error code
            if (here) Crashlytics.logException(new Exception(HERE + statusCode));
            else Crashlytics.logException(new Exception(OVERPASS + statusCode));
        }
    }

    private static void sendOtherError(Throwable error, boolean here) {
        if (error instanceof SocketTimeoutException) {
            if (here) Crashlytics.logException(new Exception(HERE + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
            else Crashlytics.logException(new Exception(OVERPASS + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
        } else if (error instanceof MalformedJsonException) {
            if (here) Crashlytics.logException(new Exception(HERE + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
            else Crashlytics.logException(new Exception(OVERPASS + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
        } else if (error instanceof UnknownHostException) {
            if (here) Crashlytics.logException(new Exception(HERE + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
            else Crashlytics.logException(new Exception(OVERPASS + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
        } else if (error instanceof ProtocolException) {
            if (here) Crashlytics.logException(new Exception(HERE + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
            else Crashlytics.logException(new Exception(OVERPASS + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
        } else if (error instanceof IllegalStateException) {
            if (here) Crashlytics.logException(new Exception(HERE + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
            else Crashlytics.logException(new Exception(OVERPASS + error.toString() + "\n\n" + Arrays.toString(error.getStackTrace()).replaceAll(", ", "\n")));
        } else { //some other exception
            if (here) Crashlytics.logException(new Exception(HERE, error));
            else Crashlytics.logException(new Exception(OVERPASS, error));
        }
    }
}
