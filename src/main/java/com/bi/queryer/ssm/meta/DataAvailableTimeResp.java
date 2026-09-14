package com.bi.queryer.ssm.meta;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2023-10-10  10:03
 * @Description: 数据可用时间返回
 */
public class DataAvailableTimeResp {

    /**
     * 最晚数据可用时间
     */
    private String lastDateAvailableTime;

    /**
     * 最大的过滤时间
     */
    private String maxFilterDateTime;

    public String getLastDateAvailableTime() {
        return lastDateAvailableTime;
    }

    public void setLastDateAvailableTime(String lastDateAvailableTime) {
        this.lastDateAvailableTime = lastDateAvailableTime;
    }

    public List<JSONObject> getUnAvailableFieldList() {
        return unAvailableFieldList;
    }

    public void setUnAvailableFieldList(List<JSONObject> unAvailableFieldList) {
        this.unAvailableFieldList = unAvailableFieldList;
    }

    /**
     * 不可用字段
     */
    List<JSONObject> unAvailableFieldList = new ArrayList<>();


    public String getMaxFilterDateTime() {
        return maxFilterDateTime;
    }

    public void setMaxFilterDateTime(String maxFilterDateTime) {
        this.maxFilterDateTime = maxFilterDateTime;
    }

}
