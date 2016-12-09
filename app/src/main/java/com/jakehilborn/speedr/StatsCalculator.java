package com.jakehilborn.speedr;

import android.location.Location;

public class StatsCalculator {
    //Time values in nanoseconds
    //Speed values in meters / second

    private Location location;
    private Location prevLocation;

    private Double limit;
    private long prevLimitTime;
    private Location prevLimitLocation; //The location when the most recent speed limit was fetched

    private double timeDiff;

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

    public boolean isLimitStale() {
        //1st Limit hasn't been fetched yet, avoiding NPE below
        if (prevLimitLocation == null || prevLimitTime == 0) return true;

        //Stale if previous Limit request was over 5 seconds ago and the user has traveled over 40 meters since the previous Limit request
        return (prevLimitTime + 5000000000L < System.nanoTime() && location.distanceTo(prevLimitLocation) > 40);
    }

    public void setLimit(Double limit) {
        prevLimitTime = System.nanoTime();
        prevLimitLocation = location;
        this.limit = limit;
    }

    public double getTimeDiff() {
        return timeDiff;
    }

    public void setTimeDiff(double timeDiff) {
        this.timeDiff = timeDiff;
    }

    public void calcTimeDiff() {
        if (prevLimitTime == 0 || //Limit data not available yet
            prevLocation == null || //Less than 2 speed data points have been captured
            limit == 0) return; //Avoiding divide by zero

        double currentDiff = ((location.getElapsedRealtimeNanos() - prevLocation.getElapsedRealtimeNanos())
                * (location.getSpeed() - limit)) / limit;

        if (currentDiff > 0) timeDiff += currentDiff;
    }
}
