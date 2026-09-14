package com.bi.queryer.ssm.engine.interceptor;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.interceptor.partition.PartitionLimitTable;
import com.bi.queryer.ssm.engine.session.QuerySession;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 11:52 2025/12/5
 * @Description 查询分区限制拦截器
 **/
@Component
@Order(1)
public class QueryPartitionLimitInterceptor extends QueryInterceptor{

    protected static Map<String, PartitionLimitTable> partitionLimitTablesCache = new HashMap<>(16);
    @Override
    public void refreshConfigureCache() {
        partitionLimitTablesCache.clear();
        BaseDao dao = DBUtil.getBaseDao();
        List<PartitionLimitTable> tables = dao.queryObjectList("ssm.session.getAllPartitionLimitTable", null, PartitionLimitTable.class);
        if(tables != null){
            tables.stream().forEach(t -> partitionLimitTablesCache.put(t.getTableName().toLowerCase(), t));
        }
    }

    @Override
    public QueryInterceptResult intercept(QuerySession querySession, QueryConfigure queryConfigure) {
        QueryInterceptResult result = new QueryInterceptResult();
        if(BIUtil.isEmpty(partitionLimitTablesCache) || BIUtil.isEmpty(querySession.getTableNames())){
            return result;
        }
        if(!"true".equalsIgnoreCase(SC.v("ssm.query.partition.limit.enable", "true"))){
            return result;
        }
        // 获取当前日期查询的时间范围长度
        QueryField commonDateField = queryConfigure.getFilterCommonDateField();
        if (commonDateField == null) {
            return result;
        }

        List<FieldValue> dateValues = commonDateField.getValues();

        // 业务日历,逻辑处理
        if(queryConfigure.getSettings().isBusinessCalendar()) {
            List<String> promotionFilterDateList = PromotionManager.getFilterDateList(commonDateField, queryConfigure);
            List<FieldValue> promotionValues = new ArrayList<>();
            promotionValues.add(new FieldValue(promotionFilterDateList.get(0), promotionFilterDateList.get(0)));
            promotionValues.add(new FieldValue(promotionFilterDateList.get(promotionFilterDateList.size() - 1), promotionFilterDateList.get(promotionFilterDateList.size() - 1)));
            dateValues = promotionValues;
        }

        List<FieldValue> dayValues = FieldUtil.getDateFieldFilterValues(commonDateField, dateValues,queryConfigure);
        if(BIUtil.isEmpty(dayValues)){
            return result;
        }

        List<Integer> maxPartitionCounts = new ArrayList<>();
        for(String queryTableName : querySession.getTableNames()){
            PartitionLimitTable partitionLimitTable = partitionLimitTablesCache.get(queryTableName.toLowerCase());
            if(partitionLimitTable == null){
                continue;
            }
            Integer maxPartitionCount = partitionLimitTable.getMaxPartitionCount();
            // 计算天数
            Long days = Math.abs(DateUtil.betweenDay(DateUtil.parseDate(dayValues.get(0).getId()), DateUtil.parseDate(dayValues.get(dayValues.size() - 1).getId()), false)) + 1;
            if(days.intValue() > maxPartitionCount){
                maxPartitionCounts.add(maxPartitionCount);
            }
        }
        if(BIUtil.isNotEmpty(maxPartitionCounts)) {
            result = new QueryInterceptResult(true, BIConsts.SSM_ERROR_TOO_MANY_PARTITION + "，请将日期范围调整到" + maxPartitionCounts.stream().sorted().findFirst().get() + "天内。若有疑问请联系[contributor]。", BIException.CODE_WARN);
            return result;
        }
        return result;
    }
}
