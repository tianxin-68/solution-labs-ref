package com.bi.queryer.ssm.enums;

/**
 * 数据切片粒度枚举
 */
public enum DataSliceGranularity {

    UNKNOWN("", "",0.0,0),
    ONE_MINUTE("1min", "1分钟",1.0,1),
    FIVE_MINUTE("5min", "5分钟",2.0,5),
    TEN_MINUTE("10min", "10分钟",3.0,10),
    ONE_HOUR("1h", "1小时",4.0,60);

    private String code;
    private String name;
    private Double sortId;
    private Integer granularity;

    DataSliceGranularity(String code, String name,Double sortId,Integer granularity) {
        this.code = code;
        this.name = name;
        this.sortId = sortId;
        this.granularity = granularity;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    public Integer getGranularity() {
        return granularity;
    }

    public void setGranularity(Integer granularity) {
        this.granularity = granularity;
    }

    public static DataSliceGranularity get(String code) {
        for (DataSliceGranularity s : DataSliceGranularity.values()) {
            if (s.getCode().equalsIgnoreCase(code)) {
                return s;
            }
        }
        return UNKNOWN;
    }

}
