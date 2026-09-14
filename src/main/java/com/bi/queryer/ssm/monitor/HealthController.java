package com.bi.queryer.ssm.monitor;

import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

@RestController
@Scope("prototype")
@RequestMapping("/health")
public class HealthController {


    /**
     * 健康检查
     *
     * @return
     */
    @RequestMapping(value = "check", method = RequestMethod.GET)
    @FreeCheckAuthority
    @ResponseBody
    public SSMResponseMessage check() {
        return new SSMResponseMessage();
    }

}
