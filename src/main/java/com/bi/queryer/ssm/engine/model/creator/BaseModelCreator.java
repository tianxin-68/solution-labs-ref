package com.bi.queryer.ssm.engine.model.creator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.MetaTableRelation;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 18:40 2022-11-01
 * @Description 模型创建器
 **/
public class BaseModelCreator {
    /**
     * 查询模板
     */
    protected QueryConfigure config;

    protected QueryContext cxt;

    public BaseModelCreator(QueryConfigure config, QueryContext cxt){
        this.config = config;
        this.cxt = cxt;
    }

    /**
     * 构建表
     */
    public List<StarModel> create(){
        List<StarModel> starModels = this.createModels();

        // 弱化维度表
        this.weakenDimTables(starModels);

        return starModels;
    }

    /**
     * 通过字段创建表
     * @param fields
     */
    protected void createTables(List<QueryField> fields, Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables, boolean isFilter){

        List<QueryField> finalFields = addDependDimTableFields(fields);

        for(QueryField field : finalFields){
            if(!field.isActive() || field.isCustomMeasure()){ //
                // 用户自定义计算指标字段不参与构造，在最终查询时根据表达式构建相应sql
                // 用户自定义计算维度字段参与构造，在单模型查询需要用到（select/group by）子句
                // 原因：用户自定义字段涉及到跨表计算，无法归属到具体表中
                continue;
            }
            if(Enabled.value(field.getIsAnalysis())) {
                // 分析字段不参与模型表构建
                continue;
            }

            // 维度表
            String dimTableId = field.getMeta().getDimTableId();
            QueryTable table = null;
            if(BIUtil.isNotEmpty(dimTableId)){
                if(dimTables.containsKey(dimTableId)){
                    // 添加字段
                    dimTables.get(dimTableId).addField(field);
                }else{
                    // 建表
                    QueryTable qt = new QueryTable(SSDMetaCacheManager.getTable(dimTableId));
                    // 添加字段
                    qt.addField(field);
                    dimTables.put(dimTableId, qt);
                    //qt.setAlias(dimAliasPrefix + (dimTables.size()));
                }
                table = dimTables.get(dimTableId);
            }

            // 事实表
            String factTableId = field.getMeta().getFactTableId();
            if(BIUtil.isNotEmpty(factTableId)){
                if(factTables.containsKey(factTableId)){
                    // 添加字段
                    factTables.get(factTableId).addField(field);
                }else{
                    // 建表
                    QueryTable qt = new QueryTable(SSDMetaCacheManager.getTable(factTableId));
                    qt.addField(field);
                    // 添加字段
                    factTables.put(factTableId, qt);
                    //qt.setAlias(factAliasPrefix + (factTables.size()));
                }
                table = factTables.get(factTableId);
            }
            if (table == null) {
                return;
            }
            QueryField tableField = table.getFieldByLogicId(field.getLogicId());
            if(tableField != null){
                if(isFilter){
                    tableField.setIsFilter(true);
                }else {
                    tableField.setIsResult(true);
                }

                QueryField resultField = config.getResult().getFieldByCode(field.getCode());
                if(resultField != null){
                    // 重置附加字段，避免在构建表时导致附加属性丢失
                    // 场景：过滤区：日期，结果区：substr(日期,1,7)，指标1，需将"日期"字段设置为附加字段，最终不会出现在select和group by子句中
                    // 原因：因为先构建的过滤区字段，后续构建结果区字段时，发现存在，则不处理
                    // 是否是附加字段：以结果字段的标识为主
                    // 需聚合方式一致，避免同一个字段name聚合方式不一致，导致都会设置为append
                    if(tableField.isMeasure()) {
                        if (BIUtil.isNotEmpty(tableField.getAggExpressionType()) && tableField.getAggExpressionType().equalsIgnoreCase(resultField.getAggExpressionType())) {
                            tableField.setAppend(resultField.isAppend());
                        }
                    }else {
                        tableField.setAppend(resultField.isAppend());
                    }
                }
            }
        }
    }

    /**
     * 场景：事实表的计算维度，表达式中使用了维表的字段。
     * 此时星型模型中缺失维表，导致查询失败。需要添加依赖的维表字段，将维表添加到模型中
     */
    protected  List<QueryField> addDependDimTableFields(List<QueryField> fields) {
        List<QueryField> result = new ArrayList<>();
        result.addAll(fields);

        for (QueryField field : fields) {

            //指标不处理
            if (field.isMeasure()) {
                continue;
            }

            // 非计算字段不处理
            if (!field.isCalc()) {
                continue;
            }

            // 没有计算字段不处理
            if (CollUtil.isEmpty(field.getMeta().getCalcAtomFields())) {
                continue;
            }

            for (MetaField dependField : field.getMeta().getCalcAtomFields()) {

                // 指标不处理
                if (Enabled.value(dependField.getIsMeasure())) {
                    continue;
                }

                // 非维表字段不处理
                if (StrUtil.isEmpty(dependField.getDimTableId())) {
                    continue;
                }

                QueryField dependQueryField = new QueryField(dependField); // Created a new QueryField object
                if (fields.contains(dependQueryField)) {
                    continue;
                }

                //设置isResult=false，在SingleModelSqlBuilder中不查询
                dependQueryField.setIsResult(false);
                dependQueryField.setAppend(true);
                result.add(dependQueryField);
            }

        }
        return result;
    }

