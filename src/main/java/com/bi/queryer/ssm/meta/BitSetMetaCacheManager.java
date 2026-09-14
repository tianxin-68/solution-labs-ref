package com.bi.queryer.ssm.meta;

import com.bi.queryer.ssm.meta.event.MetadataCacheLoadedEvent;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:07 2025/12/24
 * @Description 元信息的bitset管理类
 **/
@Component
public class BitSetMetaCacheManager {
    // key=字段编码，value=字段可归属的索引
    protected static Map<String, DatasetFieldBitSet> datasetFieldBitSetMap = new Hashtable<>(1280);
    protected static Map<Integer, MetaTable> tableIndexMapping = new Hashtable<>(128);
    protected static Map<String, Integer> tableIndexAndIdMapping = new Hashtable<>(128);

    @EventListener(MetadataCacheLoadedEvent.class)
    public void init(MetadataCacheLoadedEvent event) {
        long t1 = System.currentTimeMillis();

        synchronized (this) {
            // 先清除缓存
            clear();

            this.createTablesIndex();
            this.createDatasetFieldsIndex();
        }

        long t2 = System.currentTimeMillis();
        System.out.println(String.format("bitset索引初始化耗时:%sms", (t2 - t1)));
    }

    private void clear() {
        datasetFieldBitSetMap.clear();
        tableIndexMapping.clear();
        tableIndexAndIdMapping.clear();
    }

    /**
     * 按表进入缓存的顺序进行索引
     */
    protected void createTablesIndex(){
        Map<String, MetaTable> tableMap = SSDMetaCacheManager.getTablesCache();
        if(BIUtil.isEmpty(tableMap)){
            return;
        }
        // 主子表的子表不参与索引
        Set<String> subtableIds = new HashSet<>(32);
        tableMap.keySet().forEach(tableId -> {
            List<MetaTablePriSubCfg> subTables = SSDMetaCacheManager.getTablePriSubCfgByTableId(tableId);
            if(BIUtil.isNotEmpty(subTables)){
                subtableIds.addAll(subTables.stream().map(MetaTablePriSubCfg::getSubTableId).collect(Collectors.toSet()));
            }
        });
        int index = -1;
        for(Map.Entry<String, MetaTable> entry : tableMap.entrySet()){
            // 排除所有子表
            if(subtableIds.contains(entry.getKey())){
                continue;
            }
            index++;
            tableIndexMapping.put(index, entry.getValue());
            tableIndexAndIdMapping.put(entry.getKey(), index);
        }
    }

    /**
     * 创建字段bitset索引：即字段可以在哪些表索引中可以出现，若字段所属的维度表有关联的事实表，则字段的bitset中添加对应的事实表索引
     */
    protected void createDatasetFieldsIndex(){
        Map<String, MetaDatasetTable> datasetTables = SSDMetaCacheManager.getDatasetTables();
        if(BIUtil.isEmpty(datasetTables)){
            return;
        }
        for(MetaDatasetTable metaDatasetTable : datasetTables.values()){
            List<MetaField> fields = metaDatasetTable.getAllFields();
            if(BIUtil.isEmpty(fields)){
                continue;
            }
            DatasetFieldBitSet datasetFieldBitSet = datasetFieldBitSetMap.getOrDefault(metaDatasetTable.getDatasetId(), new DatasetFieldBitSet(metaDatasetTable.getDatasetId()));
            for(MetaField field : fields){
                MetaTable table = SSDMetaCacheManager.getTable(field.getTableId());
                Integer tableIndex = tableIndexAndIdMapping.get(field.getTableId());
                if(table == null || tableIndex == null) {
                    continue;
                }
                BitSet fieldBitSet = datasetFieldBitSet.fieldsBitSet.getOrDefault(field.getCode(), new BitSet(128));
                fieldBitSet.set(tableIndex);
                if(!Enabled.isTrue(table.getIsFactTable())){
                    // 若是维度表,则将事实表都添加到bitset中
                    List<MetaTable> relTables = SSDMetaCacheManager.getRelationTables(table.getId());
                    for(MetaTable relTable : relTables){
                        Integer relTableIndex = tableIndexAndIdMapping.get(relTable.getId());
                        if(relTableIndex != null) {
                            fieldBitSet.set(relTableIndex);
                        }
                    }
                }
                datasetFieldBitSet.fieldsBitSet.put(field.getCode(), fieldBitSet);
            }
            datasetFieldBitSetMap.put(metaDatasetTable.getDatasetId(), datasetFieldBitSet);
        }
    }


    /**
     * 获取表索引映射
     * @return key=表ID，value=表索引
     */
    public synchronized static Map<String, Integer> getTableIndexAndIdMapping() {
        return tableIndexAndIdMapping;
    }

    public synchronized static Map<Integer, MetaTable> getTableIndexMapping() {
        return tableIndexMapping;
    }

    /**
     * 根据表ID获取表索引
     * @param tableId 表ID
     * @return 表索引，如果不存在则返回null
     */
    public static Integer getTableIndex(String tableId) {
        if (BIUtil.isEmpty(tableId)) {
            return null;
        }
        return tableIndexAndIdMapping.get(tableId);
    }

    public synchronized static Map<String, DatasetFieldBitSet> getDatasetFieldBitSetMap() {
        return datasetFieldBitSetMap;
    }

    public static void setDatasetFieldBitSetMap(Map<String, DatasetFieldBitSet> datasetFieldBitSetMap) {
        BitSetMetaCacheManager.datasetFieldBitSetMap = datasetFieldBitSetMap;
    }

    public static class DatasetFieldBitSet{
        public String datasetId = "";
        // key=字段code，value=字段绑定表的索引
        public Map<String, BitSet> fieldsBitSet =  new HashMap<>(128);

        public DatasetFieldBitSet(String datasetId){
            this.datasetId = datasetId;
        }
    }
}
