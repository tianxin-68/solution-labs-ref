package com.bi.queryer.ssm.portal.template.entity;

import lombok.Data;

/**
 * @Auther: contributor
 * @Date: 2025/9/11 14:43
 * @Description:
 */
@Data
public class UserProfileEntity {
    private Long pkId;
    private String userName;
    private String entityType;
    private String entityId;
    private String entityName;
    private String createdBy;
    private String createdTime;
    private String updatedBy;
    private String updatedTime;
}
