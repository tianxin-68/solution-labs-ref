package com.bi.queryer.ssm.portal.template.vo;

import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * @Auther: contributor
 * @Date: 2024/6/20 14:37
 * @Description:
 */

@Setter
@Getter
public class AnalysisTplWidgetQueryTplRelVO {
    private String analysisTplId;
    private String analysisTplName;
    private String analysisTplDesc;
    /**
     * 业务门户ID
     */
    private String portalId;
    /**
     * 业务门户名称
     */
    private String portalName;

    public AnalysisTplWidgetQueryTplRelVO(String analysisTplId, String analysisTplName, String analysisTplDesc,
                                          String portalId, String portalName) {
        this.analysisTplId = analysisTplId;
        this.analysisTplName = analysisTplName;
        this.analysisTplDesc = analysisTplDesc;
        this.portalName = portalName;
        this.portalId = portalId;
    }

    public static AnalysisTplWidgetQueryTplRelVO of(AnalysisTemplateEntity entity, PortalMenu portalMenu) {
        if (portalMenu == null) {
            return new AnalysisTplWidgetQueryTplRelVO(entity.getAnalysisTplId(),
                    entity.getAnalysisTplName(), entity.getAnalysisTplDesc(), null, null);
        } else {
            return new AnalysisTplWidgetQueryTplRelVO(entity.getAnalysisTplId(),
                    entity.getAnalysisTplName(), entity.getAnalysisTplDesc(),
                    portalMenu.getPortalId(), portalMenu.getPortalName());
        }
    }
}
