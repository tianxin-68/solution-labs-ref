package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TemplateEntity {
    private String tplId;

    private String tplName;

    private String tplDesc;

    private String tplType;

    private String cfgId;

    private String tplOwner;

    private Double tplExpMaxRows;

    private Integer isActive;

    private String createdTime;

    private String updatedTime;

    private String createdBy;

    private String updatedBy;

    private String ctgId;

    /**
     * 分享者
     */
    private String shareBy;

    /**
     * 数据集id
     */
    private String datasetId;

    public TemplateEntity clone() {
        TemplateEntity copy = JSONObject.toJavaObject(BIUtil.toJSONObject(this), TemplateEntity.class);
        return copy;
    }
}