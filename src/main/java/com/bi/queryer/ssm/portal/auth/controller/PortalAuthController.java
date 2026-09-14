package com.bi.queryer.ssm.portal.auth.controller;

import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.auth.vo.req.*;
import com.bi.queryer.ssm.portal.auth.vo.rsp.PortalAuthUserItem;
import com.bi.queryer.ssm.portal.auth.vo.rsp.PortalResourceAuthGetRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  17:44
 * @Description: 门户权限web接口定义
 */
@RestController
@Scope("prototype")
@RequestMapping("/ssm/portal/auth")
public class PortalAuthController {


    @Autowired
    private PortalAuthService portalAuthService;

    /**
     * 新增授权
     *
     * @return
     */
    @RequestMapping(value = "save", method = RequestMethod.POST)
    @ResponseBody
    private SSMResponseMessage save(@RequestBody PortalAuthAddReq portalAuthAddReq) {
        return portalAuthService.save(portalAuthAddReq);
    }

    /**
     * 获取授权（门户 / 文件夹 / 看板 通用）。
     * portal 传 roleType；文件夹/看板无需传 roleType，额外返回继承配置字段。
     */
    @RequestMapping(value = "get", method = RequestMethod.POST)
    @ResponseBody
    private SSMResponseMessage<PortalResourceAuthGetRsp> get(@RequestBody PortalAuthQueryReq portalAuthQueryReq) {
        return SSMResponseMessage.success("获取成功", portalAuthService.get(portalAuthQueryReq));
    }

    /**
     * 权限校验 1 有权限 0 无权限
     *
     * @return
     */
    @RequestMapping(value = "check", method = RequestMethod.POST)
    @ResponseBody
    private SSMResponseMessage<Boolean> check(@RequestBody PortalAuthCheckReq portalAuthCheckReq) {
        return SSMResponseMessage.success("", portalAuthService.check(portalAuthCheckReq.getResId(), portalAuthCheckReq.getResType(), portalAuthCheckReq.getFuncCode()));
    }

    /**
     * 查询有权限的用户清单
     */
    @RequestMapping(value = "getUserAuthList", method = RequestMethod.POST)
    @ResponseBody
    private SSMResponseMessage<List<PortalAuthUserItem>> getUserAuthList(@RequestBody PortalAuthUserListReq req) {
        List<PortalAuthUserItem> list = portalAuthService.getUserAuthList(req);
        return SSMResponseMessage.success("获取成功", list);
    }

    /**
     * 导出有权限的用户清单（Excel）
     */
    @RequestMapping(value = "exportUserAuthList", method = RequestMethod.POST)
    public void exportUserAuthList(@RequestBody PortalAuthUserListReq req, HttpServletResponse response) {
        portalAuthService.exportUserAuthList(req, response);
    }

    /**
     * 获取父级资源的授权数据，由前端决定是否复制到当前资源再调 /save 保存。
     * 传 parentResId + parentResType 时直接查父级；否则从继承配置表读取父级。
     */
    @RequestMapping(value = "getParentAuth", method = RequestMethod.POST)
    @ResponseBody
    private SSMResponseMessage<PortalResourceAuthGetRsp> getParentAuth(@RequestBody PortalAuthBaseReq req) {
        return SSMResponseMessage.success("获取成功", portalAuthService.getParentAuth(req));
    }

}
