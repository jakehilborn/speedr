package com.jakehilborn.speedr.heremaps.deserial.pde;

public class GeoNode {
    private Long refId;
    private String roadName;
    private Integer functionalClass;

    public GeoNode (Long refId, String roadName, Integer functionalClass) {
        this.refId = refId;
        this.roadName = safeRoadName(roadName);
        this.functionalClass = functionalClass;
    }

    private String safeRoadName(String roadName) {
        if (roadName == null) {
            return "";
        } else {
            return roadName.toLowerCase().trim();
        }
    }

    public Long getRefId() {
        return refId;
    }

    public String getRoadName() {
        return roadName;
    }

    public Integer getFunctionalClass() {
        return functionalClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoNode geoNode = (GeoNode) o;

        if (!refId.equals(geoNode.refId)) return false;
        if (!roadName.equals(geoNode.roadName)) return false;
        return functionalClass.equals(geoNode.functionalClass);

    }

    @Override
    public int hashCode() {
        int result = refId.hashCode();
        result = 31 * result + roadName.hashCode();
        result = 31 * result + functionalClass.hashCode();
        return result;
    }
}
