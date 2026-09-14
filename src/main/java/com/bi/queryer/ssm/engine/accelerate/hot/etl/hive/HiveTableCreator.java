package com.bi.queryer.ssm.engine.accelerate.hot.etl.hive;

import com.bi.queryer.ssm.engine.accelerate.hot.HotUtil;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableField;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.BIUtil;
import io.trino.jdbc.TrinoConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:02 2024/8/19
 * @Description 通过视图sql创建hive热表
 **/
public class HiveTableCreator {
    private HotTableInfo hotTable = null;

    private EtlTableDdl ddl = new EtlTableDdl();

    public HiveTableCreator(HotTableInfo hotTable){
        this.hotTable = hotTable;
    }
    public EtlTableDdl create(){
        // 先删除热表
        String dropSql = this.buildDropHotTableSql();
        ddl.setDropHotTableSql(dropSql);

        // 再创建热表
        String createSql = this.buildCreateHotTableSql();
        ddl.setCreateHotTableSql(createSql);

        return ddl;
    }

    /**
     * 创建建表sql
     * @return
     */
    protected String buildCreateHotTableSql(){
        StringBuilder sql = new StringBuilder();

        // 是否需要建分区表
        boolean isPartitionTable = hotTable.isPartitionTable();

        // 字段
        List<EtlTableField> fields = this.getHotTableFields();
        if(BIUtil.isEmpty(fields)) {
            return sql.toString();
        }
        List<EtlTableField> commonFields = new ArrayList<>();
        EtlTableField partitionField = null;
        for(EtlTableField f : fields){
            if(f.isPartition()){
                partitionField = f;
                // 若是分分区表，添加分区字段到常规字段中，便于后续过滤
                if(!isPartitionTable){
                    commonFields.add(f);
                }
            }else {
                commonFields.add(f);
            }
        }
        if(BIUtil.isEmpty(commonFields)){
            return sql.toString();
        }
        ddl.setCommonFields(commonFields);
        ddl.setPartitionField(partitionField);

        List<String> fieldStrings = new ArrayList<>();
        for(EtlTableField commonField : commonFields){
            String fieldString = String.format("%s %s comment '%s'", commonField.getName(), commonField.getDataType().toLowerCase(), commonField.getTitle());
            fieldStrings.add(fieldString);
        }

        sql.append(String.format("create table %s(", hotTable.getHotTableName())).append("\n");
        sql.append(BIUtil.listToStr(fieldStrings, "    ,", "", "\n"));
        sql.append(" ) ");
        sql.append(String.format(" comment '多维热化表(初始化热化范围：%s天,源：trino.%s)' \n ", hotTable.getHotTableSource().getHotDataInitDays(), hotTable.getSourceTableName()));
        if(isPartitionTable && partitionField != null) {
            sql.append(String.format(" partitioned by (%s string) \n", partitionField.getName()));
        }
        sql.append(" stored as orc ");
        return sql.toString();
    }


    protected List<EtlTableField> getHotTableFields(){
        List<EtlTableField> etlTableFields = this.fetchFieldFromSourceTable();
        List<MetaField> metaFields = this.getFieldDefinitionBySourceTable();
        if(BIUtil.isEmpty(etlTableFields) || BIUtil.isEmpty(metaFields)){
            return null;
        }
        Map<String, MetaField> metaFieldMap = metaFields.stream().collect(Collectors.toMap(MetaField::getName, f->f, (f1,f2)->f1));
        for(EtlTableField etlTableField : etlTableFields){
            MetaField metaField = metaFieldMap.get(etlTableField.getName());
            if(metaField != null){
                etlTableField.setTitle(metaField.getTitle());
                etlTableField.setPartition(Enabled.isTrue(metaField.getIsCommonDate()));
            }
        }
        return etlTableFields;
    }

