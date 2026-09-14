package com.bi.queryer.ssm.portal.vo.req;

import lombok.Data;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2025/9/12 11:38
 * @Description:
 */

@Data
public class PortalPublicDomainReq {
    // 菜单id
    private List<String> ctgIds;

    // 分析模板id
    private List<String> queryTplIds;

    //看板菜单id
    private List<String> dashboardMenuIds;
}
