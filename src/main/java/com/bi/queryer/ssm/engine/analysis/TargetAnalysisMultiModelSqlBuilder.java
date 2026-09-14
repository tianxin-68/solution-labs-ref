package com.bi.queryer.ssm.engine.analysis;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryFilter;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.meta.targetValue.TargetValueCacheManager;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2025/12/12 16:05
 * @Description:
 */
public class TargetAnalysisMultiModelSqlBuilder extends AnalysisMultiModelSqlBuilder {
    private IFunction FX;
    public TargetAnalysisMultiModelSqlBuilder(QueryConfigure config, QueryContext cxt, List<StarModel> models) {
        super(config, cxt, models);
        FX = FunctionManager.getFunction();
    }

    @Override
    protected BaseOperator getAnalysisOperator(AnalysisItemConfig analysisItemConfig) {
        if (analysisItemConfig != null && analysisItemConfig.isTargetValue()) {
            return OperatorFactory.getOperator(analysisItemConfig.getTargetConfig().getTargetCalcMode());
        }
        return super.getAnalysisOperator(analysisItemConfig);
    }

    @Override
    protected List<QueryField> getRealSelectFields() {
        return getSelectFields().stream()
                .filter(f -> !f.isAppend() || SSDUtil.isAggFilter(f) || f.isTargetValue())
                .collect(Collectors.toList());
    }

    @Override
    public StringBuilder buildFromClause() {
        StringBuilder fromFragment = super.buildFromClause();
        Set<String> targetMeasures = TargetValueCacheManager.getTargetMetricCode();
        Set<String> targetDims = TargetValueCacheManager.getTargetDimCode();
        targetDims.add(BIConsts.DATE_CODE);

        // 配置的目标值字段
        Set<QueryField> cfgTargetMeasures = config.getResult().getFields().stream().filter(QueryField::isTargetValue).collect(Collectors.toSet());
        if (BIUtil.isEmpty(cfgTargetMeasures)) {
            return fromFragment;
        }

        Set<String> queryDims = config.getResult().getRowDimensions().stream().filter(v -> !v.isAppend())
                .map(QueryField::getCode).collect(Collectors.toSet());

        //select
        StringBuilder targetSql = buildTargetSelectClause(queryDims, targetDims, cfgTargetMeasures, targetMeasures);

        // from
        String targetTableName = SC.v("ssm.target.value.table.name", "bi_test.mgp_entity_target_value");
        targetSql.append(" from ").append(targetTableName);

        // where
        targetSql.append(buildTargetWhereClause(queryDims, targetDims));

        // group by
        targetSql.append(buildTargetGroupByClause(queryDims, targetDims));

        String currentSubQueryAlias = BIConsts.TARGET_VALUE_TABLE_ALIAS;
        fromFragment.append(" LEFT JOIN ")
                .append(" (").append(targetSql).append(") ").append(currentSubQueryAlias);
        fromFragment.append(" ON (");

        List<String> joinExpressions = getTargetJoinExpression(currentSubQueryAlias, queryDims);

        fromFragment.append(BIUtil.listToStr(joinExpressions, " and "));
        fromFragment.append(" )");

        return fromFragment;
    }

    //构造目标值select子句
    private StringBuilder buildTargetSelectClause(Set<String> queryDims, Set<String> targetDims,
                                                  Set<QueryField> cfgTargetMeasures, Set<String> targetMeasures) {
        // 维度字段
        QuerySettings querySettings = config.getSettings();
        StringBuilder targetSql = new StringBuilder("select ");
        int i = 0;
        for (String dim : queryDims) {
            if (i > 0) {
                targetSql.append(",");
            }
            if (Objects.equals(dim, BIConsts.DATE_CODE)) {
                if (DateGranularity.MONTH.getCode().equals(querySettings.getDateGranularity())) {
                    targetSql.append("DATE_FORMAT(STR_TO_DATE(date_value, '%Y-%m'), '%Y%m')");
                } else {
                    targetSql.append("date_value");
                }
                targetSql.append(" as ").append(BIConsts.DATE_CODE);
            } else {
                if (targetDims.contains(dim)) {
                    targetSql.append(FX.quote(StringUtils.lowerCase(dim))).append(" as ").append(dim);
                } else {
                    targetSql.append("null as ").append(dim);
                }
            }
            i++;
        }

        // 指标字段
        Set<String> codes = new HashSet<>();
        for (QueryField metric : cfgTargetMeasures) {
            String rowCode = metric.getAnalysisConfig().getMeasureCode();
            String metricCode = String.format("%s_%s_value", rowCode, metric.getAnalysisConfig().getTargetConfig().getTargetCalcMode());
            if (codes.contains(metricCode)) {
                continue;
            }
            codes.add(metricCode);
            targetSql.append(",");

            // 获取对应目标值的真实code
            String targetMetricCode = getTargetCode(metric);
            String targetCode = String.format("%s_%s_value", targetMetricCode, metric.getAnalysisConfig().getTargetConfig().getTargetCalcMode());
            if (targetMeasures.contains(targetCode)) {
                targetSql.append("max(").append(StringUtils.lowerCase(targetCode)).append(") as ").append(metricCode);
            } else {
                targetSql.append("null as ").append(metricCode);
            }
        }
        return targetSql;
    }

