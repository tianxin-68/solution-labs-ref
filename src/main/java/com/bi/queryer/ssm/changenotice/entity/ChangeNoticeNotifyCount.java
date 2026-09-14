package com.bi.queryer.ssm.changenotice.entity;

import lombok.Data;

/**
 * 用户对某条变更通知的已通知次数统计。
 */
@Data
public class ChangeNoticeNotifyCount {

    /** 通知 ID */
    private Long noticeId;
    /** 已通知次数 */
    private Integer notifyCount;
}
