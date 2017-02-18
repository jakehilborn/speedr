package com.jakehilborn.speedr.utils;

import android.content.Context;

import com.jakehilborn.speedr.R;

public class FormatTime {

    //Hours, minutes, seconds, tenths of a second separated by symbols.
    //'h', 'm', 's', '.' symbols are localized
    //1h  23m  45.6s
    //1m  23.4s
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
    //1h  23m  45s
    //1m  23s
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

    public static String nanosToShortHand(Context context, Long nanos) {
        if (context == null || nanos == null) return null;

        return nanosToShortHand(context, (double) nanos);
    }

    //Accepts longHand or shortHand input
    public static String stylizedMainActivity(Context context, String time) {
        if (context == null || time == null) return null;

        //Replace 's', '.', 'm', 'h' with identifiers in case any localized symbols share letters with HTML tags
        time = time.replace(context.getString(R.string.second_symbol), "SECOND")
                .replace(context.getString(R.string.decimal_symbol), "DECIMAL")
                .replace(context.getString(R.string.minute_symbol), "MINUTE")
                .replace(context.getString(R.string.hour_symbol), "HOUR");

        time = ("<b><big>" + time)
                .replace("SECOND", "</b></big><small>" + context.getString(R.string.second_symbol) + "</small>")
                .replace("DECIMAL", "<small>" + context.getString(R.string.decimal_symbol) + "</small>")
                .replace("MINUTE", "</b></big><small>" + context.getString(R.string.minute_symbol) + "</small><big><b>")
                .replace("HOUR", "</b></big><small>" + context.getString(R.string.hour_symbol) + "</small><big><b>");

        return time;
    }

    public static String stylizedStats(String time) {
        if (time == null) return null;

        return "<b>" + time + "</b>";
    }
}
