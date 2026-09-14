package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-06  11:37
 * @Description: 人员转岗
 */
@Data
public class EmployeeTransferReq {

    private String userName;

    private String deptNameCur;

    private String deptNameHis;

}
