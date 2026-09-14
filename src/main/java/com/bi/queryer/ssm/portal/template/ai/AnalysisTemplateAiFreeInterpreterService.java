package com.bi.queryer.ssm.portal.template.ai;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.portal.enums.AiMode;
import com.bi.queryer.ssm.portal.template.vo.*;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 分析模板AI自由解读服务
 */
@Service
@Scope("prototype")
public class AnalysisTemplateAiFreeInterpreterService {

    @Autowired
    private AgentQueryService agentQueryService;
    public AnalysisTemplateAiPreviewRsp execAiSummary(AnalysisTemplateAiPreviewReq execReq) {
        AnalysisTemplateAiPreviewRsp rsp = new AnalysisTemplateAiPreviewRsp();

        AnalysisTemplateAiCfgVO.AnalysisTplAiCfgItem aiCfgItem = execReq.getAnalysisTplAiCfgItems().get(0);

        //构造AI解读的入参
        AgentQueryReq req = new AgentQueryReq();

        List<AgentQueryDataSet> dataSets = new ArrayList<>();

        for(AnalysisTemplateAiCfgVO.DependData dependData : aiCfgItem.getDependDataList()){
            AgentQueryDataSet dataSet = new AgentQueryDataSet();
            dataSet.setName(dependData.getViewId());
            dataSet.setDescription(String.format("%s,%s", dependData.getTplName(), dependData.getViewName()));
            dataSet.setUrl(dependData.getDataPath());

            AgentQueryDataSet.DataTimeInfo dataTime = new AgentQueryDataSet.DataTimeInfo();
            dataTime.setGranularity(dependData.getDateGranularity());

            if(CollUtil.isNotEmpty(dependData.getDataRange())){
                dataTime.setStart(dependData.getDataRange().get(0));
                dataTime.setEnd(dependData.getDataRange().get(dependData.getDataRange().size()-1));
            }

            dataSet.setColumnsInfo(dependData.getColumnsInfo());
            dataSet.setDataTime(dataTime);
            dataSets.add(dataSet);
        }

        req.setDataSets(dataSets);

        req.setInterpretType(AiMode.AI.getCode());
        req.setModelName(aiCfgItem.getLlmModel());
        req.setTemplate(Guid.id());

        //构建数据上下文
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户分析意图\n");
        prompt.append(aiCfgItem.getOutputPrompt());
        req.setUserIntention(prompt.toString());

        System.out.println(JSONObject.toJSONString(req));
        //执行AI解读
        AgentQueryRsp queryRsp = agentQueryService.queryAIInterpreter(req);

        //构造AI解读结果
        String aiSummary = queryRsp.getValue();
        rsp.setSummary(aiSummary);
        rsp.setPrompt(queryRsp.getPrompt());

        return rsp;
    }

    /**
     * 构建数据日期上下文
     * @return
     */
    public String buildDateRangeContext(AnalysisTemplateAiCfgVO.DependData dependData) {

        if (CollUtil.isEmpty(dependData.getDataRange())) {
            return "";
        }

        List<String> dataRange = dependData.getDataRange();
        DateGranularity dateGranularity = DateGranularity.get(dependData.getDateGranularity());
        String result = String.format("日期范围为%s粒度%s-%s",
                dateGranularity.getDesc(),
                dataRange.get(0),
                dataRange.get(dataRange.size() - 1));

        return result;
    }

}