    protected List<StarModel> createModels(){
        // 获取所有字段对应表
        Map<String, QueryTable> factTables = new HashMap<String, QueryTable>();// 事实表
        Map<String, QueryTable> dimTables = new HashMap<String, QueryTable>(); // 维度表

        createTables(config.getFilter().getFields(), dimTables, factTables, true);
        createTables(config.getResult().getFields(), dimTables, factTables, false);

        // 通过相同维度编码或指标建表：用于解决无指标时维度互斥或优化丢失维度的问题
        this.createTableBySameCodeField(dimTables, factTables);

        // 通过间接字段建表：避免部分场景下优化器优化掉查询维度
        this.createTableByIndirectDimField(dimTables, factTables);

        // 废弃：通过相同维度编码建表：用于解决无指标时维度互斥的问题
        //this.createTableBySameCodeDimField(dimTables, factTables);



        List<StarModel> starModels = new ArrayList<>();

        // 若事实表为空，但维度表不为空，则按其关联关系创建桥接事实表
        if(factTables.isEmpty() && !dimTables.isEmpty()){
            // TODO
        }

        if(factTables.isEmpty()){
            QueryTable virtualFactTable = this.createVirtualFactTable();
            factTables.put(virtualFactTable.getId(), virtualFactTable);
        }

        int index = 1;
        for(QueryTable qt : factTables.values()){
            StarModel star = createModel(qt, dimTables);
            star.setIndex(index);
            //star.setAlias(modelAliasPrefix + index);
            star.setFactTable(qt);
            starModels.add(star);

            // 设置别名
            //qt.setAlias(factAliasPrefix + index);

            // 将维度表的别名调整为事实表别名
            // 原因：维度表与事实表join的子查询为星型模型的查询视图，此视图对外提供别名统一为事实表别名
            /*
            List<QueryTable> modelDimTables = star.getDimTables();
            if(modelDimTables != null){
                dimTables.values().forEach(d -> d.setAlias(qt.getAlias()));
            }
            */
            index++;
        }

        return starModels;
    }

    /**
     * 通过相同指标编码创建表
     * @param dimTables
     * @param factTables
     */
    protected void createTableBySameCodeField(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables) {
        boolean flag = "true".equalsIgnoreCase(SC.v("create.table.by.same.code", "true"));
        if (!flag) {
            return;
        }

        String datasetId = config.getSettings().getDatasetId();
        List<QueryField> sameCodeFields;
        if (config.getSettings().enableCreateAllTableBySameCodeOpt() && !SSDUtil.isSsmDataset(datasetId)) {
            sameCodeFields = this.createAllSameCodeFields(dimTables, factTables);
        } else {
            sameCodeFields = this.createSameCodeFields(dimTables, factTables);
        }
        createTables(sameCodeFields, dimTables, factTables, true);
        createTables(sameCodeFields, dimTables, factTables, false);
    }

