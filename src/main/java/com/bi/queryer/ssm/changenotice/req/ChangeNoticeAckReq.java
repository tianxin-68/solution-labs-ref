package com.bi.queryer.ssm.changenotice.req;

import lombok.Data;

import java.util.List;

/**
 * 变更通知已读确认请求。
 * 写入 ssm_change_notice_log 后，同一用户在同一视图下不再重复提醒这些通知。
 */
@Data
public class ChangeNoticeAckReq {

    /** 当前视图 ID */
    private String viewId;
    /** 已确认/已展示的通知 ID 列表 */
    private List<Long> noticeIds;
}