    // where 子句
    private StringBuilder buildTargetWhereClause(Set<String> queryDims, Set<String> targetDims) {
        QuerySettings querySettings = config.getSettings();
        StringBuilder targetSql = new StringBuilder(" where ");
        // 过滤条件
        if (querySettings.isBusinessCalendar()) {
            targetSql.append("date_type = 'business'");
        } else {
            targetSql.append("date_granularity").append(" = '").append(querySettings.getDateGranularity()).append("'")
                    .append(" and ").append("date_type").append(" = 'natural'");
        }

        QueryFilter queryFilter = config.getFilter();
        List<String> filterDims = new ArrayList<>();
        for (String dim : targetDims) {
            String rawDim = StringUtils.lowerCase(dim);
            if (BIConsts.DATE_CODE.equals(dim)) {
                rawDim = "date_value";
            }
            if (queryDims.contains(dim)) {
                //QueryField field = queryFilter.getFieldByCode(dim);
            } else {
                filterDims.add(FX.quote(rawDim) + " is null");
            }
        }

        if (BIUtil.isNotEmpty(filterDims)) {
            targetSql.append(" and ").append(StringUtils.join(filterDims, " and "));
        }
        return targetSql;
    }

    // group by 子句
    private String buildTargetGroupByClause(Set<String> queryDims, Set<String> targetDims) {
        List<String> groupByDims = new ArrayList<>();
        for (String dim : targetDims) {
            String rawDim = StringUtils.lowerCase(dim);
            if (BIConsts.DATE_CODE.equals(dim)) {
                rawDim = "date_value";
            }
            if (queryDims.contains(dim)) {
                groupByDims.add(FX.quote(rawDim));
            }
        }
        if (BIUtil.isNotEmpty(groupByDims)) {
            // group by
            return " group by " + BIUtil.listToStr(groupByDims);
        }
        return "";
    }

    // 获取真是目标值code
    private String getTargetCode(QueryField metric) {
        String rowCode = metric.getAnalysisConfig().getMeasureCode();
        QueryField qf = config.getResult().getFieldByCode(rowCode);
        if (qf == null) {
            return rowCode;
        }

        // 跨模型的指标
        if (FieldType.CROSS_MODEL_MEASURE == FieldType.get(qf.getFieldType()) || FieldType.CROSS_MODEL_MEASURE == FieldType.get(qf.getMeta().getFieldType())) {
            return rowCode;
        }

        if (isAvgByDay(qf)) {
            qf = qf.getCusCalcDependFields().stream().filter(v -> !isAvgByDay(v)).findAny().orElse(qf);
        }
        String replacedCode;
        if (qf.isCustom()) {
            replacedCode = qf.getCusCalcDependFields().stream().filter(v -> !v.isCustom()).findAny().orElse(qf).getCode();
        } else {
            replacedCode = qf.getCode();
        }
        //兜底逻辑
        replacedCode = replacedCode.replaceAll("_" + AggExpressionType.Avg_By_Day_Real.getCode() + "|" + "_" + AggExpressionType.Avg_By_Day.getCode(), "");
        return replacedCode;
    }

    /**
     * 判断是不是日均
     * @return
     */
    public static boolean isAvgByDay(QueryField atomField) {
        //先判断聚合方式
        AggExpressionType aggExpressionType = AggExpressionType.get(atomField.getAggExpressionType());
        if (aggExpressionType.isAvgByDay()) {
            return true;
        }

        //通过id后缀判断是不是日均值
        if (atomField.getId().endsWith(AggExpressionType.Avg_By_Day.getCode()) ||
                atomField.getId().endsWith(AggExpressionType.Avg_By_Day_Real.getCode())) {
            return true;
        }

        return false;
    }

    private List<String> getTargetJoinExpression(String currentSubQueryAlias, Set<String> queryDims) {
        List<String> joinExpressions = new ArrayList<>();
        if (BIUtil.isEmpty(queryDims)) {
            joinExpressions.add(" 1=1 ");
        } else {
            for (String dimCode : queryDims) {
                String constValue = "'" + BIConsts.SSM_ALL + "'";
                String currentFieldExpression = fx.coalesce(currentSubQueryAlias + "." + dimCode, constValue);

                List<String> fieldCodes = new ArrayList<>();
                for (StarModel model : models) {
                    if (model.getFieldByCode(dimCode) != null) {
                        fieldCodes.add(model.getAlias() + "." + dimCode);
                    }
                }
                fieldCodes.add(constValue);

                String joinExpression = String.format("%s = %s", fx.coalesce(fieldCodes), currentFieldExpression);
                joinExpressions.add(joinExpression);
            }
        }
        return joinExpressions;
    }
}
