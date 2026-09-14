package com.bi.queryer.ssm.portal.template.vo;

import lombok.Getter;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 15:38
 * @Description:
 */
@Getter
public class AnalysisTemplateDraftVO {
    private final Integer hasDraft;

    private final String draftOwner;

    private final String currentUser;

    public AnalysisTemplateDraftVO(Integer hasDraft, String draftOwner, String currentUser) {
        this.hasDraft = hasDraft;
        this.draftOwner = draftOwner;
        this.currentUser = currentUser;
    }
}
