package com.bi.queryer.ssm.portal.template;

import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVisitLogReq;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  16:26
 * @Description:
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/analysisTpl/log")
public class AnalysisTemplateLogController {

    /**
     * 新增看板访问日志
     * @return
     */
    @RequestMapping(value = "addVisitLog", method = RequestMethod.POST)
    public SSMResponseMessage addVisitLog(@RequestBody AnalysisTemplateVisitLogReq analysisTemplateVisitLogReq){
        AnalysisTemplateLogService.addVisitLog(analysisTemplateVisitLogReq);
        return SSMResponseMessage.success("新增成功！");
    }

}