    /**
     * 场景1：fact1(dim1/dim2/measure1), fact2(dim3)，在模型优化时，fact2会被优化掉（因dim3无法归属到fact1)，导致dim3会被丢失
     * 解决方案：通过dim1/dim2/dim3/measure1获取一个可以全覆盖的事实表fact3，在模型优化后，fact3会覆盖fact1/fact2，最终走fact3查询
     *
     * 场景2：行维度：平台客户端类型 互斥维度：分析业务线，但实际上这2个维度不互斥，原因：行维度找到的事实表或维度表不包含"分析业务线"
     * 解决方案：找一个事实模型（含其关联的维度表）可以覆盖所有维度，若能匹配，则返回对应表的行维度字段(code相同，id不同)，不能匹配则返回为空
     * 逻辑：
     * 1、获取所有维度和指标
     * 2、查找可以覆盖所有维度的事实表
     * 3、2返回的事实表尽量包含所有指标，若不包含指标也可以
     * @param dimTables
     * @param factTables
     * @return
     */
    protected List<QueryField> createSameCodeFields(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables){
        List<QueryField> sameCodeFields = new ArrayList<>();
        // key=fieldCode
        Map<String, QueryField> allSourceFields = new HashMap<>(20);
        // key=fieldCode
        Map<String, QueryField> allSourceDimFields = new HashMap<>(20);
        // key=fieldCode
        Map<String, QueryField> allSourceMeasureFields = new HashMap<>(50);

        // 待匹配的目标元事实表，顺序：当前查询事实表->当前查询模块下的事实表->当前数据集下的事实表
        Map<String, MetaTable> targetMetaFactTables = new LinkedHashMap<>();

        // 查询模块的id列表
        Set<String> queryModuleIdSet = new HashSet<>();

        Collection<QueryTable> allTables = new ArrayList<>();
        allTables.addAll(dimTables.values());
        allTables.addAll(factTables.values());

        String datasetId = config.getSettings().getDatasetId();
        String[] ssmDatasetIds = SC.v("ssm.dataset.ids", "3a1416400ab3405badefc558edfdadbd,123c62ef6a914ba1bb4e6b28d92fde71").split(",");
        boolean isSsmDataset = Arrays.asList(ssmDatasetIds).contains(datasetId);
        for(QueryTable table : allTables) {
            if (table.getMeta() == null) {
                continue;
            }
            /**
             // TODO 暂时写死
             if(!table.getMeta().getFullName().contains("bi_olap.")){
             return sameCodeFields;
             }
             */
            for (QueryField f : table.getFields()) {
                if (f.isCustomDimension()) {
                    // 若是计算维度，则不加入到待匹配的维度中，因为最终匹配全匹配维度，计算维度的引用维度已加入到列表，计算维度不需要匹配
                    continue;
                }
                if (isSsmDataset) {
                    //对于老的数据集（ssm数据集）
                    allSourceFields.put(f.getCode(), f);
                } else {
                    //对于mgp数据集
                    if (f.isCommonDate() || (f.getMeta() != null && StringUtils.startsWith(f.getCode(), f.getMeta().getKpiNo()))) {
                        allSourceFields.put(f.getCode(), f);
                    } else {
                        continue;
                    }
                }
                if (f.isDimension()) {
                    //20250912 相同的指标编码在不同表中，聚合方式不一样，不判断附加维度
                    //例： ads_tfc_adv_device_dau_dtl_di  - D_TFC_00003 - count(distinct)
                    // ads_tfc_deviceid_active_dtl_di - D_TFC_00003 - count( distinct case when [ABI] in ('app') then [D_TFC_00005] else null end)
                    // 如果校验附加维度，则选不到ads_tfc_adv_device_dau_dtl_di
                    if(!f.isAppend()
                            || this.isAppendDimensionFromDimensionField(f) //20260120 by contributor 仅从计算维度的覆盖维度需要添加：处理行区域添加的计算维度，同时隐藏引用的原子维度时，查询报字段不存在的问题
                      ){
                        allSourceDimFields.put(f.getCode(), f);
                    }
                } else {
                    allSourceMeasureFields.put(f.getCode(), f);
                }
                if (f.getModuleCtgId() != null) {
                    queryModuleIdSet.add(f.getModuleCtgId());
                }
            }
            if (Enabled.isTrue(table.getMeta().getIsFactTable())) {
                targetMetaFactTables.put(table.getId(), table.getMeta());
            }
        }
        int sourceDimCount = allSourceDimFields.size();
        // 模块下所有事实表
        targetMetaFactTables.putAll(SSDMetaCacheManager.getAllFactTables(datasetId, queryModuleIdSet));
        // 数据集下事实表
        targetMetaFactTables.putAll(SSDMetaCacheManager.getAllFactTables(datasetId));
        //Map<String, MetaTable> factMetaTables = SSDMetaCacheManager.getAllFactTables(datasetId);

        Map<String, MetaField> sameCodeMetaDimFields = new HashMap<>();
        Map<String, MetaField> sameCodeMetaMeasureFields = new HashMap<>();
        boolean isAllDimFound = false;
        for(MetaTable factMetaTable : targetMetaFactTables.values()){

            //如果事实表已经存在，则跳过
            //集团内部数据集不处理
//            String groupDatasetId = SC.v("ssm.group.internal.use.datasetId", "123c62ef6a914ba1bb4e6b28d92fde71");
//            if(!datasetId.equalsIgnoreCase(groupDatasetId)){
//                Long isTableExist = allTables.stream().filter(t -> t.getMeta().getFullName().equalsIgnoreCase(factMetaTable.getFullName())).count();
//                if(isTableExist > 0) {
//                    continue;
//                }
//            }

            for(String sourceFieldCode : allSourceFields.keySet()){
                QueryField sourceField = allSourceFields.get(sourceFieldCode);

                //20250912 相同的指标编码在不同表中，聚合方式不一样，不判断附加维度
                //例： ads_tfc_adv_device_dau_dtl_di  - D_TFC_00003 - count(distinct)
                // ads_tfc_deviceid_active_dtl_di - D_TFC_00003 - count( distinct case when [ABI] in ('app') then [D_TFC_00005] else null end)
                // 如果校验附加维度，则选不到ads_tfc_adv_device_dau_dtl_di
//                if((sourceField.isAppend() && sourceField.isDimension())){
                if(this.isAppendDimensionFromMeasureField(sourceField)){
                    //20260120 by contributor 仅从计算维度的覆盖维度需要添加：处理行区域添加的计算维度，同时隐藏引用的原子维度时，查询报字段不存在的问题
                    continue;
                }
                MetaField metaField = SSDMetaCacheManager.getFieldByCode(factMetaTable.getId(), sourceFieldCode);
                if(sourceField.isDimension() && !isAllDimFound) {
                    if(metaField != null) {
                        sameCodeMetaDimFields.put(sourceFieldCode, metaField);
                    } else {
                        List<MetaTable> dimMetaTables = SSDMetaCacheManager.getRelationTables(factMetaTable.getId());
                        for (MetaTable dimMetaTable : dimMetaTables) {
                            metaField = SSDMetaCacheManager.getFieldByCode(dimMetaTable.getId(), sourceFieldCode);
                            if (metaField != null) {
                                sameCodeMetaDimFields.put(sourceFieldCode, metaField);
                                break;
                            }
                        }
                    }
                }
                if(sourceField.isMeasure()){
                    if (metaField != null) {
                        sameCodeMetaMeasureFields.put(sourceFieldCode, metaField);
                    }
                }
                // 是否全包含维度
                if(sameCodeMetaDimFields.size() == sourceDimCount){
                    isAllDimFound = true;
                }
            }
            // 维度全部匹配且无指标时
            if(isAllDimFound) {
                if (allSourceMeasureFields.isEmpty()) {
                    // 若没有查询指标，则退出
                    break;
                }
                if (!sameCodeMetaMeasureFields.isEmpty()) {
                    // 有查询指标且已匹配到，则退出
                    break;
                }
            }
            // 事实表未全匹配，则清空
            isAllDimFound = false;
            sameCodeMetaDimFields.clear();
            sameCodeMetaMeasureFields.clear();
        }
        if(!isAllDimFound){
            return sameCodeFields;
        }

        Map<String, MetaField> sameCodeMetaFields = new HashMap<>();
        sameCodeMetaFields.putAll(sameCodeMetaDimFields);
        sameCodeMetaFields.putAll(sameCodeMetaMeasureFields);
        for(MetaField metaField : sameCodeMetaFields.values()){
            QueryField sourceDimField = allSourceFields.get(metaField.getCode());
            if(sourceDimField != null && !metaField.getId().equalsIgnoreCase(sourceDimField.getId())) {
                QueryField sameCodeDimField = sourceDimField.clone();
                sameCodeDimField.setMeta(metaField);
                sameCodeDimField.init();
                sameCodeFields.add(sameCodeDimField);
            }
        }
        return sameCodeFields;
    }


