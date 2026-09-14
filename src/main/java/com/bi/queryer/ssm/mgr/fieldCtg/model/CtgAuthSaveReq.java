package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

import java.util.List;

@Data
public class CtgAuthSaveReq {

    /**
     * 目录ID
     */
    private String ctgId;

    /**
     * 保存的code列表
     */
    private List<CtgNeedAuthEntity> authList;
}
