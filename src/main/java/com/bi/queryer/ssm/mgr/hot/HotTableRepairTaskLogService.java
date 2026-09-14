package com.bi.queryer.ssm.mgr.hot;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTask;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableRepairTaskLog;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 15:52 2024/10/11
 * @Description TODO
 **/
@Service
@Scope("prototype")
public class HotTableRepairTaskLogService {
    @Autowired
    protected BaseDao dao = null;

    protected String sqlIdPrefix = "ssm.hot.table.repair.task.log.";

    public void add(HotTableRepairTaskLog newLog){
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", newLog.getTaskId());
        // 先删除
        dao.delete(sqlIdPrefix + "deleteById", params, DataSourceType.Default);
        // 再插入
        dao.insert(sqlIdPrefix + "add", newLog, DataSourceType.Default);
    }

    /**
     * 取消
     * @param sourceTableName
     */
    public void cancelBySourceTable(String sourceTableName){
        Map<String, Object> params = new HashMap<>();
        params.put("sourceTableName", sourceTableName);
        dao.update("ssm.hot.table.repair.task.log.cancelBySourceTable", params, DataSourceType.Default);
    }

    public void updateStatusBySourceTable(String sourceTableName, String execStatus){
        Map<String, Object> params = new HashMap<>();
        params.put("sourceTableName", sourceTableName);
        params.put("execStatus", execStatus);
        dao.update("ssm.hot.table.repair.task.log.updateStatusBySourceTable", params, DataSourceType.Default);
    }

    public void updateStatusByTask(HotTableRepairTask task){
        Map<String, String> params = new HashMap<>();
        params.put("taskId", task.getTaskId());
        params.put("execStatus", task.getTaskExecStatus());
        params.put("execInfo", task.getTaskExecInfo());
        params.put("execEndTime", DateUtil.date().toString("yyyy-MM-dd HH:mm:ss"));
        dao.update("ssm.hot.table.repair.task.log.updateStatusById", params, DataSourceType.Default);
    }

    public void updateStatusById(List<String> taskIdList, String execStatus, String execInfo){
        Map<String, Object> params = new HashMap<>();
        params.put("taskIdList", taskIdList);
        params.put("execStatus", execStatus);
        params.put("execInfo", execInfo);
        dao.update("ssm.hot.table.repair.task.log.updateStatusById", params, DataSourceType.Default);
    }

    public void updateDataCheckStatusByTask(HotTableRepairTask task){
        dao.update("ssm.hot.table.repair.task.log.updateDataCheckStatusByTask", task, DataSourceType.Default);
    }

    public void updateMinDateStatus(List<String> taskIdList, Integer isHotMinDateUpdated){
        if(BIUtil.isEmpty(taskIdList)){
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("taskIdList", taskIdList);
        params.put("isHotMinDateUpdated", isHotMinDateUpdated);
        dao.update("ssm.hot.table.repair.task.log.updateMinDateStatus", params, DataSourceType.Default);
    }
}