    protected List<QueryField> createAllSameCodeFields(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables) {
        List<QueryField> sameCodeFields = new ArrayList<>();
        // key=fieldCode
        Map<String, QueryField> allSourceFields = new HashMap<>(20);
        // key=fieldCode
        Map<String, QueryField> allSourceDimFields = new HashMap<>(20);
        // key=fieldCode
        Map<String, QueryField> allSourceMeasureFields = new HashMap<>(50);

        // 待匹配的目标元事实表，顺序：当前查询事实表->当前查询模块下的事实表->当前数据集下的事实表
        Map<String, MetaTable> targetMetaFactTables = new LinkedHashMap<>(20);

        Collection<QueryTable> allTables = new ArrayList<>();
        allTables.addAll(dimTables.values());
        allTables.addAll(factTables.values());

        String datasetId = config.getSettings().getDatasetId();
        for (QueryTable table : allTables) {
            if (table.getMeta() == null) {
                continue;
            }
            for (QueryField f : table.getFields()) {
                if (f.isCustomDimension()) {
                    // 若是计算维度，则不加入到待匹配的维度中，因为最终匹配全匹配维度，计算维度的引用维度已加入到列表，计算维度不需要匹配
                    continue;
                }

                //对于mgp数据集
                if (f.isCommonDate() || (f.getMeta() != null && StringUtils.startsWith(f.getCode(), f.getMeta().getKpiNo()))) {
                    allSourceFields.put(f.getCode(), f);
                } else {
                    continue;
                }

                if (f.isDimension()) {
                    if (!f.isAppend() || this.isAppendDimensionFromDimensionField(f)) {
                        allSourceDimFields.put(f.getCode(), f);
                    }
                } else {
                    allSourceMeasureFields.put(f.getCode(), f);
                }
            }
            if (Enabled.isTrue(table.getMeta().getIsFactTable())) {
                targetMetaFactTables.put(table.getId(), table.getMeta());
            }
        }
        int sourceDimCount = allSourceDimFields.size();
        int sourceMeasureCount = allSourceMeasureFields.size();

        if (allSourceMeasureFields.isEmpty()) {
            return sameCodeFields;
        }

        // 数据集下事实表
        targetMetaFactTables.putAll(SSDMetaCacheManager.getFactTablesByQueryField(datasetId, allSourceMeasureFields.values()));

        List<MetaField> sameCodeMetaDimFields = new ArrayList<>(32);
        List<MetaField> sameCodeMetaMeasureFields = new ArrayList<>(32);
        Set<String> foundMetricCodes = new HashSet<>(32);
        for (MetaTable factMetaTable : targetMetaFactTables.values()) {
            boolean isAllDimFound = false;
            Map<String, MetaField> foundDimFields = new HashMap<>(sourceDimCount);
            Map<String, MetaField> foundMeasureFields = new HashMap<>(sourceMeasureCount);
            for (String sourceFieldCode : allSourceFields.keySet()) {
                QueryField sourceField = allSourceFields.get(sourceFieldCode);
                if (this.isAppendDimensionFromMeasureField(sourceField)) {
                    continue;
                }
                MetaField metaField = SSDMetaCacheManager.getFieldByCode(factMetaTable.getId(), sourceFieldCode);
                if (sourceField.isDimension() && !isAllDimFound) {
                    if (metaField != null) {
                        foundDimFields.put(sourceFieldCode, metaField);
                    } else {
                        List<MetaTable> dimMetaTables = SSDMetaCacheManager.getRelationTables(factMetaTable.getId());
                        for (MetaTable dimMetaTable : dimMetaTables) {
                            metaField = SSDMetaCacheManager.getFieldByCode(dimMetaTable.getId(), sourceFieldCode);
                            if (metaField != null) {
                                foundDimFields.put(sourceFieldCode, metaField);
                            }
                        }
                    }
                }
                if (sourceField.isMeasure()) {
                    if (metaField == null && sourceField.getMeta() != null) {
                        metaField = SSDMetaCacheManager.getFieldByCode(factMetaTable.getId(), sourceField.getMeta().getKpiNo());
                    }
                    if (metaField != null) {
                        foundMeasureFields.put(sourceFieldCode, metaField);
                    }
                }
                // 是否全包含维度
                if (foundDimFields.size() == sourceDimCount) {
                    isAllDimFound = true;
                }
            }
            // 维度全部匹配
            if (isAllDimFound) {
                if (foundMeasureFields.isEmpty()) {
                    // 没有查询指标匹配到，则退出
                    continue;
                }
                sameCodeMetaDimFields.addAll(foundDimFields.values());
                foundMeasureFields.values().forEach(v -> {
                    sameCodeMetaMeasureFields.add(v);
                    foundMetricCodes.add(v.getCode());
                });
            }

            if (foundMetricCodes.size() == sourceMeasureCount) {
                // 所有指标都找到，返回
                break;
            }
        }
        List<MetaField> sameCodeMetaFields = new ArrayList<>();
        sameCodeMetaFields.addAll(sameCodeMetaDimFields);
        sameCodeMetaFields.addAll(sameCodeMetaMeasureFields);
        for (MetaField metaField : sameCodeMetaFields) {
            QueryField sourceDimField = allSourceFields.get(metaField.getCode());
            if (sourceDimField != null && !metaField.getId().equalsIgnoreCase(sourceDimField.getId())) {
                QueryField sameCodeDimField = sourceDimField.clone();
                sameCodeDimField.setMeta(metaField);
                sameCodeDimField.init();
                sameCodeFields.add(sameCodeDimField);
            }
        }
        return sameCodeFields;
    }

