package com.bi.queryer.ssm.query.template.model;

import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 模版配置config实体类
 */
@Data
public class TemplateCfgEntity {

    /**
     * 关联模版的cfgId，一对多
     */
    private String cfgId;

    /**
     * 模版配置的json数据
     */
    private String tplConfig;

    public TemplateCfgEntity() {
    }

    public TemplateCfgEntity(String cfgId, String tplConfig) {
        this.cfgId = cfgId;
        this.tplConfig = tplConfig;
    }

    public TemplateCfgEntity clone() {
        TemplateCfgEntity copy = JSONObject.toJavaObject(BIUtil.toJSONObject(this), TemplateCfgEntity.class);
        return copy;
    }
}
