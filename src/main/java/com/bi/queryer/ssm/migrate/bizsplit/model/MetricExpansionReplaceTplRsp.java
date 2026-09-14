package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 按业务线替换模板下全部视图的响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricExpansionReplaceTplRsp {

    /** 新模板 id */
    private String newTplId;

    /** 该模板下所有视图的旧→新 viewId 映射 */
    @Builder.Default
    private List<ViewIdMapping> viewIdMappings = new ArrayList<>();

    /**
     * 视图 id 映射。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ViewIdMapping {

        private String oldViewId;

        private String newViewId;
    }
}