    /**
     * 通过间接字段建表
     * @param dimTables
     * @param factTables
     */
    protected void createTableByIndirectDimField(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables){
        boolean flag = "true".equalsIgnoreCase(SC.v("create.table.by.indirect.dim", "true"));
        if(!flag){
            return;
        }
        List<QueryField> indirectDimFields = this.createIndirectDimFields(dimTables, factTables);
        createTables(indirectDimFields, dimTables, factTables, true);
        createTables(indirectDimFields, dimTables, factTables, false);
    }

    /**
     * 创建间接维度
     * 依赖：在初步建表后再执行
     * 场景：在按查询字段获取查询表时，存在不同维度落在不同的维度表或事实表中，导致最终优化表时，部分表会被优化掉，最终导致部分维度丢失
     * 查询内容：省份、城市、GMV
     * 示例：dim1(省份),dim2(城市),fact1(GMV)， 其中fact1与dim1有关联关系，但与dim2无关联关系，优化后dim2因无事实表被优化掉，导致"城市"维度丢失
     * 解决方案：将维度再重新按事实表以及事实表关联的维度表进行归属
     * 逻辑：
     * 1、获取已创建的维度表的所有字段
     * 2、将1的维度字段，是否可归属到在已创建的事实表中，若可归属，则copy一个，并添加到返回字段中（不可以重复归属）
     * 3、若2步骤后，存在维度无法归属到事实表，则判断维度是否可归属到事实表关联的维度表中，若可归属，则copy一个，并添加到返回字段中（不可以重复归属）
     * @return
     */
    protected List<QueryField> createIndirectDimFields(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables){
        Map<String, QueryField> allSourceDimFields = new HashMap<>();
        Collection<QueryTable> allTables = new ArrayList<>();
        allTables.addAll(dimTables.values());
        allTables.addAll(factTables.values());
        for(QueryTable table : allTables){
            for(QueryField f : table.getFields()){
                if(f.isDimension() && !f.isCommonDate()) {
                    allSourceDimFields.put(f.getCode(), f);
                }
            }
        }
        List<QueryField> indirectDimFields = new ArrayList<>();
        for(String sourceDimFieldCode : allSourceDimFields.keySet()){
            QueryField dimQueryField = allSourceDimFields.get(sourceDimFieldCode);

            for(QueryTable factTable : factTables.values()){
                List<String> tableIdList = new ArrayList<>();
                // 优先归属到事实表
                tableIdList.add(factTable.getId());

                // 其次归属到维度表
                List<MetaTable> refDimTables = SSDMetaCacheManager.getRelationTables(factTable.getId());
                refDimTables.forEach(t->tableIdList.add(t.getId()));

                for(String tableId : tableIdList){
                    MetaField metaField = SSDMetaCacheManager.getFieldByCode(tableId, sourceDimFieldCode);
                    if(metaField != null && !metaField.getId().equalsIgnoreCase(dimQueryField.getId())){
                        QueryField indirectDimField = dimQueryField.clone();
                        //非计算维度，去掉自定义计算表达式
                        if(StrUtil.isEmpty(metaField.getAggExpression())){
                            indirectDimField.setCustomFieldConfigure(new CustomFieldConfigure());
                        }
                        indirectDimField.setMeta(metaField);
                        indirectDimField.init();
                        indirectDimFields.add(indirectDimField);
                        break;
                    }
                }
            }
        }
        return indirectDimFields;
    }

