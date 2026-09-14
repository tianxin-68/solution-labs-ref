package com.bi.queryer.ssm.portal.template.vo;

import lombok.Getter;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 15:29
 * @Description:
 */

@Getter
public class AnalysisTemplateEditableVO {
    private final Integer editable;

    private final String reason;

    public AnalysisTemplateEditableVO(Integer editable, String reason) {
        this.editable = editable;
        this.reason = reason;
    }
}
