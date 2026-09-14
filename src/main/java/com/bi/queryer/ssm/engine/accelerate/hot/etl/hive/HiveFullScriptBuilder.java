package com.bi.queryer.ssm.engine.accelerate.hot.etl.hive;

import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlScriptBuilder;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableField;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.WeekDateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:03 2024/8/19
 * @Description hive脚本构建器：drop + create + insert
 **/
public class HiveFullScriptBuilder extends HotEtlScriptBuilder {
    public HiveFullScriptBuilder(HotTableInfo hotTable) {
        super(hotTable);
    }

    public EtlTableDdl buildScheduleScript(){
        // 建表
        HiveTableCreator tableCreator = new HiveTableCreator(hotTable);
        EtlTableDdl ddl = tableCreator.create();

        // 构建插入sql
        String insertSql = buildInsertOverwrite(ddl);

        // 最近拼接建表+insert sql
        List<String> fragments = new ArrayList<>();
        fragments.add("\n-- ==========前置api==========\n");
        fragments.add(buildPreApiSql()+ ";");
        fragments.add("\n-- ==========删除热表==========\n");
        fragments.add(ddl.getDropHotTableSql() + ";");
        fragments.add("\n-- ==========创建热表==========\n");
        fragments.add(ddl.getCreateHotTableSql()+ ";");
        fragments.add("\n-- ==========更新热表数据==========\n");
        fragments.add(insertSql+ ";");

        if("true".equalsIgnoreCase(SC.v("hot.table.add.analyze.enable", "false"))) {
            fragments.add("\n-- ==========执行分析热表==========\n");
            fragments.add(buildAnalyzeTableSql()+";");
        }

        String etlScriptContent = BIUtil.listToStr(fragments, "");
        ddl.setEtlScriptContent(etlScriptContent);

        return ddl;
    }

    protected String buildInsertOverwrite(EtlTableDdl ddl){
        String sql = new String();

        // 是否是分区表
        boolean isPartitionTable = hotTable.isPartitionTable();

        String partitionFieldName = ddl.getPartitionField().getName();

        List<String> sqlFragments = new ArrayList<>();
        // 添加参数
        List<String> setParameters = this.getSetParameters();
        sqlFragments.addAll(setParameters);

        if(isPartitionTable) {
            sqlFragments.add(String.format(" insert overwrite table %s partition(%s) ", hotTable.getHotTableName(), partitionFieldName));
        }else{
            sqlFragments.add(String.format(" insert overwrite table %s ", hotTable.getHotTableName()));
        }
        sqlFragments.add(" select ");

        // 字段
        List<EtlTableField> commonFields = ddl.getCommonFields();
        List<String> fieldFragments = new ArrayList<>();
        for(EtlTableField field : commonFields){
            fieldFragments.add(String.format(" t1.%s", field.getName()));
        }
        if(isPartitionTable) {
            fieldFragments.add(String.format(" t1.%s", partitionFieldName));
        }
        sqlFragments.add(BIUtil.listToStr(fieldFragments, "    ,", "", "\n"));

        // from
        String sourceTableSql = ddl.getSourceTableSql();
        String rectifySyntaxRule = SC.v("hot.table.hive.syntax.rectify.rule", " varchar= string;regexp_like\\((\\w+),\\s*'([^']*)'\\)=($1 rlike '$2')");
        sourceTableSql = HotUtil.rectifySyntax(sourceTableSql, rectifySyntaxRule); //sourceTableSql.replaceAll("(?i) varchar", " string"); // hive数据类型转换：处理cast问题
        sqlFragments.add(String.format(" from (%s) t1 ", sourceTableSql));

        // where
        sqlFragments.add(buildWhereSql(partitionFieldName));

        // 最后拼接：每段需要换行
        sql = BIUtil.listToStr(sqlFragments, "\n");
        return sql;
    }


    public String buildWhereSql(String partitionFieldName) {
        String startDate = hotTable.getDataMinDate();
        String endDate = hotTable.getDataMaxDate();
        List<String> filterDateRange = this.getFilterDateRangeByDateGranularity(startDate, endDate);
        startDate = filterDateRange.get(0);
        endDate = filterDateRange.get(1);

        String whereSql = String.format(" where t1.%s between '%s' and '%s' ", partitionFieldName, startDate, endDate);
        return whereSql;
    }

    /**
     * 获取过滤日期范围
     * @return
     */
    protected List<String> getFilterDateRangeByDateGranularity(String startDate, String endDate){
        String formatString = hotTable.getHotTableSource().getDateFormatType();
        DateGranularity dateGranularity = DateGranularity.get(hotTable.getHotTableSource().getDateGranularity());
        if(BIUtil.isEmpty(formatString)){
            formatString = dateGranularity.getFormat();
        }
        if(dateGranularity != DateGranularity.WEEK){
            startDate = DateUtil.parseDate(startDate).toString(formatString);
            endDate = DateUtil.parseDate(endDate).toString(formatString);
        }else {
            // 周粒度特殊处理
            startDate = WeekDateUtil.getWeekId(DateUtil.parseDate(startDate));
            endDate = WeekDateUtil.getWeekId(DateUtil.parseDate(endDate));
        }
        List<String> range = new ArrayList<>();
        range.add(startDate);
        range.add(endDate);
        return range;
    }

    protected List<String> getSetParameters(){
        List<String> setParameters = new ArrayList<>();
        setParameters.add("set hive.exec.dynamic.partition=true; ");
        setParameters.add("set hive.exec.dynamic.partition.mode=nonstrict; ");
        setParameters.add("set hive.exec.max.dynamic.partitions=1000; ");
        setParameters.add("set hive.exec.max.dynamic.partitions.pernode=1000; ");
        setParameters.add("set hive.optimize.sort.dynamic.partition=true; ");
        return setParameters;
    }

    protected String buildAnalyzeTableSql(){
        String analyzeTableSql = String.format("analyze table %s compute statistics ",
                hotTable.getHotTableName());
        return analyzeTableSql;
    }

    protected  String buildPreApiSql() {

        String url = SC.v("hot.table.pre.api.url","");
        String apiSql = String.format("select\n" +
                        "    default.bi_api_invoke(\n" +
                        "        tmp.api_url,\n" +
                        "        '{\"etlJobName\":\"%s\"}',\n" +
                        "        'application/json;charset=UTF-8',\n" +
                        "        60\n" +
                        "    )\n" +
                        "from\n" +
                        "    (\n" +
                        "        select\n" +
                        "            '%s' as api_url\n" +
                        "    ) tmp",
                hotTable.getEtlJobName(),
                url);
        return apiSql;
    }

    /**
     * 构建补数脚本
     * @param ddl
     * @return
     */
    public EtlTableDdl buildRepairScript(EtlTableDdl ddl){
        HiveRepairScriptBuilder repairScriptBuilder = new HiveRepairScriptBuilder(this.hotTable);
        String script = repairScriptBuilder.build(ddl);
        ddl.setRepairScriptContent(script);
        return ddl;
    }

    public static void main(String[] args) {
        String sql = "select cast(x as varchar) as x, if(regexp_like(y,   'a|b|c'), 1, 0) as flag from t1";
        String rule = " varchar= string;regexp_like\\((\\w+),\\s*'([^']*)'\\)=($1 rlike '$2')";
        System.out.println(HotUtil.rectifySyntax(sql, rule));

        String originalSql = "SELECT * FROM table WHERE regexp_like(field1, 'abc')";
        String modifiedSql = originalSql.replaceAll("regexp_like\\((\\w+),\\s*'(\\w+)'\\)", "($1 rlike '$2')");

        System.out.println(modifiedSql);
    }
}
