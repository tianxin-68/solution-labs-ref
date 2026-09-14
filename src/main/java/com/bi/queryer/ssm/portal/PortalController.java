package com.bi.queryer.ssm.portal;

import com.bi.queryer.ssm.portal.enums.FuncType;
import com.bi.queryer.ssm.portal.vo.req.PortalAddReq;
import com.bi.queryer.ssm.portal.vo.req.PortalSetDefaultReq;
import com.bi.queryer.ssm.portal.vo.req.PortalUpdateReq;
import com.bi.queryer.ssm.portal.vo.rsp.PortalDetailRsp;
import com.bi.queryer.ssm.portal.vo.rsp.PortalMenuRsp;
import com.bi.queryer.ssm.portal.vo.rsp.PortalRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-17  17:44
 * @Description: 门户相关web接口定义
 */
@RestController
@Scope("prototype")
@RequestMapping("/ssm/portal")
@Slf4j
public class PortalController {

    @Autowired
    private PortalService portalService;

    /**
     * 新增门户
     *
     * @return
     */
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage add(@RequestBody PortalAddReq portalAddReq) {
        return portalService.add(portalAddReq);
    }

    /**
     * 管理门户
     *
     * @return
     */
    @RequestMapping(value = "update", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage update(@RequestBody PortalUpdateReq updateReq) {
        return portalService.update(updateReq);
    }

    /**
     * 获取门户详情
     *
     * @return
     */
    @RequestMapping(value = "getDetail", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<PortalDetailRsp> getDetail(@RequestParam String portalId) {
        return portalService.getDetail(portalId);
    }

    /**
     * 查询门户列表
     *
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<PortalRsp>> list(){
        return SSMResponseMessage.success("查询成功",portalService.list(FuncType.VIEW.getCode(),true));
    }

    /**
     * 查询有权限编辑的门户
     *
     * @return
     */
    @RequestMapping(value = "listUserEditablePortals", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<PortalRsp>> listUserEditablePortals() {
        List<PortalRsp> portalRspList = portalService.list(FuncType.EDIT.getCode(), true);
        return SSMResponseMessage.success("查询成功", portalRspList);
    }

    /**
     * 查询门户下的菜单树
     *
     * @return
     */
    @RequestMapping(value = "getMenuTree", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<PortalMenuRsp>> getMenuTree(String portalId){
        return portalService.getMenuTree(portalId);
    }

    /**
     * 查询工作台下的门户菜单树
     *
     * @return
     */
    @RequestMapping(value = "studio/tree", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<PortalMenuRsp>> getStudioMenuTree(){
        return portalService.getStudioMenuTree();
    }

    /**
     * 获取用户模块权限
     * @return
     */
    @RequestMapping(value = "getUserPortalModuleAuth", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<String>> getUserPortalModuleAuth(){
        return portalService.getUserPortalModuleAuth();
    }

    /**
     * 设置默认的门户
     * @return
     */
    @RequestMapping(value = "default/set", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<Boolean> setDefaultPortal(@RequestBody PortalSetDefaultReq req) {
        try {
            return SSMResponseMessage.success("设置成功", portalService.setDefaultPortal(req));
        } catch (Exception e) {
            log.error("set default portal error, req: {}", req, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

}
