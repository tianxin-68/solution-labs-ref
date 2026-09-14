package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

import java.util.List;

/**
 * 已生成门户模板原地重跑膨胀/筛选替换的请求。
 *
 * 固定 UPDATE 语义：tplId / viewId / cfgId 不变，直接更新正式 cfg 与 cfg_dtl，不写影子表。
 */
@Data
public class MetricExpansionUpdateTplReq {

    /** 已生成的目标模板 id 列表（须曾通过 replace/template COPY 生成） */
    private List<String> tplIds;

    /** 源业务线 */
    private String sourceBusinessline;

    /** 目标业务线 */
    private String targetBusinessline;

    /** 可选：仅更新指定视图；为空则更新 tplIds 下全部视图 */
    private List<String> viewIds;
}
