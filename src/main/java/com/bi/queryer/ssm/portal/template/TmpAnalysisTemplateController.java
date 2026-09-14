package com.bi.queryer.ssm.portal.template;

import com.bi.queryer.ssm.portal.enums.AnalysisTplType;
import com.bi.queryer.ssm.portal.template.ai.AgentQueryService;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.portal.template.vo.GenerateTmpAnalysisTplNameReq;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2026/3/30 10:28
 * @Description: 临时看板（个人多模板看板）
 */
@RestController
@Scope("prototype")
@Slf4j
@RequestMapping("ssm/tmp/analysisTpl")
public class TmpAnalysisTemplateController {
    @Autowired
    private AnalysisTemplateService analysisTplService;

    @Autowired
    private AgentQueryService agentQueryService;

    @Autowired
    private TmpAnalysisTemplateService tmpAnalysisTemplateService;

    /**
     * 我的空间下有权限的临时看板。
     */
    @RequestMapping(value = "listByMySpace", method = RequestMethod.GET)
    public SSMResponseMessage<List<AnalysisTemplateVO>> listByMySpace() {
        return SSMResponseMessage.success("查询成功", tmpAnalysisTemplateService.listMySpaceMountedTmpAnalysisTpl());
    }

    /**
     * 创建个人多模板看板
     *
     * @param vo
     * @return
     */
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public ResponseMessage create(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(analysisTplService.createTmpTpl(vo));
        } catch (Exception e) {
            log.error("create temp analysis template error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 保存个人多模板看板
     *
     * @param vo
     * @return
     */
    @RequestMapping(value = "save", method = RequestMethod.POST)
    public ResponseMessage save(@RequestBody AnalysisTemplateVO vo) {
        try {
            vo.setAnalysisTplType(AnalysisTplType.TMP.getCode());
            analysisTplService.update(vo);
            analysisTplService.publish(vo);
            return ResponseMessage.success(true);
        } catch (Exception e) {
            log.error("update analysis template error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 删除个人多模板看板
     *
     * @param vo
     * @return
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST)
    public ResponseMessage delete(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(analysisTplService.deleteTmpTpl(vo));
        } catch (Exception e) {
            log.error("delete temp analysis template error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 根据多组一一对应的模板名称与视图名称，调用第三方生成临时看板名称
     */
    @RequestMapping(value = "generateName", method = RequestMethod.POST)
    public ResponseMessage generateName(@RequestBody GenerateTmpAnalysisTplNameReq req) {
        try {
            return ResponseMessage.success(agentQueryService.generateTmpAnalysisTplName(req));
        } catch (Exception e) {
            log.error("generate tmp analysis template name error, param: {}", req, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }
    /** genAI_feature/v3.15.0_end */
}
