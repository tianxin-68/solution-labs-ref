package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.SingleModelBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author contributor
 * @Date 09:26 2023-09-26
 * @Description 分析场景的日均值sql构建器包装类
 **/
public class AnalysisSingleModelSqlBuilderAvgWrapper extends SingleModelBuilderAvgWrapper {

    protected AnalysisSingleModelSqlBuilder defaultAnalysisSingleModelSqlBuilder = null;

    public AnalysisSingleModelSqlBuilderAvgWrapper(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    public AnalysisSingleModelSqlBuilderAvgWrapper(SingleModelSqlBuilder singleModelSqlBuilder) {
        super(singleModelSqlBuilder);
        this.defaultAnalysisSingleModelSqlBuilder = (AnalysisSingleModelSqlBuilder) singleModelSqlBuilder;
    }


    /**
     * // sum指标 + 无日均指标
     * @return
     */
    protected String buildCommonAggSql(){
        commonFields.forEach(f->this.setFieldVirtual(f,false));
        distinctFields.forEach(f->this.setFieldVirtual(f,true));

        if(BIUtil.isEmpty(commonFields) && BIUtil.isEmpty(config.getResult().getRowDimensions()) && BIUtil.isEmpty(config.getResult().getColDimensions())) {
            return getEmptySql();
        }

        // 当后台配置的计算指标的原子指标，此原子指标同时在distinct列表中时，需要将此原子指标改Virtual=false，避免sql查询报错
        for(QueryField distinctField : commonFields){
            Set<QueryField> calcAtomFields = distinctField.getCalcAtomFields();
            if(BIUtil.isEmpty(calcAtomFields)){
                continue;
            }

            calcAtomFields.forEach(atomField -> {
                if(distinctFields.contains(atomField)){
                    atomField.setVirtual(false);
                }
                // 原子字段在distinct中的字段的原子字段列表中出现，也同时改为false
                // 避免场景：2C销售额B0-ARPU，同时设置原值+日均值，会导致在计算日均时将其原子字段设置为虚拟字段
                distinctFields.forEach(df -> {
                    if(df.getCalcAtomFields().contains(atomField)){
                        atomField.setVirtual(false);
                    }
                });
            });
        }

        AnalysisSingleModelSqlBuilder singleModelSqlBuilder = new AnalysisSingleModelSqlBuilder(config, cxt);
        singleModelSqlBuilder.copyPropertiesFrom(defaultAnalysisSingleModelSqlBuilder);

        String sql = singleModelSqlBuilder.build(model);
        return sql;
    }

    /**
     * 按日去重
     * @return
     */
    protected String buildDistinctAggSql(){
        commonFields.forEach(f->this.setFieldVirtual(f,true));
        distinctFields.forEach(f->this.setFieldVirtual(f,false));

        // 当前模型的所有字段
        Map<String, QueryField> modelAllFields = new HashMap<>();
        this.model.getTables().forEach(t -> {
            t.getFields().forEach(f->modelAllFields.put(f.getCode(), f));
        });

        // 当后台配置的计算指标的原子指标，此原子指标同时在common列表中时，需要将此原子指标改Virtual=false，避免sql查询报错
        for(QueryField distinctField : distinctFields){
            Set<QueryField> calcAtomFields = distinctField.getCalcAtomFields();
            if(BIUtil.isEmpty(calcAtomFields)){
                continue;
            }
            calcAtomFields.forEach(atomField -> {
                if(commonFields.contains(atomField)){
                    atomField.setVirtual(false);
                    // 同步更新模型对应字段：因为计算字段的原字段在日均值和非日均时是独立的，不是同一个对象
                    if(modelAllFields.containsKey(atomField.getCode())){
                        modelAllFields.get(atomField.getCode()).setVirtual(false);
                    }
                }

                // 原子字段在distinct中的字段的原子字段列表中出现，也同时改为false
                // 避免场景：2C销售额B0-ARPU，同时设置原值+日均值，会导致在计算日均时将其原子字段设置为虚拟字段
                commonFields.forEach(df -> {
                    if(df.getCalcAtomFields().contains(atomField)){
                        atomField.setVirtual(false);

                        // 同步更新模型对应字段：因为计算字段的原字段在日均值和非日均时是独立的，不是同一个对象
                        if(modelAllFields.containsKey(atomField.getCode())){
                            modelAllFields.get(atomField.getCode()).setVirtual(false);
                        }
                    }
                });
            });
        }

        String sql = "";
        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        boolean hasTotal = totalConfig != null && totalConfig.isActive();

        // 是否需要总计日均
        boolean needTotalAvg = true;
        QueryField commonDateField = config.getFilterCommonDateField();
        if(commonDateField == null){
            needTotalAvg = false;
        }

        /** 废弃
         * 原因：日粒度不汇总，但有列总计时也需要计算日均
        if(!commonDateField.isAggQuery() && DateGranularity.DAY == DateGranularity.get(commonDateField.getQueryDateGranularity())){
            needTotalAvg = false;
        }*/

        boolean hasCountDistinct = distinctFields.stream().filter(f -> f.isMeasure() && !f.isVirtual()).anyMatch(v -> BIUtil.isCountDistinctAggExpression(v.getMeta().getAggExpression()));

        needTotalAvg = needTotalAvg && hasTotal;
        if(needTotalAvg && hasCountDistinct){
            // 先禁用汇总：获取明细日均
            totalConfig.setIsActive(Enabled.NO.getId());
            AnalysisSingleModelSqlBuilder analysisSingleModelSqlBuilder = new AnalysisSingleModelSqlBuilder(config, cxt);
            analysisSingleModelSqlBuilder.copyPropertiesFrom(defaultAnalysisSingleModelSqlBuilder);
            String detailSql = analysisSingleModelSqlBuilder.build(model);

            // 再开启汇总：只获取总计日均（去掉明细数据）
            totalConfig.setIsActive(Enabled.YES.getId());
            analysisSingleModelSqlBuilder = new AnalysisSingleModelAvgTotalSqlBuilder(config, cxt);
            analysisSingleModelSqlBuilder.copyPropertiesFrom(defaultAnalysisSingleModelSqlBuilder);
            String totalSql = analysisSingleModelSqlBuilder.build(model);

            sql = String.format("%s union all %s", detailSql, totalSql);

        }else {
            AnalysisSingleModelSqlBuilder analysisSingleModelSqlBuilder = new AnalysisSingleModelSqlBuilder(config, cxt);
            analysisSingleModelSqlBuilder.copyPropertiesFrom(defaultAnalysisSingleModelSqlBuilder);
            sql = analysisSingleModelSqlBuilder.build(model);
        }
        return sql;
    }

    public AnalysisSingleModelSqlBuilder getDefaultAnalysisSingleModelSqlBuilder() {
        return defaultAnalysisSingleModelSqlBuilder;
    }

    public void setDefaultAnalysisSingleModelSqlBuilder(AnalysisSingleModelSqlBuilder defaultAnalysisSingleModelSqlBuilder) {
        this.defaultAnalysisSingleModelSqlBuilder = defaultAnalysisSingleModelSqlBuilder;
    }
}
