package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-25  14:38
 * @Description: 数据集和目录的权限配置实体
 */
@Data
public class DatasetCtgDataAuthDetailReq {

    /**
     * 个人：域账号 部门：部门id
     */
    private String ownerId;

    /**
     * 个人：user 部门：dept
     */
    private String ownerType;

}
