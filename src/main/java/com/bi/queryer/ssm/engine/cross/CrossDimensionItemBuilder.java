package com.bi.queryer.ssm.engine.cross;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryExecuteParameter;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.parameter.WidgetParameter;
import com.bi.queryer.ssm.engine.prepare.PrepareQueryResult;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.DateType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.enums.SortType;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.period.DateUtil;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;

/**
 * @Author contributor
 * @Date 15:09 2022-09-05
 * @Description 交叉表头构建器
 **/
public class CrossDimensionItemBuilder {

    protected QueryConfigure config = null;

    /**
     * sql所需列维度字段
     */
    protected List<QueryField> colDimFields = new ArrayList<>();

    protected List<WidgetParameter> parameters = null;

    protected Map<String, QueryField> colDimItemFieldMap = new HashMap<>();

    /**
     * sql所需的列维度项字段：列维度查询后的所有字段
     */
    protected List<QueryField> colDimItemFields = new ArrayList<>();

    /**
     * 列维度参数，构建完后此属性合并到整体查询参数列表中
     */
    protected QueryEngine engine = null;

    protected boolean isBuilt = false;

    protected QueryContext cxt = null;

    /**
     * 存储通过sql构建的结果数据集，避免同环比计算是多次查询
     */
    protected static ThreadLocal<ResultDataSet> resultDataSetThreadCache = new ThreadLocal<>();

    public CrossDimensionItemBuilder(QueryConfigure config, QueryContext cxt) {
        this.config = config;
        this.cxt = cxt;
        colDimItemFields.clear();
        colDimItemFieldMap.clear();
        this.colDimFields = config.getResult().getColDimensions();
    }

    /**
     * 预处理
     */
    protected void prepare() {
        colDimItemFields.clear();
        colDimItemFieldMap.clear();
        this.engine = new QueryEngine(config, cxt);

        // 从上下文中获取预处理数据集，避免后续重复通过sql获取表头
        PrepareQueryResult prepareQueryResult = cxt.getPrepareQueryResult();
        if(cxt.getPrepareQueryResult() != null && prepareQueryResult.getCrossDimensionItemDataSet() != null){
            ResultDataSet prepareDataSet = prepareQueryResult.getCrossDimensionItemDataSet();
            if(BIUtil.isNotEmpty(prepareDataSet.getRows())){
                resultDataSetThreadCache.set(prepareDataSet);
            }
        }

    }

