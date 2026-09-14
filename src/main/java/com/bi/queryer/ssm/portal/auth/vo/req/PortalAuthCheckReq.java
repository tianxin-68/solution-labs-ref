package com.bi.queryer.ssm.portal.auth.vo.req;

import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-18  14:27
 * @Description: 门户权限校验入参
 */
@Data
public class PortalAuthCheckReq {

    /**
     * 资源id
     */
    private String resId;
    /**
     * 资源类型
     */
    private String resType;
    /**
     * 功能编码
     */
    private String funcCode;
}
