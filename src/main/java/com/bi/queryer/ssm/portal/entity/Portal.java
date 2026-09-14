package com.bi.queryer.ssm.portal.entity;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  17:59
 * @Description: 门户实体
 */
@Data
public class Portal {

    /**
     * 门户id
     */
    private String portalId;

    /**
     * 门户名称
     */
    private String portalName;

    /**
     * 门户描述
     */
    private String portalDesc;

    /**
     * 是否可用
     */
    private Integer isActive;

    /**
     * 排序
     */
    private Double sortId;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

}