    /**
     * 构建表头字段
     * 通过列维度字段查询的结果，构造为树形结构的字段列表
     */
    public List<QueryField> build() {
        if(isBuilt){
            return colDimItemFields;
        }
        this.prepare();

        if (isEmpty(colDimFields)) {
            return colDimItemFields;
        }
        // 获取数据集
        // 走参数
        ResultDataSet dataSet = this.buildHeaderDataSetByParameter();

        // 走sql查询
        if (dataSet == null) {
            dataSet = this.buildHeaderDataSetBySQL();
        }

        if (dataSet == null || isEmpty(dataSet.getRows())) {
            return colDimItemFields;
        }

        // 构建维度项字段：含层级结构
        List<String> isOnlyOrderByFieldCodes = this.getOnlyOrderByFieldCodes();
        List<ResultDataSetColumn> cols = dataSet.getColumns().stream().filter(c -> {
            return !isOnlyOrderByFieldCodes.contains(c.getCode());
        }).collect(Collectors.toList());

        Map<String, QueryField> colDimFieldMap = colDimFields.stream().collect(Collectors.toMap(QueryField::getCode, QueryField->QueryField,(f1,f2)->f1));

        resortColDimValues(dataSet.getRows());

        for (int i = 0; i < cols.size(); i++) {
            ResultDataSetColumn col = cols.get(i);
            QueryField qf = config.getResult().getFieldByCode(col.getCode());
            if(qf == null) {
                continue;
            }
            MetaField metaField = qf.getMeta();
            QueryField colDimQueryField = colDimFieldMap.get(metaField.getCode()); //colDimFields.stream().filter(f -> f.getCode().equalsIgnoreCase(metaField.getCode())).findFirst().get();
            if (metaField == null || colDimQueryField == null) {
                continue;
            }

            int size = dataSet.getSize();
            for (int j = 0; j < size; j++) {
                Map<String, Object> row = dataSet.getRows().get(j);
                String value = row.get(col.getCode()) + "";
                QueryField itemField = new QueryField(metaField);
                String guid = "";
                String parentGuid = "";
                for (int k = 0; k <= i; k++) {
                    guid = guid + row.get(cols.get(k).getCode()) + BIConsts.SEPARATOR;
                    if (k > 0) {
                        parentGuid = parentGuid + row.get(cols.get(k - 1).getCode()) + BIConsts.SEPARATOR;
                    }
                }
                String itemCode = BIConsts.COLUMN_DIM_FIELD_SUFFIX + i + "_" + j;
                itemField.setId(guid);
                itemField.setName(itemCode);
                itemField.setCode(itemCode);
                itemField.setQueryDateGranularity(qf.getQueryDateGranularity());

                String title = row.get(col.getCode() + "_title") + "";
                if(isEmpty(title)) {
                    title = FieldUtil.getPivotItemFieldTitle(itemField, value, this.engine); //this.getItemFieldTitle(itemField, value);
                }
                itemField.setTitle(title + "");
                itemField.setDisplayTitle(isEmpty(title) ? " " : title);

                itemField.getValues().add(new FieldValue(value, value));
                itemField.setRawCode(colDimQueryField.getCode());
                itemField.setQueryArea(QueryArea.ColumnDimension);
                // 设置上下级
                QueryField parent = colDimItemFieldMap.get(parentGuid);
                itemField.setParent(parent);

                // 设置来源字段
                itemField.setSourceField(colDimQueryField);

                // 父级添加子级：通过字段标题去重
                if (parent != null && parent.getChildren() != null) {
                    List<QueryField> children = parent.getChildren();
                    if (children.stream().filter(c -> {
                        return c.getTitle().equals(value);
                    }).count() == 0) {
                        children.add(itemField);
                    }
                }

                if (!colDimItemFieldMap.containsKey(guid)) {
                    colDimItemFieldMap.put(guid, itemField);
                    colDimItemFields.add(itemField);
                }
            }

        }
        // 若是日期放在列维度时，需要重复构建，因为同环比时其枚举值不一样
        isBuilt = true;
        return colDimItemFields;
    }

