package com.bi.queryer.ssm.engine.model.creator;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.accelerate.route.TablePriSubCfgRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 16:04 2025/12/24
 * @Description 星型模型创建器
    1、通过bitset匹配后的表进行路由
    2、可支持按一定规则来路由表：指标/维度组合规则、表组合规则、CBO
    3、支持按字段、表的优先级路由，有静态、动态优先级
    4、覆盖各种场景：
    - 白皮书编码换绑：编码A关联在表A，将编码A关联到表B上
    - 多个表有相同白皮书编码时，路由不优
    - 同一个白皮书指标，可能是表的原生字段，也可能是派生字段，如：sum(case when dim='xx'  then f1 else null end)
    - 后台计算字段：计算维度、夸模型计算指标
    - 前端计算字段：计算维度、计算指标
    - 过滤：维度、计算维度、指标、计算指标、lod指标
    - 主子表的逻辑兼容
    - 行级权限注入
 --------------------------------
 元数据区分：
 1、自定义维度：
 - 后台配置的自定义维度：引用维度来源事实表或维度表，模型构建时此维度可作为一个整体，即自己的star模型中需要有相关引用字段，其他star模型可不需要
 - 前台配置的自定义维度：需要所有模型都支持引用的维度

 2、自定义指标（四则运算）
 - 后台配置的自定义指标：引用的字段（维度或指标）来源于事实表或维度表，模型构建时此维度可作为一个整体，即自己的star模型中需要有相关引用字段，其他star模型可不需要
 - 前台配置的自定义指标：需要star模型覆盖所有维度（含前台自定义维度），且引用指标需被模型覆盖

 3、跨模型指标：拆分为多个引用指标

 4、lod指标：？？

 5、 四则运算中包含了维度和指标：？？

 6、lod四则

 7、分析字段的四则：同环占比的四则

--------------------------------
 问题：
 1、无匹配的表组合时的原因提示
 2、字段优先级
 **/
public class StarModelCreator extends JoinModelCreator{
    public StarModelCreator(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }

    @Override
    public List<StarModel> create() {
        if(BIUtil.isNotEmpty(models)){
            return models;
        }
        // 通过config中的结果、过滤字段编码
        List<QueryField> allFields = this.getAllQueryFields();
        // 获取数据集下的字段bitset
        Map<String, BitSet> fieldBitSetMap = this.getFieldBitSetMap(config.getSettings().getDatasetId());

        // 表索引与表id的映射关系
        Map<String, Integer> tableIndexAndIdMapping = BitSetMetaCacheManager.getTableIndexAndIdMapping();
        if (BIUtil.isEmpty(allFields) || BIUtil.isEmpty(fieldBitSetMap) || BIUtil.isEmpty(tableIndexAndIdMapping)) {
            return super.create();
        }

        // 匹配维度和指标的表bitsets
        List<BitSet> matchedTableBitSets = this.getMatchedTableBitSets(allFields, fieldBitSetMap);
        //debugPrintTablesByBitSet(matchedTableBitSets);

        // 最佳匹配表索引
        List<Integer> matchedTableIndexes = this.getBestMatchedTableIndexes(matchedTableBitSets, allFields, fieldBitSetMap);
        debugPrintTablesByIndex(matchedTableIndexes);

        // 通过最近表索引，创建星型模型
        models = createStarModels(matchedTableIndexes, allFields);

        JoinModelOptimizer optimizer = new JoinModelOptimizer(config, models);
        optimizer.optimize();

        //主子表替换
        TablePriSubCfgRouter.route(config, models);

        //构建模型实时表的更新时间
        buildModelDataUpdateTime();

        return models;
    }

