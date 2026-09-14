package com.bi.queryer.ssm.portal.template.vo;

import com.bi.queryer.util.BIUtil;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/21 15:35
 * @Description:
 */
public class AnalysisTemplateAuthVO {
    private final Integer hasAllDataAuth;

    private final List<String> noAuthCtgIds;

    public AnalysisTemplateAuthVO(List<String> noAuthCtgIds) {
        this.noAuthCtgIds = noAuthCtgIds;
        this.hasAllDataAuth = BIUtil.isEmpty(noAuthCtgIds) ? 1 : 0;
    }

    public Integer getHasAllDataAuth() {
        return hasAllDataAuth;
    }

    public List<String> getNoAuthCtgIds() {
        return noAuthCtgIds;
    }
}
