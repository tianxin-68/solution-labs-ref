package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 变更通知用户维度统计，对应表 ssm_change_notice_user_stat。
 * 按 created_by + notice_id 唯一，记录累计通知次数。
 */
@Data
public class ChangeNoticeUserStat {

    /** 主键 */
    private Long pkid;
    /** 通知 ID */
    private Long noticeId;
    /** 累计通知次数（跨视图） */
    private Integer notifyCount;
    /** 创建人（域账号） */
    private String createdBy;
    /** 创建时间 */
    private String createdTime;
    /** 更新人（域账号） */
    private String updatedBy;
    /** 更新时间 */
    private String updatedTime;
}