    // 创建星型模型
    private List<StarModel> createStarModels(List<Integer> matchedTableIndexes, List<QueryField> allFields) {
        if (BIUtil.isEmpty(matchedTableIndexes)) {
            return new ArrayList<>();
        }

        int index = 1;
        Map<Integer, MetaTable> tableIndexMapping = BitSetMetaCacheManager.getTableIndexMapping();
        List<StarModel> starModels = new ArrayList<>(matchedTableIndexes.size());
        for (Integer tableIndex : matchedTableIndexes) {
            MetaTable metaTable = tableIndexMapping.get(tableIndex);
            QueryTable factTable = buildQueryTable(metaTable, allFields);
            // 在事实表出了的，就不需要在维表里出了
            List<QueryField> dimTableFields = new ArrayList<>(allFields);
            dimTableFields.removeAll(factTable.getFields());
            Map<String, QueryTable> dimTables = buildDimTables(metaTable.getId(), dimTableFields);
            StarModel starModel = createModel(factTable, dimTables);
            starModel.setIndex(index);
            starModel.setFactTable(factTable);
            starModels.add(starModel);
            index++;
        }
        return starModels;
    }

    // 创建查询的表
    private QueryTable buildQueryTable(MetaTable metaTable, List<QueryField> allFields) {
        QueryTable queryTable = new QueryTable(metaTable);
        Map<String, MetaField> fieldMap = SSDMetaCacheManager.getTableFieldMap(metaTable.getId());
        for (QueryField field : allFields) {
            MetaField metaField = fieldMap.get(field.getCode());
            if (metaField != null) {
                if (Objects.equals(field.getId(), metaField.getId())) {
                    queryTable.addField(field);
                } else {
                    QueryField sameCodeDimField = field.clone();
                    sameCodeDimField.setMeta(metaField);
                    sameCodeDimField.init();
                    queryTable.addField(sameCodeDimField);
                }
            }
        }

        return queryTable;
    }

    // 创建星型模型中的维表
    private Map<String, QueryTable> buildDimTables(String factTableId, List<QueryField> allFields) {
        List<MetaTableRelation> relations = SSDMetaCacheManager.getRelations(factTableId);
        Map<String, QueryTable> dimTables = new HashMap<>(relations.size());
        for (MetaTableRelation relation : relations) {
            MetaTable dimTable = SSDMetaCacheManager.getTable(relation.getSubTableId());
            QueryTable dimTableQueryTable = buildQueryTable(dimTable, allFields);
            if (BIUtil.isEmpty(dimTableQueryTable.getFields())) {
                // 此维表不出任何字段, 不加入到星型模型中
                continue;
            }
            dimTables.put(relation.getSubTableId(), dimTableQueryTable);
        }
        return dimTables;
    }

