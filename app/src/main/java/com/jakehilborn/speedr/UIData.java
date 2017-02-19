package com.jakehilborn.speedr;

public class UIData {
    private Integer speed;
    private Integer limit;
    private Double timeDiff; //nanoseconds
    private long firstLimitTime;
    private boolean networkDown;

    //DriveTime usually updates once per second. On start up, resume, shutdown, and first speed limit received
    //we want to update the drive time outside of the regular 1 second cadence for immediate visibility of new data
    private boolean forceDriveTimeUpdate;

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Double getTimeDiff() {
        return timeDiff;
    }

    public void setTimeDiff(Double timeDiff) {
        this.timeDiff = timeDiff;
    }

    public long getFirstLimitTime() {
        return firstLimitTime;
    }

    public void setFirstLimitTime(long firstLimitTime) {
        this.firstLimitTime = firstLimitTime;
    }

    public boolean isNetworkDown() {
        return networkDown;
    }

    public void setNetworkDown(boolean networkDown) {
        this.networkDown = networkDown;
    }

    public boolean isForceDriveTimeUpdate() {
        return forceDriveTimeUpdate;
    }

    public void setForceDriveTimeUpdate(boolean forceDriveTimeUpdate) {
        this.forceDriveTimeUpdate = forceDriveTimeUpdate;
    }
}
