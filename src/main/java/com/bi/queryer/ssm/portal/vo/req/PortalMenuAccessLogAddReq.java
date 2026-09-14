package com.bi.queryer.ssm.portal.vo.req;

import lombok.Data;

/**
 * @Description: 门户菜单访问日志-新增请求
 */
@Data
public class PortalMenuAccessLogAddReq {

    /**
     * 菜单id
     */
    private String menuId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 门户id
     */
    private String portalId;

}