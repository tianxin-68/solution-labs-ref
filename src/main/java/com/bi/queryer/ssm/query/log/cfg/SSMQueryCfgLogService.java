package com.bi.queryer.ssm.query.log.cfg;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Auther: contributor
 * @Date: 2024/9/3 14:32
 * @Description: 前端日志打点服务实现
 */
@Service
@Qualifier("SSMQueryCfgLogService")
public class SSMQueryCfgLogService {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);

    @Autowired
    protected BaseDao dao;

    public void logQueryCfgEvent(String sessionId, String templateId, Map<String, Object> vo) {
        final String userName = UserManager.get().getName();
        EXECUTOR.execute(() -> {
            SSMQueryCfgLogEntity feLogEntity = buildQueryCfgLogEntity(sessionId, templateId, userName, vo);
            if (feLogEntity == null || BIUtil.isEmpty(feLogEntity.getLogContent())) {
                return;
            }
            String sqlId = "ssm.query.cfg.log.add";
            dao.update(sqlId, feLogEntity);
        });
    }

    //构造日志entity
    private SSMQueryCfgLogEntity buildQueryCfgLogEntity(String sessionId, String templateId, String userName, Map<String, Object> entity) {
        final Map<String, String> logFieldMap = getLogFieldMap();
        final Map<String, String> defaultValueMap = getLogFieldDefaultValueMap();
        final List<String> logFields = new ArrayList<>(8);
        final List<Object> logValues = new ArrayList<>(8);
        final Map<String, Object> logContent = new HashMap<>(8);
        for (Map.Entry<String, String> e : logFieldMap.entrySet()) {
            Object value = entity.get(e.getKey());
            if (value != null && !Objects.equals(value.toString(), defaultValueMap.get(e.getKey()))) {
                String valueStr = value.toString();
                logFields.add(e.getValue());
                logValues.add(valueStr);
                logContent.put(e.getValue(), valueStr);
            }
        }

        return new SSMQueryCfgLogEntity.SSMQueryCfgLogEntityBuilder()
                .sessionId(sessionId)
                .userName(userName)
                .env(BIUtil.getRuntimeEnv().getCode())
                .templateId(templateId)
                .logContent(logContent)
                .logValues(logValues)
                .logFields(logFields)
                .build();
    }

    private Map<String, String> getLogFieldMap() {
        String fieldMap = SC.v("log.query.cfg.field.map", "isMergeCell=is_merge_cell,isCompactLayouts=is_compact_layouts,isVerticalSort=is_vertical_sort,isHorizontalSort=is_horizontal_sort,conditionalFormat=conditional_format");
        return getKeyValueMap(fieldMap);
    }

    private Map<String, String> getKeyValueMap(String fieldMap) {
        Map<String, String> map = new HashMap<>();
        Arrays.stream(fieldMap.split(",")).forEach(s -> {
            String[] kv = s.split("=");
            map.put(kv[0], kv[1]);
        });
        return map;
    }

    private Map<String, String> getLogFieldDefaultValueMap() {
        String fieldMap = SC.v("log.query.cfg.field.default.value.map", "isMergeCell=0,isCompactLayouts=0,isVerticalSort=0,isHorizontalSort=0,conditionalFormat=[]");
        return getKeyValueMap(fieldMap);
    }
}