    /** genAI_tuning/优化互斥性能_start */
    /**
     * 通过已匹配所有字段的表索引bitset集合，找到最佳匹配的表索引
     * 表字段查询逻辑：多个表通过相同的维度进行join后带出各自表的特有指标
     * 最佳匹配逻辑：
     * 1、尽量减少表的数量，降低不必要的表join（贪心算法）
     * 2、支持规则逻辑：如field1，field2同时查询时，优先走表A
     * 3、支持字段权重：权重越大优先权重大的字段所在表
     *
     * @param matchedTableBitSets 每个BitSet表示一个能匹配所有维度和至少一个指标的表组合
     * @param allFields 所有查询字段，用于计算字段权重
     * @param fieldBitSetMap 字段bitset映射，用于匹配规则
     * @return 最佳匹配的表索引列表
     */
    protected List<Integer> getBestMatchedTableIndexes(List<BitSet> matchedTableBitSets, List<QueryField> allFields, Map<String, BitSet> fieldBitSetMap){
        if(BIUtil.isEmpty(matchedTableBitSets)){
            return new ArrayList<>();
        }

        // 如果只有一个匹配的BitSet，直接返回其中的表索引
        if(matchedTableBitSets.size() == 1){
            BitSet singleBitSet = matchedTableBitSets.get(0);
            return extractTableIndexes(singleBitSet);
        }

        // 合并所有BitSet，找到能覆盖所有指标的表组合
        BitSet unionBitSet = new BitSet(128);
        for(BitSet bitSet : matchedTableBitSets){
            unionBitSet.or(bitSet);
        }

        // 如果合并后的BitSet为空，返回空列表
        if(unionBitSet.isEmpty()){
            return new ArrayList<>();
        }

        // 使用贪心算法：选择能覆盖所有指标且表数量最少的组合
        // 策略：优先选择事实表，然后选择能覆盖最多未覆盖指标的表
        List<Integer> allTableIndexes = extractTableIndexes(unionBitSet);
        
        // 按表类型和覆盖能力排序：事实表优先，表数量少的优先
        Map<String, Integer> tableIndexAndIdMapping = BitSetMetaCacheManager.getTableIndexAndIdMapping();
        if(BIUtil.isEmpty(tableIndexAndIdMapping)){
            return allTableIndexes;
        }

        // 分离事实表和维度表，并计算每个表的权重和规则优先级
        List<TableIndexWithPriority> factTableList = new ArrayList<>();
        List<TableIndexWithPriority> dimTableList = new ArrayList<>();
        
        for(Integer tableIndex : allTableIndexes){
            String tableId = getTableIdByIndex(tableIndex, tableIndexAndIdMapping);
            if(BIUtil.isEmpty(tableId)){
                continue;
            }
            MetaTable metaTable = SSDMetaCacheManager.getTable(tableId);
            if(metaTable == null){
                continue;
            }
            
            // 计算表的优先级：规则优先级 > 字段权重 > 表权重
            TableIndexWithPriority tablePriority = new TableIndexWithPriority();
            tablePriority.tableIndex = tableIndex;
            tablePriority.tableId = tableId;
            
            // 1. 规则逻辑优先级（如field1，field2同时查询时，优先走表A）
            tablePriority.rulePriority = getRulePriority(tableId, allFields, fieldBitSetMap);
            
            // 2. 字段权重：计算该表能覆盖的字段的总权重
            tablePriority.fieldWeight = calculateTableFieldWeight(tableIndex, allFields, fieldBitSetMap);
            
            // 3. 表权重
            Double tableWeight = SSDMetaCacheManager.getMaxWeight(tableId);
            tablePriority.tableWeight = tableWeight != null ? tableWeight : 0.0;
            
            if(Enabled.isTrue(metaTable.getIsFactTable())){
                factTableList.add(tablePriority);
            } else {
                dimTableList.add(tablePriority);
            }
        }

        // 按优先级排序：规则优先级 > 字段权重 > 表权重 > 事实表优先
        factTableList.sort((a, b) -> compareTablePriority(a, b, true));
        dimTableList.sort((a, b) -> compareTablePriority(a, b, false));

        // 贪心算法：选择最少的表组合来覆盖所有指标
        List<Integer> selectedIndexes = new ArrayList<>();
        BitSet coveredBitSet = new BitSet(128);
        
        // 优先选择事实表（已按优先级排序）
        for(TableIndexWithPriority factTable : factTableList){
            Integer factIndex = factTable.tableIndex;
            BitSet factBitSet = createSingleTableBitSet(factIndex);
            // 检查这个事实表是否能覆盖至少一个未覆盖的指标
            BitSet temp = (BitSet) factBitSet.clone();
            temp.and(unionBitSet);
            if(!temp.isEmpty() && !isFullyCovered(temp, coveredBitSet)){
                selectedIndexes.add(factIndex);
                coveredBitSet.or(temp);
                // 如果已经覆盖所有指标，可以提前退出
                if(isAllCovered(coveredBitSet, matchedTableBitSets)){
                    break;
                }
            }
        }

        // 如果事实表已经覆盖所有指标，直接返回
        if(isAllCovered(coveredBitSet, matchedTableBitSets)){
            return selectedIndexes;
        }

        // 如果事实表不能完全覆盖，添加必要的维度表（已按优先级排序）
        for(TableIndexWithPriority dimTable : dimTableList){
            Integer dimIndex = dimTable.tableIndex;
            BitSet dimBitSet = createSingleTableBitSet(dimIndex);
            BitSet temp = (BitSet) dimBitSet.clone();
            temp.and(unionBitSet);
            if(!temp.isEmpty() && !isFullyCovered(temp, coveredBitSet)){
                selectedIndexes.add(dimIndex);
                coveredBitSet.or(temp);
                // 如果已经覆盖所有指标，可以提前退出
                if(isAllCovered(coveredBitSet, matchedTableBitSets)){
                    break;
                }
            }
        }

        // 如果仍然没有完全覆盖，返回所有表索引（降级方案）
        if(!isAllCovered(coveredBitSet, matchedTableBitSets)){
            return allTableIndexes;
        }

        return selectedIndexes;
    }

