package com.jakehilborn.speedr.utils;

public class UnitUtils {

    public static Integer msToMph(Double ms) {
        if (ms == null) return null;

        return (int) Math.round(ms * 2.23694);
    }

    public static Integer msToKph(Double ms) {
        if (ms == null) return null;

        return (int) Math.round(ms * 3.6);
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

    public static Double nanosToSeconds(Double nanos) {
        if (nanos == null) return null;

        return nanos / 1000000000;
    }

    public static Integer nanosTo10thsModuloSeconds(Double nanos) {
        if (nanos == null) return null;

        return (int) ((nanos % 1000000000L) / 100000000L); //mod by seconds, divide by tenth of a second
    }

    public static Integer nanosToSecondsModuloMinutes(Double nanos) {
        if (nanos == null) return null;

        return (int) ((nanos % 60000000000L) / 1000000000L); //mod by minutes, divide by seconds
    }

    public static Integer nanosToMinutesModuloHours(Double nanos) {
        if (nanos == null) return null;

        return (int) ((nanos % 3600000000000L) / 60000000000L); //mod by hours, divide by minutes
    }

    public static Integer nanosToHoursModuloMinutes(Double nanos) {
        if (nanos == null) return null;

        return (int) (nanos / 3600000000000L); //divide by hours
    }
}
