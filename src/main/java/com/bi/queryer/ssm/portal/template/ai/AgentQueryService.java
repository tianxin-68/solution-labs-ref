package com.bi.queryer.ssm.portal.template.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.Env;
import com.bi.queryer.ssm.portal.template.vo.AgentQueryReq;
import com.bi.queryer.ssm.portal.template.vo.AgentQueryRsp;
import com.bi.queryer.ssm.portal.template.vo.AiScriptGenReq;
import com.bi.queryer.ssm.portal.template.vo.GenerateTmpAnalysisTplNameReq;
import com.bi.queryer.ssm.portal.template.vo.GenerateTmpAnalysisTplNameRsp;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AgentQueryService {

    /**
     * 查询AI解读
     *
     * @param req
     * @return
     */
    public AgentQueryRsp queryAIInterpreter(AgentQueryReq req) {

        AgentQueryRsp rsp = new AgentQueryRsp();

        User user = UserManager.get();
        String url = SC.v("ssm.agent.query.ai.interpreter.base.url", "");
        url = url + "/llm/olap/aipush/generate";

        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        /**
         * 构建请求入参
         */
        Map<String, Object> reqMap = JSON.parseObject(JSON.toJSONString(req), Map.class);
        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 60 * 60 * 1000);
        JSONObject responseData = JSONObject.parseObject(response);
        if (responseData == null) {
            throw new BIException("查询AI解答异常");
        }

        if (!responseData.getBooleanValue("success")) {
            throw new BIException("调用agent服务异常, " + responseData.getString("message"));
        }

        rsp = JSON.toJavaObject(responseData.getJSONObject("data"), AgentQueryRsp.class);
        return rsp;
    }

    private String getAgentQueryUrl() {
        Env env = SSDUtil.getEnv();
        if (Env.UT == env) {
            return "https://bi-agent-server-ut.example.com/new";
        } else {
            return SC.v("ssm.agent.query.ai.interpreter.base.url", "");
        }
    }

    public AgentQueryRsp execPythonScript(AgentQueryReq req) {
        User user = UserManager.get();
        String url = getAgentQueryUrl();
        url = url + "/llm/olap/aipush/execute";
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        /**
         * 构建请求入参
         */
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("dataSets", req.getDataSets());
        reqMap.put("template", req.getTemplate());
        reqMap.put("code", req.getScriptCode());

        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 60 * 60 * 1000);
        log.info("execPythonScript, requestData: {},  responseData: {}, user: {}", JSONObject.toJSONString(reqMap), response, user.getName());
        JSONObject responseData = JSONObject.parseObject(response);
        if (responseData == null) {
            throw new BIException("查询AI解答异常");
        }

        if (!responseData.getBooleanValue("success")) {
            throw new BIException("调用agent服务异常, " + responseData.getString("message"));
        }

        return JSON.toJavaObject(responseData.getJSONObject("data"), AgentQueryRsp.class);
    }

    public AgentQueryRsp genPythonScript(AiScriptGenReq req) {
        User user = UserManager.get();
        String url = getAgentQueryUrl();
        url = url + "/llm/olap/aipush/generate";
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

        /**
         * 构建请求入参
         */
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("dataSets", req.getDataSets());
        reqMap.put("interpretType", req.getInterpretType());
        reqMap.put("template", req.getTemplate());
        reqMap.put("modelName", req.getModelName());
        reqMap.put("dataContext", req.getDataContext());
        reqMap.put("userIntention", req.getUserIntention());
        reqMap.put("outputFormat", req.getOutputFormat());

        String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 60 * 60 * 1000);
        log.info("genPythonScript, requestData: {},  responseData: {}, user: {}", JSONObject.toJSONString(reqMap), response, user.getName());
        JSONObject responseData = JSONObject.parseObject(response);
        if (responseData == null) {
            throw new BIException("查询AI解答异常");
        }

        if (!responseData.getBooleanValue("success")) {
            throw new BIException("调用agent服务异常, " + responseData.getString("message"));
        }

        return JSON.toJavaObject(responseData.getJSONObject("data"), AgentQueryRsp.class);
    }

	/**
	 * 根据多组一一对应的「查询模板名称 + 视图名称」生成临时看板名称。
	 * POST /llm/tool/generate-template-name，入参 name 为每组「模板/视图」用斜杠连接，组间英文逗号拼接。
	 */
	public GenerateTmpAnalysisTplNameRsp generateTmpAnalysisTplName(GenerateTmpAnalysisTplNameReq req) {
		List<GenerateTmpAnalysisTplNameReq.QueryTemplateName> pairs = CollUtil.emptyIfNull(req.getTplViewPairs());
		if (CollUtil.isEmpty(pairs)) {
			throw new BIException("模板与视图对应关系不能为空");
		}
		List<String> segments = new ArrayList<>();
		for (GenerateTmpAnalysisTplNameReq.QueryTemplateName p : pairs) {
			if (p == null) {
				continue;
			}
			String tpl = StrUtil.trimToEmpty(p.getTemplateName());
			String view = StrUtil.trimToEmpty(p.getViewName());
			segments.add(tpl + "_" + view);
		}
		String nameParam = String.join(",", segments);

		String modelName = req.getModelName();
		User user = UserManager.get();
		String url = SC.v("ssm.agent.query.ai.interpreter.base.url", "");
		url = url + "/llm/tool/generate-template-name";
		Map<String, String> headers = new HashMap<>();
		headers.put("u_token", UserTokenManager.createByUserName(user.getName()));

		Map<String, Object> reqMap = new HashMap<>();
		reqMap.put("name", nameParam);
		reqMap.put("modelName", StrUtil.isNotBlank(modelName)
				? modelName
				: SC.v("ssm.agent.generate.tpl.name.model", "chatgpt-5.4"));

		String response = HttpUtil.doPost(url, reqMap, "application/json", headers, 5 * 60 * 1000);
		JSONObject responseData = JSONObject.parseObject(response);
		if (responseData == null) {
			throw new BIException("生成临时看板名称异常：响应为空");
		}
		if (!responseData.getBooleanValue("success")) {
			throw new BIException("调用生成看板名称服务异常, " + responseData.getString("message"));
		}
		JSONObject data = responseData.getJSONObject("data");
		if (data == null) {
			throw new BIException("生成临时看板名称异常：data 为空");
		}
		GenerateTmpAnalysisTplNameRsp rsp = new GenerateTmpAnalysisTplNameRsp();
		rsp.setGeneratedName(data.getString("generatedName"));
		return rsp;
	}

}
