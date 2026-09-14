package com.bi.queryer.ssm.portal.entity;

import com.bi.queryer.sys.enums.Enabled;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  10:47
 * @Description: 门户菜单
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortalMenu {

    /**
     * 菜单id
     */
    private String menuId;

    /**
     * 门户id
     */
    private String portalId;

    /**
     * 门户名称
     */
    private String portalName;

    /**
     * 菜单路径
     */
    private String menuUrl;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单类型
     */
    private String menuType;

    /**
     * 父级菜单名称
     */
    private String parentMenuId;

    /**
     * 菜单描述
     */
    private String menuDesc;

    /**
     * 内容引用id
     */
    private String contentRefId;

    /**
     * 排序字段
     */
    private Double sortId;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 子菜单
     */
    private List<PortalMenu> children = new ArrayList<>();

    private Integer isActive = Enabled.YES.getId();

}
