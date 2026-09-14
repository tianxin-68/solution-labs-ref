package com.bi.queryer.ssm.engine.cross;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.*;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.engine.session.QuerySessionSettingManager;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 18:36 2023-03-30
 * @Description 维度交叉查询：主要有解决列维度查询的场景
 **/
public class CrossDimensionQueryEngine extends QueryEngine {

    protected List<QueryField> colDimItemFields = new ArrayList<>();

    /**
     * 记录列维度字段：用于修改后恢复
     */
    protected Map<String, QueryField> colDimFieldMap = new LinkedHashMap<>();

    // 自动排序类型map
    /**
     * 记录排序：用于修改后恢复
     */
    protected Map<String, FieldSortType> fieldSortTypeMap = new HashMap<>();

    protected List<ResultDataSetColumn> leafColumns = new ArrayList<>();

    protected CrossDimensionSqlBuilder crossSqlBuilder = null;

    protected boolean needSort = true;

    protected long t1 = 0;

    public CrossDimensionQueryEngine(QueryConfigure template, QueryContext cxt) {
        super(template, cxt);
        this.leafColumns.clear();
        this.fieldSortTypeMap.clear();
        //this.crossSqlBuilder = new CrossDimensionSqlBuilder(this);

        // 默认去掉空列
        this.config.getSettings().setIsHideNullColumn(Enabled.YES.getId());
        this.needSort = template.getSettings().getNeedSort();

        this.rowBuilder = new CrossDimensionResultDataSetRowBuilder(this);
    }

    /*
    @Override
    public ResultDataSet execute(String sql, Integer maxRowCount, String queryId, List<ResultDataSetColumn> columns) {
        ResultDataSet dataSet = super.execute(sql, maxRowCount, queryId, columns);
        long t2 = System.currentTimeMillis();
        dataSet.getProperties().put("consumeTime", (t2 - t1));
        return dataSet;
    }
     */

    @Override
    public ResultDataSet execute(String sql, QueryExecuteParameter executeParameter) {
        ResultDataSet dataSet = super.execute(sql, executeParameter);
        long t2 = System.currentTimeMillis();
        dataSet.getProperties().put("consumeTime", (t2 - t1));
        return dataSet;
    }

    /**
     * 迭代结果集并封装结果集和多表头
     *
     * @param rs
     * @param dataSet
     * @throws SQLException
     */
    @Override
    public void buildDataSet(ResultSet rs, ResultDataSet dataSet) throws SQLException {
       super.buildDataSet(rs, dataSet);
    }

    @Override
    public int getLimitRow() {
        Integer searchLimit = super.getLimitRow();

        //交叉表 limit 数量需要乘以列维度的数量
        if (CollUtil.isNotEmpty(this.colDimItemFields)) {

            //1 sql转置时，不需要扩大数量
            //2 olap-api不需要扩大数量
            if(!SSDUtil.isQueryPivot(this.config) ){

                int colDimSize = this.colDimItemFields.size();

                String username = SC.v("ssm.search.limit.special.username", "chenmin");
                if (StrUtil.isNotEmpty(username)) {
                    if (username.contains(UserManager.get().getName())) {
                        String specialColSize = SC.v("ssm.search.limit.special.col.size", "200");
                        colDimSize = Integer.valueOf(specialColSize);
                    }
                }

                searchLimit = searchLimit * (colDimSize + 1);

            }

        }

        return searchLimit;
    }

    private String getColumnFormat(ResultDataSetColumn column) {
        ResultDataSetColumn dataSetColumn = this.leafColumns.stream().filter(item -> item.getCode().equals(column.getCode())).findFirst().orElse(null);
        return dataSetColumn == null ? null : dataSetColumn.getDataFormat();
    }

    /**
     * 构建列：构建列的上下级关系
     *
     * @return
     */
    public List<ResultDataSetColumn> buildDataSetColumns(Map<String, Boolean> isNullColumns) {

        // 此结果列只包含了最终查询结构的列，即最末级列
        Map<String, ResultDataSetColumn> leafColumnMap = this.leafColumns.stream().collect(Collectors.toMap(ResultDataSetColumn::getCode, DataSetColumn -> DataSetColumn, (f1, f2) -> f1));

        List<QueryField> measures = this.config.getResult().getMeasures().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());

