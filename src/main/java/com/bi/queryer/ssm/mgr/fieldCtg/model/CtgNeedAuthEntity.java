package com.bi.queryer.ssm.mgr.fieldCtg.model;

import com.bi.queryer.ssm.enums.DataEnv;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class CtgNeedAuthEntity {

    private String ctgId;

    private String dimCode;

    private String itemCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createdTime;

    private String createdBy;

    private String dataEnv = DataEnv.OLD_SSM.getCode();
}
