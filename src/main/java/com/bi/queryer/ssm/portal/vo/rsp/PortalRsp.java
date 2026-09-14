package com.bi.queryer.ssm.portal.vo.rsp;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  10:19
 */
@Data
public class PortalRsp {

    /**
     * 门户id
     */
    private String portalId;

    /**
     * 门户名称
     */
    private String portalName;

    /**
     * 门户描述
     */
    private String portalDesc;

    /**
     * 是否有权限 1:有权限，0：无权限
     */
    private Integer hasAuth = Enabled.YES.getId();

    /**
     * 是否是默认门户 1:是，0：否
     */
    private Integer isDefaultPortal = Enabled.NO.getId();

    /**
     * 当前用户是否是该门户管理员 1:是，0：否
     */
    private Integer isAdmin = Enabled.NO.getId();

}
