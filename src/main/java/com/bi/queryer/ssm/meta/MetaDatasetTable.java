package com.bi.queryer.ssm.meta;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.*;

/**
 * @Author contributor
 * @Date 14:37 2024/12/23
 * @Description 数据集、模块、表关联关系
 **/
public class MetaDatasetTable {
    private String datasetId;

    private Map<String, MetaTable> factTables = new HashMap<>(100);

    private Map<String, MetaTable> dimTables = new HashMap<>(50);

    private Map<String, Map<String, MetaTable>> moduleFactTables = new HashMap<>(30);

    private Map<String, Map<String, MetaTable>> moduleDimTables = new HashMap<>(30);

    public MetaDatasetTable(String datasetId) {
        this.datasetId = datasetId;
    }

    public void addTable(MetaTable table){
        if(table == null){
            return;
        }
        if(Enabled.isTrue(table.getIsFactTable())){
            factTables.put(table.getId(), table);
        }else {
            dimTables.put(table.getId(), table);
        }
    }

    public void addTable(MetaTable table, Set<String> moduleIds){
        if(table == null){
            return;
        }
        this.addTable(table);
        if(BIUtil.isEmpty(moduleIds)){
            return;
        }
        for(String moduleId : moduleIds){
            if(Enabled.isTrue(table.getIsFactTable())){
                Map<String, MetaTable> tables = moduleFactTables.getOrDefault(moduleId, new HashMap<>(50));
                tables.put(table.getId(), table);
                moduleFactTables.put(moduleId, tables);
            }else {
                Map<String, MetaTable> tables = moduleDimTables.getOrDefault(moduleId, new HashMap<>(50));
                tables.put(table.getId(), table);
                moduleDimTables.put(moduleId, tables);
            }
        }
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Map<String, MetaTable> getFactTables() {
        return factTables;
    }

    public Map<String, MetaTable> getDimTables() {
        return dimTables;
    }

    public Map<String, MetaTable> getModuleFactTables(String moduleId) {
        if(moduleId == null){
            return null;
        }
        return moduleFactTables.get(moduleId);
    }

    public Map<String, MetaTable> getModuleFactTables(Set<String> moduleIds) {
        Map<String, MetaTable> moduleTables = new HashMap<>(50);
        for (String moduleId : moduleIds){
            Map<String, MetaTable> tables = getModuleFactTables(moduleId);
            if(BIUtil.isNotEmpty(tables)){
                moduleTables.putAll(tables);
            }
        }
        return moduleTables;
    }

    public Map<String, MetaTable> getModuleDimTables(String moduleId) {
        return moduleDimTables.get(moduleId);
    }

    public Map<String, MetaTable> getModuleDimTables(Set<String> moduleIds) {
        Map<String, MetaTable> moduleTables = new HashMap<>(50);
        for (String moduleId : moduleIds){
            Map<String, MetaTable> tables = getModuleDimTables(moduleId);
            if(BIUtil.isNotEmpty(tables)){
                moduleTables.putAll(tables);
            }
        }
        return moduleTables;
    }

    public List<MetaField> getAllFields(){
        List<MetaField> fields = new ArrayList<>();
        List<MetaTable> allTables = new ArrayList<>();
        allTables.addAll(factTables.values());
        allTables.addAll(dimTables.values());

        for(MetaTable table : allTables){
            fields.addAll(table.getFields());
        }
        return fields;
    }

}