    /**
     * 从源表中提取字段
     * @return
     */
    protected List<EtlTableField> fetchFieldFromSourceTable(){
        List<EtlTableField> etlTableFields = new ArrayList<>();
        String sourceTableSql = this.getSourceTableSql();
        ddl.setSourceTableSql(sourceTableSql);

        if(BIUtil.isEmpty(sourceTableSql)){
            return etlTableFields;
        }
        try{
            String sql = String.format("%s select t.* from (%s) t where 1=2", getSqlTips(), sourceTableSql);
            Statement stmt = null;
            ResultSet rs = null;
            Connection conn = null;
            try {
                conn = DBUtil.getConn(DataSourceType.Trino_Master);
                TrinoConnection tc =  conn.unwrap(TrinoConnection.class);
                // 设置查询超时时间为1分钟
                tc.setSessionProperty("query_max_run_time",  "60s");
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for(int i = 1; i <= columnCount; i++){
                    EtlTableField tf = new EtlTableField();
                    tf.setName(metaData.getColumnName(i));
                    tf.setDataType(DBUtil.getDataType(metaData.getColumnType(i)).toString());
                    etlTableFields.add(tf);
                }
            } catch (Throwable e) {
               throw new BIException(e);
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return etlTableFields;
    }

    /**
     * 获取源表后台的字段定义
     * @return
     */
    protected List<MetaField> getFieldDefinitionBySourceTable(){
        MetaTable metaTable = SSDMetaCacheManager.getTableByFullName(hotTable.getSourceTableName());
        if(metaTable == null){
            return null;
        }
        List<MetaField> metaFields = metaTable.getFields();
        return metaFields;
    }

    protected String getSourceTableSql(){
        try {
            StringBuilder sql = new StringBuilder();

            String[] tableInfos = hotTable.getSourceTableName().split("\\.");
            String schema = tableInfos[0];
            String tableShortName = tableInfos[tableInfos.length - 1];

            sql.append(this.getSqlTips());
            sql.append(" select t1.view_definition from INFORMATION_SCHEMA.VIEWS t1 ");
            sql.append(String.format(" where t1.table_schema = '%s'", schema));
            sql.append(String.format(" and t1.table_name = '%s' ", tableShortName));
            sql.append("and t1.view_definition not like '%bi_view%'");

            BaseDao dao = DBUtil.getBaseDao();
            List list = dao.queryObjectListByTrinoSQL(sql.toString(), new HashMap(), DataSourceType.Trino_Master);
            if(BIUtil.isNotEmpty(list)){
                BIMap map = (BIMap) list.get(0);
                String sourceSql = (String) map.values().iterator().next();
                if(BIUtil.isNotEmpty(sourceSql)) {
                    // 中文转义
                    sourceSql = HotUtil.escape(sourceSql);

                    String rectifySyntaxRule = SC.v("hot.table.trino.syntax.rectify.rule", "DECIMAL '([^']*)'=$1;hive3.=;BOTH FROM =");
                    sourceSql = HotUtil.rectifySyntax(sourceSql, rectifySyntaxRule);

                    // 替换 DECIMAL '6.0' -> 6.0
                    //sourceSql = sourceSql.replaceAll("DECIMAL '([^']*)'", "$1");

                    // 替换 hive3.
                    //sourceSql = sourceSql.replaceAll("hive3.", "");

                    // 替换BOTH FROM
                    //sourceSql = sourceSql.replaceAll("BOTH FROM ", "");
                }

                return sourceSql;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    protected String buildDropHotTableSql(){
        String sql = String.format("drop table if exists %s", hotTable.getHotTableName());
        return sql;
    }

    protected String getSqlTips(){
        return "/*ssm:hot*/ ";
    }

    public static void main(String[] args) {
        String sql = "(CASE WHEN (businessline = U&'\\4FDD\\517B') THEN finish_user END) by_finish_user";
        sql = HotUtil.escape(sql);
        System.out.println(sql);

        sql = "abc cast('agb' as varchar)";
        sql = sql.replaceAll("(?i) varchar"," string");
        System.out.println(sql);

        sql = "select DECIMAL '1.2' as x , lower(BOTH FROM y) as y from hive3.table1";
        String rule = "DECIMAL '([^']*)'=$1;hive3.=;BOTH FROM =";
        System.out.println(HotUtil.rectifySyntax(sql, rule));
    }
}
