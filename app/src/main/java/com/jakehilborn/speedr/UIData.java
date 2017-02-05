package com.jakehilborn.speedr;

public class UIData {
    private Integer speed;
    private Integer limit;
    private Double timeDiff; //nanoseconds
    private boolean networkDown;

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

    public boolean isNetworkDown() {
        return networkDown;
    }

    public void setNetworkDown(boolean networkDown) {
        this.networkDown = networkDown;
    }
}
