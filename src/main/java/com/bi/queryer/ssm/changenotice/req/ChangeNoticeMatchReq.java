package com.bi.queryer.ssm.changenotice.req;

import lombok.Data;

import java.util.List;

/**
 * 变更通知匹配请求入参
 */
@Data
public class ChangeNoticeMatchReq {

    /**
     * 查询配置 JSON 字符串。
     * 格式与引擎 QueryConfigure.config 一致（含 filter/result/setting/analysis/meso）。
     */
    private String queryConfig;

    /**
     * 已下线的字段 ID 集合。
     * 仅 REPLACE_OFFLINE（替换/下线）场景使用；Service 内会转为字段 code 后再参与命中。
     */
    private List<String> offlineFieldIds;

    /**
     * 当前视图 ID。
     * 供 REPLACE_OFFLINE 新对象提醒校验 template_field_replace_log 使用；
     * 在 checkHistoryNotify=1 时也用于剔除本视图已读通知。
     */
    private String viewId;

    /**
     * 是否校验历史已通知过：1=校验（默认），0=不校验。
     * 为 1 时会剔除本视图已读日志，并按用户累计通知次数上限过滤。
     */
    private Integer checkHistoryNotify;
}