    //把列维度中的维度特殊值排到最后去
    private void resortColDimValues(List<Map<String, Object>> rows) {
        if (isEmpty(rows) || isEmpty(colDimFields)) {
            return;
        }

        List<Map<String, Object>> specialDimValueRows = new ArrayList<>(4);
        Map<String, String> dimNumberMap = BIConsts.SPECIAL_DIM_VALUE_NUMBER_MAP;
        QueryField col = colDimFields.get(0);
        Iterator<Map<String, Object>> it = rows.iterator();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            String value = row.get(col.getCode()) + "";
            if (dimNumberMap.containsKey(value)) {
                specialDimValueRows.add(row);
                it.remove();
            }
        }
        rows.addAll(specialDimValueRows);
    }

    /**
     * 获取字段标题
     * @param itemField
     * @return
     */
    protected String getItemFieldTitle(QueryField itemField, String value){
        String title = value;
        if(isEmpty(title)) {
            return title;
        }
        FieldFilterType filterType = FieldUtil.getFilterType(itemField);
        String formatString = "";
        if(filterType == FieldFilterType.BooleanSelect){
            formatString = IFunction.Format_Boolean;
        }

        if (itemField.getMeta() != null && CollUtil.isNotEmpty(itemField.getMeta().getDimValueMap())) {
            formatString = IFunction.Format_Map;
        }

        // 公共日期
        if(itemField.isCommonDate()){
            DateGranularity dateGranularity = DateGranularity.get(itemField.getQueryDateGranularity());
            switch (dateGranularity){
                case WEEK:
                    formatString = IFunction.Format_Week;
                    break;
                case MONTH:
                    formatString = IFunction.Format_Month;
                    break;
                case DAY:
                    if(this.config.isShowLunarDate()){
                        formatString = IFunction.Format_Lunar_Date;
                    }
                    break;
            }
        }
        if(BIUtil.isNotEmpty(formatString)) {
            title = this.engine.formatValue(value, itemField.getMeta().getDataType(), formatString, itemField.getRawCode()) + "";
        }
        return title;
    }

    /**
     * 获取列表头的最大列数
     *
     * @return
     */
    protected int getHeaderColumnMaxCount() {
        // 现在最大列数
        int maxCount = BIConsts.COLUMN_DIM_ITEM_MAX_COUNT;
        /*
        Integer columnCountPerDimValue = engine.getCxt().getColumnCountPerDimValue();
        if (columnCountPerDimValue == null) {
            return maxCount;
        }

        int maxColumnCount = Integer.parseInt(SC.v("ssm.grid.max.column.count", "500"));
        // 计算有应该有多少个列维值
        int dimValueCount = maxColumnCount / columnCountPerDimValue;

        // 包含行总计
        return Math.min(dimValueCount - 1, maxCount);

         */
        return maxCount;
    }

    /**
     * 获取仅用于排序的字段编码，此类字段最终不体现到表头中
     *
     * @return
     */
    protected List<String> getOnlyOrderByFieldCodes() {
        List<String> isOnlyOrderByFields = new ArrayList<>(); // 仅仅用于排序的字段别名列表

        Map<String, QueryField> sqlColDimFieldMap = this.colDimFields.stream().collect(Collectors.toMap(QueryField::getName, QueryField -> QueryField, (f1, f2) -> f1));
        /**
         * 排序字段和查询字段不一致，则需要把排序字段添加进行标识记录
         */
        for (QueryField qf : colDimFields) {
            String orderByField = qf.getName();
            String orderByFieldAlias = orderByField;
            QueryField orderByQueryField = config.getResult().getFieldByCode(orderByField);
            if (orderByQueryField == null) {
                continue;
            }
            MetaField orderByMeta = orderByQueryField.getMeta();
            if (orderByMeta != null && !sqlColDimFieldMap.containsKey(orderByField)) {
                isOnlyOrderByFields.add(orderByFieldAlias);
            }
        }

        return isOnlyOrderByFields;
    }

    protected boolean canBuildHeaderDataSetByParameter(){
        if (this.colDimFields.size() != 1) {
            return false;
        }

        QueryField qf = colDimFields.get(0);

        QueryField filterColumnField = config.getFilter().getFieldByCode(qf.getCode());
        if (filterColumnField == null
                || FieldValueFilterType.isExclude(filterColumnField.getFilterValueType())
                || isEmpty(qf.getValues())) {
            return false;
        }

        // 只处理日期过滤值
        FieldFilterType filterType = FieldUtil.getFilterType(qf); //FieldFilterType.get(qf.getMeta().getFilterShowType());
        if (!filterType.isDateRange()) {
            return false;
        }
        return true;
    }


    /**
     * 通过查询参数构建表头数据集
     *
     * @return
     */
    protected ResultDataSet buildHeaderDataSetByParameter() {
        /**构建数据集:
         *若只有一个列维度且排序字段是其自己且过滤条件中有对该维度的过滤值且不是排除项，则直接通过过滤参数值构造数据集，否则执行sql获取
         */
        ResultDataSet dataSet = null;
        /*
        if (this.colDimFields.size() != 1) {
            return dataSet;
        }

        QueryField qf = colDimFields.get(0);

        QueryField filterColumnField = config.getFilter().getFieldByCode(qf.getCode());
        if (filterColumnField == null
                || FieldValueFilterType.isExclude(filterColumnField.getFilterValueType())
                || BIUtil.isEmpty(qf.getValues())) {
            return dataSet;
        }

        // 只处理日期过滤值
        FieldFilterType filterType = FieldUtil.getFilterType(qf); //FieldFilterType.get(qf.getMeta().getFilterShowType());
        if (!filterType.isDateRange()) {
            return dataSet;
        }
         */
        if(!canBuildHeaderDataSetByParameter()){
            return dataSet;
        }
        QueryField qf = colDimFields.get(0);
        //QueryField filterColumnField = config.getFilter().getFieldByCode(qf.getCode());

        // 排序：过滤值、日期的分类过滤
        dataSet = new ResultDataSet();
        List<ResultDataSetColumn> columnList = new ArrayList<>();

        // code列
        ResultDataSetColumn valueColumnInfo = new ResultDataSetColumn();
        String valueColumnName = qf.getCode();
        valueColumnName = valueColumnName.split("\\.").length > 1 ? valueColumnName.split("\\.")[1] : valueColumnName;
        valueColumnInfo.setCode(valueColumnName);
        valueColumnInfo.setDataType(DataType.String.toString());
        columnList.add(valueColumnInfo);

        // title列
        ResultDataSetColumn titleColumnInfo = valueColumnInfo.clone();
        String titleColumnName = valueColumnName + "_title";
        titleColumnInfo.setCode(titleColumnName);
        columnList.add(titleColumnInfo);

        dataSet.setColumns(columnList);

        List<Map<String, Object>> rows = Lists.newArrayList();
        /*
        // 下拉过滤
        qf.getValues().forEach(fv -> {
            fv.setId(EnvVariableManager.value(fv.getId()));
        });
        List<String> filterValues = qf.getValues().stream().map(FieldValue::getId).collect(Collectors.toList());
         */
        List<KeyValuePair> filterValueAndTitles = this.getDateFilterParameterValueAndTitles(qf);
        /*
        // 为空或不为空的数据不走参数表头
        if (filterValues.contains(BIConsts.Filter_Value_Is_Not_Null) || filterValues.contains(BIConsts.Filter_Value_Is_Null)) {
            return null;
        }
         */
        // 日期范围过滤
        /*
        if (isRange &&
                (dataType == FieldDataType.Date
                        || dataType == FieldDataType.Datetime
                        || filterType.isDateRange())

         */

        /*
        if(FieldUtil.isDateField(qf)){
            int valueCount = (filterValues.size() / 2) * 2; // 只取2的倍数值
            List<String> dateValues = new ArrayList<>();
            for (int v = 0; v < valueCount; v = v + 2) {
                String v1 = filterValues.get(v);
                String v2 = filterValues.get(v + 1);

                DateGranularity dateGranularity = DateGranularity.get(filterColumnField.getQueryDateGranularity());
                DateType dateType = DateType.get(dateGranularity.toString());
                dateValues.addAll(DateUtil.getRangeList(v1, v2, dateType));
            }
            filterValues = dateValues;
        }
         */

        if (BIUtil.isNotEmpty(filterValueAndTitles)) {
            /*
            // 保持和字段设置的顺序一致
            Collections.sort(filterValues);
            if (qf.getSortType() != FieldSortType.ASC) {
                filterValues = ListUtil.reverse(filterValues);
            }
             */

            int maxCount = this.getHeaderColumnMaxCount();
            for (KeyValuePair kv : filterValueAndTitles) {
                // 限制最大个数
                if(rows.size() >= maxCount){
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put(valueColumnName, kv.getKey());
                row.put(titleColumnName, kv.getValue());
                rows.add(row);
            }
        }
        dataSet.setRows(rows);

        return dataSet;
    }

    /**
     * 获取日期过滤查询参数的枚举值
     * 此方法会被分析场景覆写，用于日期放在列维度上时修改其case when值
     * @param qf
     * @return
     */
    public List<KeyValuePair> getDateFilterParameterValueAndTitles(QueryField qf){
        qf.getValues().forEach(fv -> {
            fv.setId(EnvVariableManager.value(fv.getId()));
        });
        List<String> filterValues = qf.getValues().stream().map(FieldValue::getId).collect(Collectors.toList());

        // 为空或不为空的数据不走参数表头
        if (isEmpty(filterValues) || filterValues.contains(BIConsts.Filter_Value_Is_Not_Null) || filterValues.contains(BIConsts.Filter_Value_Is_Null)) {
            return null;
        }
        // 日期范围过滤
        /*
        if (isRange &&
                (dataType == FieldDataType.Date
                        || dataType == FieldDataType.Datetime
                        || filterType.isDateRange())

         */
        if(filterValues.size() == 1){
            filterValues.add(filterValues.get(0));
        }
        List<KeyValuePair> result = new ArrayList<>();
        QueryField filterColumnField = config.getFilter().getFieldByCode(qf.getCode());

        //业务日历不计算日期范围
        if(!config.getSettings().isBusinessCalendar()){

            if(FieldUtil.isDateField(qf)){
                String v1 = filterValues.get(0);
                String v2 = filterValues.get(1);

                DateGranularity dateGranularity = DateGranularity.get(filterColumnField.getQueryDateGranularity());
                DateType dateType = DateType.get(dateGranularity.toString());
                filterValues = DateUtil.getRangeList(v1, v2, dateType);
            }

            // 保持和字段设置的顺序一致
            Collections.sort(filterValues);
            // 按降序排序
            filterValues = ListUtil.reverse(filterValues);

        }

        for(String value : filterValues){
            String title = FieldUtil.getPivotItemFieldTitle(qf, value, this.engine); //this.getItemFieldTitle(qf, value);
            KeyValuePair kv = new KeyValuePair(value, title);
            result.add(kv);
        }
        return result;
    }

    /**
     * 通过sql查询获取表头数据集
     *
     * @return
     */
    protected ResultDataSet buildHeaderDataSetBySQL() {
        if(resultDataSetThreadCache.get() != null){
            return resultDataSetThreadCache.get();
        }
        // 现在最大列数
        String queryId = "Header_" + Guid.id();
        int maxCount = this.getHeaderColumnMaxCount();

        // 重新构建配置
        QueryConfigure newCfg = new QueryConfigure();

        // 列维度 是否有排序类型
        boolean colFieldHasSortType = false;

        // 优先从模型中获取列维度，因为模型中的列维度是经过优化后的，直接从config中获取的列维度可能会被优化掉导致查询报错
        List<QueryField> colFields = null;
        List<StarModel> starModels = this.engine.getModels();
        if(BIUtil.isNotEmpty(starModels)) {
            colFields = starModels.get(0).getDimFields().stream()
                    .filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension
                            && !f.isAppend()
                    ).collect(Collectors.toList());
        }
        if(isEmpty(colFields)) {
            colFields = config.getResult().getColDimensions();
        }
        for(QueryField colField : colFields){
            // 若是日期则按日期升序
            FieldFilterType filterType = FieldUtil.getFilterType(colField);
            if(filterType.isDateRange()) {
                colField.setSortType(FieldSortType.ASC);
                colFieldHasSortType = true;
                break;
            }
            colFieldHasSortType = colFieldHasSortType || colField.getSortType() != FieldSortType.NONE;
        }

        // 若列维度未设置排序，则默认按第一个指标降序
        QueryField measureField = null;
        List<QueryField> measures = new ArrayList<>();
        /*
        if(!colFieldHasSortType && BIUtil.isNotEmpty(config.getResult().getMeasures())) {
            measureField = config.getResult().getMeasures().get(0);
            measureField = measureField.clone();
            measureField.setSortType(FieldSortType.DESC);
            // 调整所有字段的聚合方式为默认聚合方式，避免去重日均值计算，提升查询效率
            measureField.setAggExpressionType("");
            measures.add(measureField);
        }
         */

        // 添加指标：避免但模块下多维度时，通过指标来限定正确的查询表（无法找到正确的查询表）
        // 如：轮胎运营分析模块中：城市线级、轮胎规格、轮胎规格分类
        if(BIUtil.isNotEmpty(config.getResult().getMeasures())) {

            List<QueryField> normalMeasures =  config.getResult().getMeasures().stream().filter(f-> !Enabled.value(f.getIsAnalysis())).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(normalMeasures)){
                QueryField mf = normalMeasures.get(0);
                measureField = mf.clone();
                if(!colFieldHasSortType) {
                    measureField.setSortType(FieldSortType.DESC);
                }

                measureField.setAggExpressionType("");
                measures.add(measureField);
            }

        }

        // 将列维度转为行维度查询
        List<QueryField> newRowFields = new ArrayList<>();
        colFields.forEach(f-> {
            QueryField copy = f.clone();
            copy.setQueryArea(QueryArea.RowDimension);
            if(!newRowFields.contains(copy)) {
                newRowFields.add(copy);
            }
        });

        newCfg.getResult().setMeasures(measures);
        newCfg.getResult().setRowDimensions(newRowFields);
        newCfg.getResult().addAdditionalField();

        List<QueryField> newFilterFields = new ArrayList<>();
        config.getFilter().getFields().forEach(
                f-> {
                    QueryField copy = f.clone();
                    if (copy.isCommonDate()) {
                        // 若是公共日期，则设置为汇总，避免日均指标查询时没有日期维度在行区域中
                        copy.setIsAggQuery(Enabled.YES.getId());
                    } else if (QueryArea.Measure.equals(copy.getRawQueryArea())) {
                        return;
                    }
                    newFilterFields.add(copy);
                }
        );
        newCfg.getFilter().setFields(newFilterFields);

        newCfg.getResult().getFields().forEach(
                f-> {
                    if (f.isCommonDate() && f.isAppend()) {
                        //公共日期为计算字段引入，粒度不准问题修复。
                        f.setQueryDateGranularity(this.config.getSettings().getDateGranularity());
                    }
                }
        );

        // 设置模板id
        SSDQueryTemplate tplEntity = new SSDQueryTemplate();
        if(config.getTemplateEntity() != null){
            tplEntity.setId(config.getTemplateEntity().getId() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
            tplEntity.setName(config.getTemplateEntity().getName() + BIConsts.CROSS_HEADER_KEY_SUFFIX);
        }
        newCfg.setTemplateEntity(tplEntity);

        // 当前线程下主查询的数据源
        DataSourceType currentDataSource = DataSourceRouter.getCurrentDataSourceType();
        QueryContext cxt = engine.getCxt().clone();

        newCfg.setSettings(config.getSettings().clone());
        newCfg.getSettings().setDateGranularity(config.getSettings().getDateGranularity());

        QueryEngine newEngine = QueryFactory.createEngine(newCfg, cxt); // new QueryEngine(newCfg, cxt);
        // 按当前查询引擎重新设置查询引擎的数据源，解决场景：主查询使用doris，但此列维度枚举值查询可能是trino，导致查询语法不一致，最终报错
        DataSourceRouter.setQueryEngineDefaultDataSource(newEngine);
        String sql = newEngine.buildSql();
        BIException biException = null;
        ResultDataSet dataSet = new ResultDataSet();
        try {
            dataSet = newEngine.execute(sql, new QueryExecuteParameter(maxCount, queryId, null));// newEngine.execute(sql, maxCount, queryId, null);
            resultDataSetThreadCache.set(dataSet);
        }catch (BIException e) {
            if (BIUtil.isEmpty(newEngine.getLog().getInfo())) {
                newEngine.getLog().setInfo(e.getMessage());
            }
            e.printStackTrace();
            biException = e;
        }finally {
            // 还原数据源
            DataSourceRouter.setCurrentDataSourceType(currentDataSource);
        }

        newEngine.post(dataSet);

        // 此处不外抛异常，避免重复发送邮件
        if (biException != null) {
            throw biException;
        }

        return dataSet;
    }

    /**
     * 构建列维度表头sql:通过维度过滤sql，有且仅有一个列维度时
     *
     * @return
     */
    protected StringBuilder buildHeaderSQLByFilterSql() {
        List<String> groupByFragment = new ArrayList<>();
        List<String> selectFragment = new ArrayList<>();
        List<String> orderByFragment = new ArrayList<>();
        List<String> whereFragment = new ArrayList<>();

        QueryField colDimField = colDimFields.get(0);

        String datasourceFieldName = "name";

        List<QueryField> filterColumnFields = new ArrayList<>();
        for (QueryField qf : colDimFields) {
            MetaField meta = qf.getMeta();
            if (meta == null) {
                continue;
            }
            String fieldName = datasourceFieldName;
            FieldFilterType filterType = FieldUtil.getFilterType(qf);
            /*
            if (FieldFilterType.BooleanSelect == filterType){//FieldFilterType.get(meta.getFilterShowType())) {
                fieldName = getBooleanSelectSqlName(meta, fieldName);
            }
             */
            selectFragment.add(fieldName + " as " + qf.getCode());
            groupByFragment.add(fieldName);

            /**添加参数过滤*/
            whereFragment.add(fieldName + " is not null ");

            QueryField filterColumnField = config.getFilter().getFieldByCode(qf.getCode());
            if (filterColumnField != null) {
                filterColumnFields.add(filterColumnField);
            }
        }

        /*** where条件：可能是数据集中字段，一定是列维度字段 **/
        if (BIUtil.isNotEmpty(filterColumnFields)) {
            for (QueryField filterField : filterColumnFields) {
                if (isEmpty(filterField.getValues()) || !colDimField.getCode().equalsIgnoreCase(filterField.getCode())) {
                    continue;
                }
                List<String> realValues = filterField.getValues().stream().map(FieldValue::getId).collect(Collectors.toList());
                DataType dataType = DataType.getType(filterField.getMeta().getDataType());
                if (!(dataType.isDecimal())) {
                    realValues = realValues.stream().map(v -> "'" + v + "'").collect(Collectors.toList());
                }
                // 先判断参数对应的字段是否存在
                String filterFieldName = datasourceFieldName;
                String containType = (FieldValueFilterType.get(filterField.getFilterValueType()) != FieldValueFilterType.exclude) ? " " : " not ";
                FieldFilterType filterType = FieldUtil.getFilterType(filterField);
                if (filterType.isDateRange()) { // 范围值
                    whereFragment.add(containType + filterFieldName + " between " + realValues.get(0) + " and " + realValues.get(1));
                } else {
                    whereFragment.add(filterFieldName + containType + " in (" + BIUtil.listToStr(realValues, ",") + ")");
                }
            }
        }


        /**
         * 排序字段和查询字段不一致，则需要把排序字段添加到select和group by子句中
         */
        for (QueryField qf : colDimFields) {
            String orderByField = datasourceFieldName;
            orderByFragment.add(orderByField + " " + qf.getSortType().getCode());
        }

        String datasourceSql = colDimField.getMeta().getFilterSQL();

        StringBuilder sql = new StringBuilder();
        sql.append(" select ");
        sql.append(BIUtil.listToStr(selectFragment, ","));
        sql.append(" from ");
        sql.append("(").append(datasourceSql).append(") _tmp_ ");

        if (BIUtil.isNotEmpty(whereFragment)) {
            sql.append(" where ").append(BIUtil.listToStr(whereFragment, " and ", ""));
        }

        sql.append(" group by ").append(BIUtil.listToStr(groupByFragment));
        sql.append(" order by ").append(BIUtil.listToStr(orderByFragment));

        return sql;
    }

    private String getBooleanSelectSqlName(MetaField meta, String fieldName) {
        String sqlName = "";
        if (DataType.getType(meta.getDataType()) == DataType.String) {
            sqlName = "CASE WHEN " + fieldName + " = '0' THEN '否' WHEN " + fieldName + " = '1' THEN '是' ELSE '其他' END";
        } else {
            sqlName = "CASE WHEN " + fieldName + " = 0 THEN '否' WHEN " + fieldName + " = 1 THEN '是' ELSE '其他' END";
        }
        return sqlName;
    }

    protected String dateFormat(MetaField meta, String filedFullName) {
        String formatStr = meta.getShowFormatExpression();
        if (!StringUtil.isEmpty(formatStr)) {
            IFunction dateFunction = FunctionManager.getFunction();
            filedFullName = dateFunction.date2Char(filedFullName, formatStr);
        }
        return filedFullName;
    }

    public List<QueryField> getColDimItemFields() {
        return colDimItemFields;
    }

    public void setColDimItemFields(List<QueryField> colDimItemFields) {
        this.colDimItemFields = colDimItemFields;
    }

    /**
     * 废弃
     * 获取日期表头排序类型
     * 默认降序，若自定义对比日期长度不一致时，则升序（兼容处理：日期在行维度时，为升序与自定义日期对比）
     * @return
     */
    @Deprecated
    protected SortType getDateHeaderSortType(){
        SortType sortType = SortType.DESC;
        AnalysisCompareConfig compareConfig = config.getAnalysis().getCompare();
        if(compareConfig == null || !compareConfig.isActive()){
            return sortType;
        }

        boolean isSameSize = true;

        List<AnalysisCompareItemConfig> compareItemConfigs = compareConfig.getItems();
        for(AnalysisCompareItemConfig compareItemConfig : compareItemConfigs){
            DateGranularity dateGranularity = DateGranularity.get(compareItemConfig.getDateGranularity());
            DateType dateType = DateType.get(dateGranularity.toString());

            List<String> baseDates = compareItemConfig.getBaseDates();
            List<String> compareDates = compareItemConfig.getCompareDates();

            if(isEmpty(baseDates) || isEmpty(compareDates)){
                continue;
            }
            if(baseDates.size() < 2 || compareDates.size() < 2){
                continue;
            }
            List<String> baseValues = DateUtil.getRangeList(baseDates.get(0), baseDates.get(1), dateType);
            List<String> compareValues = DateUtil.getRangeList(compareDates.get(0), compareDates.get(1), dateType);

            isSameSize = isSameSize && (baseValues.size() == compareValues.size());
        }
        sortType = isSameSize ? SortType.DESC : SortType.ASC;
        return sortType;
    }

    public static void clearResultDataSetThreadCache() {
        resultDataSetThreadCache.remove();
    }
}
