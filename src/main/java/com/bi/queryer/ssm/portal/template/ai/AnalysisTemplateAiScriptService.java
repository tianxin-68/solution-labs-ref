package com.bi.queryer.ssm.portal.template.ai;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.portal.enums.AiMode;
import com.bi.queryer.ssm.portal.enums.AiScriptStatus;
import com.bi.queryer.ssm.portal.template.AnalysisTplViewService;
import com.bi.queryer.ssm.portal.template.entity.*;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateExecMode;
import com.bi.queryer.ssm.portal.template.vo.*;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分析模板 AI 脚本服务类
 * @Auther: contributor
 * @Date: 2026/3/6 14:46
 * @Description: Python 代码状态机管理
 */
@Service
@Scope("prototype")
@Slf4j
public class AnalysisTemplateAiScriptService {
    // 默认分析模板配置 ID, ai脚本可能创建在看板前
    private final String MODIFIED_ANALYSIS_TPL_CFG_ID = "modified_draft";

    private final String STATIC_AI_ITEM_CONFIG_TYPE = "static";

    @Autowired
    private BaseDao dao;

    @Autowired
    private AgentQueryService agentQueryService;
    @Autowired
    private AnalysisTplViewService analysisTplViewService;

    public List<AnalysisTemplateAiPreviewRsp> execPythonScriptSummary(AnalysisTemplateAiExecReq aiCfgReq) {
        // 生成脚本
        List<String> scriptIds = aiCfgReq.getAnalysisTplAiCfgItems().stream()
                .map(AnalysisTemplateAiCfgVO.AnalysisTplAiCfgItem::getScriptId).collect(Collectors.toList());
        List<AnalysisTplAiScriptEntity> scriptEntities = getAiScript(aiCfgReq.getAnalysisTplId(), aiCfgReq.getViewId(), aiCfgReq.getExecMode(), scriptIds);
        Map<String, AnalysisTplAiScriptEntity> scriptMap = scriptEntities.stream().collect(Collectors.toMap(AnalysisTplAiScriptEntity::getScriptId, v -> v));

        // 运行脚本
        List<AnalysisTemplateAiPreviewRsp> rspList = new ArrayList<>();
        for (AnalysisTemplateAiCfgVO.AnalysisTplAiCfgItem item : aiCfgReq.getAnalysisTplAiCfgItems()) {
            // 获取脚本
            String result;
            AnalysisTemplateAiPreviewRsp rsp = new AnalysisTemplateAiPreviewRsp();
            if (STATIC_AI_ITEM_CONFIG_TYPE.equals(item.getType())) {
                result = item.getOutputFormat();
            } else {
                AnalysisTplAiScriptEntity script = scriptMap.get(item.getScriptId());
                if (script == null || StringUtils.isBlank(script.getScriptContent())) {
                    continue;
                }
                String scriptContent = script.getScriptContent();
                // 运行脚本
                result = execPythonScript(scriptContent, item.getItemId(), item.getDependDataList());
                rsp.setScript(scriptContent);
                rsp.setScriptId(script.getScriptId());
            }
            // 获取结果
            rsp.setItemId(item.getItemId());
            rsp.setSummary(result);
            rspList.add(rsp);
        }

        return rspList;
    }

