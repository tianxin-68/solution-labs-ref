package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

/**
 * 目录权限及结束时间
 */
@Data
public class CtgAuthAcl {

    /**
     * 目录ID
     */
    private String ctgId;

    /**
     * 权限结束时间，格式 yyyy-MM-dd
     */
    private String authEndDate;
}