        Map<String, ResultDataSetColumn> allDimItemColumns = new LinkedHashMap<>();
        List<QueryField> rootDimItemFields = new ArrayList<>();
        List<ResultDataSetColumn> rootDimItemColumns = new ArrayList<>();
        for (QueryField dimItemField : this.colDimItemFields) {
            ResultDataSetColumn dimItemColumn = this.createDataSetColumn(dimItemField);
            dimItemColumn.setRawQueryArea(dimItemField.getQueryArea().toString());
            if (dimItemField.isCommonDate()) {
                dimItemColumn.setRawTitle("日期");
            } else {
                dimItemColumn.setRawTitle(dimItemField.getSourceField().getTitle());
            }

            /*
            // boolean值转换，便于给到前端为中文名
            if(FieldUtil.getFilterType(dimItemField) == FieldFilterType.BooleanSelect){
                dimItemColumn.setTitle(fx.formatBoolean(dimItemColumn.getTitle(), IFunction.Format_Boolean));
            }
             */

            for (QueryField measureField : measures) {
                String key = measureField.getCode() + dimItemField.getCode();
                ResultDataSetColumn measureColumn = leafColumnMap.get(key);
                if (measureColumn != null) {
                    dimItemColumn.addChild(measureColumn);
                }
            }
            if (isNullColumns.get(dimItemColumn.getCode()) == null) {
                isNullColumns.put(dimItemColumn.getCode(), false); // 列为维度项初始化
            }
            allDimItemColumns.put(dimItemColumn.getCode(), dimItemColumn);
            if (dimItemField.getParent() == null) {
                rootDimItemFields.add(dimItemField);
                rootDimItemColumns.add(dimItemColumn);
            }
        }

        // 构建多个列维度时的上下级关系
        this.createColumnCascade(rootDimItemFields, allDimItemColumns);

        // 新的结果列=行维度column+root列维度项column
        List<ResultDataSetColumn> newResultColumns = this.leafColumns.stream().filter(f -> f.getRawQueryArea() == QueryArea.RowDimension.toString()).collect(Collectors.toList());

        // 去掉空列
        if(Enabled.isTrue(config.getSettings().getIsHideNullColumn())) {
            rootDimItemColumns = removeNullColumns(rootDimItemColumns, allDimItemColumns, isNullColumns);
        }

        newResultColumns.addAll(rootDimItemColumns);

