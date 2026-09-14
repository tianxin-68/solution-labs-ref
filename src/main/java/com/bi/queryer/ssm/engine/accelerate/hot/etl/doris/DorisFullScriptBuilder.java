package com.bi.queryer.ssm.engine.accelerate.hot.etl.doris;

import com.bi.queryer.ssm.engine.accelerate.hot.etl.HotEtlScriptBuilder;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.hive.HiveRepairScriptBuilder;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableField;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:03 2024/8/19
 * @Description doris同步脚本构建器
 **/
public class DorisFullScriptBuilder extends HotEtlScriptBuilder {
    protected HotTableInfo hotTable = null;

    protected String hotTableSchema = "";
    protected String hotTableShortName = "";

    // 是否需要建分区表
    protected boolean isPartitionTable = true;

    // 是否是按日粒度进行分区
    protected boolean isPartitionByDay = true;

    public DorisFullScriptBuilder(HotTableInfo hotTable) {
        super(hotTable);
        this.hotTable = hotTable;
        String[] tableInfos = hotTable.getHotTableName().split("\\.");
        hotTableSchema = tableInfos[0];
        hotTableShortName = tableInfos[tableInfos.length - 1];
        isPartitionTable = hotTable.isPartitionTable();
        isPartitionByDay =  DateGranularity.DAY == DateGranularity.get(hotTable.getHotTableSource().getDateGranularity());
    }

    public EtlTableDdl buildScheduleScript(){
        // 建表
        DorisTableCreator tableCreator = new DorisTableCreator(hotTable);
        EtlTableDdl ddl = tableCreator.create();

        // 最近拼接建表+bulk load
        List<String> fragments = new ArrayList<>();

        fragments.add(String.format("use %s; \n", hotTableSchema));

        fragments.add("\n-- ==========前置api==========\n");
        fragments.add(buildPreApiSql()+ ";");

        fragments.add("\n-- ==========删除热表==========\n");
        fragments.add(ddl.getDropHotTableSql() + ";");
        fragments.add("\n-- ==========创建热表==========\n");
        fragments.add(ddl.getCreateHotTableSql()+ ";");

        fragments.add(" \n--splitflag=sql \n");

        fragments.add("\n-- ==========加载热表数据==========\n");
        // 构建插入sql
        String loadSql = buildBulkLoadScript(ddl);
        fragments.add(loadSql+ ";");

        fragments.add(String.format(" \n--splitflag=load,%s,%s \n", hotTableSchema, hotTableShortName));

        String scriptContent = BIUtil.listToStr(fragments, "");
        ddl.setEtlScriptContent(scriptContent);

        return ddl;
    }

    @Override
    public EtlTableDdl buildRepairScript(EtlTableDdl ddl) {
        DorisRepairScriptBuilder repairScriptBuilder = new DorisRepairScriptBuilder(this.hotTable);
        String script = repairScriptBuilder.build(ddl);
        ddl.setRepairScriptContent(script);
        return ddl;
    }

    protected String buildBulkLoadScript(EtlTableDdl ddl){
        String sql = new String();

        String partitionFieldName = ddl.getPartitionField().getName();

        List<EtlTableField> commonFields = ddl.getCommonFields();
        List<String> fieldNames = new ArrayList<>();
        List<String> fieldExpressions = new ArrayList<>();
        for(EtlTableField f : commonFields){
            fieldNames.add(f.getName());
            fieldExpressions.add(String.format("%s=%s", f.getName(), f.getName()));
        }

        List<String> sqlFragments = new ArrayList<>();
        sqlFragments.add(String.format("load label %s.${v_label}", hotTableSchema));
        sqlFragments.add(" ( "); // load begin

        String fileListString = BIUtil.listToStr(this.buildDataFileList(),",", "\"");
        sqlFragments.add(String.format(String.format("    data infile(%s)", fileListString, hotTableSchema, hotTableShortName)));
        sqlFragments.add(String.format("    into table %s format as \"orc\" (", hotTableShortName));
        sqlFragments.add(String.format("    %s", BIUtil.listToStr(fieldNames)));
        sqlFragments.add("    ) ");
        if(isPartitionTable) {
            sqlFragments.add(String.format("    columns from path as (%s)", partitionFieldName));
        }
        sqlFragments.add("    set (");
        sqlFragments.add(String.format("    %s", BIUtil.listToStr(fieldExpressions)));
        sqlFragments.add("    ) ");

        sqlFragments.add(buildWhereSql(partitionFieldName));
        sqlFragments.add(" ) "); // load end

        sqlFragments.add("with broker allbrokers (\"username\"=\"hive\",\"password\"=\"\")");

        sqlFragments.add("properties(");
        sqlFragments.add(BIUtil.listToStr(getSetParameters(), ",", "", "\n"));
        sqlFragments.add(")");

        // 最后拼接：每段需要换行
        sql = BIUtil.listToStr(sqlFragments, "\n");
        return sql;
    }

    protected String buildWhereSql(String partitionFieldName){
        return "";
    }

    protected List<String> getSetParameters(){
        List<String> setParameters = new ArrayList<>();
        setParameters.add("\"exec_mem_limit\"=\"10737418240\"");
        return setParameters;
    }

    protected  String buildPreApiSql() {

        String url = SC.v("hot.table.pre.api.url","");
        String apiSql = String.format("select bi_api_invoke('%s','{\"etlJobName\":\"%s\"}','application/json;charset=UTF-8',60) ",
                url,
                hotTable.getEtlJobName());
        return apiSql;
    }

    protected List<String> buildDataFileList(){
        List<String> fileList = new ArrayList<>();
        String file = "";
        if(isPartitionTable) {
            file = String.format("hdfs://ahdpns/user/hive/warehouse/%s.db/%s/*/*", hotTableSchema, hotTableShortName);
        }else {
            file = String.format("hdfs://ahdpns/user/hive/warehouse/%s.db/%s/*", hotTableSchema, hotTableShortName);
        }
        fileList.add(file);
        return fileList;
    }
}
