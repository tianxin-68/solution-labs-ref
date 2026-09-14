package com.bi.queryer.ssm.portal.vo.rsp;

import com.bi.queryer.ssm.portal.vo.req.PortalAddReq;
import lombok.Data;

/**
 * @Auther: contributor
 * @Date: 2025/7/17 10:01
 * @Description:
 */
@Data
public class PortalDetailRsp extends PortalAddReq {
    private String portalId;
}
