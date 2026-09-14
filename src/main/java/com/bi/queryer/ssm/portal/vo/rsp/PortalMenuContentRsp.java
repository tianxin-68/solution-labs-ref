package com.bi.queryer.ssm.portal.vo.rsp;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Builder;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-20  11:35
 * @Description: 门户菜单的元信息
 */
@Data
@Builder
public class PortalMenuContentRsp {

    /**
     * 发布状态
     */
    private String publishStatus;

    /**
     * 是否有草稿
     */
    private Integer hasDraft = Enabled.NO.getId();

    /** 技能集目录下技能数量（ai_skill_ctg 使用） */
    private Integer skillCount;

}
