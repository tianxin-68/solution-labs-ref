package com.bi.queryer.ssm.portal.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 门户菜单访问日志实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortalMenuAccessLogEntity {

    private Long logId;

    private String userName;

    private String menuId;

    private String menuName;

    private String portalId;

}