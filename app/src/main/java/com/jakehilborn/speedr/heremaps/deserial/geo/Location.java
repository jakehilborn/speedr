package com.jakehilborn.speedr.heremaps.deserial.geo;

public class Location {
    private Address Address;
    private MapReference MapReference;
    private LinkInfo LinkInfo;

    public Address getAddress() {
        return Address;
    }

    public void setAddress(Address address) {
        this.Address = address;
    }

    public MapReference getMapReference() {
        return MapReference;
    }

    public void setMapReference(MapReference mapReference) {
        this.MapReference = mapReference;
    }

    public LinkInfo getLinkInfo() {
        return LinkInfo;
    }

    public void setLinkInfo(LinkInfo linkInfo) {
        this.LinkInfo = linkInfo;
    }
}
