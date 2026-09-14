package com.bi.queryer.ssm.sensitive;


import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.sensitive.model.SensitiveDownloadInfoRV;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多维分析-敏感字段导出解密申请
 * @author contributor
 */
@RestController
@RequestMapping("ssd/sensitiveType")
public class SensitiveApplyController extends BaseController{

    @Autowired
    private SensitiveApplyService sensitiveApplyService = null;

    @Autowired
    protected TemplateConfigService service = null;

    /**
     * 查询下载多维分析的敏感级别类型和工单内容
     * @return
     */
    @RequestMapping("getDownloadSensitiveAndWorkOrderInfo")
    @ResponseBody
    public ResponseMessage getDownloadSensitiveAndWorkOrderInfo() throws Exception {
        ResponseMessage result = new ResponseMessage();
        QueryEngine engine = this.createEngine();
        String templateJsonStr = JSONObject.toJSONString(params);
        SensitiveDownloadInfoRV downloadRequest = JSONObject.parseObject(templateJsonStr, SensitiveDownloadInfoRV.class);
        result.setData(sensitiveApplyService.getDownloadSensitiveAndWorkOrderInfo(engine, downloadRequest));
        return result;
    }

    protected QueryEngine createEngine(){
        SSDQueryTemplate queryTemplate = this.createTemplateFromRequest();
        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();
        QueryEngine engine = QueryFactory.createEngine(queryConfigure, new QueryContext(getRequest())); // new QueryEngine(queryConfigure, new QueryContext(getRequest()));

        // 设置当前引擎查询默认数据源
        DataSourceRouter.setQueryEngineDefaultDataSource(engine);

        return engine;
    }

    /**
     * 从请求中创建查询模板
     * @return
     */
    protected SSDQueryTemplate createTemplateFromRequest(){
        SSDQueryTemplate template = null;
        String templateId = this.stringValue("templateId") ;
        String viewId = this.stringValue("viewId") ;
        if(BIUtil.isNotEmpty(templateId)) { // 通过id获取
            template = service.getTemplateById(templateId,viewId);
        }else { // 通过当前配置获取
            String templateConfig = stringValue("templateConfig");
            String templateJsonStr = BIUtil.isEmpty(templateConfig) ? JSONObject.toJSONString(this.params) : templateConfig;
            template = JSONObject.parseObject(templateJsonStr, SSDQueryTemplate.class);
        }
        return template;
    }

}