    /**
     * 从BitSet中提取表索引列表
     */
    private List<Integer> extractTableIndexes(BitSet bitSet){
        List<Integer> indexes = new ArrayList<>();
        for(int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)){
            indexes.add(i);
        }
        return indexes;
    }

    /**
     * 通过表索引获取表ID
     */
    private String getTableIdByIndex(Integer tableIndex, Map<String, Integer> tableIndexAndIdMapping){
        for(Map.Entry<String, Integer> entry : tableIndexAndIdMapping.entrySet()){
            if(entry.getValue().equals(tableIndex)){
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 创建单个表的BitSet
     */
    private BitSet createSingleTableBitSet(Integer tableIndex){
        BitSet bitSet = new BitSet(128);
        bitSet.set(tableIndex);
        return bitSet;
    }

    /**
     * 检查temp是否完全被coveredBitSet覆盖
     */
    private boolean isFullyCovered(BitSet temp, BitSet coveredBitSet){
        BitSet check = (BitSet) temp.clone();
        check.andNot(coveredBitSet);
        return check.isEmpty();
    }

    /**
     * 检查coveredBitSet是否覆盖了所有matchedTableBitSets
     */
    private boolean isAllCovered(BitSet coveredBitSet, List<BitSet> matchedTableBitSets){
        for(BitSet matchedBitSet : matchedTableBitSets){
            BitSet temp = (BitSet) matchedBitSet.clone();
            temp.and(coveredBitSet);
            if(temp.isEmpty()){
                return false;
            }
        }
        return true;
    }

    /**
     * 表索引和优先级信息
     */
    private static class TableIndexWithPriority {
        Integer tableIndex;
        String tableId;
        int rulePriority = 0;  // 规则优先级，越大越优先
        double fieldWeight = 0.0;  // 字段权重总和
        double tableWeight = 0.0;  // 表权重
    }

    /**
     * 获取规则优先级
     * 规则逻辑：如field1，field2同时查询时，优先走表A
     * TODO: 后续可以从配置中读取规则
     * 
     * @param tableId 表ID
     * @param allFields 所有查询字段
     * @param fieldBitSetMap 字段bitset映射
     * @return 规则优先级，越大越优先
     */
    private int getRulePriority(String tableId, List<QueryField> allFields, Map<String, BitSet> fieldBitSetMap){
        // TODO: 实现规则匹配逻辑
        // 示例：如果field1和field2同时存在，且都在表A中，则表A的规则优先级+1
        // 这里先返回0，后续可以根据实际规则配置实现
        return 0;
    }

    /**
     * 计算表的字段权重总和
     * 权重越大优先权重大的字段所在表
     * 
     * @param tableIndex 表索引
     * @param allFields 所有查询字段
     * @param fieldBitSetMap 字段bitset映射
     * @return 字段权重总和
     */
    private double calculateTableFieldWeight(Integer tableIndex, List<QueryField> allFields, Map<String, BitSet> fieldBitSetMap){
        double totalWeight = 0.0;
        
        for(QueryField field : allFields){
            if(!field.isActive() || field.isCustomMeasure() || Enabled.value(field.getIsAnalysis())){
                continue;
            }
            String fieldCode = field.getCode();
            if(BIUtil.isEmpty(fieldCode)){
                continue;
            }
            
            // 检查该字段是否在该表中
            BitSet fieldBitSet = fieldBitSetMap.get(fieldCode);
            if(fieldBitSet == null || fieldBitSet.isEmpty()){
                continue;
            }
            
            // 检查字段的bitset是否包含该表索引
            if(fieldBitSet.get(tableIndex)){
                // 获取字段权重
                if(field.getMeta() != null && field.getMeta().getWeight() != null){
                    int weight = field.getMeta().getWeight();
                    totalWeight += weight;
                }
            }
        }
        
        return totalWeight;
    }

    /**
     * 比较两个表的优先级
     * 优先级顺序：规则优先级 > 字段权重 > 表权重 > 事实表优先
     * 
     * @param a 表A
     * @param b 表B
     * @param isFactTable 是否都是事实表（用于同类型表比较）
     * @return 比较结果
     */
    private int compareTablePriority(TableIndexWithPriority a, TableIndexWithPriority b, boolean isFactTable){
        // 1. 规则优先级：越大越优先
        if(a.rulePriority != b.rulePriority){
            return Integer.compare(b.rulePriority, a.rulePriority);
        }
        
        // 2. 字段权重：越大越优先
        if(Double.compare(a.fieldWeight, b.fieldWeight) != 0){
            return Double.compare(b.fieldWeight, a.fieldWeight);
        }
        
        // 3. 表权重：越大越优先
        if(Double.compare(a.tableWeight, b.tableWeight) != 0){
            return Double.compare(b.tableWeight, a.tableWeight);
        }
        
        // 4. 如果都是事实表或都是维度表，按表ID排序（保证稳定性）
        return a.tableId.compareTo(b.tableId);
    }
    /** genAI_tuning/优化互斥性能_end */



    /**
     * 获取匹配指标和维度的表索引bitset集合
     * @param allFields
     * @param fieldBitSetMap
     * @return
     */
    protected List<BitSet> getMatchedTableBitSets(List<QueryField> allFields, Map<String, BitSet> fieldBitSetMap){
        // 所有匹配的bitset：匹配所有维度且包含至少一个指标
        List<BitSet> matchedBitSets = new ArrayList<>();

        List<QueryField> allQueryFields = allFields.stream().filter(f-> f.isActive() && !Enabled.value(f.getIsAnalysis())).distinct().collect(Collectors.toList());

        // 所有维度字段
        List<QueryField> dimensionFields = allQueryFields.stream().filter(f -> f.isDimension()).collect(Collectors.toList());
        // 如果存在维度字段，计算所有维度字段的bitset交集
        BitSet dimensionIntersection = null;
        for (QueryField dimField : dimensionFields) {
            if(dimField.isCustomDimension()){
                // 排除自定义维度
                // 1、引用字段也在遍历的维度字段中，不需重复判断
                // 2、自定义字段没有对应的bitset
                continue;
            }
            // 从计算指标（派生指标）的附加维度不需要处理，因为不是所有模型都需要此附加维度
            // 场景：指标A为表A的原生字段，但指标B为表B的派生维度指标，此时派生的维度在表A中不一定存在
            if(this.isAppendDimensionFromMeasureField(dimField)){
                continue;
            }
            BitSet dimFieldBitSet = fieldBitSetMap.get(dimField.getCode());
            if(BIUtil.isEmpty(dimFieldBitSet)) {
                continue;
            }
            if(dimensionIntersection == null) {
                // 第一个维度字段，直接克隆作为初始交集
                dimensionIntersection = (BitSet) dimFieldBitSet.clone();
            } else {
                dimensionIntersection.and(dimFieldBitSet);
                // 如果交集为空，说明维度字段互斥
                if (dimensionIntersection.isEmpty()) {
                    break;
                }
            }
        }

        // 有维度但没有表交集：即没有表都支持所有维度
        if(BIUtil.isNotEmpty(dimensionFields) && BIUtil.isEmpty(dimensionIntersection)){
            return matchedBitSets;
        }

        // 取出所有指标字段
        List<QueryField> measureFields = new ArrayList<>(allQueryFields.size());
        for (QueryField field : allQueryFields) {
            if(field.isMeasure()){
                if(field.isAppend()){
                    // 附加指标不参与选表:派生指标
                    // 注意：前端的自定义计算指标的引用指标在UICalcFieldNormalizer中已经默认带入指标区域，估这些引用指标append=false。此类指标是需要参与选表。
                    continue;
                }
                if (field.isCustom()) {
                    // 前端配置自定义计算字段加直接依赖的字段, 这些字段的append=false也需要参入选表
                    measureFields.addAll(field.getCusCalcDependFields());
                } else {
                    measureFields.add(field);
                }
            }
        }
        for(QueryField measureField : measureFields) {
            /*
            if(measureField.isCustomMeasure()){
                // 排除前端自定义指标
                // 1、引用字段也在遍历的指标字段中，不需重复判断
                // 2、自定义字段没有对应的bitset
                continue;
            }
            // 附加指标不参与选表:派生指标
            // 注意：前端的自定义计算指标的引用指标在UICalcFieldNormalizer中已经默认带入指标区域，估这些引用指标append=false。此类指标是需要参与选表。
            if(measureField.isAppend()){
                continue;
            }*/
            BitSet measureFieldBitSet = fieldBitSetMap.get(measureField.getCode());
            if (BIUtil.isEmpty(measureFieldBitSet)) {
                return matchedBitSets;
            }
            // 如果没有维度字段，直接使用指标的bitset
            if(dimensionIntersection == null){
                matchedBitSets.add(measureFieldBitSet);
            } else {
                BitSet intersection = (BitSet) dimensionIntersection.clone();
                intersection.and(measureFieldBitSet);
                if (!intersection.isEmpty()) {
                    matchedBitSets.add(intersection);
                }
            }
        }
        matchedBitSets = matchedBitSets.stream().distinct().collect(Collectors.toList());
        return matchedBitSets;
    }

    /**
     * 获取数据集下的字段bitset
     * @param datasetId
     * @return
     */
    protected Map<String, BitSet> getFieldBitSetMap(String datasetId){
        Map<String, BitSet> fieldBitSetMap = new HashMap<>();
        Map<String, BitSetMetaCacheManager.DatasetFieldBitSet> datasetFieldBitSetMap = BitSetMetaCacheManager.getDatasetFieldBitSetMap();

        if(BIUtil.isEmpty(datasetFieldBitSetMap)){
            return fieldBitSetMap;
        }

        BitSetMetaCacheManager.DatasetFieldBitSet datasetFieldBitSet = datasetFieldBitSetMap.get(datasetId);
        if(datasetFieldBitSet == null || BIUtil.isEmpty(datasetFieldBitSet.fieldsBitSet)){
            return fieldBitSetMap;
        }

        fieldBitSetMap = datasetFieldBitSet.fieldsBitSet;
        return fieldBitSetMap;
    }

    protected List<QueryField> getAllQueryFields(){
        List<QueryField> allFields = new ArrayList<>();
        if(config.getResult() != null && BIUtil.isNotEmpty(config.getResult().getFields())){
            allFields.addAll(config.getResult().getFields());
        }
        if(config.getFilter() != null && BIUtil.isNotEmpty(config.getFilter().getFields())){
            allFields.addAll(config.getFilter().getFields());
        }
        return allFields;
    }


    @Override
    public ModelValidateResult validate(boolean isNeedInvalidInfo) {
        models = create();
        return super.validate(isNeedInvalidInfo);
    }

    protected void debugPrintTablesByBitSet(List<BitSet> fieldBitSets){
        fieldBitSets.stream().forEach(fieldBitSet -> {debugPrintTables(fieldBitSet);});
    }

    protected void debugPrintTables(BitSet fieldBitSet){
        int index = fieldBitSet.nextSetBit(0); // 从索引0开始找第一个置位
        System.out.println("---------------begin----------------");
        while (index != -1) {
            debugPrintTable(index);
            index = fieldBitSet.nextSetBit(index + 1);
        }
        System.out.println("---------------end----------------");
    }

    protected void debugPrintTablesByIndex(List<Integer> tableIndexes){
        System.out.println("---------------begin----------------");
        for(int index : tableIndexes){
            debugPrintTable(index);
        }
        System.out.println("---------------end----------------");
    }

    protected void debugPrintTable(Integer tableIndex){
        Map<Integer, MetaTable> tableIndexMapping = BitSetMetaCacheManager.getTableIndexMapping();
        MetaTable metaTable = tableIndexMapping.get(tableIndex);
        if(metaTable == null) {
            return;
        }
        System.out.println("置位索引：" + tableIndex + " → 对应表:" + metaTable.getFullName());
    }
}
