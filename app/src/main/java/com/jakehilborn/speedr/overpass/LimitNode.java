package com.jakehilborn.speedr.overpass;

public class LimitNode {
    private Double limit;
    private String roadName;

    public LimitNode() {
        setLimit(null);
        setRoadName(null);
    }

    public LimitNode(Double limit, String roadName) {
        setLimit(limit);
        setRoadName(roadName);
    }

    public Double getLimit() {
        return limit;
    }

    //Handle nulls for easier comparison in OverpassManager
    public void setLimit(Double limit) {
        if (limit == null) {
            this.limit = 0D;
        } else {
            this.limit = limit;
        }
    }

    public String getRoadName() {
        return roadName;
    }

    //Handle nulls for easier comparison in OverpassManager
    public void setRoadName(String roadName) {
        if (roadName == null) {
            this.roadName = "";
        } else {
            this.roadName = roadName.toLowerCase().trim();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LimitNode limitNode = (LimitNode) o;

        if (limit != null ? !limit.equals(limitNode.limit) : limitNode.limit != null) return false;
        return roadName != null ? roadName.equals(limitNode.roadName) : limitNode.roadName == null;

    }

    @Override
    public int hashCode() {
        int result = limit != null ? limit.hashCode() : 0;
        result = 31 * result + (roadName != null ? roadName.hashCode() : 0);
        return result;
    }
}
