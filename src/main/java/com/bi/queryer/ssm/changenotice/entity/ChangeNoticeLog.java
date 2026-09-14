package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 变更通知已读日志，对应表 ssm_change_notice_log。
 * 同一用户在同一视图下已通知过的 notice_id 不再重复提醒。
 */
@Data
public class ChangeNoticeLog {

    /** 主键 */
    private Long pkid;
    /** 视图 ID */
    private String viewId;
    /** 通知 ID */
    private Long noticeId;
    /** 创建人（域账号） */
    private String createdBy;
    /** 创建时间 */
    private String createdTime;
}
