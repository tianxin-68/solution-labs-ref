package com.bi.queryer.ssm.portal.vo.req;

import lombok.Data;

/**
 * @Auther: contributor
 * @Date: 2025/7/14 17:47
 * @Description:
 */
@Data
public class PortalMenuContentReq {
    /**
     * 菜单id
     */
    private String contentId;

    /**
     * 菜单名称
     */
    private String contentName;
}
