package com.bi.queryer.ssm.export;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.*;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.prepare.PrepareQueryEngine;
import com.bi.queryer.ssm.query.SSDQueryManager;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.sensitive.SensitiveApplyService;
import com.bi.queryer.ssm.sensitive.model.DownloadWorkOrderInfoRV;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.export.log.SSDExportLogEntity;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.*;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * User: contributor
 * Date: 2022/9/23
 * Time: 12:16
 * Description:
 * 下载请求
 */
@Controller
@Scope("prototype")
@RequestMapping("ssd")
public class SSDExporterController extends BaseController{

    @Autowired
    protected SSDExportService service;

    @Autowired
    protected SSDQueryService queryService;

    @Autowired
    protected TemplateConfigService templateService;

    @Autowired
    protected SensitiveApplyService sensitiveApplyService;

    protected QueryEngine createEngine(){
        SSDQueryTemplate queryTemplate = this.createTemplateFromRequest();
        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();

        QueryContext cxt = new QueryContext(getRequest());

        // 预处理：正式数据查询前置数据查询
        PrepareQueryEngine prepareQueryEngine = QueryFactory.createPrepareEngine(queryConfigure, cxt);
        cxt.setPrepareQueryResult(prepareQueryEngine.execute());

        QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt); // new QueryEngine(queryConfigure, new QueryContext(getRequest()));
        SSDQueryManager.setQueryEngine(engine);
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
            template = templateService.getTemplateById(templateId,viewId);
        }else { // 通过当前配置获取
            String templateConfig = stringValue("templateConfig");
            String templateJsonStr = BIUtil.isEmpty(templateConfig) ? JSONObject.toJSONString(this.params) : templateConfig;
            template = JSONObject.parseObject(templateJsonStr, SSDQueryTemplate.class);
        }
        return template;
    }

    /**
     * 下载专用从参数中创建查询模板
     * @return
     */
    protected SSDQueryTemplate createTemplateFromRequestExport(Map<String, Object> exportMap){
        SSDQueryTemplate template = null;
        if(exportMap.containsKey("templateId")){
            String templateId = String.valueOf(exportMap.get("templateId"));
            String viewId = String.valueOf(exportMap.get("viewId"));
            if(BIUtil.isNotEmpty(templateId)) {
                template = templateService.getTemplateById(templateId,viewId);
                return template;
            }
        }
        if(exportMap.containsKey("templateConfig")){
            String templateConfig = exportMap.get("templateConfig").toString();
            if(BIUtil.isNotEmpty(templateConfig) ) {
                template = JSONObject.parseObject(templateConfig, SSDQueryTemplate.class);
                return template;
            }
        }
//        String templateConfig = stringValue("templateConfig");
        String templateJsonStr = JSONObject.toJSONString(exportMap);
        template = JSONObject.parseObject(templateJsonStr, SSDQueryTemplate.class);
        return template;
    }

    /**
     * 下载专用引擎生成器
     * @param exportMap
     * @return
     */
    protected QueryEngine createEngineExport(Map<String, Object> exportMap){
        SSDQueryTemplate queryTemplate = this.createTemplateFromRequestExport(exportMap);
        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();

        QueryContext cxt = new QueryContext(getRequest());

        // 预处理：正式数据查询前置数据查询
        PrepareQueryEngine prepareQueryEngine = QueryFactory.createPrepareEngine(queryConfigure, cxt);
        cxt.setPrepareQueryResult(prepareQueryEngine.execute());

        QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt); // new QueryEngine(queryConfigure, new QueryContext(getRequest()));

        // 设置当前引擎查询默认数据源
        DataSourceRouter.setQueryEngineDefaultDataSource(engine);

        SSDQueryManager.setQueryEngine(engine);
        return engine;
    }

    @RequestMapping("submitExportTask")
    @ResponseBody
    public ResponseMessage submitExportTask() throws Exception {
        ResponseMessage result = new ResponseMessage();
        QueryEngine engine = this.createEngine();
        String templateJsonStr = JSONObject.toJSONString(params);
        DownloadWorkOrderInfoRV downloadWorkOrderInfoRV = JSONObject.parseObject(templateJsonStr, DownloadWorkOrderInfoRV.class);
        //页面请求，都加needApply属性(默认是走工单的)
        downloadWorkOrderInfoRV.setNeedApply(true);
        result.setData(service.submitExportTask(engine, downloadWorkOrderInfoRV));
        return result;
    }

    /**
     * 远端文件系统回调接口
     * @return
     * @throws Exception
     */
    @RequestMapping("exportDataCallBack")
    public ResponseMessage exportDataCallBack() throws Exception {
        ResponseMessage result = new ResponseMessage();
        SSDExportBaseRV SSDExportBaseRV = new SSDExportBaseRV();

        String templateJsonStr = JSONObject.toJSONString(params);
        SSDExportBaseRV = JSONObject.parseObject(templateJsonStr, SSDExportBaseRV.class);

        result.setData(service.exportDataCallBack(SSDExportBaseRV));
        System.out.println("***********执行完成回调函数结束");

        //推送安全kafka日志
        if(StringUtils.isNotBlank(SSDExportBaseRV.getBusinessId())){
           // queryService.sendLogToKafka(SSDExportBaseRV.getBusinessId(), 1);
        }

        return result;
    }

    /**
     * 远端调用本地下载接口
     * @return
     * @throws Exception
     */
    @RequestMapping("exportDataToRemote")
    public void exportDataToRemote(String logId){
        System.out.println("***********进入远端调用本地下载接口：exportDataToRemote");
        String errorMsg = "false";
        try {
            //查询下载日志表信息获取里面的参数
            List<SSDExportLogEntity> exportLogList =  service.querySubmitExportLog(logId);
            if(CollUtil.isEmpty(exportLogList) && exportLogList.size() < 1){
                throw new Exception("下载日志没有找到!");
            }
            SSDExportLogEntity exportLogEntity = exportLogList.get(0);
            String mapStr = exportLogEntity.getQueryConfig();

            JSONObject jsonObject = JSONObject.parseObject(mapStr);
            Map<String, Object> exportConfig = (Map<String,Object>)jsonObject;

            getRequest().setCharacterEncoding("utf-8");
            QueryEngine engine = this.createEngineExport(exportConfig);

            ResponseMessage responseMessage = service.export(engine, exportLogEntity, this.getRequest(), this.getResponse());

            if(!responseMessage.getSuccess()){
                getResponse().addHeader("error_msg", URLEncoder.encode(responseMessage.getMessage(), "UTF8"));
                writeJSON(responseMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorMsg = e.getMessage();
        }finally {
            try {
                this.getResponse().setHeader("error_msg", URLEncoder.encode(errorMsg,"UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
        System.out.println("***********远端调用本地下载接口结束：exportDataToRemote");
    }

    /**
     * 复制时保存日志
     * @return
     */
    @RequestMapping("replicateSaveExportLog")
    @ResponseBody
    public ResponseMessage replicateSaveExportLog() {
        ResponseMessage result = new ResponseMessage();
        QueryEngine engine = this.createEngine();
        String templateJsonStr = JSONObject.toJSONString(params);
        DownloadWorkOrderInfoRV downloadWorkOrderInfoRV = JSONObject.parseObject(templateJsonStr, DownloadWorkOrderInfoRV.class);
        downloadWorkOrderInfoRV.setNeedApply(false);
        String logId = Guid.id();
        if (service.saveExportLog(engine, downloadWorkOrderInfoRV, logId, "replicate")) {
            result.setData(true);
        } else {
            result.set(false, "保存日志报错！");
        }

        return result;
    }


}
