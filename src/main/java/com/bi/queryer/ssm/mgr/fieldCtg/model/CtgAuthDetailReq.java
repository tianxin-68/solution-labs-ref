package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

import java.util.List;

@Data
public class CtgAuthDetailReq {

    /**
     * 目录ID
     */
    private String ctgId;

    /**
     * 权限配置项dimCodeList
     */
    private List<String> dimCodeList;
}
