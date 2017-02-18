package com.jakehilborn.speedr.utils;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;

import com.jakehilborn.speedr.R;

public class FormatTime {

    //Clock format with no second tenths, no leading zeros, 's' if only seconds
    //1:05:59 - 1 hour, 5 minutes, 59 seconds
    //3:04 - 3 minutes, 4 seconds
    //20s - 20 seconds
    public static String nanosToClock(Double nanos) {
        if (nanos == null) return null;

        int hours = UnitUtils.nanosToHoursModuloMinutes(nanos);
        int minutes = UnitUtils.nanosToMinutesModuloHours(nanos);
        int seconds = UnitUtils.nanosToSecondsModuloMinutes(nanos);

        String formattedTime = "";
        if (hours != 0) {
            formattedTime += hours + ":";
            if (minutes < 10) formattedTime += "0";
            formattedTime += minutes + ":";
            if (seconds < 10) formattedTime += "0";
            formattedTime += seconds;
        } else if (minutes != 0) {
            formattedTime += minutes + ":";
            if (seconds < 10) formattedTime += "0";
            formattedTime += seconds;
        } else {
            formattedTime += seconds + "s";
        }

        return formattedTime;
    }

    public static String nanosToClock(long nanos) {
        return nanosToClock((double) nanos);
    }

    //Hours, minutes, seconds, tenths of a second separated by symbols.
    //'h', 'm', 's', '.' symbols are localized
    //1h 23m 45.6s
    //1m 23.4s
    //0.0s
    public static String nanosToLongHand(Context context, Double nanos) {
        if (context == null || nanos == null) return null;

        StringBuilder timeDiffString = new StringBuilder(
                UnitUtils.nanosToSecondsModuloMinutes(nanos) + context.getString(R.string.decimal_symbol) +
                        UnitUtils.nanosTo10thsModuloSeconds(nanos) + context.getString(R.string.second_symbol)
        ); //always show seconds

        if (nanos >= UnitUtils.NANO_ONE_MINUTE) {
            timeDiffString.insert(0, UnitUtils.nanosToMinutesModuloHours(nanos) + context.getString(R.string.minute_symbol) + "  ");
        }
        if (nanos >= UnitUtils.NANO_ONE_HOUR) {
            timeDiffString.insert(0, UnitUtils.nanosToHoursModuloMinutes(nanos) + context.getString(R.string.hour_symbol) + "  ");
        }

        return timeDiffString.toString();
    }

    //Hours, minutes, seconds, tenths of a second separated by symbols.
    //'h', 'm', 's' symbols are localized
    //1h 23m 45s
    //1m 23s
    //0s
    public static String nanosToShortHand(Context context, Double nanos) {
        if (context == null || nanos == null) return null;

        nanos = UnitUtils.roundNanosToNearestSecond(nanos);

        StringBuilder timeDiffString = new StringBuilder(
                UnitUtils.nanosToSecondsModuloMinutes(nanos) + context.getString(R.string.second_symbol)
        ); //always show seconds

        if (nanos >= UnitUtils.NANO_ONE_MINUTE) {
            timeDiffString.insert(0, UnitUtils.nanosToMinutesModuloHours(nanos) + context.getString(R.string.minute_symbol) + "  ");
        }
        if (nanos >= UnitUtils.NANO_ONE_HOUR) {
            timeDiffString.insert(0, UnitUtils.nanosToHoursModuloMinutes(nanos) + context.getString(R.string.hour_symbol) + "  ");
        }

        return timeDiffString.toString();
    }

    //Accepts longHand or shortHand input
    public static Spanned stylizedTimeSaved(Context context, String time) {
        if (context == null || time == null) return null;

        time = insertPlaceholders(context, time);
        time = ("<b><big>" + time + "</big></b>")
                .replace("SECOND", "</b></big><small>s</small>")
                .replace("DECIMAL", "</big><small>.</small><big>")
                .replace("MINUTE", "</b></big><small>m</small><big><b>")
                .replace("HOUR", "</b></big><small>h</small><big><b>");

        return Html.fromHtml(time);
    }

    //Accepts longHand or shortHand input
    public static Spanned stylizeDriveTime(Context context, String time) {
        if (time == null) return null;

        time = insertPlaceholders(context, time);
        time = ("<b><big>" + time + "</big></b>")
                .replace("SECOND", "</b></big><small>s</small>")
                .replace("DECIMAL", "</big><small>.</small><big>")
                .replace("MINUTE", "</b></big><small>m</small><big><b>")
                .replace("HOUR", "</b></big><small>h</small><big><b>");

        return Html.fromHtml(time);
    }

    private static String insertPlaceholders(Context context, String time) {
        //Replace 's', '.', 'm', 'h' with identifiers in case any localized symbols share letters with HTML tags
        return time.replace(context.getString(R.string.second_symbol), "SECOND")
                .replace(context.getString(R.string.decimal_symbol), "DECIMAL")
                .replace(context.getString(R.string.minute_symbol), "MINUTE")
                .replace(context.getString(R.string.hour_symbol), "HOUR");
    }
}
