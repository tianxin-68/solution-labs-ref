package com.bi.queryer.ssm.llm;

import cn.hutool.core.text.UnicodeUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.llm.entity.LLMAnalysisDate;
import com.bi.queryer.ssm.llm.entity.LLMAnalysisMetric;
import com.bi.queryer.ssm.llm.req.BuildAgentFilterReq;
import com.bi.queryer.ssm.llm.resp.AgentQueryField;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.QueryController;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:37 2025/3/24
 * @Description 大模型agent相关api
 **/

@Controller
@Scope("prototype")
@RequestMapping("/llm/agent/api")
public class LLMAgentApiController extends QueryController {

    @Autowired
    private LLMAgentApiService llmAgentApiService;

    @RequestMapping("query")
    @ResponseBody
    public ResponseMessage query() {
        this.initStreamParameters();
        ResponseMessage response = new ResponseMessage();

        String llmQueryConfigStr = stringValue("query_config");
        QueryContext cxt = new QueryContext(getRequest());
        JSONObject dataSet =  llmAgentApiService.query(llmQueryConfigStr,cxt);
        response.setData(dataSet);

        return response;
    }

    protected List<MetaField> getModuleFields(String moduleId){
        List<MetaField> ctgMetaFields = SSDMetaCacheManager.getFieldByCategory(moduleId);
        if(BIUtil.isEmpty(ctgMetaFields)){
            return new ArrayList<>();
        }

        ctgMetaFields = ctgMetaFields.stream().filter(f-> Enabled.isTrue(f.getIsShow())).collect(Collectors.toList());
        ctgMetaFields = FieldUtil.getMaxWeightFields(ctgMetaFields, moduleId);
        return ctgMetaFields;
    }

    @RequestMapping("get/field/info")
    @ResponseBody
    public ResponseMessage getFieldMetaInfo() {
        this.initStreamParameters();
        ResponseMessage result = new ResponseMessage();
        String fieldTitleStr = stringValue("fieldTitles");
        if(fieldTitleStr == null){
            return result;
        }
        String moduleId = stringValue("moduleId");

        List<MetaField> ctgMetaFields = this.getModuleFields(moduleId);
        if(BIUtil.isEmpty(ctgMetaFields)){
            return result;
        }
        List<JSONObject> targetFields = new ArrayList<>();
        String[] titles = fieldTitleStr.split(",");
        for (String title : titles) {
            for(MetaField m : ctgMetaFields){
                if(m.getTitle().equalsIgnoreCase(title) || m.getCode().equalsIgnoreCase(title)){
                    JSONObject item = new JSONObject();
                    item.put("id", m.getId());
                    item.put("code", m.getCode());
                    item.put("title", m.getTitle());
                    targetFields.add(item);
                }
            }
        }
        result.setData(targetFields);
        return result;
    }

    /**
     * 获取分析对象信息
     * @return
     */
    @RequestMapping("get/analysis/object")
    @ResponseBody
    public ResponseMessage getAnalysisObject() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            this.initStreamParameters();
            SSDQueryTemplate queryTemplate = this.createTemplateFromRequest();
            QueryConfigure queryConfigure = createQueryConfigure(queryTemplate);
            queryConfigure.load();

            LLMAnalysisMetric metrics = JSON.parseObject(this.stringValue("analysisMetric"), LLMAnalysisMetric.class);
            LLMAnalysisDate analysisDate = JSON.parseObject(this.stringValue("analysisDate"), LLMAnalysisDate.class);

            JSONObject analysisInfo = llmAgentApiService.getAnalysisObject(queryConfigure, metrics, analysisDate);

            responseMessage.setData(analysisInfo);
        } catch (Exception e) {
            responseMessage = new ResponseMessage(e);
            responseMessage.setData("获取分析对象失败");
        }
        return responseMessage;
    }

    /**
     * 获取大模型的过滤条件
     * @return
     */
    @RequestMapping("buildAgentFilter")
    @ResponseBody
    public SSMResponseMessage<List<AgentQueryField>> buildAgentFilter(@RequestBody BuildAgentFilterReq req) {
        return SSMResponseMessage.success("", llmAgentApiService.buildAgentFilter(req));
    }

    @Override
    protected QueryConfigure createQueryConfigure(SSDQueryTemplate queryTemplate) {
        return new QueryConfigure(queryTemplate);
    }

    public static void main(String[] args) {
        String s = "{\"\\u9ed8\\u8ba4\\u5907\\u8d27\\u4ed3\\u7c7b\\u578b\": \"\\u4fdd\\u517b\\u4ed3\", \"\\u76ee\\u7684\\u4ed3\\u7701\\u4efd\": \"\\u4e0a\\u6d77\\u5e02\", \"\\u5f02\\u5e38\\u539f\\u56e0\": \"\\u6570\\u636e\\u4e0d\\u8db3\\uff0c\\u65e0\\u6cd5\\u83b7\\u53d6\\u95e8\\u5e97\\u548c\\u672c\\u4ed3\\u8d21\\u732e\\u5ea6\\u73af\\u6bd4\\u6570\\u636e\", \"\\u5f52\\u56e0\\u7ed3\\u679c\": \"\\u65e0\\u6cd5\\u786e\\u5b9a\"}";
        s = UnicodeUtil.toString(s);
        System.out.println(s);
    }
}
