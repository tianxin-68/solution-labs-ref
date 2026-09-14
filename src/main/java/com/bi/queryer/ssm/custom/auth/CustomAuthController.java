package com.bi.queryer.ssm.custom.auth;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Scope("prototype")
@RequestMapping("ssm/custom/auth")
public class CustomAuthController extends BaseController {

    @Autowired
    private CustomAuthService customAuthService;

    /**
     * 同步城市经理/战区经理权限
     * <p>
     * 重建系统写入的字段权限白名单，并清理对应人员的系统访问黑名单，使城市经理/战区经理权限即时生效。
     * 访问路径：ssm/custom/auth/syncCityRegionManagerAuth
     * </p>
     *
     * @return 同步结果（含处理用户数）
     */
    @RequestMapping("syncCityRegionManagerAuth")
    @ResponseBody
    public SSMResponseMessage syncCityRegionManagerAuth() {
        return customAuthService.syncCityRegionManagerAuth();
    }
}
