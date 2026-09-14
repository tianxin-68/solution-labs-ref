package com.bi.queryer.ssm.portal.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * 门户技能集目录树节点（来自 AI Skill 服务）。
 */
@Data
public class AiSkillPortalCtgVO {

    private String ctgId;
    private String ctgName;
    private String parentCtgId;
    private Double sortId;
    private Integer skillCount;
    private List<AiSkillPortalCtgVO> children;
}
