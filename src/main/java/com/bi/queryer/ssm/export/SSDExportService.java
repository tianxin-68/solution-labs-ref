package com.bi.queryer.ssm.export;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.AnalysisEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionQueryEngine;
import com.bi.queryer.ssm.export.log.SSDExportLogEntity;
import com.bi.queryer.ssm.export.log.SSDExportLogService;
import com.bi.queryer.ssm.meta.SSDExportBaseRV;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.query.log.SSDQueryLogEntity;
import com.bi.queryer.ssm.query.log.SSDQueryLogService;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.sensitive.SensitiveApplyService;
import com.bi.queryer.ssm.sensitive.model.DownloadWorkOrderInfoRV;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: contributor
 * Date: 2020/2/4
 * Time: 12:28
 * Description:
 */
@Service
@Scope("prototype")
@Qualifier("SSDExportService")
public class SSDExportService{
    private static final Integer MAX_THREAD_NUM = 3;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(MAX_THREAD_NUM);

    private static String FileServerExport_Path= "export/data/async";

    @Autowired
    private BaseDao dao = null;

    @Autowired
    private SSDQueryService queryService;

    @Autowired
    private TemplateConfigService templateService ;

    @Autowired
    private SensitiveApplyService sensitiveApplyService;

    public String submitExportTask(QueryEngine engine, DownloadWorkOrderInfoRV downloadWorkOrderInfoRV) throws Exception {

        User user = UserManager.get();
        String u_token = user.getToken();
        if(StringUtils.isBlank(u_token)){
            throw new Exception("没有获取到用户token！");
        }

        try {
            QueryContext queryContext =  engine.getCxt();

            // 导出时添加人员的敏感字段权限
            List<String> aclSensitiveCodes = queryService.getAuthFieldCodes(queryContext.getUser().getName(), SSDUtil.Field_Auth_Module, SSDUtil.Field_Sensitive_Auth_Dim);
            queryContext.setAclSensitiveFields(aclSensitiveCodes);

            //查询文件名称
            String fileName = this.getFileName(engine.getConfig());

            //查询总数量校验
            Integer totalSize =  downloadWorkOrderInfoRV.getRowNum();
            if(totalSize == null || totalSize < 0){
                throw new Exception("没有获取到文件下载总量……");
            }
            BigDecimal maxExportRows = new BigDecimal(SC.v("ssm.export.rows","200"));
            BigDecimal bigDecimal = maxExportRows.multiply(new BigDecimal(10000));
            BigDecimal total = new BigDecimal(totalSize);
            //超过最大量时，直接调用BI Queryer通返回错误
            if(total.compareTo(bigDecimal) > 0) {
                // 通知前端
                String name = SC.v("ssd.admin", "contributor(contributor)");
                String resMsg = "【多维分析】导出数据量已超过" + maxExportRows + "万条，无法正常导出。若导出更多数据，请邮件申请，具体申请内容和流程见多维分析常见问题QA中序号3：https://docs.qq.com/doc/DT09SYWRib0xVTVVF";
                List<String> sendUser = new ArrayList<>();
                sendUser.add(user.getName());
                queryService.sendMsg(resMsg,sendUser);
                throw new Exception(resMsg);
            }

            //记录两个日志
            String logId = Guid.id();
            if(!this.saveExportLog(engine, downloadWorkOrderInfoRV, logId, "export")){
                throw new Exception("保存下载日志报错！请检查");
            }

            //远程调用文件服务下载接口参数配置
            // 用户文件服务获取多维下载的文件
            String fileserverExportDataToRemote = SC.v("fileserver.ssm.exportDataToRemote");

            // 文件下载完成后文件服务器回调多维
            String fileserverCallBack = SC.v("fileserver.ssm.fileserverCallBack");

            SSDExportBaseRV fileServiceExportBaseRV = new SSDExportBaseRV(user.getName(), logId, fileserverCallBack, fileName,
                    "file_api",fileserverExportDataToRemote + "=" + logId, BIUtil.nvl(downloadWorkOrderInfoRV.getDecryptSensitiveField(), 0));

            //工单信息添加
            fileServiceExportBaseRV = sensitiveApplyService.transferWorkOrderToFileServiceExportInfo(fileServiceExportBaseRV, downloadWorkOrderInfoRV);

            //下载权限用户
            HashSet<String> auths = new HashSet<>();
            auths.add(user.getName());

            //调用远端任务提交接口
            String exportData = this.invokeFileServiceExport(fileServiceExportBaseRV, auths, user.getToken());
            if(StringUtils.isNotBlank(exportData)){
                throw new Exception("调用远端接口错误：" + exportData);
            }

            //6、发送BI Queryer通
            String messageText = "下载任务已提交(" + fileName + ")，任务完成后会收到BI Queryer通消息！";
            if(fileServiceExportBaseRV.getNeedApply()!=null && fileServiceExportBaseRV.getNeedApply()){
                messageText = "下载任务已提交(" + fileName + ")，因下载内容的记录数超过10W条且存在敏感数据，或有个人敏感数据解密导出，已提交工单流程审批，可在工单3.0中查看审批进度。";
            }
            List<String> sendUser = new ArrayList<>();
            sendUser.add(user.getName());
            queryService.sendMsg(messageText,sendUser);

            return logId;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("提交任务报错，请先保存当前模板，清空页面缓存后重新打开多维分析界面，然后再查询导出。错误信息：" + e.getMessage());
        }
    }
    /**
     * 远程调用文件系统下载接口
     * @param fileServiceExportBaseRV
     * @param auths
     * @param token
     * @return
     * @throws Exception
     */
    public String invokeFileServiceExport(SSDExportBaseRV fileServiceExportBaseRV, Set<String> auths, String token) throws Exception {

        JSONObject json = new JSONObject();

        JSONArray jsonAuth = new JSONArray();
        for(String owner : auths){
            Map mapAuth = new HashMap();
            mapAuth.put("owner",owner);
            jsonAuth.add(mapAuth);
        }
        json.put("auths", jsonAuth);

        JSONArray jsonMetas = new JSONArray();
        Map mapMetas = new HashMap();
        mapMetas.put("dsId", fileServiceExportBaseRV.getDsId());
        mapMetas.put("querySql", fileServiceExportBaseRV.getQuerySql());
        jsonMetas.add(mapMetas);

        json.put("userName", fileServiceExportBaseRV.getUserName());
        json.put("businessId", fileServiceExportBaseRV.getBusinessId());
        json.put("businessSource", fileServiceExportBaseRV.getBusinessSource());
        json.put("businessModule", fileServiceExportBaseRV.getBusinessModule());
        json.put("callbackUrl", fileServiceExportBaseRV.getCallbackUrl());
        json.put("fileName", fileServiceExportBaseRV.getFileName());
        json.put("remark", fileServiceExportBaseRV.getRemark());
        json.put("noticeChannel", fileServiceExportBaseRV.getNoticeChannel());
        json.put("isNoticeLeader", fileServiceExportBaseRV.getIsNoticeLeader());
        json.put("noticeSign", fileServiceExportBaseRV.getNoticeSign());
        json.put("metaList", jsonMetas);

        /**
         * 废弃
        //添加工单信息(商业敏感)
        //解密导出，才算个人敏感
        Integer isUserSensitiveData = 0;
        if(fileServiceExportBaseRV.getPersonSensitive() && Enabled.value(fileServiceExportBaseRV.getDecryptSensitiveField())){
            isUserSensitiveData = 1;
        }
        json.put("isUserSensitiveData", isUserSensitiveData);
        */
        json.put("isBizSensitiveData", (fileServiceExportBaseRV.getBusinessSensitive() != null && fileServiceExportBaseRV.getBusinessSensitive())?1:0);
        json.put("isNeedApply", fileServiceExportBaseRV.getNeedApply());
        json.put("dataSensitiveType", fileServiceExportBaseRV.getSensitiveType());
        json.put("downloadContent", fileServiceExportBaseRV.getDownloadContent());
        json.put("downloadData", fileServiceExportBaseRV.getDownloadData());
        json.put("dataNumber", fileServiceExportBaseRV.getRptRows());

        JSONObject applyInfo = new JSONObject();
        applyInfo.put("applyUser", fileServiceExportBaseRV.getApplicant());
        applyInfo.put("applyReason", fileServiceExportBaseRV.getDownloadRemark()!=null? fileServiceExportBaseRV.getDownloadRemark():"暂无");
        applyInfo.put("applyDataLevel", fileServiceExportBaseRV.getSensitiveLevel());
        applyInfo.put("moduleOwner", fileServiceExportBaseRV.getModuleOwnerEmail());
        applyInfo.put("applyContentDesc", fileServiceExportBaseRV.getDownloadContent());
        applyInfo.put("approveDesc","请谨慎审批，切勿盲审/错审。若相关数据发生泄露或滥用，审批人的不当操作将是追责的重要环节。");
        applyInfo.put("applySystemName", "多维分析");
        json.put("applyInfo", applyInfo);

        //调用文件服务的接口
        String fileserverUrl = SC.v("fileserver.url");
        System.out.println("下载任务提交url：" + fileserverUrl + FileServerExport_Path);
        System.out.println("下载任务参数：" + json.toJSONString());

        String result = HttpRequest.post(fileserverUrl + FileServerExport_Path)
                .header("u_token",token)
                .body(String.valueOf(json))
                .execute().body();

        System.out.println("下载任务结果：" + result);
        if(StringUtils.isNotBlank(result)){
            JSONObject jo = JSONObject.parseObject(new String(result));
            String data = jo.getString("success");
            if(StringUtils.isNotBlank(data)&&"true".equalsIgnoreCase(data)){
                return "";
            }else{
                return "*****返回结果：" + result.toString() + "*****传入参数：" +  json.toJSONString();
            }
        }

        return "调用fileserver下载接口报错";
    }

