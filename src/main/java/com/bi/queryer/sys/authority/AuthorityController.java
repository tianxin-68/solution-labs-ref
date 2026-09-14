package com.bi.queryer.sys.authority;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Scope("prototype")
@RequestMapping("authority")
public class AuthorityController extends BaseController {

    @Autowired
    private AuthorityService service;

    @ResponseBody
    @RequestMapping(value = "authApplyApprove")
    public ResponseMessage authApplyApprove(@RequestBody String taskId) {
        return service.authApplyApprove(taskId);
    }
}
