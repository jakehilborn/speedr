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
    private static final String TIME_OF_HERE_CREDS = "time_of_here_creds";
    private static final String PENDING_HERE_ACTIVATION = "pending_here_activation";
    private static final String SESSION_TIME_DIFF = "session_time_diff";
    private static final String SESSION_TIME_TOTAL = "session_time_total";
    private static final String TIME_DIFF_WEEK = "time_diff_week";
    private static final String TIME_DIFF_MONTH = "time_diff_month";
    private static final String TIME_DIFF_YEAR = "time_diff_year";
    private static final String TIME_DIFF_WEEK_NUM = "time_diff_week_num";
    private static final String TIME_DIFF_MONTH_NUM = "time_diff_month_num";
    private static final String TIME_DIFF_YEAR_NUM = "time_diff_year_num";
    private static final String TIME_TOTAL_WEEK = "time_total_week";
    private static final String TIME_TOTAL_MONTH = "time_total_month";
    private static final String TIME_TOTAL_YEAR = "time_total_year";

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

    public static void setTimeOfHereCreds(Context context, long time) {
        editPrefs(context).putLong(TIME_OF_HERE_CREDS, time).apply();
    }

    public static long getTimeOfHereCreds(Context context) {
        return prefs(context).getLong(TIME_OF_HERE_CREDS, 0);
    }

    public static void setPendingHereActivation(Context context, boolean pendingHereActivation) {
        editPrefs(context).putBoolean(PENDING_HERE_ACTIVATION, pendingHereActivation).apply();
    }

    public static boolean isPendingHereActivation(Context context) {
        return prefs(context).getBoolean(PENDING_HERE_ACTIVATION, false);
    }

    public static void setSessionTimeDiff(Context context, Double timeDiff) {
        //SharedPrefs has no putDouble method. Longs use the same number of bytes so this is a lossless conversion.
        editPrefs(context).putLong(SESSION_TIME_DIFF, Double.doubleToRawLongBits(timeDiff)).apply();
    }

    public static Double getSessionTimeDiff(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(SESSION_TIME_DIFF, 0));
    }

    public static void setSessionTimeTotal(Context context, long time) {
        editPrefs(context).putLong(SESSION_TIME_TOTAL, time).apply();
    }

    public static long getSessionTimeTotal(Context context) {
        return prefs(context).getLong(SESSION_TIME_TOTAL, 0);
    }

    public static void setTimeDiffWeek(Context context, Double timeDiff) {
        editPrefs(context).putLong(TIME_DIFF_WEEK, Double.doubleToRawLongBits(timeDiff)).apply();
    }

    public static Double getTimeDiffWeek(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(TIME_DIFF_WEEK, 0));
    }

    public static void setTimeDiffMonth(Context context, Double timeDiff) {
        editPrefs(context).putLong(TIME_DIFF_MONTH, Double.doubleToRawLongBits(timeDiff)).apply();
    }

    public static Double getTimeDiffMonth(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(TIME_DIFF_MONTH, 0));
    }

    public static void setTimeDiffYear(Context context, Double timeDiff) {
        editPrefs(context).putLong(TIME_DIFF_YEAR, Double.doubleToRawLongBits(timeDiff)).apply();
    }

    public static Double getTimeDiffYear(Context context) {
        return Double.longBitsToDouble(prefs(context).getLong(TIME_DIFF_YEAR, 0));
    }

    public static void setTimeDiffWeekNum(Context context, int week) {
        editPrefs(context).putInt(TIME_DIFF_WEEK_NUM, week).apply();
    }

    public static int getTimeDiffWeekNum(Context context) {
        return prefs(context).getInt(TIME_DIFF_WEEK_NUM, 0);
    }

    public static void setTimeDiffMonthNum(Context context, int month) {
        editPrefs(context).putInt(TIME_DIFF_MONTH_NUM, month).apply();
    }

    public static int getTimeDiffMonthNum(Context context) {
        return prefs(context).getInt(TIME_DIFF_MONTH_NUM, 0);
    }

    public static void setTimeDiffYearNum(Context context, int year) {
        editPrefs(context).putInt(TIME_DIFF_YEAR_NUM, year).apply();
    }

    public static int getTimeDiffYearNum(Context context) {
        return prefs(context).getInt(TIME_DIFF_YEAR_NUM, 0);
    }

    public static void setTimeTotalWeek(Context context, long time) {
        editPrefs(context).putLong(TIME_TOTAL_WEEK, time).apply();
    }

    public static long getTimeTotalWeek(Context context) {
        return prefs(context).getLong(TIME_TOTAL_WEEK, 0);
    }

    public static void setTimeTotalMonth(Context context, long time) {
        editPrefs(context).putLong(TIME_TOTAL_MONTH, time).apply();
    }

    public static long getTimeTotalMonth(Context context) {
        return prefs(context).getLong(TIME_TOTAL_MONTH, 0);
    }

    public static void setTimeTotalYear(Context context, long time) {
        editPrefs(context).putLong(TIME_TOTAL_YEAR, time).apply();
    }

    public static long getTimeTotalYear(Context context) {
        return prefs(context).getLong(TIME_TOTAL_YEAR, 0);
    }
}
