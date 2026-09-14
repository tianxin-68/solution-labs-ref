package com.bi.queryer.ssm.portal.vo.rsp;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  10:28
 * @Description: 门户菜单返回实体
 */
@Data
@Builder
public class PortalMenuRsp {

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
     * 菜单路径
     */
    private String menuUrl;

    /**
     * 菜单描述
     */
    private String menuDesc;

    /**
     * 菜单类型
     */
    private String menuType;

    /**
     * 菜单路径
     */
    private String menuPath;


    /**
     * 引用的内容相关属性
     */
    private PortalMenuContentRsp content ;

    /**
     * 内容引用id
     */
    private String contentRefId;

    /**
     * 父级菜单id
     */
    private String parentMenuId;

    private List<PortalMenuRsp> children = new ArrayList<>();

    /**
     * 是否有权限
     */
    private Integer hasAuth = Enabled.NO.getId();

    /**
     * 是否有编辑权限
     */
    private Integer hasEditAuth = Enabled.NO.getId();
    // 是否被收藏
    private Integer isFav = Enabled.NO.getId();
    //收藏的目录
    private String favCtgId;
}