    public AnalysisTemplateAiPreviewRsp previewPythonScriptSummary(AnalysisTemplateAiPreviewReq previewReq) {
        String analysisTplId = previewReq.getAnalysisTplId();
        AnalysisTemplateAiCfgVO.AnalysisTplAiCfgItem aiCfgItem = previewReq.getAnalysisTplAiCfgItems().get(0);

        AnalysisTemplateAiPreviewRsp rsp = new AnalysisTemplateAiPreviewRsp();
        if (STATIC_AI_ITEM_CONFIG_TYPE.equals(aiCfgItem.getType())) {
            rsp.setItemId(aiCfgItem.getItemId());
            rsp.setSummary(aiCfgItem.getOutputFormat());
            return rsp;
        }

        if (AiScriptStatus.GENERATED.getCode().equals(aiCfgItem.getScriptStatus()) ||
                AiScriptStatus.MANUAL_EDITED.getCode().equals(aiCfgItem.getScriptStatus())) {
            AnalysisTemplateAiExecReq execReq = new AnalysisTemplateAiExecReq();
            execReq.setAnalysisTplId(analysisTplId);
            execReq.setExecMode(AnalysisTemplateExecMode.LOCAL.getCode());
            execReq.setAnalysisTplAiCfgItems(previewReq.getAnalysisTplAiCfgItems());
            List<AnalysisTemplateAiPreviewRsp> rspList = execPythonScriptSummary(execReq);
            if (CollectionUtils.isNotEmpty(rspList)) {
                rsp = rspList.get(0);
            }
        } else {
            // 封装agent参数
            String modelName = SC.v("ssm.agent.query.ai.script.model", "chatgpt");
            AiScriptGenReq req = new AiScriptGenReq();
            req.setModelName(modelName);
            req.setInterpretType(AiMode.PYTHON_SCRIPT.getCode());
            req.setDataSets(buildDataSets(aiCfgItem.getDependDataList()));
            req.setTemplate(aiCfgItem.getItemId());
            req.setUserIntention(aiCfgItem.getOutputPrompt());
            req.setOutputFormat(aiCfgItem.getOutputFormat());
            req.setDataContext("");
            AgentQueryRsp queryRsp = agentQueryService.genPythonScript(req);
            // 获取返回结果
            String pythonScript = queryRsp.getCode();
            // 保存脚本
            String scriptId = saveScript(aiCfgItem.getScriptId(), MODIFIED_ANALYSIS_TPL_CFG_ID, analysisTplId, pythonScript, AiScriptStatus.GENERATED.getCode());
            rsp.setPrompt(queryRsp.getPrompt());
            rsp.setItemId(aiCfgItem.getItemId());
            rsp.setScript(pythonScript);
            rsp.setScriptId(scriptId);
            rsp.setSummary(queryRsp.getValue());
            rsp.setTrace(queryRsp.getTrace());
        }
        return rsp;
    }

    public String execPythonScript(String pythonScript, String itemId, List<AnalysisTemplateAiCfgVO.DependData> dependDataList) {
        if (StringUtils.isEmpty(pythonScript)) {
            return "";
        }

        // 保持脚本
        AgentQueryReq queryReq = new AgentQueryReq();
        queryReq.setScriptCode(pythonScript);
        queryReq.setDataSets(buildDataSets(dependDataList));
        queryReq.setTemplate(itemId);
        AgentQueryRsp queryRsp = agentQueryService.execPythonScript(queryReq);

        return queryRsp == null ? "" : queryRsp.getValue();
    }

    private List<AgentQueryDataSet> buildDataSets(List<AnalysisTemplateAiCfgVO.DependData> dependDataList) {
        List<AgentQueryDataSet> dataSets = new ArrayList<>(8);
        if (CollectionUtils.isNotEmpty(dependDataList)) {
            for (AnalysisTemplateAiCfgVO.DependData dependData : dependDataList) {
                AgentQueryDataSet dataSet = new AgentQueryDataSet();
                dataSet.setName(dependData.getViewId());
                dataSet.setDescription(String.format("%s,%s", dependData.getTplName(), dependData.getViewName()));
                dataSet.setUrl(dependData.getDataPath());
                dataSet.setDataTime(buildDataTimeInfo(dependData));
                dataSet.setColumnsInfo(dependData.getColumnsInfo());
                dataSets.add(dataSet);
            }
        }
        return dataSets;
    }