    /**
     * 通过相同维度编码创建表
     * @param dimTables
     * @param factTables
     */
    protected void createTableBySameCodeDimField(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables){
        boolean flag = "true".equalsIgnoreCase(SC.v("create.table.by.same.code.dim", "true"));
        if(!flag){
            return;
        }
        List<QueryField> sameCodeDimFields = this.createSameCodeDimFields(dimTables, factTables);
        createTables(sameCodeDimFields, dimTables, factTables, true);
        createTables(sameCodeDimFields, dimTables, factTables, false);
    }

    /**
     * 统计相同编码的维度字段，仅适用于无度量时调用
     * 依赖：在初步建表后再执行
     * 示例：行维度：平台客户端类型 互斥维度：分析业务线，但实际上这2个维度不互斥，原因：行维度找到的事实表或维度表不包含"分析业务线"
     * 解决方案：找一个事实模型（含其关联的维度表）可以覆盖所有维度，若能匹配，则返回对应表的行维度字段(code相同，id不同)，不能匹配则返回为空
     * @param dimTables
     * @param factTables
     * @return
     */
    protected List<QueryField> createSameCodeDimFields(Map<String, QueryTable> dimTables, Map<String, QueryTable> factTables){
        List<QueryField> sameCodeDimFields = new ArrayList<>();
        Map<String, QueryField> allSourceDimFields = new HashMap<>();
        Collection<QueryTable> allTables = new ArrayList<>();
        allTables.addAll(dimTables.values());
        allTables.addAll(factTables.values());
        for(QueryTable table : allTables){
            for(QueryField f : table.getFields()){
                if(f.isMeasure()){
                    // 有度量则返回
                    return sameCodeDimFields;
                }
                if(f.isDimension()) {
                    // 此处包含公共日期：用于当查询维度code只找到了事实表关联的维度表时，需要公共日期带入到其事实表中
                    allSourceDimFields.put(f.getCode(), f);
                }
            }
        }
        // 事实表
        Map<String, MetaTable> factMetaTables = SSDMetaCacheManager.getAllFactTables(config.getSettings().getDatasetId());
        Map<String, MetaField> sameCodeMetaDimFields = new HashMap<>();
        boolean isAllFound = false;
        for(MetaTable factMetaTable : factMetaTables.values()){
            if(!factMetaTable.getFullName().contains("bi_olap.")){
                continue;
            }
            for(String dimCode : allSourceDimFields.keySet()){
                MetaField dimMetaField = SSDMetaCacheManager.getFieldByCode(factMetaTable.getId(), dimCode);
                if(dimMetaField != null) {
                    sameCodeMetaDimFields.put(dimCode, dimMetaField);
                }else {
                    List<MetaTable> dimMetaTables = SSDMetaCacheManager.getRelationTables(factMetaTable.getId());
                    for(MetaTable dimMetaTable : dimMetaTables){
                        dimMetaField = SSDMetaCacheManager.getFieldByCode(dimMetaTable.getId(), dimCode);
                        if(dimMetaField != null) {
                            sameCodeMetaDimFields.put(dimCode, dimMetaField);
                            break;
                        }
                    }
                }
                // 是否全包含
                if(sameCodeMetaDimFields.size() == allSourceDimFields.size()){
                    isAllFound = true;
                    break;
                }
            }
            if(isAllFound) {
                break;
            }else {
                // 事实表未全匹配，则清空
                sameCodeMetaDimFields.clear();
            }
        }
        if(!isAllFound){
            return sameCodeDimFields;
        }

        for(MetaField metaField : sameCodeMetaDimFields.values()){
            QueryField sourceDimField = allSourceDimFields.get(metaField.getCode());
            if(sourceDimField != null) {
                QueryField sameCodeDimField = sourceDimField.clone();
                sameCodeDimField.setMeta(metaField);
                sameCodeDimField.init();
                sameCodeDimFields.add(sameCodeDimField);
            }
        }
        return sameCodeDimFields;
    }