    /**
     * 下载导出表数据回写
     * @param ssdExportBaseRV
     * @return
     */
    public String exportDataCallBack(SSDExportBaseRV ssdExportBaseRV) throws Exception {

        System.out.println("***********进入回调函数：" + ssdExportBaseRV.toString());
        if(StringUtils.isBlank(ssdExportBaseRV.getBusinessId())){
            throw new Exception("businessId参数错误！");
        }
        List<SSDExportLogEntity> logList = (List<SSDExportLogEntity>) dao.queryObjectList("ssm.query.querySubmitExportLog", ssdExportBaseRV.getBusinessId());

        if(logList.isEmpty()){
            throw new Exception("导出日志表未找到记录！");
        }

        String exportExecInfo = ssdExportBaseRV.getMessage();
        String exportStatus = ssdExportBaseRV.getSuccess();
        Integer success = 1;
        if("true".equalsIgnoreCase(exportStatus)){
            success = 1;
        }else{
            success = 0;
        }
        SSDExportLogEntity log = new SSDExportLogEntity(ssdExportBaseRV.getBusinessId(), success, exportExecInfo, ssdExportBaseRV.getRptRows(),
                ssdExportBaseRV.getFileName(), ssdExportBaseRV.getStatus());
        if(StringUtils.isNotBlank(ssdExportBaseRV.getFileName()) && ssdExportBaseRV.getFileName().contains(".")){
            String tempExt = ssdExportBaseRV.getFileName().substring(ssdExportBaseRV.getFileName().lastIndexOf("."));
            log.setFileExt(tempExt);
        }else{
            log.setFileExt(".csv");
        }

        dao.update("ssm.query.updateSubmitExportLog", log);
        System.out.println("***********执行完成回调函数");
        return "操作成功！";
    }

