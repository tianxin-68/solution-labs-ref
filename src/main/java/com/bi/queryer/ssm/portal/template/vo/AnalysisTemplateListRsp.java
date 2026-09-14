package com.bi.queryer.ssm.portal.template.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-21  15:47
 * 看板列表返回实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisTemplateListRsp {

    private long total;

    private List<AnalysisTemplateListItemRsp> list = new ArrayList<>();
}
