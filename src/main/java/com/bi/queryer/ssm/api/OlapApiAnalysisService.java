package com.bi.queryer.ssm.api;

import com.bi.queryer.ssm.api.vo.req.AnalysisDataReq;
import com.bi.queryer.ssm.api.vo.rsp.AnalysisDataRsp;
import com.bi.queryer.ssm.api.vo.rsp.OlapDatasetAndMetadataData;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author contributor
 * @Date 20:01 2026/4/7
 * @Description TODO
 **/
@Service
@Slf4j
public class OlapApiAnalysisService {

    @Autowired
    private OlapApiService olapApiService;

    /**
     * 通过pandas-ai进行数据分析
     * 交互方案见wiki：https://wiki.example.com/pages/viewpage.action?pageId=695021838
     * 1、通过查询视图id获取csv文件(csv或zip)
     * 2、调用agent服务的prepare获取sessionId
     * 3、调用agent服务的execute执行并返回结果
     */
    public AnalysisDataRsp pandasAI(AnalysisDataReq analysisDataReq, String olapApiKey, String olapApiUserName,
                                    HttpServletRequest request) {

        AnalysisDataRsp  analysisDataRsp = new AnalysisDataRsp();
        /** 参数check并设置用户信息*/
        this.validate(analysisDataReq, olapApiKey, olapApiUserName, request);

        /** 获取视图数据，此时获取的数据为数据文件地址 */
        // 调整结果数据集相关格式，便于传递给agent server
        analysisDataReq.getDataSet().setCsvDelimiter('\t');
        analysisDataReq.getDataSet().setToFile(true);
        analysisDataReq.getDataSet().setZipFile(true);

        OlapDatasetAndMetadataData olapDatasetAndMetadataData = olapApiService.queryOlapDataWithMetadata(
                analysisDataReq, olapApiKey, olapApiUserName, "", "", request);
        String datasetUrl = olapDatasetAndMetadataData.getDataset();

        if(BIUtil.isEmpty(datasetUrl)) {
            throw new BIException("结果集文件地址为空！");
        }

        /**调用agent server*/
        String agentServerBaseUrl = SC.v("ssm.agent.query.ai.interpreter.base.url","");
        String pandasAIUrl =  agentServerBaseUrl + "/api/llm/pandasai/execute";


        Map<String, String> headerMap = new HashMap<String, String>();
        String userToken = UserTokenManager.createByUserName(analysisDataReq.getUserName());
        headerMap.put("u_token", userToken);

        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("url",  datasetUrl);
        String prompt = analysisDataReq.getPrompt() + "\n 注意事项：" + BIUtil.toJSONString(olapDatasetAndMetadataData.getRemark());
        requestParameters.put("query", prompt);
        requestParameters.put("modelName",  analysisDataReq.getModelName());

        String response = HttpUtil.doPost(pandasAIUrl, requestParameters,"application/json", headerMap, 10 * 60 * 1000);
        JSONObject agentResponse = JSON.parseObject(response);
        JSONObject agentResponseData = agentResponse.getJSONObject("data");
        if(agentResponse.getBoolean("success") && BIUtil.isNotEmpty(agentResponseData)) {
            analysisDataRsp.setAnalysisResult(agentResponseData.getString("answer"));
            JSONArray codeArray = agentResponseData.getJSONArray("codes");
            if(codeArray != null && codeArray.size() > 0) {
                analysisDataRsp.setAnalysisProcessPythonCodes(codeArray.toJavaList(String.class));
            }
        }

        return analysisDataRsp;
    }

    public void validate(AnalysisDataReq analysisDataReq, String olapApiKey, String olapApiUserName,
                         HttpServletRequest request){
        // check skill token
        if(BIUtil.isEmpty(analysisDataReq.getSkillToken()) || analysisDataReq.getSkillToken().length() != 32){
            throw new BIException("此mcp不允许直接调用，请使用[skill-olap-query-and-analysis]skill调用");
        }
        olapApiService.validate(analysisDataReq, olapApiKey, olapApiUserName, "", "", request);
    }
}
