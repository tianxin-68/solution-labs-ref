package com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper;


import lombok.Data;

@Data
public class DimBasicInfo {

    /**
     * 维度编码
     */
    private String dimCode;

    /**
     * 维度名称
     */
    private String dimName;

    /**
     * 维度释义
     */
    private String dimDesc;

    /**
     * 维度类型
     */
    private String dimDomainName;

    /**
     * 实体路径名称 -- 数据域-实体
     */
    private String dimEntityPathName;

    /**
     * 实体名称
     */
    private String dimEntityName;

    /**
     * 维度值配置对象
     */
    private DimItemConfig dimItemConfig;


}