    protected QueryTable createVirtualFactTable(){
        // 虚拟事实表
        QueryTable virtualFactTable = new QueryTable();
        MetaTable metaTable = new MetaTable();
        metaTable.setId(Guid.id());
        metaTable.setTableSchema("");
        metaTable.setIsFactTable(Enabled.YES.getId());
        metaTable.setName("(select 1)");
        virtualFactTable.setMeta(metaTable);
        virtualFactTable.setVirtual(true);
        //virtualFactTable.setAlias(virtualFactAliasPrefix);

        return virtualFactTable;
    }

    /**
     * 创建桥接事实表：必须所有的维度表有相同的事实表 TODO
     * @param dimTables
     * @return
     */
    protected QueryTable createBridgeFactTable(Map<String, QueryTable> dimTables){
        QueryTable factTable = null;
        List<List<String>> dimFactTableIdList = new ArrayList<>();
        for(QueryTable dimTable : dimTables.values()){
            String dimTableId = dimTable.getId();
            List<MetaTableRelation> relations = SSDMetaCacheManager.getRelations(dimTableId);
            if(BIUtil.isEmpty(relations)){
                continue;
            }
            List<String> factTableIds = new ArrayList<>();
            for(MetaTableRelation rel : relations){
                if(dimTableId.equals(rel.getSubTableId())){
                    factTableIds.add(rel.getPrimaryTableId());
                }else if(dimTableId.equals(rel.getPrimaryTableId())){
                    factTableIds.add(rel.getSubTableId());
                }
            }
            dimFactTableIdList.add(factTableIds);
        }
        if(BIUtil.isEmpty(dimFactTableIdList)){
            return factTable;
        }
        List<String> list = dimFactTableIdList.get(0);


        return factTable;
    }

    /**
     * 创建星型模型
     * @param factTable
     * @param dimTables
     * @return
     */
    protected StarModel createModel(QueryTable factTable, Map<String, QueryTable> dimTables){
        // 以事实表为主体，查找关联的所有维度表，并构建星型模型
        StarModel star = new StarModel(factTable);
        String tableId = factTable.getId();
        List<MetaTableRelation> relations = SSDMetaCacheManager.getRelations(tableId);
        for(MetaTableRelation mr : relations){
            String dimTableId = null;
            if(mr.getPrimaryTableId().equals(tableId)){
                dimTableId = mr.getSubTableId();
            }else if(mr.getSubTableId().equals(tableId)){
                dimTableId = mr.getPrimaryTableId();
            }
            QueryTable dimTable = dimTables.get(dimTableId);
            if(dimTable != null){
                // 添加维度表
                // 此处需要克隆，因同一个维度表隶属于不同模型
                star.addDimTable(dimTable.clone());
            }
        }
        return star;
    }

    /**
     * 弱化维度表
     * 1、若事实表的字段包含了维度表的字段，则将此类字段移入到事实表中，在维度表中删除此类字段
     * 2、将无字段的维度表删除
     */
    protected void weakenDimTables(List<StarModel> starModels){
        if(!config.getSettings().isEnableWeakenDimTableOnCreateStarModel()){
            return;
        }
        for(StarModel model : starModels){

            List<QueryTable> dimTables = model.getDimTables();
            if(BIUtil.isEmpty(dimTables)){
                continue;
            }

            QueryTable factTable = model.getFactTable();
            List<String> factTableFields = factTable.getFields().stream().map(QueryField::getCode).collect(Collectors.toList());

            // 调整为循环赋值：提升遍历性能
            // 废弃简写（有性能损耗）
            // List<String> factTableMetaFields = SSDMetaCacheManager.getTableFields(factTable.getId()).stream().map(MetaField::getCode).collect(Collectors.toList());
            Map<String, MetaField> factTableMetaFields = new HashMap<>();
            List<MetaField> metaFields = SSDMetaCacheManager.getTableFields(factTable.getId());
            metaFields.forEach(f-> {factTableMetaFields.put(f.getCode(), f);});


            for(QueryTable dimTable: dimTables){
                Set<String> dimTableFields = dimTable.getFields().stream().map(QueryField::getCode).collect(Collectors.toSet());
                Collection<String> intersection = CollectionUtil.intersection(factTableMetaFields.keySet(), dimTableFields);
                if(BIUtil.isNotEmpty(intersection)) {
                    List<QueryField> dropFields = dimTable.dropFieldsByCode(intersection);
                    for(QueryField dropField : dropFields){
                        if(!factTableFields.contains(dropField.getCode())) {
                            // 事实表不存在，则添加到事实表中
                            QueryField copy = dropField.clone();
                            copy.setMeta(factTableMetaFields.get(copy.getCode()));
                            factTable.addField(copy);
                        }
                    }
                }
            }

            List<QueryTable> newDimTables = new ArrayList<>();
            for(QueryTable dimTable: dimTables) {
                //如果维表只含有计算维度，则过滤掉。避免关联维表，却不从维表查询任何字段，影响查询性能
                Long dimCount = dimTable.getFields().stream().filter(f -> !f.isCalc()).count();
                if (dimCount > 0) {
                    newDimTables.add(dimTable);
                }
            }

            model.setDimTables(newDimTables);
        }
    }

