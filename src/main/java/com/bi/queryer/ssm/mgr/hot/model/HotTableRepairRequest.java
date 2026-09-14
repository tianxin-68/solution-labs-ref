package com.bi.queryer.ssm.mgr.hot.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 13:50 2024/10/11
 * @Description 热表补数请求类
 **/
public class HotTableRepairRequest {
    private String sourceTableName;

    private String status;

    private List<String> taskIdList = new ArrayList<>();

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTaskIdList() {
        return taskIdList;
    }

    public void setTaskIdList(List<String> taskIdList) {
        this.taskIdList = taskIdList;
    }
}
