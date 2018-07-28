package com.jakehilborn.speedr.utils;

public class UnitUtils {

    public static final long NANO_TENTH_SECOND = 100000000L;
    public static final long NANO_ONE_SECOND = 1000000000L;
    public static final long NANO_ONE_MINUTE = 60000000000L;
    public static final long NANO_ONE_HOUR = 3600000000000L;

    public static Integer msToMph(Double ms) {
        if (ms == null) return null;

        return (int) Math.round(ms * 2.23694);
    }

    public static Integer msToKph(Double ms) {
        if (ms == null) return null;

        return (int) Math.round(ms * 3.6);
    }

    public static Integer roundToFive(Integer kph) { //meters per second to nearest increment of 5 kph
        if (kph == null) return null;

        return 5 * Math.round(kph / 5);
    }

    public static Integer msToMphRoundToFive(Double ms) { //meters per second to nearest increment of 5 mph
        if (ms == null) return null;

        return (int) (5 * Math.round(ms * 2.23694 / 5));
    }

    public static Integer msToKphRoundToFive(Double ms) { //meters per second to nearest increment of 5 kph
        if (ms == null) return null;

        return (int) (5 * Math.round(ms * 3.6 / 5));
    }

    public static Double mphToMs(Integer mph) {
        if (mph == null) return null;

        return mph * 0.44704;
    }

    public static Double kphToMs(Integer kph) {
        if (kph == null) return null;

        return kph * 0.277778;
    }

    public static Double knotsToMs(Integer knots) {
        if (knots == null) return null;

        return knots * 0.514444;
    }

    public static Integer nanosToSeconds(Long nanos) {
        if (nanos == null) return null;

        return nanosToSeconds((double) nanos);
    }

    public static Integer nanosToSeconds(Double nanos) {
        if (nanos == null) return null;

        return (int) (nanos / NANO_ONE_SECOND);
    }

    public static Double roundNanosToNearestSecond(Double nanos) {
        if (nanos == null) return null;

        return (double) (Math.round(nanos / NANO_ONE_SECOND) * NANO_ONE_SECOND);
    }

    public static Integer nanosTo10thsModuloSeconds(Double nanos) {
        if (nanos == null) return null;

        return (int) ((nanos % NANO_ONE_SECOND) / NANO_TENTH_SECOND);
    }

    public static Integer nanosToSecondsModuloMinutes(Double nanos) {
        if (nanos == null) return null;

        return (int) ((nanos % NANO_ONE_MINUTE) / NANO_ONE_SECOND);
    }

    public static Integer nanosToMinutesModuloHours(Double nanos) {
        if (nanos == null) return null;

        return (int) ((nanos % NANO_ONE_HOUR) / NANO_ONE_MINUTE);
    }

    public static Integer nanosToHoursModuloMinutes(Double nanos) {
        if (nanos == null) return null;

        return (int) (nanos / NANO_ONE_HOUR);
    }

    public static Long secondsToNanos(Integer seconds) {
        if (seconds == null) return null;

        return (long) seconds * NANO_ONE_SECOND;
    }

    public static Long secondsToMillis(Integer seconds) {
        if (seconds == null) return null;

        return (long) seconds * 1000;
    }
}
