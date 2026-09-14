package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

import java.util.List;

/**
 * 业务线行级权限初始化请求。
 */
@Data
public class BizSplitDataAuthInitReq {

    /**
     * 指定组织 id 列表，对应 {@link com.bi.queryer.ssm.migrate.bizsplit.enums.BizSplitDeptBusinessLineAuth} 中的 deptId。
     * 为空或未传时处理全部预置映射。
     */
    private List<String> deptIds;

    /**
     * 指定人员域账号列表（对应 HR user_name）。
     * 为空或未传时不限定用户，走当前默认全量逻辑。
     */
    private List<String> userNames;
}
