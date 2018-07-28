package com.jakehilborn.speedr.heremaps;

public class PDEArgs {
    private Double lat;
    private Double lon;
    private Integer functionalClass;

    public PDEArgs(Double lat, Double lon, Integer functionalClass) {
        this.lat = lat;
        this.lon = lon;
        this.functionalClass = functionalClass;
    }

    public String getLayer() {
        return "SPEED_LIMITS_FC" + functionalClass;
    }

    public Integer getLevel() {
        return functionalClass + 8;
    }

    public Integer getTileX() {
        return (int) ((lon + 180) / this.getTileSize());
    }

    public Integer getTileY() {
        return (int) ((lat + 90) / this.getTileSize());
    }

    private Double getTileSize() {
        return 180 / (Math.pow(2, this.getLevel()));
    }
}
