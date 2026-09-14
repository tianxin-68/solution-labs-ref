package com.bi.queryer.ssm.engine.accelerate.hot.etl;

import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.mgr.hot.HotTableDdlService;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 14:11 2024/8/21
 * @Description etl热化管理器
 **/
public abstract class HotEtlManager {
    public List<String> execute(List<HotTableInfo> hotTables){
        List<String> result = new ArrayList<>();
        if(BIUtil.isEmpty(hotTables)) {
            return result;
        }
        for(HotTableInfo hotTable : hotTables){
            List<String> logInfos = this.execute(hotTable);
            result.addAll(logInfos);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public List<String> execute(HotTableInfo hotTable){
        List<String> result = new ArrayList<>();

        long t1 = System.currentTimeMillis();

        result.add(String.format("==========%s(begin)=========", getType().getDesc()));
        result.add(String.format("source=%s，hot=%s",hotTable.getSourceTableName(), hotTable.getHotTableName() ));

        // etl脚本
        String tag = String.format("构建建表&etl脚本");
        result.add(HotUtil.logFormat(tag, false));

        HotEtlScriptBuilder builder = this.createScriptBuilder(hotTable);
        EtlTableDdl ddl = builder.buildScheduleScript();

        result.add(HotUtil.logFormat(tag, true));

        // 调度脚本上线发布
        tag = String.format("调度脚本发布上线", hotTable.getEtlJobName());
        result.add(HotUtil.logFormat(tag, false));

        List<String> schedulePublishLogs = this.publishScheduleScript(ddl, hotTable);
        result.addAll(schedulePublishLogs);

        result.add(HotUtil.logFormat(tag, true));

        long t2 = System.currentTimeMillis();
        result.add(String.format("耗时(s):%s", (t2-t1)/1000.0));
        result.add(String.format("==========%s(end)=========", getType().getDesc()));

        // 保存ddl&etl脚本
        this.saveDdl(ddl, hotTable, result);
        return result;
    }

    /**
     * 发布上线：调度脚本
     * @param ddl
     */
    protected List<String> publishScheduleScript(EtlTableDdl ddl, HotTableInfo hotTable){
        List<String> result = new ArrayList<>();

        HotEtlType etlType = this.getType();

        // 请求参数
        Map<String, String> parameters = new HashMap<>();
        parameters.put("jobName", hotTable.getEtlJobName());
        parameters.put("jobOwner", HotUtil.getHotTableOwner());
        parameters.put("scriptContent", ddl.getEtlScriptContent());
        parameters.put("tableName", etlType == HotEtlType.Hive ? hotTable.getHotTableName() : String.format("%s.%s", "drs2", hotTable.getHotTableName()));
        parameters.put("jobPriority", "29");
        parameters.put("jobType", etlType == HotEtlType.Hive ? "Hive3" : "doris");
        parameters.put("etlServer", SC.v("hot.table.etl.server", "ETL24"));
        parameters.put("etlSystem", etlType == HotEtlType.Hive ? "HOT" : "DRH");
        parameters.put("jobDesc", String.format("[多维分析-热化加速]%s(%s->%s)", etlType.getDesc(), hotTable.getSourceTableName(), hotTable.getHotTableName()));

        // 请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", HotUtil.getHotTableAuthorityToken());


        // 调用发布接口
        String baseUrl = HotUtil.getDataStudioServerApiUrl();
        if(BIUtil.isEmpty(baseUrl)){
            result.add("发布失败：datastudio调度作业接口地址为空");
            return result;
        }
        String url = baseUrl + "/api/ssm-hot-table/onlineHotTableJob";
        try {
            String tag = "调用datastudio调度作业发布接口";
            result.add(HotUtil.logFormat(tag, false));

            String response = HttpUtil.doPost(url, parameters, "application/json", headers, 60 * 1000);
            JSONObject responseJson = JSONObject.parseObject(response);
            if("true".equalsIgnoreCase(responseJson.get("success") + "")){
                result.add(HotUtil.logFormat(tag, true));
            }else {
                String errorMsg = HotUtil.logFormat(tag + "[失败]：" + responseJson.get("message"), true);
                result.add(errorMsg);
                throw new BIException(errorMsg);
            }
        }catch (Exception e) {
            String errorMsg = String.format("发布失败：请求地址:%s,原因:%s", url, e.getMessage());
            result.add(errorMsg);
            e.printStackTrace();
            throw new BIException(errorMsg);
        }
        return result;
    }

    /**
     * 发布上线：补数脚本
     * @param ddl
     */
    protected List<String> publishRepairScript(EtlTableDdl ddl, HotTableInfo hotTable){
        List<String> result = new ArrayList<>();

        HotEtlType etlType = this.getType();

        // 请求参数
        Map<String, String> parameters = new HashMap<>();
        parameters.put("jobName", String.format("MANUAL_%s", hotTable.getEtlJobName()));
        parameters.put("jobOwner", HotUtil.getHotTableOwner());
        parameters.put("scriptContent", ddl.getRepairScriptContent());
        parameters.put("tableName", etlType == HotEtlType.Hive ? hotTable.getHotTableName() : String.format("%s.%s", "drs2", hotTable.getHotTableName()));
        parameters.put("jobType", etlType == HotEtlType.Hive ? "Hive3" : "doris");
        parameters.put("jobDesc", String.format("[热表补数](%s)", hotTable.getHotTableName()));
        parameters.put("ctgId", SC.v("hot.table.repair.script.ctg.id", "fb8382299e2d4701a7ee09cadacc08fe"));

        // 请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", HotUtil.getHotTableAuthorityToken());


        // 调用发布接口
        String baseUrl = HotUtil.getDataStudioServerApiUrl();
        if(BIUtil.isEmpty(baseUrl)){
            result.add("发布失败：datastudio补数作业接口地址为空");
            return result;
        }
        String url = baseUrl + "/api/ssm-hot-table/onlineHotTableRepairJob";
        try {
            String tag = "调用datastudio补数作业发布接口";
            result.add(HotUtil.logFormat(tag, false));

            String response = HttpUtil.doPost(url, parameters, "application/json", headers, 60 * 1000);
            JSONObject responseJson = JSONObject.parseObject(response);
            if("true".equalsIgnoreCase(responseJson.get("success") + "")){
                result.add(HotUtil.logFormat(tag, true));
            }else {
                result.add(HotUtil.logFormat(tag + "[失败]：" + responseJson.get("message"), true));
            }
        }catch (Exception e){
            result.add(String.format("发布失败：请求地址:%s,原因:%s", url, e.getMessage()));
            e.printStackTrace();
        }
        return result;
    }


    protected void saveDdl(EtlTableDdl ddl, HotTableInfo hotTable, List<String> logContents){
        HotTableDdl tableDdl = new HotTableDdl();
        tableDdl.setHotTableName(hotTable.getHotTableName());
        tableDdl.setDbEngine(hotTable.getHotDbEngine());
        tableDdl.setSourceTableName(hotTable.getSourceTableName());

        tableDdl.setFieldList(BIUtil.toJSONString(ddl.getCommonFields()));
        tableDdl.setPartitionBy(ddl.getPartitionField().getName());

        tableDdl.setDdlEtlContent(ddl.getEtlScriptContent());
        tableDdl.setDdlEtlMode(hotTable.getHotTableSource().getDataUpdateMode());
        tableDdl.setLogContent(BIUtil.listToStr(logContents, "\n"));

        tableDdl.setCreatedBy(HotUtil.getHotTableOwner());
        HotTableDdlService service = (HotTableDdlService) SpringContextUtil.getBean("hotTableDdlService");
        service.save(tableDdl);
    }

    public abstract HotEtlType getType();

    public abstract HotEtlScriptBuilder createScriptBuilder(HotTableInfo hotTable);

}
