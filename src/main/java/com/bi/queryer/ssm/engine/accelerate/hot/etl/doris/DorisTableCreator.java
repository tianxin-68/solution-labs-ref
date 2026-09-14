package com.bi.queryer.ssm.engine.accelerate.hot.etl.doris;

import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableDdl;
import com.bi.queryer.ssm.engine.accelerate.hot.etl.model.EtlTableField;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdl;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.mgr.hot.HotTableDdlService;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.db.DBType;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 17:03 2024/8/19
 * @Description doris建表
 **/
public class DorisTableCreator {
    private HotTableInfo hotTable = null;

    private EtlTableDdl ddl = new EtlTableDdl();

    public DorisTableCreator(HotTableInfo hotTable){
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

    protected String buildDropHotTableSql(){
        String sql = String.format("drop table if exists %s", hotTable.getHotTableName());
        return sql;
    }

    protected String buildCreateHotTableSql(){
        StringBuilder sql = new StringBuilder();

        // 是否需要建分区表
        boolean isPartitionTable = hotTable.isPartitionTable();

        // 是否是按日粒度进行分区
        boolean isPartitionByDay =  DateGranularity.DAY == DateGranularity.get(hotTable.getHotTableSource().getDateGranularity());

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

        for(EtlTableField f : fields){
            String fieldString = String.format("%s %s comment '%s'", f.getName(), f.getDataType().toLowerCase(), f.getTitle());
            fieldStrings.add(fieldString);
        }

        sql.append(String.format("create table %s(", hotTable.getHotTableName())).append("\n");
        sql.append(BIUtil.listToStr(fieldStrings, "    ,", "", "\n"));
        sql.append(")");

        // 添加doris参数设置
        List<String> settings = new ArrayList<>();
        settings.add(" engine=olap ");
        settings.add(String.format(" duplicate key(`%s`) ", partitionField.getName()));
        settings.add(String.format(" comment '多维热化表(初始化热化范围：%s天,源：trino.%s)' ", hotTable.getHotTableSource().getHotDataInitDays(), hotTable.getSourceTableName()));

        // 分区表添加分区属性
        if(isPartitionTable) {
            if(isPartitionByDay) {
                // 日粒度：通过date类型range动态分区
                settings.add(String.format(" partition by range(`%s`)", partitionField.getName()));
            }else{
                // 非日粒度：通过varchar类型自动分期
                settings.add(String.format(" auto partition by list(`%s`)", partitionField.getName()));
            }
            settings.add(" () ");
        }
        settings.add(String.format(" distributed by hash(`%s`) buckets  1 ", partitionField.getName()));

        List<KeyValuePair> properties = new ArrayList<>();
        properties.add(new KeyValuePair("replication_allocation", "tag.location.default: 3"));
        properties.add(new KeyValuePair("storage_format", "V2"));
        properties.add(new KeyValuePair("light_schema_change", "true"));
        properties.add(new KeyValuePair("disable_auto_compaction", "false"));

        // 分区表添加分区属性
        if(isPartitionTable && isPartitionByDay) {
            // 添加日粒度的动态分区属性
            properties.add(new KeyValuePair("dynamic_partition.enable", "true"));
            properties.add(new KeyValuePair("dynamic_partition.time_unit", "DAY"));
            properties.add(new KeyValuePair("dynamic_partition.time_zone", "Asia/Shanghai"));
            properties.add(new KeyValuePair("dynamic_partition.start", -1 * hotTable.getHotTableSource().getHotDataMaxDays()));
            properties.add(new KeyValuePair("dynamic_partition.end", "1"));
            properties.add(new KeyValuePair("dynamic_partition.prefix", partitionField.getName()));
            properties.add(new KeyValuePair("dynamic_partition.replication_allocation", "tag.location.default: 3"));
            properties.add(new KeyValuePair("dynamic_partition.buckets", "1"));
            properties.add(new KeyValuePair("dynamic_partition.create_history_partition", "true"));
            properties.add(new KeyValuePair("dynamic_partition.history_partition_num", "-1"));
            properties.add(new KeyValuePair("dynamic_partition.hot_partition_num", "0"));
            properties.add(new KeyValuePair("dynamic_partition.reserved_history_periods", "NULL"));
            properties.add(new KeyValuePair("dynamic_partition.storage_policy", ""));
        }

        settings.add(" properties ( ");
        List<String> kvs = new ArrayList<>();
        properties.forEach(p -> kvs.add(String.format("\"%s\"=\"%s\"", p.getKey(), p.getValue())));
        settings.add(BIUtil.listToStr(kvs, "    ,", "", "\n"));
        settings.add(" ) ");

        sql.append(BIUtil.listToStr(settings, "\n"));

        return sql.toString();
    }

    protected List<EtlTableField> getHotTableFields(){
        HotTableDdlService ddlService = (HotTableDdlService) SpringContextUtil.getBean("hotTableDdlService");
        HotTableDdl tableDdl = ddlService.get(hotTable.getHotTableName(), DBType.Trino.toString().toLowerCase()); // 此处使用trino建表的字段信息：因为trino表先创建
        if(tableDdl == null){
            return null;
        }
        // 常规字段
        String fieldList = tableDdl.getFieldList();
        String partitionBy = tableDdl.getPartitionBy();
        if(BIUtil.isEmpty(fieldList)){
            return null;
        }

        List<EtlTableField> allFields = new ArrayList<>();
        // 先添加分区字段，再添加常规字段
        // 分区字段:此处数据类型指定为日期，便于动态分区
        EtlTableField partitionField = null;
        DateGranularity dateGranularity = DateGranularity.get(hotTable.getHotTableSource().getDateGranularity());
        if(dateGranularity == DateGranularity.DAY){
            partitionField = new EtlTableField(partitionBy, DataType.Date.toString(), "日期分区");
        }else {
            // 非日粒度使用varchar数据类型，且使用list分区模式
            partitionField = new EtlTableField(partitionBy, "varchar(50)", "日期分区");
        }
        partitionField.setPartition(true);

        allFields.add(partitionField);

        // 常规字段
        List<EtlTableField> commonFields = JSONArray.parseArray(fieldList, EtlTableField.class);
        for(EtlTableField f : commonFields){
            if(DataType.getType(f.getDataType()) == DataType.String){
                f.setDataType("string");
            }

            if(f.getName().equals(partitionField.getName())){
                // 分区字段已添加：避免重复添加
                continue;
            }
            allFields.add(f);
        }
//        allFields.addAll(commonFields);

        return allFields;
    }
}
