package com.bi.queryer.ssm.engine.accelerate.hot.repair.executor;

import com.bi.queryer.ssm.engine.accelerate.hot.HotTableCacheManager;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 19:44 2024/10/12
 * @Description doris执行器
 **/
public class DorisScriptExecutor extends BaseScriptExecutor {
    public ScriptExecuteResult execute(HotTableRepairTask task) {
        ScriptExecuteResult result = new ScriptExecuteResult();
        String scriptContent = task.getTaskScriptContent();
        if(BIUtil.isEmpty(scriptContent)){
            result.setSuccess(true);
            return result;
        }
        HotTableInfo hotTable = HotTableCacheManager.getHotTablesBySource(task.getSourceTableName(), DBType.Doris);
        if(hotTable == null){
            result.setSuccess(false);
            result.setExecInfo("热表不存在");
            return result;
        }
        // 获取doris热表
        String execId = String.format("%s_%s_%s", hotTable.getEtlJobName(), task.getTaskBatchNo(), System.currentTimeMillis());
        scriptContent = StringUtils.replaceOnce(scriptContent,"${v_label}", execId);

        List<String> sqlList = BIUtil.splitScriptBySemicolon(scriptContent);
        BaseDao dao = DBUtil.getBaseDao();
        try {
            for(String sql : sqlList) {
                if(sql.contains("splitflag=load")){
                    continue;
                }
                dao.executeSqlByDoris(sql, DataSourceType.Doris_Master);
            }
            // 查看是否提交成功
            String sql = String.format("show load from bi_hot where label = '%s'", execId);
            List<Map> list = dao.queryObjectListByDorisSQL(sql, DataSourceType.Doris_Master);
            if(BIUtil.isEmpty(list)){
                result.setSuccess(false);
            }else {
                result.setSuccess(true);
            }
        }catch (Exception e){
            e.printStackTrace();
            result.setSuccess(false);
            result.setExecInfo(e.getMessage());
        }
        result.setExecId(execId);
        return result;
    }

    @Override
    public ScriptExecuteResult kill(HotTableRepairTask task) {
        ScriptExecuteResult result = new ScriptExecuteResult();
        result.setSuccess(true);
        BaseDao dao = DBUtil.getBaseDao();
        try {
            String sql = String.format("cancel load from bi_hot where label = '%s'", task.getTaskExecId());
            dao.executeSqlByDoris(sql, DataSourceType.Doris_Master);
            result.setSuccess(true);
        }catch (Exception e){
            e.printStackTrace();
            result.setSuccess(false);
            result.setExecInfo(e.getMessage());
        }
        return result;
    }

    @Override
    public ScriptExecuteResult getProgress(HotTableRepairTask task) {
        ScriptExecuteResult result = new ScriptExecuteResult();
        // 默认为running
        result.setExecStatus(TaskExecStatus.RUNNING.getCode());
        result.setSuccess(true);
        BaseDao dao = DBUtil.getBaseDao();
        try {
            String sql = String.format("show load from bi_hot where label = '%s'", task.getTaskExecId());
            List<Map> list = dao.queryObjectListByDorisSQL(sql, DataSourceType.Doris_Master);
            if(BIUtil.isNotEmpty(list)){
                Map map = list.get(0);
                String state = map.get("State") + "";
                if("FINISHED".equalsIgnoreCase(state)){
                    result.setExecStatus(TaskExecStatus.SUCCESS.getCode());
                }
                String errorInfo = "";
                if("CANCELLED".equalsIgnoreCase(state)){
                    result.setExecStatus(TaskExecStatus.FAIL.getCode());
                    errorInfo = map.get("ErrorMsg") + "";
                }
                String progress = map.get("Progress") + "";
                result.setExecInfo(progress + ":" + errorInfo);
                result.setExecEndTime(map.get("LoadFinishTime") + "");
            }
            result.setSuccess(true);
        }catch (Exception e){
            e.printStackTrace();
            result.setSuccess(false);
            result.setExecInfo(e.getMessage());
        }
        return result;
    }
}
