package com.jakehilborn.speedr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Prefs {

    private static final String USE_KPH = "use_kph";
    private static final String HERE_SUGGESTION_ACKNOWLEDGED = "here_suggestion_acknowledged";
    private static final String HERE_APP_ID = "here_app_id";
    private static final String HERE_APP_CODE = "here_app_code";
    private static final String USE_HERE_MAPS = "use_here_maps";
    private static final String HERE_MAPS_TERMS_ACCEPTED = "here_maps_terms_accepted";
    private static final String SESSION_TIME_DIFF = "session_time_diff";

    private static SharedPreferences.Editor editPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit();
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setUseKph(Context context, boolean useKph) {
        editPrefs(context).putBoolean(USE_KPH, useKph).apply();
    }

    public static boolean isUseKph(Context context) {
        return prefs(context).getBoolean(USE_KPH, false);
    }

    public static void setHereSuggestionAcknowledged(Context context, boolean hereSuggestionAcknowledged) {
        editPrefs(context).putBoolean(HERE_SUGGESTION_ACKNOWLEDGED, hereSuggestionAcknowledged).apply();
    }

    public static boolean isHereSuggestionAcknowledged(Context context) {
        return prefs(context).getBoolean(HERE_SUGGESTION_ACKNOWLEDGED, false);
    }

    public static void setHereAppId(Context context, String appId) {
        editPrefs(context).putString(HERE_APP_ID, appId).apply();
    }

    public static String getHereAppId(Context context) {
        return prefs(context).getString(HERE_APP_ID, null);
    }

    public static void setHereAppCode(Context context, String appCode) {
        editPrefs(context).putString(HERE_APP_CODE, appCode).apply();
    }

    public static String getHereAppCode(Context context) {
        return prefs(context).getString(HERE_APP_CODE, null);
    }

    public static void setUseHereMaps(Context context, boolean useHereMaps) {
        editPrefs(context).putBoolean(USE_HERE_MAPS, useHereMaps).apply();
    }

    public static boolean isUseHereMaps(Context context) {
        return prefs(context).getBoolean(USE_HERE_MAPS, false);
    }

    public static void setHereMapsTermsAccepted(Context context, boolean accepted) {
        editPrefs(context).putBoolean(HERE_MAPS_TERMS_ACCEPTED, accepted).apply();
    }

    public static boolean isHereMapsTermsAccepted(Context context) {
        return prefs(context).getBoolean(HERE_MAPS_TERMS_ACCEPTED, false);
    }

    public static void setSessionTimeDiff(Context context, Double timeDiff) {
        //SharedPrefs has no putDouble method. Longs use the same number of bytes so this is a lossless conversion.
        editPrefs(context).putLong(SESSION_TIME_DIFF, Double.doubleToRawLongBits(timeDiff)).apply();
    }

    public static Double getSessionTimeDiff(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(SESSION_TIME_DIFF, 0));
    }
}
