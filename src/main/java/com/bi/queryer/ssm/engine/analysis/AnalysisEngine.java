package com.bi.queryer.ssm.engine.analysis;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.*;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cross.AnalysisCrossDimensionResultDataSetRowBuilder;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.analysis.operator.impl.TotalOperator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.cross.CrossDimensionQueryEngine;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:35 2023-06-12
 * @Description 分析引擎
 **/
public class AnalysisEngine extends QueryEngine {

    protected QueryEngine queryEngine = null;

    protected String analysisSql = "";

    public AnalysisEngine(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
        this.initAnalysisConfig();
        this.queryEngine = this.createQueryEngine();
    }

    public QueryEngine createQueryEngine(){
        QueryEngine engine = null;
        if(config != null && BIUtil.isNotEmpty(config.getResult().getColDimensions())) {
            engine = new CrossDimensionQueryEngine(config, cxt);
            engine.setRowBuilder(new AnalysisCrossDimensionResultDataSetRowBuilder(engine));
        }else {
            engine = new QueryEngine(config, cxt);
            engine.setRowBuilder(new AnalysisResultDataSetRowBuilder(engine));
        }
        return engine;
    }

    protected void initAnalysisConfig(){
        // 先查询有哪些计算方式
        Set<String> calcModeSet = new HashSet<>();
        config.getResult().getMeasures().stream().filter(f->Enabled.value(f.getIsAnalysis())).forEach(f->{
            calcModeSet.add(f.getAnalysisConfig().getCalcMode());
        });
        this.initAnalysisConfig(calcModeSet);

        // 总计计算方式
        Set<String> totalCalcModeSet = new HashSet<>();
        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        if(totalConfig.isActive()){
            totalConfig.getItems().stream().filter(f->f.isActive()).forEach(f->{
                totalCalcModeSet.add(f.getCalcMode());
            });
        }
        this.initAnalysisConfig(totalCalcModeSet);

    }

    protected void initAnalysisConfig(Set<String> calcModeSet){
        // 需按枚举值的顺序进行初始化
        for(AnalysisCalcMode calcMode : AnalysisCalcMode.values()){
            if(!calcModeSet.contains(calcMode.getCode())){
                continue;
            }
            BaseOperator operator = OperatorFactory.getOperator(calcMode);
            if(operator != null) {
                operator.initQueryConfig(config, cxt);
            }
        }
    }

    /**
     * 实际执行调用queryEngine：便于动态支持普通查询 + 列维度查询的dataset结构创建
     * @param sql
     * @param executeParameter
     * @return
     */
    @Override
    public ResultDataSet execute(String sql, QueryExecuteParameter executeParameter) {
        AnalysisTotalConfig totalConfig = config.getAnalysis().getTotal();
        ResultDataSet resultDataSet = new ResultDataSet();

        List<ResultDataSetColumn> columns = executeParameter.getColumns();
        Integer maxRowCount = executeParameter.getMaxRowCount();
        String sessionId = executeParameter.getSessionId();

        // 总计时需要额外添加列
        if(totalConfig.isActive()){
            List<AnalysisTotalType> totalTypes = totalConfig.getItems().stream().map(c->c.getTotalType()).collect(Collectors.toList());
            List<ResultDataSetColumn> totalLeafColumns = new ArrayList<>();
            List<ResultDataSetColumn> totalRootColumns = new ArrayList<>();
            for(AnalysisTotalType totalType : totalTypes){
                TotalOperator totalOperator = OperatorFactory.getTotalOperator(totalType);
                if(totalOperator != null) {
                    totalLeafColumns.addAll(totalOperator.createTotalLeafColumns(config));

                    //添加时需要去重
                    List<ResultDataSetColumn> totalColumns = totalOperator.createTotalColumns(config);
                    for (ResultDataSetColumn totalColumn : totalColumns) {
                        if (totalRootColumns.contains(totalColumn)) {
                            continue;
                        }
                        totalRootColumns.add(totalColumn);
                    }
                }
            }
            columns.removeAll(totalLeafColumns);
            columns.addAll(totalLeafColumns);

            resultDataSet = this.queryEngine.execute(sql, new QueryExecuteParameter(maxRowCount, sessionId, columns)); //this.queryEngine.execute(sql, maxRowCount, queryId, columns);

            // 再次确认最终的结果集中是否有总计列，若没有再次加入：因有列维度时，数据集列按父子关系重新构建，导致总计列丢失
//            resultDataSet.getColumns().removeAll(totalLeafColumns);
//            resultDataSet.getColumns().addAll(totalLeafColumns);

            resultDataSet.getColumns().removeAll(totalLeafColumns);
            resultDataSet.getColumns().removeAll(totalRootColumns);
            resultDataSet.getColumns().addAll(totalRootColumns);
        }else {
            resultDataSet = this.queryEngine.execute(sql, new QueryExecuteParameter(maxRowCount, sessionId, columns));//this.queryEngine.execute(sql, maxRowCount, queryId, columns);
        }

        AnalysisResultDataSetBuilder dataSetBuilder = new AnalysisResultDataSetBuilder(config, resultDataSet, cxt);
        dataSetBuilder.build();

        return resultDataSet;
    }

    @Override
    public int getLimitRow() {
        Integer searchLimit = this.queryEngine.getLimitRow();
        return searchLimit;
    }

    /**
     * 实际执行调用queryEngine，此处必须重写，否则无法正常记录日志！！！
     * @param dataSet
     */
    @Override
    public void post(ResultDataSet dataSet) {
        // 因实际查询时在queryEngine中执行，其日志属于queryEngine中，需要将此日志最终存储依然需要使用queryEngine
        this.queryEngine.post(dataSet);
    }

    @Override
    public String buildSql() throws BIException {
        if (isBuilt) {
            return analysisSql;
        }
        // 获取查询引擎
        this.queryEngine = this.getQueryEngine();

        AnalysisSqlBuilder sqlBuilder = this.createAnalysisSqlBuilder(config, cxt, queryEngine); //new AnalysisSqlBuilder(config, cxt, queryEngine);
        String sql = sqlBuilder.build();

        analysisSql = this.getSqlTips() + " " + sql;
        isBuilt = true;

        // 设置sql片段
        this.sqlFragments = new QuerySqlFragments(sqlBuilder.getSelectFragments());

        return analysisSql;
    }

    public static void main(String[] args) {
        String splitStr = "ORDER BY /*sort*/"; // SingleModelSqlBuilder.ORDER_BY;
        String sql = "select 1 from t " + splitStr + " 1";
        int index = sql.indexOf(splitStr);
        String str1 = sql.substring(0, index);
        System.out.println(str1);
        String str2 = sql.substring(index, sql.length());
        System.out.println(str2);
        String str = String.format("%s = %s",1,2);
        System.out.println(str);
    }

    public QueryEngine getQueryEngine() {
        return queryEngine;
    }

    public void setQueryEngine(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    public QueryEngineType getType(){
        return QueryEngineType.Analysis;
    }

    protected AnalysisSqlBuilder createAnalysisSqlBuilder(QueryConfigure config, QueryContext cxt, QueryEngine rawEngine){
        AnalysisSqlBuilder sqlBuilder = new AnalysisSqlBuilder(config, cxt, rawEngine);
        return sqlBuilder;
    }

}
