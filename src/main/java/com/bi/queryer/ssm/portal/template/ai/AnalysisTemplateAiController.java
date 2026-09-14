package com.bi.queryer.ssm.portal.template.ai;

import com.bi.queryer.ssm.portal.template.vo.*;
import com.bi.queryer.sys.common.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * @Auther: contributor
 * @Date: 2026/3/9 11:16
 * @Description:
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/analysisTpl/ai")
public class AnalysisTemplateAiController {
    private final static Logger LOG = LoggerFactory.getLogger(AnalysisTemplateAiController.class);

    @Autowired
    private AnalysisTemplateAiService analysisTemplateAiService;

    /**
     * 预览ai解读
     * @param previewReq
     * @return
     */
    @RequestMapping(value = "preview", method = RequestMethod.POST)
    public ResponseMessage preview(@RequestBody AnalysisTemplateAiExecReq previewReq) {
        try {
            return ResponseMessage.success(analysisTemplateAiService.previewAiSummary(previewReq));
        } catch (Exception e) {
            LOG.error("preview dashboard ai error, param: {}", previewReq, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 运行ai解读
     * @param aiCfgVO
     * @return
     */
    @RequestMapping(value = "exec", method = RequestMethod.POST)
    public ResponseMessage exec(@RequestBody AnalysisTemplateAiExecReq aiCfgVO) {
        try {
            return ResponseMessage.success(analysisTemplateAiService.execAiSummary(aiCfgVO));
        } catch (Exception e) {
            LOG.error("exec dashboard ai error, param: {}", aiCfgVO, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 保存ai脚本
     * @param scriptSaveReq
     * @return
     */
    @RequestMapping(value = "script/save", method = RequestMethod.POST)
    public ResponseMessage saveScript(@RequestBody AnalysisTemplateAiScriptSaveReq scriptSaveReq) {
        try {
            return ResponseMessage.success(analysisTemplateAiService.saveScript(scriptSaveReq));
        } catch (Exception e) {
            LOG.error("save dashboard ai script error, param: {}", scriptSaveReq, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 获取ai脚本
     * @param scriptSaveReq
     * @return
     */
    @RequestMapping(value = "script/get", method = RequestMethod.POST)
    public ResponseMessage getAiScript(@RequestBody AnalysisTemplateAiScriptQueryReq queryReq) {
        try {
            return ResponseMessage.success(analysisTemplateAiService.getAiScript(queryReq));
        } catch (Exception e) {
            LOG.error("get dashboard ai script error, param: {}", queryReq, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * ai解读编辑撤销
     * @param scriptSaveReq
     * @return
     */
    @RequestMapping(value = "undoScript", method = RequestMethod.POST)
    public ResponseMessage undoScript(@RequestBody AnalysisTemplateAiUndoReq req) {
        try {
            analysisTemplateAiService.undoUpdateScript(req);
            return ResponseMessage.success(true);
        } catch (Exception e) {
            LOG.error("undo update dashboard ai script error, param: {}", req, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }
}
