package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.Date;

@Data
public class TemplateShareEntity {

    private String pkid;

    private String tplId;

    private String receiverType;

    private String receiverId;

    private String shareUserName;

    private String sharedUserName;

    private String sharedUserRealname;

    private Date shareTime;

    private Integer isActive;

    private String receiverDesc;
}