package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-26  14:12
 * @Description:
 */
@Data
public class DatasetCtgDataAuthDetailRsq {

    /**
     * 个人：域账号 部门：部门id
     */
    private String ownerId;

    /**
     * 个人：user 部门：dept
     */
    private String ownerType;


}
