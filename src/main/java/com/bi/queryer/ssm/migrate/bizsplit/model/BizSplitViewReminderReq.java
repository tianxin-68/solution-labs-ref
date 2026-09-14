package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 业务线拆分视图失效提醒查询请求。
 */
@Data
public class BizSplitViewReminderReq {

    /** 待查询的（老）视图 id */
    private String viewId;
}
