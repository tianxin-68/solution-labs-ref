package com.bi.queryer.ssm.migrate.bizsplit.controller;

import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitCtgDataAuthInitReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitDataAuthInitReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitPortalRoleAuthInitReq;
import com.bi.queryer.ssm.migrate.bizsplit.service.BizSplitCtgDataAuthInitService;
import com.bi.queryer.ssm.migrate.bizsplit.service.BizSplitDataAuthInitService;
import com.bi.queryer.ssm.migrate.bizsplit.service.BizSplitPortalRoleAuthInitService;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务线拆分权限初始化 HTTP 入口。
 *
 * 请求路径前缀：/ssm/bizsplit/data/auth
 */
@RestController
@Scope("prototype")
@Slf4j
@RequestMapping("ssm/bizsplit/data/auth")
public class BizSplitDataAuthInitController {

    @Autowired
    private BizSplitDataAuthInitService bizSplitDataAuthInitService;

    @Autowired
    private BizSplitCtgDataAuthInitService bizSplitCtgDataAuthInitService;

    @Autowired
    private BizSplitPortalRoleAuthInitService bizSplitPortalRoleAuthInitService;

    /**
     * 初始化业务线行级权限 HTTP 入口。
     *
     * 写入 bi_portal.sys_data_auth，固定 owner_type=User、module_code=ssm_row、dim_code=ssm_dim_bizline。
     * 映射组织用户：无权限复制默认业务线，有权限补充缺失项；非映射组织用户按源业务线扩展。
     * 已存在同 item_code 时由 REPLACE INTO 覆盖。详见 BizSplitDataAuthInitService.initBusinessLineRowAuth。
     *
     * @param req 可选 deptIds 限定组织范围、userNames 限定人员域账号；body 可空表示全量
     */
    @RequestMapping(value = "init/businessline", method = RequestMethod.POST)
    public SSMResponseMessage<Void> initBusinessLineRowAuth(
            @RequestBody(required = false) BizSplitDataAuthInitReq req) {
        try {
            bizSplitDataAuthInitService.initBusinessLineRowAuth(req);
            return SSMResponseMessage.success("");
        } catch (Exception e) {
            log.error("bizsplit init businessline row auth error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 方法2：多维目录权限初始化 HTTP 入口。
     *
     * 从旧目录 id 复制 Portal User 权限与数据集侧 user/dept 权限至新目录 id；
     * excludeDeptIds 命中的用户/组织不写入。
     * excludeDeptIds 须传完整部门 id 列表，子部门也需逐一传入，不会根据父部门自动展开。
     * 详见 BizSplitCtgDataAuthInitService.initCtgDataAuth。
     *
     * @param req 新目录 id、旧目录 id、排除部门 id 集合
     */
    @RequestMapping(value = "init/ctg", method = RequestMethod.POST)
    public SSMResponseMessage<Void> initCtgDataAuth(@RequestBody BizSplitCtgDataAuthInitReq req) {
        try {
            bizSplitCtgDataAuthInitService.initCtgDataAuth(req);
            return SSMResponseMessage.success("");
        } catch (Exception e) {
            log.error("bizsplit init ctg data auth error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 方法3：多维门户角色权限初始化 HTTP 入口。
     *
     * 从旧角色 id 复制 ssm_portal_role_user / ssm_portal_role_dept 至新角色 id；
     * excludeDeptIds 命中的用户/组织不写入。
     * excludeDeptIds 须传完整部门 id 列表，子部门也需逐一传入，不会根据父部门自动展开。
     * 详见 BizSplitPortalRoleAuthInitService.initPortalRoleAuth。
     *
     * @param req 新角色 id、旧角色 id、排除部门 id 集合
     */
    @RequestMapping(value = "init/portalrole", method = RequestMethod.POST)
    public SSMResponseMessage<Void> initPortalRoleAuth(@RequestBody BizSplitPortalRoleAuthInitReq req) {
        try {
            bizSplitPortalRoleAuthInitService.initPortalRoleAuth(req);
            return SSMResponseMessage.success("");
        } catch (Exception e) {
            log.error("bizsplit init portal role auth error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }
}
