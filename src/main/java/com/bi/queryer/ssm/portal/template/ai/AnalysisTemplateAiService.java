package com.bi.queryer.ssm.portal.template.ai;

import com.bi.queryer.ssm.portal.enums.AiMode;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplAiExecLogEntity;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplAiScriptEntity;
import com.bi.queryer.ssm.portal.template.vo.*;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.user.UserManager;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2026/3/9 15:11
 * @Description:
 */
@Service
@Scope("prototype")
@Slf4j
public class AnalysisTemplateAiService {
    @Autowired
    private BaseDao dao;

    @Autowired
    private AnalysisTemplateAiScriptService aiScriptService;

    @Autowired
    private AnalysisTemplateAiFreeInterpreterService aiFreeInterpreterService;

    // ai解读预览
    public AnalysisTemplateAiPreviewRsp previewAiSummary(AnalysisTemplateAiPreviewReq previewReq) {
        if (CollectionUtils.isEmpty(previewReq.getAnalysisTplAiCfgItems())) {
            return null;
        }

        AiMode aiMode = AiMode.get(previewReq.getAnalysisMode());
        switch (aiMode) {
            case PYTHON_SCRIPT:
                // 模板解读
                return aiScriptService.previewPythonScriptSummary(previewReq);
            case AI:
                return aiFreeInterpreterService.execAiSummary(previewReq);
        }

        return null;
    }

    public List<AnalysisTemplateAiPreviewRsp> execAiSummary(AnalysisTemplateAiExecReq aiCfgVO) {
        if (CollectionUtils.isEmpty(aiCfgVO.getAnalysisTplAiCfgItems())) {
            return Collections.emptyList();
        }

        AnalysisTplAiExecLogEntity logEntity = new AnalysisTplAiExecLogEntity();
        logEntity.setAnalysisTplId(aiCfgVO.getAnalysisTplId());
        logEntity.setAnalysisMode(aiCfgVO.getAnalysisMode());
        logEntity.setAnalysisInput(JSONObject.toJSONString(aiCfgVO));
        logEntity.setEnv(SSDUtil.getEnv().getCode());
        logEntity.setCreatedBy(UserManager.get().getName());

        List<AnalysisTemplateAiPreviewRsp> rspList = Collections.emptyList();
        long startTime = System.currentTimeMillis();
        try {
            AiMode aiMode = AiMode.get(aiCfgVO.getAnalysisMode());
            switch (aiMode) {
                case PYTHON_SCRIPT:
                    // 模板解读
                    rspList = aiScriptService.execPythonScriptSummary(aiCfgVO);
                    break;
                case AI:
                    rspList = Collections.singletonList(aiFreeInterpreterService.execAiSummary(aiCfgVO));
                    break;
            }
            logEntity.setExecResult(ResponseMessage.MSG_SUCCESS);
            logEntity.setExecDuration(System.currentTimeMillis() - startTime);
            // 记录日志
            logExecution(logEntity);
        } catch (Exception e) {
            log.error("exec dashboard ai error, param: {}", aiCfgVO, e);
            logEntity.setExecResult(ResponseMessage.MSG_FAIL);
            logEntity.setErrorMsg(e.getMessage());
            logExecution(logEntity);
            throw e;
        }
        return rspList;
    }

    public String saveScript(AnalysisTemplateAiScriptSaveReq scriptSaveReq) {
        return aiScriptService.saveTempScript(scriptSaveReq);
    }

    public List<AnalysisTplAiScriptEntity> getAiScript(AnalysisTemplateAiScriptQueryReq queryReq) {
        return aiScriptService.getAiScript(queryReq);
    }

    public void undoUpdateScript(AnalysisTemplateAiUndoReq req) {
        aiScriptService.undoUpdateScript(req);
    }

    /**
     * 记录执行日志
     */
    public void logExecution(AnalysisTplAiExecLogEntity log) {
        dao.insert("ssm.analysis.template.ai.exec.log.addAiExecLog", log);
    }
}
