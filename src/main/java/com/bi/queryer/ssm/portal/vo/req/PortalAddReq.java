package com.bi.queryer.ssm.portal.vo.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  17:47
 * @Description: 门户新增实体
 */
@Data
public class PortalAddReq {

    /**
     * 门户名称
     */
    private String portalName;

    /**
     * 门户描述
     */
    private String portalDesc;

    /**
     * 排序
     */
    private Double sortId;
    /**
     * 门户管理员名单
     */
    public List<String> adminUserNameList = new ArrayList<>();
    /**
     * 门户协作者名单
     */
    public List<String> workerUserNameList = new ArrayList<>();
    /**
     * 关联的查询模板共享空间list
     */
    public List<PortalMenuContentReq> queryTplSpaceCtgList = new ArrayList<>();

    /**
     * 配置看板的查询模板共享空间list
     */
    public List<PortalMenuContentReq> configUsedSpaceCtgList = new ArrayList<>();

}