    /**
     * 创建 AI 脚本
     */
    public String saveTempScript(AnalysisTemplateAiScriptSaveReq saveReq) {
        return saveScript(saveReq.getScriptId(), MODIFIED_ANALYSIS_TPL_CFG_ID, saveReq.getAnalysisTplId(),
                saveReq.getScript(), saveReq.getScriptStatus());
    }

    public List<AnalysisTplAiScriptEntity> getAiScript(AnalysisTemplateAiScriptQueryReq queryReq) {
        // 生成脚本
        return getAiScript(queryReq.getAnalysisTplId(), queryReq.getViewId(), queryReq.getExecMode(), queryReq.getScriptIds());
    }

    private List<AnalysisTplAiScriptEntity> getAiScript(String analysisTplId, String viewId, String execMode, List<String> scriptIds) {
        List<String> analysisTplCfgIds = null;
        List<AnalysisTplCfgProdEntity> entities = dao.queryObjectList("ssm.analysisTemplate.getOnlineConfigs"
                , Collections.singletonList(analysisTplId), AnalysisTplCfgProdEntity.class);
        if (AnalysisTemplateExecMode.PROD.getCode().equals(execMode)) {
            AnalysisTplViewEntity viewEntity = analysisTplViewService.getEntityById(viewId);
            if (viewEntity != null) {
                analysisTplCfgIds = Collections.singletonList(viewEntity.getAnalysisTplCfgId());
            } else if (CollectionUtils.isNotEmpty(entities)) {
                analysisTplCfgIds = Collections.singletonList(entities.get(0).getAnalysisTplCfgId());
            }
        } else {
            List<AnalysisTplCfgLocalEntity> localEntities = dao.queryObjectList("ssm.analysisTemplate.getLocalConfig"
                    , analysisTplId, AnalysisTplCfgLocalEntity.class);
            if (CollectionUtils.isNotEmpty(localEntities)) {
                analysisTplCfgIds = Arrays.asList(localEntities.get(0).getAnalysisTplCfgId(), MODIFIED_ANALYSIS_TPL_CFG_ID);
            } else if (CollectionUtils.isNotEmpty(entities)) {
                analysisTplCfgIds = Arrays.asList(entities.get(0).getAnalysisTplCfgId(), MODIFIED_ANALYSIS_TPL_CFG_ID);
            }
        }
        Map<String, Object> param = new HashMap<>();
        param.put("analysisTplId", analysisTplId);
        param.put("scriptIds", scriptIds);
        param.put("analysisTplCfgIds", analysisTplCfgIds);
        String sqlId = "ssm.analysis.template.ai.script.listAiScriptByCondition";
        return dao.queryObjectList(sqlId, param, AnalysisTplAiScriptEntity.class);
    }

    public void undoUpdateScript(AnalysisTemplateAiUndoReq req) {
        if (StringUtils.isEmpty(req.getAnalysisTplId()) || !AiMode.PYTHON_SCRIPT.getCode().equals(req.getAnalysisMode())) {
            return;
        }
        Map<String, Object> param1 = new HashMap<>();
        param1.put("analysisTplId", req.getAnalysisTplId());
        param1.put("analysisTplCfgIds", Collections.singletonList(MODIFIED_ANALYSIS_TPL_CFG_ID));
        dao.delete("ssm.analysis.template.ai.script.deleteAiScriptByCondition", param1);
    }

    public String saveScript(String scriptId, String analysisTplCfgId, String analysisTplId, String scriptContent, String scriptStatus) {
        User user = UserManager.get();
        // 每次保存重新生成脚本ID，便于版本管理
        String newScriptId = Guid.id();
        AnalysisTplAiScriptEntity script = new AnalysisTplAiScriptEntity();
        script.setScriptId(newScriptId);
        script.setScriptType("python");
        script.setAnalysisTplCfgId(analysisTplCfgId);
        script.setScriptContent(scriptContent);
        script.setAnalysisTplId(analysisTplId);
        script.setScriptStatus(scriptStatus);
        script.setCreatedBy(user.getName());
        script.setUpdatedBy(user.getName());
        dao.insert("ssm.analysis.template.ai.script.addAiScript", script);

        // 删除无用的修改过的版本
        if (StringUtils.isNotEmpty(scriptId)) {
            Map<String, Object> param1 = new HashMap<>();
            param1.put("scriptId", scriptId);
            param1.put("analysisTplCfgIds", Collections.singletonList(MODIFIED_ANALYSIS_TPL_CFG_ID));
            dao.delete("ssm.analysis.template.ai.script.deleteAiScriptByCondition", param1);
        }

        return newScriptId;
    }

