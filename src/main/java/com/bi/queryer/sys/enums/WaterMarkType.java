package com.bi.queryer.sys.enums;

public enum WaterMarkType {


    unknown("9999", "unknown"),
    bi_portal_uat("22", "https://biportalut.example.com"),
    bi_portal("23", "https://biportal.example.com");

    private WaterMarkType(String waterMarkId, String url) {
        this.waterMarkId = waterMarkId;
        this.url = url;
    }

    private String waterMarkId;

    private String url;

    public String getWaterMarkId() {
        return waterMarkId;
    }

    public void setWaterMarkId(String waterMarkId) {
        this.waterMarkId = waterMarkId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static WaterMarkType get(String url) {
        for (WaterMarkType t : values()) {
            if (t.getUrl().equalsIgnoreCase(url)) {
                return t;
            }
        }
        return unknown;
    }
}