    /**
     * 查询日志表信息
     * @param logId
     * @return
     */
    public List<SSDExportLogEntity> querySubmitExportLog(String logId) throws Exception {

        List<SSDExportLogEntity> logList = (List<SSDExportLogEntity>) dao.queryObjectList("ssm.query.querySubmitExportLog", logId);

        return logList;
    }

    public ResponseMessage export(QueryEngine engine, SSDExportLogEntity exportLogEntity, HttpServletRequest request, HttpServletResponse response){

        JSONObject jsonObject = JSONObject.parseObject(exportLogEntity.getQueryConfig());
        Map<String, Object> exportConfig = (Map<String,Object>)jsonObject;

        //设置要下载的用户（需要看fileserver情况设置，应该是直接取不需要自己设）
        User user = UserManager.get();
        if(user == null || StringUtil.isEmpty(user.getName())){
            User userTemp = new User(exportLogEntity.getUserName());
            UserManager.set(userTemp);
        }

        QueryContext queryContext =  engine.getCxt();

        queryContext.setExport(true);

        // 个人敏感字段是否需要解密
        if(Enabled.value(exportLogEntity.getDecryptSensitiveField())){
            queryContext.setDecryptSensitiveField(exportLogEntity.getDecryptSensitiveField());
        }

        // 导出时添加人员的敏感字段权限
        List<String> aclSensitiveCodes = queryService.getAuthFieldCodes(queryContext.getUser().getName(), SSDUtil.Field_Auth_Module, SSDUtil.Field_Sensitive_Auth_Dim);
        queryContext.setAclSensitiveFields(aclSensitiveCodes);

        queryContext.setBlackBox(exportLogEntity.getBlackBox());
        queryContext.setExportLogId(exportLogEntity.getId());

        String fileName = this.getFileName(engine.getConfig());
        Double maxSize = null;

        String id = String.valueOf(exportConfig.get("id"));
        if(BIUtil.isNotEmpty(id)) {
            SSDQueryTemplate template = templateService.getTemplateById(id,"");
            if (template != null) {
                maxSize = template.getTplExpMaxRows();
            }
        }

        /**
        SSDFastDataExporter exporter = this.getExporter(request, response, exportLogEntity.getRows(), engine);
        exporter.setFileName(fileName);
        ResponseMessage responseMessage = exporter.export(maxSize, exportConfig, engine.buildSql());
        */

        SSMExporter exporter = new SSMExporter(request, response, exportLogEntity.getRows(), engine);
        ResponseMessage responseMessage = exporter.export(fileName, maxSize);
        return responseMessage;
    }

