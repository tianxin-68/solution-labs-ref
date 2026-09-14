package com.bi.queryer.ssm.query.template.change.log;

import com.bi.queryer.ssm.query.template.change.log.model.TemplateChangeLogRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@Scope("prototype")
@RequestMapping("ssd/template/change/log")
public class TemplateChangeLogController {

    @Autowired
    private TemplateChangeLogService templateChangeLogService;


    /**
     * 根据模板ID获取模板变更记录
     * @param tplId 模版id
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateChangeLogRsp>> list(String tplId){
        return SSMResponseMessage.success("",templateChangeLogService.list(tplId));
    }

    @RequestMapping(value = "initTemplateLastChangeTimeToRedis", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage initTemplateLastChangeTimeToRedis() {
        templateChangeLogService.initTemplateLastChangeTimeToRedis();
        return SSMResponseMessage.success("");
    }

}
