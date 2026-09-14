package com.bi.queryer.ssm.portal.vo.req;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-20  16:15
 * @Description:
 */
@Data
public class PortalMenuUpdateReq {

    /**
     * 门户id
     */
    private String portalId;

    /**
     * 菜单id
     */
    private String menuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单描述
     */
    private String menuDesc;

    /**
     * 菜单类型
     */
    private String menuType;

    /**
     * 父级菜单id
     */
    private String parentMenuId;

    /**
     * 内容引用id
     */
    private String contentRefId;

}
