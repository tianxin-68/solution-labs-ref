package com.bi.queryer.ssm.portal;

import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.ssm.portal.vo.req.PortalMenuAccessLogAddReq;
import com.bi.queryer.ssm.portal.vo.req.PortalMenuUpdateReq;
import com.bi.queryer.ssm.portal.vo.rsp.PortalMenuRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-22  15:17
 * @Description: 门户菜单
 */
@RestController
@Scope("prototype")
@RequestMapping("/ssm/portal/menu")
public class PortalMenuController {
    @Autowired
   private PortalMenuService portalMenuService;
    @Autowired
    private PortalService portalService;

    /**
     * 新增目录
     * @return
     */
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> add(@RequestBody PortalMenu portalMenu) {
        try {
            portalMenu.setMenuType(PortalMenuType.PORTAL_CTG.getCode());
            String menuId = portalMenuService.addMenu(portalMenu);
            return SSMResponseMessage.success("创建成功", menuId);
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 更新菜单
     * @return
     */
    @RequestMapping(value = "update", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> update(@RequestBody PortalMenuUpdateReq portalMenuUpdateReq){
        return portalMenuService.update(portalMenuUpdateReq);
    }

    /**
     * 获取菜单
     * @param menuId
     * @return
     */
    @RequestMapping(value = "get", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<PortalMenuRsp> getMenuById(String menuId){
        return SSMResponseMessage.success("",portalMenuService.get(menuId));
    }

    /**
     * 删除菜单
     * @return
     */
    @RequestMapping(value = "delete", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> delete(String menuId){
        return portalMenuService.delete(menuId);
    }

    /**
     * 菜单移动，传递被移动的菜单id和排在其后面第一位的菜单id
     * @param menuId
     * @param targetMenuId
     * @param dragType : before  after 放在目标节点的位置
     * @return
     */
    @RequestMapping(value = "move", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage move(String menuId,String targetMenuId,String dragType) {
        if (portalService.move(menuId, targetMenuId, dragType)) {
            return SSMResponseMessage.success("菜单移动成功");
        }

        return portalMenuService.move(menuId, targetMenuId, dragType);
    }

    /**
     * 新增菜单访问日志
     * @param req
     * @return
     */
    @RequestMapping(value = "accessLog/add", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> addAccessLog(@RequestBody PortalMenuAccessLogAddReq req) {
        if (portalMenuService.addAccessLog(req)) {
            return SSMResponseMessage.success("新增成功");
        }
        return SSMResponseMessage.operationFailed("新增失败");
    }

}
