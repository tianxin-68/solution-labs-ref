package com.bi.queryer.ssm.meta;

/**
 * User: contributor
 * Date: 2021/11/21
 * Description: 接口日志
 */
public class APIKafkaLog {

    private String startTime;
    private String endTime;

    private String source;
    private String eventType;
    private String userName;

    private String firstlySector;
    private String secondarySector;
    private String thirdSector;

    private String filterInfo;
    private String rowCount;

    private String sensitiveField;
    private String dataType;
    private String sensitiveLevel;

    private String status;
    private String tags;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFirstlySector() {
        return firstlySector;
    }

    public void setFirstlySector(String firstlySector) {
        this.firstlySector = firstlySector;
    }

    public String getSecondarySector() {
        return secondarySector;
    }

    public void setSecondarySector(String secondarySector) {
        this.secondarySector = secondarySector;
    }

    public String getThirdSector() {
        return thirdSector;
    }

    public void setThirdSector(String thirdSector) {
        this.thirdSector = thirdSector;
    }

    public String getFilterInfo() {
        return filterInfo;
    }

    public void setFilterInfo(String filterInfo) {
        this.filterInfo = filterInfo;
    }

    public String getRowCount() {
        return rowCount;
    }

    public void setRowCount(String rowCount) {
        this.rowCount = rowCount;
    }

    public String getSensitiveField() {
        return sensitiveField;
    }

    public void setSensitiveField(String sensitiveField) {
        this.sensitiveField = sensitiveField;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public APIKafkaLog() {
    }
}
