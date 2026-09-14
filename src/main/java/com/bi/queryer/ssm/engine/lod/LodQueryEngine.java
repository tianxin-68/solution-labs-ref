package com.bi.queryer.ssm.engine.lod;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.*;
import com.bi.queryer.ssm.engine.analysis.cfg.ctr.AnalysisContributionRateItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisZbThbItemConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.lod.calc.LodCalcManager;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 14:37 2023-12-18
 * @Description lod表达式指标查询引擎
 **/
public class LodQueryEngine extends QueryEngine {

    // 所有lod字段查询配置
    protected LodQueryConfigure lodQueryConfigure = null;

    // lod二次计算字段管理器
    protected LodCalcManager lodCalcMgr = null;

    public LodQueryEngine(QueryConfigure template, QueryContext cxt) {
        super(template, cxt);
        this.setRowBuilder(new LodResultDataSetRowBuilder(this));
    }

    /**
     * 重写创建模型：用于获取主+lod的所有模型，便于判断数据源
     * @return
     */
    @Override
    public List<StarModel> createModels() {
        List<StarModel> lodModels = new ArrayList<>();
        // 重新创建查询配置，避免查询配置重复使用
        QueryConfigure configure = this.config.clone();
        configure.load();
        configure.getSettings().setIsAgentQuery(this.config.getSettings().getIsAgentQuery());
        configure.getSettings().setEnableCreateAllTableBySameCodeOpt(this.config.getSettings().isEnableCreateAllTableBySameCodeOpt());

        LodQueryConfigureCreator lodQueryConfigureCreator = new LodQueryConfigureCreator(configure, this.cxt);
        LodQueryConfigure lodQueryConfigure = lodQueryConfigureCreator.create();
        List<LodQueryConfigureItem> items = lodQueryConfigure.getItems();
        for (LodQueryConfigureItem item : items) {
            QueryEngine engine = QueryFactory.createEngine(item.getConfig(), this.cxt);
            lodModels.addAll(engine.getModels());
        }
        return lodModels;
    }

    @Override
    public String buildSql() throws BIException {
        LodQueryConfigureCreator configureCreator = new LodQueryConfigureCreator(this.config, this.cxt);
        this.lodQueryConfigure = configureCreator.create();
        this.lodCalcMgr = configureCreator.getLodCalcMgr();

        LodQuerySqlBuilder sqlBuilder = new LodQuerySqlBuilder(lodQueryConfigure, cxt, this.lodCalcMgr);

        String sql = sqlBuilder.build();

        sql = this.getSqlTips() + sql;

        buildPivotConfig();

        return sql;
    }

    /**
     * 构建pivot配置,将主视图的转置配置赋值到config中
     */
    public void buildPivotConfig() {
        for (LodQueryConfigureItem item : this.lodQueryConfigure.getItems()) {
            if (!item.isMainConfig()) {
                continue;
            }

            this.config.getResult().getPivotConfig().setColDimValues(
                    item.getConfig().getResult().getPivotConfig().getColDimValues()
            );
        }
    }

    /*
    @Override
    public ResultDataSet execute(String sql, Integer maxRowCount, String queryId, List<ResultDataSetColumn> columns) {
        List<ResultDataSetColumn> leafColumns = new ArrayList<>();
        // 将有层级结构的列，获取所有叶子列
        this.buildLeafColumns(columns, leafColumns);

        ResultDataSet dataSet = super.execute(sql, maxRowCount, queryId, leafColumns);
        dataSet.setColumns(columns);
        return dataSet;
    }
     */

    @Override
    public ResultDataSet execute(String sql, QueryExecuteParameter executeParameter) {
        List<ResultDataSetColumn> leafColumns = new ArrayList<>();
        // 将有层级结构的列，获取所有叶子列
        this.buildLeafColumns(executeParameter.getColumns(), leafColumns);

        // 对于lod二次计算的分析字段处理过程中，额外引入相关同环比的原子字段，需要按配置隐藏掉，便于下一步封装数据集
        leafColumns = lodCalcMgr.removeAppendDataSetColumns(executeParameter.getColumns(), leafColumns);

        List<ResultDataSetColumn> resultColumns = executeParameter.getColumns();

        // 按叶子节点封装数据集
        executeParameter.setColumns(leafColumns);
        ResultDataSet dataSet = super.execute(sql, executeParameter);

        // 去掉隐藏列
        resultColumns = lodCalcMgr.removeHiddenDataSetColumns(resultColumns);
        dataSet.setColumns(resultColumns);

        return dataSet;
    }

