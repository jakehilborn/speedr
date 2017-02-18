package com.jakehilborn.speedr;

import android.location.Location;

import com.jakehilborn.speedr.utils.UnitUtils;

public class StatsCalculator {
    //Time values in nanoseconds
    //Speed values in meters / second

    private Location location;
    private Location prevLocation;

    private Double limit;
    private long firstLimitTime;
    private long prevLimitTime;
    private Location prevLimitLocation; //The location when the most recent speed limit was fetched
    private boolean forceLimitStale;

    private boolean networkDown;
    private long prevNetworkCheckTime;

    private double timeDiff;

    public interface Callback {
        void handleNetworkUpdate();
    }

    //Callback for StatsCalculator to push speed limit updates to MainService
    public Callback callback;
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setLocation(Location location) {
        prevLocation = this.location;
        this.location = location;
    }

    public Double getSpeed() {
        if (location == null) return null;

        return (double) this.location.getSpeed();
    }

    public Double getLimit() {
        return this.limit;
    }

    public long getFirstLimitTime() {
        return firstLimitTime;
    }

    public boolean isLimitStale() {
        //1st Limit hasn't been fetched yet, avoiding NPE below
        if (prevLimitLocation == null || prevLimitTime == 0) return true;

        if (forceLimitStale) {
            forceLimitStale = false;
            return true;
        }

        if (networkDown) {
            return true;
        }

        //Stale if previous Limit request was over 5 seconds ago and the user has traveled over 40 meters since the previous Limit request
        return (prevLimitTime + UnitUtils.secondsToNanos(5) < System.nanoTime() && location.distanceTo(prevLimitLocation) > 25);
    }

    public void setForceLimitStale(boolean forceLimitStale) {
        this.forceLimitStale = forceLimitStale;
    }

    public boolean isNetworkCheckStale() {
        return prevNetworkCheckTime + UnitUtils.secondsToNanos(10) < System.nanoTime() //Check is network down at most every 10s
                && prevLimitTime + UnitUtils.secondsToNanos(10) < System.nanoTime(); //Only check if a new speed limit hasn't been received in over 10s
    }

    public boolean isNetworkDown() {
        return this.networkDown;
    }

    public void setNetworkDown(boolean networkDown) {
        this.prevNetworkCheckTime = System.nanoTime();
        this.networkDown = networkDown;
    }

    //When limit is set to null it means there is no speed limit data available so continue showing existing speed limit.
    //When limit is set to 0 it means there is no speed limit data available, set speed limit to missing.
    public void setLimit(Double limit, Double lat, Double lon) {
        prevLimitTime = System.nanoTime();
        if (limit != null) {
            this.limit = limit;
            if (firstLimitTime == 0 && limit != 0) firstLimitTime = prevLimitTime;
        }
        prevLimitLocation = new Location("fused");
        prevLimitLocation.setLatitude(lat);
        prevLimitLocation.setLongitude(lon);
        networkDown = false;

        callback.handleNetworkUpdate();
    }

    public double getTimeDiff() {
        return timeDiff;
    }

    public void setTimeDiff(double timeDiff) {
        this.timeDiff = timeDiff;
    }

    public void calcTimeDiff() {
        if (prevLimitTime == 0 //Limit data not available yet
            || prevLocation == null //Less than 2 speed data points have been captured
            || limit == null
            || limit == 0) return; //Avoiding divide by zero

        double currentDiff = ((location.getElapsedRealtimeNanos() - prevLocation.getElapsedRealtimeNanos())
                * (location.getSpeed() - limit)) / limit;

        if (currentDiff > 0) timeDiff += currentDiff;
    }
}