    public boolean copyAnalysisTplAiScript(String analysisTplId, String oldAnalysisTplCfgId, String newAnalysisTplCfgId) {
        if (StringUtils.isBlank(analysisTplId) || StringUtils.isBlank(oldAnalysisTplCfgId) || StringUtils.isBlank(newAnalysisTplCfgId)) {
            return false;
        }
        if (StringUtils.equals(oldAnalysisTplCfgId, newAnalysisTplCfgId)) {
            return true;
        }

        // 清空新配置下已有脚本，再按旧配置复制
        Map<String, Object> param1 = new HashMap<>(4);
        param1.put("analysisTplId", analysisTplId);
        param1.put("analysisTplCfgIds", Collections.singletonList(newAnalysisTplCfgId));
        dao.delete("ssm.analysis.template.ai.script.deleteAiScriptByCondition", param1);

        Map<String, Object> queryParam = new HashMap<>(4);
        queryParam.put("analysisTplId", analysisTplId);
        queryParam.put("analysisTplCfgIds", Collections.singletonList(oldAnalysisTplCfgId));
        List<AnalysisTplAiScriptEntity> oldScripts = dao.queryObjectList(
                "ssm.analysis.template.ai.script.listAiScriptByAnalysisTplCfgId",
                queryParam, AnalysisTplAiScriptEntity.class);
        if (CollectionUtils.isEmpty(oldScripts)) {
            return true;
        }

        User user = UserManager.get();
        List<AnalysisTplAiScriptEntity> addScripts = oldScripts.stream()
                .peek(v -> {
                    v.setAnalysisTplCfgId(newAnalysisTplCfgId);
                    v.setCreatedBy(user.getName());
                    v.setUpdatedBy(user.getName());
                })
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(addScripts)) {
            return true;
        }
        dao.insert("ssm.analysis.template.ai.script.batchInsertAiScript", addScripts);
        return true;
    }

    // 保存时把修改过的版本替换到默认草稿版本
    public boolean saveAnalysisTplAiScript(String analysisTplCfgId, String oldAnalysisTplCfgId, AnalysisTplWidgetEntity aiWidget) {
        if (StringUtils.isBlank(analysisTplCfgId) || aiWidget == null || StringUtils.isBlank(aiWidget.getWidgetId())) {
            return false;
        }

        String widgetOptions = aiWidget.getWidgetOptions();
        AnalysisTemplateAiCfgVO aiCfgVO = JSONObject.parseObject(widgetOptions, AnalysisTemplateAiCfgVO.class);
        if (aiCfgVO == null || !Boolean.TRUE.equals(aiCfgVO.getActive()) || CollectionUtils.isEmpty(aiCfgVO.getAnalysisTplAiCfgItems())) {
            return false;
        }

        User user = UserManager.get();
        List<AnalysisTemplateAiCfgVO.AnalysisTplAiCfgItem> aiCfgItems = aiCfgVO.getAnalysisTplAiCfgItems();
        String widgetId = aiWidget.getWidgetId();
        List<String> scriptIds = new ArrayList<>(16);
        List<AnalysisTplAiQueryTplRelEntity> relList = new ArrayList<>(16);
        for (AnalysisTemplateAiCfgVO.AnalysisTplAiCfgItem item : aiCfgItems) {
            if (StringUtils.isBlank(item.getScriptId())) {
                continue;
            }
            scriptIds.add(item.getScriptId());
            for (AnalysisTemplateAiCfgVO.DependData dependData : item.getDependDataList()) {
                AnalysisTplAiQueryTplRelEntity rel = new AnalysisTplAiQueryTplRelEntity();
                rel.setScriptId(item.getScriptId());
                rel.setWidgetId(widgetId);
                rel.setAnalysisTplCfgId(analysisTplCfgId);
                rel.setAnalysisTplId(aiWidget.getAnalysisTplId());
                rel.setLocalTplId(dependData.getTplId());
                rel.setCreatedBy(user.getName());
                rel.setUpdatedBy(user.getName());
                relList.add(rel);
            }
        }

        if (StringUtils.isBlank(oldAnalysisTplCfgId)) {
            // 没有老版本，则用线上版本
            List<AnalysisTplCfgProdEntity> entities = dao.queryObjectList("ssm.analysisTemplate.getOnlineConfigs"
                    , Collections.singletonList(aiWidget.getAnalysisTplId()), AnalysisTplCfgProdEntity.class);
            if (CollectionUtils.isNotEmpty(entities)) {
                oldAnalysisTplCfgId = entities.get(0).getAnalysisTplCfgId();
            }
        }

        List<AnalysisTplAiScriptEntity> modifiedScripts = null;
        if (CollectionUtils.isNotEmpty(scriptIds)) {
            Map<String, Object> param = new HashMap<>(4);
            param.put("scriptIds", scriptIds);
            param.put("analysisTplCfgIds", Arrays.asList(MODIFIED_ANALYSIS_TPL_CFG_ID, oldAnalysisTplCfgId));
            String sqlId = "ssm.analysis.template.ai.script.listAiScriptByCondition";
            modifiedScripts = dao.queryObjectList(sqlId, param, AnalysisTplAiScriptEntity.class);
        }

        // 删掉无用的版本
        Map<String, Object> param1 = new HashMap<>(4);
        param1.put("analysisTplId", aiWidget.getAnalysisTplId());
        param1.put("analysisTplCfgIds", Arrays.asList(MODIFIED_ANALYSIS_TPL_CFG_ID, analysisTplCfgId));
        dao.delete("ssm.analysis.template.ai.script.deleteAiScriptByCondition", param1);

        if (CollectionUtils.isNotEmpty(modifiedScripts)) {
            List<AnalysisTplAiScriptEntity> addScripts = modifiedScripts.stream()
                    .peek(v -> {
                        v.setAnalysisTplCfgId(analysisTplCfgId);
                        v.setWidgetId(widgetId);
                        v.setCreatedBy(user.getName());
                    }).collect(Collectors.toList());
            dao.insert("ssm.analysis.template.ai.script.batchInsertAiScript", addScripts);
        }

        if (CollectionUtils.isNotEmpty(relList)) {
            Map<String, Object> params = new HashMap<>();
            params.put("list", relList);
            dao.update("ssm.analysis.template.ai.script.batchInsertAiQueryTplRel", params);
        }
        return true;
    }

    /**
     * 构建数据日期上下文
     * @return
     */
    public AgentQueryDataSet.DataTimeInfo buildDataTimeInfo(AnalysisTemplateAiCfgVO.DependData dependData) {
        if (CollUtil.isEmpty(dependData.getDataRange())) {
            return null;
        }

        List<String> dataRange = dependData.getDataRange();
        DateGranularity dateGranularity = DateGranularity.get(dependData.getDateGranularity());

        AgentQueryDataSet.DataTimeInfo dataTime = new AgentQueryDataSet.DataTimeInfo();
        dataTime.setGranularity(dateGranularity.getDesc());
        dataTime.setStart(dataRange.get(0));
        dataTime.setEnd(dataRange.get(1));
        return dataTime;
    }
}
