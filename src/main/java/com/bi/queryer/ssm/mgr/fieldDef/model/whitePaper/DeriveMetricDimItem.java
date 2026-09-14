package com.bi.queryer.ssm.mgr.fieldDef.model.whitePaper;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeriveMetricDimItem {


    /**
     * 维度id
     */
    private String dimId;

    /**
     * 维度编码
     */
    private String dimCode;

    /**
     * 维度名称
     */
    private String dimName;


    /**
     * 维度项名称 - 列表
     */
    private List<String> dimItemNameList = new ArrayList<>();

}
