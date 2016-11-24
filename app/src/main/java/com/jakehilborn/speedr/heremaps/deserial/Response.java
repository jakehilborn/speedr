package com.jakehilborn.speedr.heremaps.deserial;

public class Response {
    private Link[] link;
    private String type;
    private String subtype;
    private String details;

    public Link[] getLink() {
        return link;
    }

    public void setLink(Link[] link) {
        this.link = link;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
