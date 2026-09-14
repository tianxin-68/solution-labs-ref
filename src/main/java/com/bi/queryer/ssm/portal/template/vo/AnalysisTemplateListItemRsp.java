package com.bi.queryer.ssm.portal.template.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-20  14:29
 * @Description: 看板列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTemplateListItemRsp {

    /**
     * 看板id
     */
    private String analysisTplId;

    /**
     * 看板名称
     */
    private String analysisTplName;

    /**
     * 状态
     */
    private String publishStatus;

    /**
     * 路径
     */
    private String path;

    /**
     * 看板描述
     */
    private String analysisTplDesc;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 菜单id
     */
    private String menuId;

    /**
     * 门户id
     */
    private String portalId;

}
