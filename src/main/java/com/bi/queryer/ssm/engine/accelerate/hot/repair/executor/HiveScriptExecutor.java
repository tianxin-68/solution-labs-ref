package com.bi.queryer.ssm.engine.accelerate.hot.repair.executor;

import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author contributor
 * @Date 19:22 2024/10/12
 * @Description hive执行器
 **/
public class HiveScriptExecutor extends BaseScriptExecutor {
    @Override
    public ScriptExecuteResult execute(HotTableRepairTask task) {
        ScriptExecuteResult result = new ScriptExecuteResult();

        // 请求参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dsId", SC.v("hot.table.repair.hive.dsId", "smny6r02b95cfre5a0732e92buri78ut")); // TODO
        parameters.put("scriptId", SC.v("hot.table.repair.hive.scriptId", "hot.table.repair.hive.scriptId"));// TODO
        parameters.put("dbName", "bi_hot");
        parameters.put("queryEngine", "hive");
        parameters.put("scriptType", "hive3");
        parameters.put("limitNum", "1000");
        parameters.put("varList", new ArrayList());
        parameters.put("sqlList", new String[]{task.getTaskScriptContent()});

        // 调用查询接口
        String url = HotUtil.getDataStudioServerApiUrl() + "/datasource/executeQuery";
        JSONObject responseJson = this.invokeApi(url, parameters, true);
        if("true".equalsIgnoreCase(responseJson.get("success") + "")){
            result.setSuccess(true);
            result.setExecStatus(TaskExecStatus.RUNNING.getCode());
            result.setExecId(responseJson.get("data") + "");
        }else {
            result.setSuccess(false);
            result.setExecId(responseJson.get("message") + "");
        }
        return result;
    }

    @Override
    public ScriptExecuteResult kill(HotTableRepairTask task) {
        ScriptExecuteResult result = new ScriptExecuteResult();
        String url = HotUtil.getDataStudioServerApiUrl() + String.format("/script/stop?logId=%s", task.getTaskExecId());
        JSONObject responseJson = this.invokeApi(url, new HashMap(), false);
        if("true".equalsIgnoreCase(responseJson.get("success") + "")){
            result.setSuccess(true);
            result.setExecStatus(TaskExecStatus.FAIL.getCode());
            result.setExecId(responseJson.get("data") + "");
        }else {
            result.setSuccess(false);
            result.setExecInfo(responseJson.get("message") + "");
        }
        return result;
    }

    @Override
    public ScriptExecuteResult getProgress(HotTableRepairTask task) {
        ScriptExecuteResult result = new ScriptExecuteResult();
        // 默认为running
        result.setExecStatus(TaskExecStatus.RUNNING.getCode());
        result.setSuccess(true);

        String url =  HotUtil.getDataStudioServerApiUrl() + "/script/getHistoryResultById";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("logId", task.getTaskExecId());
        JSONObject responseJson = this.invokeApi(url, parameters, true);
        // 先判断状态再获取结果
        if("true".equalsIgnoreCase(responseJson.get("success") + "")){
            JSONObject detailJson = responseJson.getJSONObject("data");
            if(detailJson != null){
                if("true".equalsIgnoreCase(detailJson.get("isSuccess") + "")){
                    result.setExecStatus(TaskExecStatus.SUCCESS.getCode());
                }else {
                    result.setExecStatus(TaskExecStatus.FAIL.getCode());
                    result.setExecInfo(detailJson.getString("msg"));
                }
            }
        }else {
            result.setSuccess(false);
            result.setExecInfo(responseJson.get("message") + "");
        }

        /*
        String url =  HotUtil.getDataStudioServerApiUrl() + String.format("/ide/datasource/getExecuteLog?token=%s", task.getTaskExecId());
        JSONObject responseJson = this.invokeApi(url, null, false);
        // 先判断状态再获取结果
        if("true".equalsIgnoreCase(responseJson.get("success") + "")){
            JSONObject detailJson = responseJson.getJSONObject("data");
            if("yes".equalsIgnoreCase(detailJson.get("isOver") + "")){
                url = HotUtil.getDataStudioServerApiUrl() + String.format("/ide/datasource/getExecuteResult?token=%s", task.getTaskExecId());
                JSONObject execResultJson = this.invokeApi(url, null, false);
                if("true".equalsIgnoreCase(execResultJson.get("success") + "")){
                    detailJson = execResultJson.getJSONObject("data");
                    if("true".equalsIgnoreCase(detailJson.get("isSuccess") + "")){
                        result.setExecStatus(TaskExecStatus.SUCCESS.getCode());
                    }else {
                        result.setExecStatus(TaskExecStatus.FAIL.getCode());
                    }
                    result.setExecInfo(detailJson.get("msg") + "");
                }
            }
        }else {
            result.setSuccess(false);
            result.setExecInfo(responseJson.get("message") + "");
        }
         */
        return result;
    }

    protected JSONObject invokeApi(String url, Map<String, Object> parameters, boolean isPost){
        // 请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("u_token", HotUtil.getHotTableAuthorityToken());
        String response = "{}";
        if(isPost){
            response = HttpUtil.doPost(url, parameters, "application/json", headers, 60 * 1000);
        }else {
            response = HttpUtil.doGet(url, parameters, "application/json", headers, 60 * 1000);
        }
        JSONObject responseJson = new JSONObject();
        try {
            responseJson = JSONObject.parseObject(response);
        }catch (Exception e){
            e.printStackTrace();
            responseJson.put("success", "false");
            responseJson.put("message", e.getMessage());
        }
        return responseJson;
    }
}
