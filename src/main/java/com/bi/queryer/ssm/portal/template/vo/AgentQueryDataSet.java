package com.bi.queryer.ssm.portal.template.vo;

import java.util.List;

public class AgentQueryDataSet {

    /**
     * 唯一标识
     */
    private String name;

    private String description;

    /**
     * 文件链接
     */
    private String url;

    /**
     * 数据时间范围信息
     */
    private DataTimeInfo dataTime;

    private List<ColumnsInfo> columnsInfo;

    public List<ColumnsInfo> getColumnsInfo() {
        return columnsInfo;
    }

    public void setColumnsInfo(List<ColumnsInfo> columnsInfo) {
        this.columnsInfo = columnsInfo;
    }

    public DataTimeInfo getDataTime() {
        return dataTime;
    }

    public void setDataTime(DataTimeInfo dataTime) {
        this.dataTime = dataTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 数据时间范围内部类
     */
    public static class DataTimeInfo {
        /**
         * 数据粒度：日、周、月等
         */
        private String granularity;

        /**
         * 开始日期
         */
        private String start;

        /**
         * 结束日期
         */
        private String end;

        public String getGranularity() {
            return granularity;
        }

        public void setGranularity(String granularity) {
            this.granularity = granularity;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }
}
