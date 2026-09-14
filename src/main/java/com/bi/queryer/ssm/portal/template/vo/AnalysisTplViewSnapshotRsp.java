package com.bi.queryer.ssm.portal.template.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视图快照接口返回：新临时看板与首屏视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTplViewSnapshotRsp {
    private String analysisTplId;
    private String viewId;
}
