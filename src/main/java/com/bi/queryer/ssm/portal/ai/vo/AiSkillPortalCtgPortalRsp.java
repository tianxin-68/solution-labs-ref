package com.bi.queryer.ssm.portal.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiSkillPortalCtgPortalRsp {

    private String portalId;
    private List<AiSkillPortalCtgVO> ctgs;
}
