package com.bi.queryer.ssm.query.ctg.model;

import lombok.Data;

import java.util.Date;

/**
 * 空间共享实体类
 */
@Data
public class TemplateSpaceEntity {
    private String pkid;

    private String spaceCtgId;

    private String ownerType;

    private String ownerId;

    private String ownerDesc;

    private String ownerRole;

    private String createdBy;

    private Date createdTime;

    private String updatedBy;

    private Date updatedTime;
}