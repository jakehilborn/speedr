package com.jakehilborn.speedr.heremaps.deserial.pde;

public class Rows {
    private String LINK_ID;
    private String FROM_REF_SPEED_LIMIT;
    private String TO_REF_SPEED_LIMIT;

    public String getLINK_ID() {
        return LINK_ID;
    }

    public void setLINK_ID(String LINK_ID) {
        this.LINK_ID = LINK_ID;
    }

    public String getFROM_REF_SPEED_LIMIT() {
        return FROM_REF_SPEED_LIMIT;
    }

    public void setFROM_REF_SPEED_LIMIT(String FROM_REF_SPEED_LIMIT) {
        this.FROM_REF_SPEED_LIMIT = FROM_REF_SPEED_LIMIT;
    }

    public String getTO_REF_SPEED_LIMIT() {
        return TO_REF_SPEED_LIMIT;
    }

    public void setTO_REF_SPEED_LIMIT(String TO_REF_SPEED_LIMIT) {
        this.TO_REF_SPEED_LIMIT = TO_REF_SPEED_LIMIT;
    }
}
