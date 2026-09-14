package com.bi.queryer.ssm.governance.service;

import com.bi.queryer.ssm.governance.entity.GovernanceAuditLog;
import com.bi.queryer.ssm.governance.enums.GovStatus;
import com.bi.queryer.sys.base.BaseDao;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 治理审计日志服务
 */
@Service
public class GovernanceAuditService {

    @Autowired
    private BaseDao dao;

    /**
     * 记录一条审计日志
     */
    public void log(String objectType, String objectId, String objectName, String action,
                    String fromStatus, String toStatus, String operator, String remark) {
        GovernanceAuditLog log = GovernanceAuditLog.builder()
                .objectType(objectType)
                .objectId(objectId)
                .objectName(objectName)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .operator(operator)
                .remark(remark)
                .build();
        dao.insert("ssm.governance.audit.insert", log);
    }

    /**
     * 批量记录审计日志（扫描批量打标时使用，避免逐条 insert）
     */
    public void batchLog(java.util.List<GovernanceAuditLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        for (java.util.List<GovernanceAuditLog> chunk : cn.hutool.core.collection.CollUtil.split(logs, 1000)) {
            dao.insert("ssm.governance.audit.batchInsert", chunk);
        }
    }

    /**
     * 分页查询审计日志
     */
    @SuppressWarnings("unchecked")
    public PageInfo<GovernanceAuditLog> list(String objectType, String objectId, String operator,
                                             String keyword, int pageNum, int pageSize) {
        Map<String, Object> param = new HashMap<>();
        param.put("objectType", objectType);
        param.put("objectId", objectId);
        param.put("operator", operator);
        param.put("keyword", keyword);
        param.put("startNum", (pageNum - 1) * pageSize);
        param.put("pageSize", pageSize);
        List<GovernanceAuditLog> list = (List<GovernanceAuditLog>) dao.queryObjectList("ssm.governance.audit.list", param);
        // 前状态为 pending（待处理初始态）时不展示，归一化为 null
        if (list != null) {
            for (GovernanceAuditLog l : list) {
                if (GovStatus.PENDING.getCode().equals(l.getFromStatus())) {
                    l.setFromStatus(null);
                }
            }
        }
        Integer total = dao.queryCount("ssm.governance.audit.listCount", param);
        PageInfo<GovernanceAuditLog> page = new PageInfo<>(list);
        page.setTotal(total == null ? 0 : total);
        return page;
    }
}
