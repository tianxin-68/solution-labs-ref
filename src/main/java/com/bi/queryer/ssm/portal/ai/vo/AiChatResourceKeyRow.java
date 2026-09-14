package com.bi.queryer.ssm.portal.ai.vo;

import lombok.Data;

import java.util.Date;

/** genAI_feature/v3.15.0_start */
/**
 * 按 chat_base 聚合后的资源维度；{@code businessId} 对应 {@code chat_business_id}，约定全局唯一作为主键。
 */
@Data
public class AiChatResourceKeyRow {
    private String businessType;
    private String businessId;
    /** 该资源下会话最近活动时间 */
    private Date maxUpdatedTime;
}
/** genAI_feature/v3.15.0_end */
