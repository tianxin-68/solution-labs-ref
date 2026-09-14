package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.SingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilder;
import com.bi.queryer.ssm.engine.analysis.AnalysisSingleModelSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.FieldFilterMode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:48 2024-08-08
 * @Description lod单模型日均sql构建器包装类
 **/
public class LodSingleModelSqlBuilderAvgWrapper extends AnalysisSingleModelSqlBuilderAvgWrapper {

    protected LodQueryConfigureItem item = null;

    public LodSingleModelSqlBuilderAvgWrapper(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    public LodSingleModelSqlBuilderAvgWrapper(SingleModelSqlBuilder singleModelSqlBuilder) {
        super(singleModelSqlBuilder);
        this.defaultAnalysisSingleModelSqlBuilder = (AnalysisSingleModelSqlBuilder) singleModelSqlBuilder;
        this.item = ( (LodSingleModelSqlBuilder) this.defaultAnalysisSingleModelSqlBuilder).getItem();
    }

    @Override
    public String build(StarModel model) {
        LodSingleModelSqlBuilder lodSingleModelSqlBuilder = (LodSingleModelSqlBuilder) this.defaultAnalysisSingleModelSqlBuilder;
        if(!this.item.getAggExpressionType().isAvgByDay()){
            return lodSingleModelSqlBuilder.build(model);
        }

        // 若是日均值，此处因获取日均值相关sql
        this.model = model;
        this.prepare();
        String sql = super.buildDistinctAggSql();
        item.setSql(sql);
        item.getFragments().setSelectFragments(lodSingleModelSqlBuilder.buildSelectFragments(model));

        String aggSql = lodSingleModelSqlBuilder.buildLodAggSql(item);

        // 再次设置其sql
        item.setSql(aggSql);
        return aggSql;
    }

    @Override
    protected void prepare() {
        List<QueryField> resultMeasureFields = model.getFields().stream().filter(f->f.getIsResult() && f.isMeasure()).collect(Collectors.toList());

        // 日粒度且不汇总，则不处理
        QueryField commonDateField = config.getFilterCommonDateField();
        if(commonDateField == null){
            return;
        }
        /**
         * 废弃
         * 原因：日粒度不汇总，但有列总计时也需要计算日均
        if(!commonDateField.isAggQuery() && DateGranularity.DAY == DateGranularity.get(commonDateField.getQueryDateGranularity())){
            return;
        }
         */
        for(QueryField f : resultMeasureFields ){
            if(f.isVirtual()){
                continue;
            }
            distinctFields.add(f);
        }

        List<QueryField> filterMeasureFields = model.getFields().stream().filter(f->f.getIsResult() && f.isMeasure()).collect(Collectors.toList());
        for (QueryField field : filterMeasureFields) {
            if (!model.isExistsByMetaCode(field)) {
                continue;
            }
            if (!field.isActive()) {
                continue;
            }
            //不是度量字段直接跳过
            if (!field.isMeasure()) {
                continue;
            }

            //由于模板可能存在没有FilterValueMode的情况
            if (FieldFilterMode.detail == FieldFilterMode.get(field.getFilterValueMode())) {
                continue;
            }

            //************正式开始装配sql****************
            List<FieldValue> values = field.getValues();
            if (values == null || values.isEmpty()) {
                continue;
            }
            havingFilterFields.add(field);
        }
    }
}