    @Override
    public int getLimitRow() {
        Integer searchLimit = super.getLimitRow();

        //交叉表 limit 数量需要乘以列维度的数量
        int colDimSize = this.config.getResult().getPivotConfig().getColDimValues().size();
        if (colDimSize > 0) {

            //sql转置时，不需要扩大数量
            if (!SSDUtil.isQueryPivot(this.config)) {
                String username = SC.v("ssm.search.limit.special.username", "chenmin");
                if (StrUtil.isNotEmpty(username)) {
                    if (username.contains(UserManager.get().getName())) {
                        String specialColSize = SC.v("ssm.search.limit.special.col.size", "200");
                        colDimSize = Integer.valueOf(specialColSize);
                        ;
                    }
                }

                searchLimit = searchLimit * (colDimSize + 1);
            }

        }

        return searchLimit;
    }

    @Override
    public List<ResultDataSetColumn> buildColumns() {
        List<LodQueryConfigureItem> items = lodQueryConfigure.getItems();
        List<ResultDataSetColumn> finalColumns = new ArrayList<>();

        try {
            for (LodQueryConfigureItem item : items) {
                QueryEngine itemEngine = QueryFactory.createEngine(item.getConfig(), this.cxt);
                String noDataSql = item.getSql().replace(this.getSqlTips(), "");
                noDataSql = String.format("%s select _lod_h.* from (%s) _lod_h where 1=2", this.getSqlTips(), noDataSql);

                String sessionId = cxt.getUser().getName() + "_lod_header_" + System.currentTimeMillis();
                //ResultDataSet dataSet = itemEngine.execute(noDataSql, -1, sessionId);
                ResultDataSet dataSet = itemEngine.execute(noDataSql, new QueryExecuteParameter(-1, sessionId, itemEngine.buildColumns()));

                List<ResultDataSetColumn> columns = dataSet.getColumns();

                if (!item.isMainConfig()) {
                    // 子视图：只添加额外补充列
                    List<QueryField> subAggDimensions = item.getAggToMainLevelDimensions();
                    Set<String> subAggDimCodes = subAggDimensions.stream().map(QueryField::getCode).collect(Collectors.toSet());
                    List<ResultDataSetColumn> newColumns = new ArrayList<>();
                    for (ResultDataSetColumn c : columns) {
                        QueryArea queryArea = QueryArea.get(c.getRawQueryArea());
                        if (queryArea != QueryArea.RowDimension) {
                            newColumns.add(c);
                        } else if (subAggDimCodes.contains(c.getRawCode())) {
                            newColumns.add(c);
                        }
                    }
                    columns = newColumns;
                }

                for (ResultDataSetColumn c : columns) {
                    if (!finalColumns.contains(c)) {
                        finalColumns.add(c);
                    }
                }
            }
        }catch (Throwable e){
            System.out.println("获取lod列失败：" + e.getMessage());
        }

        List<ResultDataSetColumn> lodCalcColumns = lodCalcMgr.createResultDataSetColumns(this);
        finalColumns.addAll(lodCalcColumns);

        //添加带分析指标的四则运算列
        List<QueryField> analysisCalcFields = this.lodQueryConfigure.getAnalysisCalcFields();
        finalColumns.addAll(createAnalysisCalcDataSetColumns(this, analysisCalcFields));

        double index = 0;
        Map<String, ResultDataSetColumn> finalColumnMap = new HashMap<>(); // 只存储父级节点列
        for(ResultDataSetColumn col :finalColumns){
            col.setSortId(index++);
            finalColumnMap.put(col.getRawCode(), col);
        }

        // 调整指标列顺序
        QueryConfigure rawQueryConfigure = this.lodQueryConfigure.getRawConfig();
        List<QueryField> queryMeasures = rawQueryConfigure.getResult().getMeasures();
        index = 100;
        for(QueryField measure : queryMeasures){
            ResultDataSetColumn col = finalColumnMap.get(measure.getRawCode());
            if(col != null){
                col.setSortId(index++);
            }
        }

        Collections.sort(finalColumns);

        return finalColumns;
    }


