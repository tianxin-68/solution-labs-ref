package com.bi.queryer.ssm.portal.auth.vo.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  19:36
 * @Description: 门户权限新增请求实体
 */
@Data
public class PortalAuthAddReq extends PortalAuthBaseReq{

    /** 权限分配的用户 */
    public List<String> userNameList = new ArrayList<>();

    /** 权限分配的组织 */
    public List<String> deptIdList = new ArrayList<>();

    // ---------- 文件夹 / 看板 继承配置（portal 类型忽略）----------

    /** 是否开启权限继承 1=开启 0=关闭，null 表示不修改 */
    private Integer inheritEnabled;

}
