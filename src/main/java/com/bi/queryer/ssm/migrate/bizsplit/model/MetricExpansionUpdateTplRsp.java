package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 已生成门户模板原地重跑结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricExpansionUpdateTplRsp {

    /** 逐视图更新结果 */
    @Builder.Default
    private List<ViewResult> viewResults = new ArrayList<>();

    /**
     * 单视图原地更新结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewResult {

        /** 目标模板 id */
        private String tplId;

        /** 目标视图 id（不变） */
        private String viewId;

        /** 正式 cfg id（不变） */
        private String cfgId;

        /** 是否已成功写库更新 */
        private Boolean updated;

        /** 是否跳过（预留，当前失败走 errorMessage） */
        private Boolean skipped;

        /** 失败原因；非空表示该视图未更新 */
        private String errorMessage;
    }
}
