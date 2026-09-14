package com.bi.queryer.ssm.api.ticket;

import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@Scope("prototype")
@RequestMapping("/ticket/data-security/api")
@Slf4j
public class TicketDataSecurityController {

    @Autowired
    private TicketDataSecurityService ticketDataSecurityService;

    /**
     * 查询同部门人员拥有目录权限情况
     */
    @RequestMapping(value = "department-permissions", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<UserDeptCtgAuthRsp> getDepartmentPermissions(@RequestBody UserDeptCtgAuthReq req,
                                                                            HttpServletRequest request) {
        UserDeptCtgAuthRsp rsp = ticketDataSecurityService.getUserDeptCtgAuth(req);
        return SSMResponseMessage.success("查询成功", rsp);
    }

    /**
     * 查询目录下敏感指标/维度情况
     */
    @RequestMapping(value = "sensitive-fields", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<CtgSensitiveFieldRsp> getTopNSensitiveMetrics(@RequestBody CtgSensitiveFieldReq req,
                                                                         HttpServletRequest request) {
        CtgSensitiveFieldRsp rsp = ticketDataSecurityService.getTopNCtgSensitiveField(req.getCtgName(), req.getTaskId());
        return SSMResponseMessage.success("查询成功", rsp);
    }

}