        return newResultColumns;
    }

    /**
     * 去掉空列:从最末级依次向上递归判断是否为空，若节点下的所有子节点都为空，则删除该列
     *
     * @param isNullColumns
     * @return
     */
    protected List<ResultDataSetColumn> removeNullColumns(List<ResultDataSetColumn> rootColumns, Map<String, ResultDataSetColumn> allDimItemColumns, Map<String, Boolean> isNullColumns) {
        if(!"true".equalsIgnoreCase(SC.v("cross.table.hide.null.column.enable", "true"))){
            return rootColumns;
        }
        if (BIUtil.isEmpty(isNullColumns)) {
            return rootColumns;
        }

        Map<String, ResultDataSetColumn> parentMap = new HashMap<>();
        for (ResultDataSetColumn leafColumn : leafColumns) {
            ResultDataSetColumn parentColumn = allDimItemColumns.get(leafColumn.getParentCode());
            if (parentColumn != null) {
                parentMap.put(parentColumn.getCode(), parentColumn);
            }
        }

        while (!parentMap.isEmpty()) {
            Map<String, ResultDataSetColumn> newParentMap = new HashMap<>();
            for (ResultDataSetColumn column : parentMap.values()) {
                List<ResultDataSetColumn> childrenColumns = column.getChildren();
                long count = childrenColumns.stream().filter(c -> {
                    return isNullColumns.get(c.getCode()) != null && !isNullColumns.get(c.getCode());
                }).count();
                isNullColumns.put(column.getCode(), count == 0);

                ResultDataSetColumn parentColumn = allDimItemColumns.get(column.getParentCode());
                if (parentColumn != null) {
                    newParentMap.put(parentColumn.getCode(), parentColumn);
                }
            }
            parentMap.clear();
            parentMap.putAll(newParentMap);
        }

        rootColumns = this.removeNullColumnsCascade(rootColumns, isNullColumns);

        return rootColumns;
    }

    protected List<ResultDataSetColumn> removeNullColumnsCascade(List<ResultDataSetColumn> parentColumns, Map<String, Boolean> isNullColumns) {
        if (BIUtil.isEmpty(parentColumns)) {
            return parentColumns;
        }
        parentColumns = parentColumns.stream().filter(f -> !isNullColumns.get(f.getCode())).collect(Collectors.toList());
        if (BIUtil.isEmpty(parentColumns)) {
            return parentColumns;
        }
        for (ResultDataSetColumn column : parentColumns) {
            column.setChildren(removeNullColumnsCascade(column.getChildren(), isNullColumns));
        }
        return parentColumns;
    }

    /**
     * 级联创建列：用于根据上下级关系结果的上下级父子关系
     */
    protected void createColumnCascade(List<QueryField> parentDimItemFields, Map<String, ResultDataSetColumn> allDimItemColumns) {
        for (QueryField parentField : parentDimItemFields) {
            List<QueryField> children = parentField.getChildren();
            if (BIUtil.isEmpty(children)) {
                continue;
            }
            ResultDataSetColumn parentColumn = allDimItemColumns.get(parentField.getCode());
            for (QueryField child : children) {
                ResultDataSetColumn childColumn = allDimItemColumns.get(child.getCode());
                if (childColumn != null) {
                    parentColumn.addChild(childColumn);
                }
                if (BIUtil.isNotEmpty(child.getChildren())) {
                    createColumnCascade(child.getChildren(), allDimItemColumns);
                }
            }
        }
    }

    @Override
    public String buildSql() throws BIException {
        this.crossSqlBuilder = QuerySqlBuilderFactory.createCrossSqlBuilder(this); //new CrossDimensionSqlBuilder(this);

        t1 = System.currentTimeMillis();
        this.leafColumns.clear();

        String datasourceSql = this.getDatasourceSql();
        if (BIUtil.isEmpty(datasourceSql)) {
            return datasourceSql;
        }
        String tips = this.getSqlTips();
        datasourceSql = datasourceSql.replace(tips, "");
        String executeSql = crossSqlBuilder.build(datasourceSql);

        this.colDimItemFields = crossSqlBuilder.getColDimItemFields();
        this.leafColumns = crossSqlBuilder.getLeafColumns();

        if (BIUtil.isEmpty(colDimItemFields)) {
            return datasourceSql;
        }

        executeSql = tips + executeSql;

        // 设置sql片段
        this.sqlFragments = new QuerySqlFragments(crossSqlBuilder.getSelectFragments());

        return executeSql;
    }

    protected String getDatasourceSql() {
        /**修改配置*/
        this.modifyConfig();
        String sql = super.buildSql();
        /**恢复配置*/
        this.restoreConfig();
        return sql;
    }

    /**
     * 修改配置，便于后面构建数据源的sql
     * 修改内容：
     * 1、将列维度全部转为行维度
     * 2、去掉所有字段的排序
     */
    protected void modifyConfig() {
        // 列维度
        List<QueryField> rawColDimFields = config.getResult().getFields().stream().filter(f->f.getRawQueryArea() == QueryArea.ColumnDimension).collect(Collectors.toList());
        if (BIUtil.isNotEmpty(rawColDimFields)) {
            colDimFieldMap.putAll(rawColDimFields.stream().collect(Collectors.toMap(QueryField::getCode, QueryField -> QueryField, (f1,f2)->f1)));

            // 先删除
            config.getResult().remove(rawColDimFields, QueryArea.RowDimension);
            config.getResult().remove(rawColDimFields, QueryArea.ColumnDimension);

            // 再添加
            config.getResult().addAll(rawColDimFields, QueryArea.RowDimension);
        }

        // 排序字段
        List<QueryField> rawFields = config.getResult().getFields();
        for (QueryField f : rawFields) {
            if (f.getSortType() != null && f.getSortType() != FieldSortType.NONE) {
                fieldSortTypeMap.put(f.getId(), f.getSortType());
                f.setSortType(FieldSortType.NONE);
            }
        }
        config.getSettings().setNeedSort(false);
    }

    /**
     * 将修改的配置，恢复到初始状态
     * 恢复内容：
     * 1、添加列维度
     * 2、删除行维度中的列维度
     * 2、恢复行列字段的排序
     */
    public void restoreConfig() {
        // 行列维度
        if (BIUtil.isNotEmpty(this.colDimFieldMap)) {
            List<QueryField> colFields = colDimFieldMap.values().stream().collect(Collectors.toList());

            // 先删除
            this.config.getResult().remove(colFields, QueryArea.RowDimension);
            this.config.getResult().remove(colFields, QueryArea.ColumnDimension);

            // 再添加
            this.config.getResult().addAll(colFields, QueryArea.ColumnDimension);

        }
        // 排序
        List<QueryField> rawFields = config.getResult().getFields();
        for (QueryField f : rawFields) {
            if (f.isMeasure()) {
                continue;
            }
            FieldSortType sortType = fieldSortTypeMap.get(f.getId());
            if (sortType == null) {
                sortType = FieldSortType.NONE;
            }
            f.setSortType(sortType);
        }
        config.getSettings().setNeedSort(needSort);
    }

    public static void main(String[] args) {
        String code = "total_sale" + BIConsts.COLUMN_DIM_FIELD_SUFFIX + "_0_1";
        String[] codes = code.split(BIConsts.COLUMN_DIM_FIELD_SUFFIX);
        for (String c : codes) {
            System.out.println(c);
        }
    }

    @Override
    public Integer getDataSetTotalSize(String sql) {
        Integer rows = super.getDataSetTotalSize(sql);
        if(rows == null || rows == 0){
            return 0;
        }
        //if(BIUtil.isNotEmpty(this.colDimItemFields) && this.cxt.isExport()) {
        //    int colItemCount = this.colDimItemFields.size();
        //    rows = rows * colItemCount;
        //}
        return rows;
    }

    public List<ResultDataSetColumn> getLeafColumns() {
        return leafColumns;
    }

    public QueryEngineType getType(){
        return QueryEngineType.Cross;
    }
}