    /**
     * 获取导出器
     * @param request
     * @param response
     * @param totalSize
     * @param engine
     * @return
     */
    protected SSDFastDataExporter getExporter(HttpServletRequest request, HttpServletResponse response, Integer totalSize, QueryEngine engine ){
        SSDFastDataExporter exporter = new SSDFastDataExporter(request, response, totalSize, engine);
        QueryEngine queryEngine = engine;
        if(queryEngine instanceof AnalysisEngine){
            queryEngine = ((AnalysisEngine)engine).getQueryEngine();
        }

        if(queryEngine instanceof CrossDimensionQueryEngine){
            exporter = new SSDFastCrossDimensionDatasetExporter(request, response, totalSize, queryEngine);
        }

        return exporter;
    }

    protected String getFileName(QueryConfigure config){
        String fileName = config.getTemplateEntity().getName();
        if(fileName != null && fileName.contains("/")){
            fileName = fileName.replace("/","_");
        }
        if(BIUtil.isEmpty(fileName)){
            fileName = "多维分析_" + new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
        }
        return fileName;
    }

    /**
     * 记录下载日志和字段日志
     * @return true 成功，false 保存失败
     */
    public Boolean saveExportLog(QueryEngine engine, DownloadWorkOrderInfoRV downloadWorkOrderInfoRV, String logId, String expType) {
        //记录两个日志

        try {
            User user = UserManager.get();
            String expSource = "export";
            String expMode = "fileServer";
            boolean isReplicateExport = false;
            if (StringUtils.isNotBlank(expType) && "replicate".equalsIgnoreCase(expType)) {
                expSource = "replicate";
                expMode = "replicate";
                isReplicateExport = true;
            }

            //记录查询字段日志
            SSDQueryLogService fieldLogService = (SSDQueryLogService) SpringContextUtil.getBean("SSDQueryLogService");
            SSDQueryLogEntity fieldLog = new SSDQueryLogEntity();
            fieldLog.setId(logId);
            if (engine.getConfig().getTemplateEntity() != null) {
                fieldLog.setTemplateId(engine.getConfig().getTemplateEntity().getId());
                fieldLog.setViewId(engine.getConfig().getTemplateEntity().getViewId());
            }

            if (user != null && StringUtils.isNotBlank(user.getName())) {
                fieldLog.setUserName(user.getName());
            }

            //读取合并前的字段，存入字段日志表
            List<QueryField> allFields = SSDUtil.getFinalQueryAllFields(engine.getConfig(), engine.getModels()); //loadAllFieldList(engine.getConfig(), engine.getModels());
            fieldLogService.logFinalQueryFields(allFields, fieldLog, expSource);

            //记录导出日志
            SSDExportLogEntity expLog = new SSDExportLogEntity(logId, BIUtil.nvl(downloadWorkOrderInfoRV.getId(), ""),
                    downloadWorkOrderInfoRV.toSaveDBConfig(), user.getName(), Calendar.getInstance().getTime(), expMode, downloadWorkOrderInfoRV.getRowNum(),
                    "", BIUtil.nvl(downloadWorkOrderInfoRV.getDecryptSensitiveField(), 0), downloadWorkOrderInfoRV.getNeedApply() ? 1 : 0);

            try{
                String sql = engine.buildSql();
                expLog.setQuerySql(sql);
                expLog.setQueryFilter(SSDUtil.getQueryConfigFilterDesc(engine.getConfig()));
                expLog.setSessionId(engine.getConfig().getSessionId());

                if(isReplicateExport){
                    // 复制导出：将复制内容上传到文件服务器
                    String fileName = this.uploadReplicateContent(downloadWorkOrderInfoRV);
                    expLog.setFileExt("csv");
                    expLog.setFileName(fileName);
                    expLog.setInfo(JSONArray.toJSONString(downloadWorkOrderInfoRV.getDownloadContent()));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            // 插入导出日志
            SSDExportLogService expLogService = (SSDExportLogService) SpringContextUtil.getBean("SSDExportLogService");
            expLog.setBlackBox(user.getBlackBox());
            if (engine.getConfig().getTemplateEntity() != null) {
                expLog.setViewId(engine.getConfig().getTemplateEntity().getViewId());
            }

            expLogService.submitLog(expLog);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 上传复制内容
     * 将复制的具体内容转为csv然后上传到服务
     * @return 上传文件名
     */
    protected String uploadReplicateContent(DownloadWorkOrderInfoRV downloadWorkOrderInfoRV){
        String datasetString = downloadWorkOrderInfoRV.getDatasetString();
        if(BIUtil.isEmpty(datasetString)){
            return "";
        }
        CsvWriter csvWriter = null;
        File csvFile = null;
        try {
            String fileName = Guid.id() + ".csv";
            csvFile = FileUtil.newFile(fileName);
            csvWriter = new CsvWriter(new FileOutputStream(csvFile), ',', Charset.forName("UTF-8"));
            String[] rows = datasetString.split("\n");
            for (String row : rows) {
                csvWriter.writeRecord(row.split("\t"), true);
            }
            csvWriter.flush();

            // 上传到文件服务器
            RestTemplate restTemplate = new RestTemplate();

            // 创建请求体（multipart）
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("file", new FileSystemResource(csvFile));

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送 POST 请求
            String fileServerBaseUrl = SC.v("file.server.base.url", "http://fileserver.example.com:9010");
            ResponseEntity<String> response = restTemplate.postForEntity(fileServerBaseUrl + "/upload/uploadPrivateFile", requestEntity, String.class);

            EXECUTOR.execute(()-> {
                try {
                    Thread.sleep(3000);
                    Files.deleteIfExists(Paths.get(fileName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //System.out.println("Response Status: " + response.getStatusCode());
            //System.out.println("Response Body: " + response.getBody());
            if(response.getStatusCode() == HttpStatus.OK){
                JSONObject responseJson = JSONObject.parseObject(response.getBody());
                return responseJson.getString("data");
            }

        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (csvWriter != null) {
                    csvWriter.close();
                }
                if(csvFile != null){
                    csvFile.deleteOnExit();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return "";
    }

}