    /**
     * 废弃：已通过维度归属逻辑覆盖了此方法逻辑
     * 扩充维度表：用于事实表与维度表构建星型模型。
     * 补充内容需同时满足以下条件：
     * 1、有字段编码，且包含所有查询的维度字段
     * 2、维度表配置了与事实表的关联关系
     *
     * 场景：
     * 后台配置：
     * 1、维度字段：如：区县等级，此字段在多个维表中存在（dimTable1,dimTable2)，但前台目前上使用的是dimTable1表的字段。
     * 2、表关联：factTable1 left join dimTable1 , factTable2 left join dimTable2
     * 查询场景：
     * 维度：日期、dimTable1.区县等级
     * 指标：factTable1.指标1、factTable2.指标2
     *
     * 后台构建表：维度表=dimTable1，事实表=factTable1,factTable2
     * 问题：模型构建错误，因为factTable2.指标2与dimTable1.区县等级，没有关联关系
     * 解决方案：扩充维度表，维度表=dimTable1,dimTable2
     */
    protected Map<String, QueryTable> supplementDimTables(Map<String, QueryTable> dimTables){
        if(BIUtil.isEmpty(dimTables)){
            return dimTables;
        }
        // 获取所有配置了关联关系的维度表
        Map<String, MetaTable> dimMetaTables = SSDMetaCacheManager.getAllDimTables(true);
        if(BIUtil.isEmpty(dimMetaTables)){
            return dimTables;
        }

        // 将所有维度表，按id存储到map，同时将每个维度表中的字段编码存储到map中
        Map<String, Map<String, String>> dimTableFields = new HashMap<>();
        for(MetaTable metaTable : dimMetaTables.values()){
            Map<String, String> fieldCodes = metaTable.getFields().stream().collect(Collectors.toMap(MetaField::getCode, MetaField::getCode, (c1,c2)->c1));
            dimTableFields.put(metaTable.getId(), fieldCodes);
        }

        List<QueryTable> supplementDimTableList = new ArrayList<>();
        for(QueryTable queryTable : dimTables.values()){
            Map<String, String> queryFieldCodes = queryTable.getFields().stream().collect(Collectors.toMap(QueryField::getCode, QueryField::getCode, (c1,c2)->c1));
            for(String metaTableId : dimTableFields.keySet()){
                if(metaTableId.equals(queryTable.getId())){
                    continue;
                }
                Map<String, String> dimTableFieldCodes = dimTableFields.get(metaTableId);
                if(BIUtil.isNotEmpty(dimTableFieldCodes)){
                    // 判断当前维度表是否包含所有查询的维度字段
                    boolean isContains = dimTableFieldCodes.keySet().containsAll(queryFieldCodes.keySet());
                    if(isContains){
                        // 补充维度表
                        QueryTable dimTable = queryTable.clone();
                        dimTable.setMeta(dimMetaTables.get(metaTableId));
                        //dimTables.put(dimTable.getId(), dimTable);
                        supplementDimTableList.add(dimTable);
                    }
                }
            }
        }

        // 添加到返回结果中
        for(QueryTable dimTable : supplementDimTableList){
            dimTables.put(dimTable.getId(), dimTable);
        }

        return dimTables;
    }

    /**
     * 从计算维度的附加维度
     * @param queryField
     * @return
     */
    protected boolean isAppendDimensionFromDimensionField(QueryField queryField){
        if(queryField == null){
            return false;
        }
        QueryArea queryArea = queryField.getQueryArea();
        if(queryArea == null){
            return false;
        }
        return queryField.isAppend() && queryField.isDimension() && queryField.getQueryArea().isDimensionArea();
    }

    /**
     * 从计算指标的附加维度
     * @param queryField
     * @return
     */
    protected boolean isAppendDimensionFromMeasureField(QueryField queryField){
        if(queryField == null){
            return false;
        }
        QueryArea queryArea = queryField.getQueryArea();
        if(queryArea == null){
            return false;
        }
        return queryField.isAppend() && queryField.isDimension() && !queryField.getQueryArea().isDimensionArea();
    }
}