    /**
     * 创建结果集列
     * @return
     */
    public List<ResultDataSetColumn> createAnalysisCalcDataSetColumns(QueryEngine engine, List<QueryField> analysisCalcFields) {
        List<ResultDataSetColumn> columns = new ArrayList<>();
        List<QueryField> calcFields = analysisCalcFields.stream()
                .filter(v -> !Enabled.YES.getId().equals(v.getIsAnalysis()))
                .filter(v -> Enabled.value(v.getIsShow()))
                .collect(Collectors.toList());

        Map<String, List<QueryField>> analysisMap = new HashMap<>(8);
        for (QueryField measure : calcFields) {
            for (QueryField analysisField : analysisCalcFields) {
                if (measure.getCode().equals(analysisField.getCode())) {
                    continue;
                }
                if (analysisField.getCode().startsWith(measure.getCode())) {
                    List<QueryField> list = analysisMap.computeIfAbsent(measure.getCode(), k -> new ArrayList<>(4));
                    list.add(analysisField);
                }
            }
        }

        Map<String, String> aclCodes = engine.getCxt().getAclFields();
        for (QueryField calcMeasure : calcFields) {
            ResultDataSetColumn calcColumn = engine.createDataSetColumn(calcMeasure, calcMeasure.getCode());
            boolean hasAuth = aclCodes.containsKey(calcMeasure.getRawCode());
            calcColumn.setHasAuth(hasAuth);

            if (this.config.hasAnalysis()) {
                // 有分析字段，则需要调整层级结构
                ResultDataSetColumn parentColumn = calcColumn.clone();
                parentColumn.setCode("/" + calcColumn.getCode());

                // 添加本期
                calcColumn.setTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
                calcColumn.setRawTitle(BIConsts.ANALYSIS_CURRENT_TITLE);
                parentColumn.addChild(calcColumn);

                // 获取其分析字段
                List<QueryField> analysisFields = analysisMap.get(calcMeasure.getCode());
                if (BIUtil.isNotEmpty(analysisFields)) {
                    // 添加同环对比
                    for (QueryField analysisField : analysisFields) {
                        ResultDataSetColumn analysisColumn = engine.createDataSetColumn(analysisField, analysisField.getCode());
                        analysisColumn.setHasAuth(hasAuth);
                        analysisColumn.setCalcMode(analysisField.getAnalysisConfig().getCalcMode());
                        analysisColumn.setCalcType(analysisField.getAnalysisConfig().getCalcType());
                        analysisColumn.setCompareIndex(analysisField.getAnalysisConfig().getCompareIndex());

                        if (AnalysisCalcMode.CONTRIBUTION_RATE == AnalysisCalcMode.get(analysisField.getAnalysisConfig().getCalcMode())) {
                            AnalysisContributionRateItemConfig itemConfig = (AnalysisContributionRateItemConfig) analysisField.getAnalysisConfig();
                            analysisColumn.setCtrCalcMode(itemConfig.getCtrCalcMode());
                        }

                        if (AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(analysisField.getAnalysisConfig().getCalcMode())) {
                            AnalysisZbThbItemConfig itemConfig = (AnalysisZbThbItemConfig) analysisField.getAnalysisConfig();
                            analysisColumn.getZbThbConfig().setThbCalcMode(itemConfig.getThbCalcMode());
                            analysisColumn.getZbThbConfig().setZbCalcMode(itemConfig.getZbCalcMode());
                        }

                        parentColumn.addChild(analysisColumn);
                    }
                }
                columns.add(parentColumn);
            } else {
                columns.add(calcColumn);
            }

        }
        return columns;
    }

    protected void buildLeafColumns(List<ResultDataSetColumn> columns, List<ResultDataSetColumn> leafColumns){
        for(ResultDataSetColumn c : columns){
            List<ResultDataSetColumn> children = c.getChildren();
            if(BIUtil.isEmpty(children)){
                leafColumns.add(c);
            }else {
                buildLeafColumns(children, leafColumns);
            }
        }
    }

    public QueryEngineType getType(){
        return QueryEngineType.Lod;
    }
}
