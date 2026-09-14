package com.bi.queryer.ssm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldExpressionIdMapping;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cfg.AnalysisItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.operator.BaseOperator;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorContext;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldCategoryAggregationItem;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortItem;
import com.bi.queryer.ssm.engine.function.FunctionManager;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.model.creator.JoinModelCreator;
import com.bi.queryer.ssm.engine.model.creator.ModelValidateResult;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.promotion.PromotionManager;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.env.EnvVariableManager;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.period.QuarterDateUtil;
import com.bi.queryer.util.period.WeekDateUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;

/**
 * @Author contributor
 * @Date 17:50 2022-10-28
 * @Description 字段相关的工具类
 **/
public abstract class FieldUtil {

    /**
     * 获取互斥字段列表
     * 通过最终的查询引擎是否适配来确定是否互斥
     * @param fieldIds
     * @return
     */
    public static Set<String> getExcludeFieldById(String datasetId, List<String> fieldIds, String moduleCtgIds,String compareFieldIds,String compareFieldType) {
        Set<String> excludeIdSet = new HashSet<>();

        List<MetaField> compareFields = new ArrayList<>();
        Map<String, MetaField> fieldMap = SSDMetaCacheManager.getFieldsCache();

        //获取模块下的字段
        List<MetaField> queryFieldList = new ArrayList<>();
        if (StrUtil.isNotEmpty(moduleCtgIds)) {
            String[] moduleCtgArray = moduleCtgIds.split(",");
            for (String moduleCtgId : moduleCtgArray) {
                List<MetaField> metaFieldList = SSDMetaCacheManager.getFieldByCategory(moduleCtgId);
                if (CollUtil.isEmpty(metaFieldList)) {
                    continue;
                }
                queryFieldList.addAll(metaFieldList);
            }
        }

        //前端传递对比字段，直接获取
        //没有传，则使用目录下的字段
        if (StrUtil.isEmpty(compareFieldIds)) {

            long t1 = System.currentTimeMillis();
            // 通过前台目录获取去重后的字段，和前台tree保持一致
            // 获取所有叶子目录
            Map<String, List<MetaField>> allCtgFields = new HashMap<>();

            //没有传目录，则使用所有字段
            if (CollUtil.isEmpty(queryFieldList)) {

                //lod查询互斥场景，只需要比维度
                if (FieldType.DIM == FieldType.get(compareFieldType)) {
                    queryFieldList = fieldMap.values().stream()
                            .filter(f -> !Enabled.value(f.getIsMeasure()))
                            .collect(Collectors.toList());
                } else {
                    queryFieldList.addAll(fieldMap.values());
                }
            }

            for (MetaField f : queryFieldList) {
                if (!Enabled.isTrue(f.getIsShow())) {
                    continue;
                }

                List<String> categoryIdList = f.getCategoryIdList();
                if (CollUtil.isEmpty(categoryIdList)) {
                    continue;
                }

                for (String ctgId : categoryIdList) {
                    if (BIUtil.isEmpty(ctgId) || allCtgFields.containsKey(ctgId)) {
                        continue;
                    }

                    MetaFieldCategory ctg = SSDMetaCacheManager.getCategoryById(ctgId);
                    if (ctg == null) {
                        continue;
                    }

                    //增加数据集为空的判断，兼容前端不传的逻辑
                    if (datasetId == null || Objects.equals(datasetId, ctg.getDatasetId())) {
                        List<MetaField> ctgFields = ctg.getFields().stream()
                                .filter(cf -> Enabled.isTrue(cf.getIsShow()))
                                .filter(cf -> Enabled.isFalse(cf.getIsCommonDate())) // 排除公共日期字段
                                .collect(Collectors.toList());
                        ctgFields = getMaxWeightFields(ctgFields, ctgId);

                        allCtgFields.put(ctgId, ctgFields);
                    }
                }

            }

            long t2 = System.currentTimeMillis();
            System.out.println("getAllCtgFields:" + (t2 - t1) + "ms");

            for (List<MetaField> ctgFields : allCtgFields.values()) {
                compareFields.addAll(ctgFields);
            }

        } else {

            List<String> compareFieldList = Arrays.asList(compareFieldIds.split(","));
            for (String compareFieldId : compareFieldList) {
                MetaField field = fieldMap.get(compareFieldId);
                if (field == null) {
                    continue;
                }
                compareFields.add(field);
            }

        }

        //过滤需要判断互斥的字段类型
        //场景1： lod只需要判断维度互斥
        if (StrUtil.isNotEmpty(compareFieldType)) {

            if (FieldType.DIM == FieldType.get(compareFieldType)) {
                compareFields = compareFields.stream()
                        .filter(f -> !Enabled.value(f.getIsMeasure()))
                        .collect(Collectors.toList());
            } else {
                compareFields = compareFields.stream()
                        .filter(f -> Enabled.value(f.getIsMeasure()))
                        .collect(Collectors.toList());
            }

        }
        String userName = UserManager.get() != null ? UserManager.get().getName() : "";
        Double rateLimitThreshold = Double.valueOf(SC.v("ssm.field.exclude.rate.limit.threshold", "1"));
        List<String> whiteList = BIUtil.newArrayList(StringUtils.split(SC.v("ssm.field.exclude.whitelist", null)));
        List<String> blackList = BIUtil.newArrayList(StringUtils.split(SC.v("ssm.field.exclude.blacklist", null)));
        boolean isHitRateLimit = SSDUtil.isHitRateLimitByUserName(userName, rateLimitThreshold, whiteList, blackList);
        if (isHitRateLimit) {
            excludeIdSet = FieldExclusiveUtil.getExclusiveFieldByBitSet(datasetId, fieldIds, compareFields);
        } else {
            //入参传了模块id，走按模块id获取互斥的逻辑
            if (StrUtil.isEmpty(moduleCtgIds) && StrUtil.isEmpty(compareFieldIds) && StrUtil.isEmpty(compareFieldType)) {
                excludeIdSet = getExcludeFieldById(fieldIds, compareFields);
            } else {
                excludeIdSet = getExcludeFieldByIdAndModuleCtgIds(datasetId, fieldIds, compareFields);
            }
        }

        return excludeIdSet;
    }


    /**
     * 获取互斥字段列表
     * 通过最终的查询引擎是否适配来确定是否互斥
     * @param fieldIds
     * @return
     */
    public static Set<String> getExcludeFieldByIdOld(String datasetId, List<String> fieldIds, String moduleCtgIds,String compareFieldIds,String compareFieldType) {
        Set<String> excludeIdSet = new HashSet<>();

        List<MetaField> compareFields = new ArrayList<>();
        Map<String, MetaField> fieldMap = SSDMetaCacheManager.getFieldsCache();

        //构建模块目录字段id map，方便后续过滤
        Map<String, String> moduleCtgFieldIdMap = new HashMap<>();
        if (StrUtil.isNotEmpty(moduleCtgIds)) {
            String[] moduleCtgArray = moduleCtgIds.split(",");
            for (String moduleCtgId : moduleCtgArray) {
                List<MetaField> metaFieldList =  SSDMetaCacheManager.getFieldByCategory(moduleCtgId);
                if(CollUtil.isEmpty(metaFieldList)){
                    continue;
                }
                metaFieldList.stream().forEach(m->moduleCtgFieldIdMap.put(m.getId(),""));
            }
        }

        //前端传递对比字段，直接获取
        //没有传，从目录获取
        if(StrUtil.isEmpty(compareFieldIds)){

            // 通过前台目录获取去重后的字段，和前台tree保持一致
            // 获取所有叶子目录
            Map<String, List<MetaField>> allCtgFields = new HashMap<>();
            for (MetaField f : fieldMap.values()) {
                if (!Enabled.isTrue(f.getIsShow())) {
                    continue;
                }

                if (moduleCtgFieldIdMap.size() > 0 && !moduleCtgFieldIdMap.containsKey(f.getId())) {
                    continue;
                }

                List<String> categoryIdList = f.getCategoryIdList();
                if (CollUtil.isEmpty(categoryIdList)) {
                    continue;
                }

                for (String ctgId : categoryIdList) {
                    if (BIUtil.isEmpty(ctgId) || allCtgFields.containsKey(ctgId)) {
                        continue;
                    }

                    MetaFieldCategory ctg = SSDMetaCacheManager.getCategoryById(ctgId);
                    if (ctg == null) {
                        continue;
                    }

                    //增加数据集为空的判断，兼容前端不传的逻辑
                    if (datasetId == null || Objects.equals(datasetId, ctg.getDatasetId())) {
                        List<MetaField> ctgFields = ctg.getFields().stream()
                                .filter(cf -> Enabled.isTrue(cf.getIsShow()))
                                .filter(cf -> Enabled.isFalse(cf.getIsCommonDate())) // 排除公共日期字段
                                .collect(Collectors.toList());
                        ctgFields = getMaxWeightFields(ctgFields,ctgId);

                        allCtgFields.put(ctgId, ctgFields);
                    }
                }

            }

            for(List<MetaField> ctgFields:allCtgFields.values()) {
                compareFields.addAll(ctgFields);
            }

        }else{

            List<String> compareFieldList = Arrays.asList(compareFieldIds.split(","));
            for(String compareFieldId:compareFieldList){
                MetaField field = fieldMap.get(compareFieldId);
                if(field == null){
                    continue;
                }
                compareFields.add(field);
            }

        }

        //过滤需要判断互斥的字段类型
        //场景1： lod只需要判断维度互斥
        if(StrUtil.isNotEmpty(compareFieldType)) {

            if (FieldType.DIM == FieldType.get(compareFieldType)) {
                compareFields = compareFields.stream()
                        .filter(f -> !Enabled.value(f.getIsMeasure()))
                        .collect(Collectors.toList());
            } else {
                compareFields = compareFields.stream()
                        .filter(f -> Enabled.value(f.getIsMeasure()))
                        .collect(Collectors.toList());
            }

        }
        //入参传了模块id，走按模块id获取互斥的逻辑
        if (moduleCtgFieldIdMap.size() == 0 && StrUtil.isEmpty(compareFieldIds) && StrUtil.isEmpty(compareFieldType)) {
            excludeIdSet = getExcludeFieldById(fieldIds, compareFields);
        } else {
            excludeIdSet = getExcludeFieldByIdAndModuleCtgIds(datasetId, fieldIds, compareFields);
        }

        return excludeIdSet;
    }

    public static Set<String> getExcludeFieldById(List<String> fieldIds, List<MetaField> compareFields){
        long t1 = System.currentTimeMillis();

        Set<String> excludeIdSet = new HashSet<>();

        // 去掉lod字段
        List<String> fieldIdList = fieldIds.stream().filter(f-> !f.contains(CustomFieldType.LOD.getIdentifier())).collect(Collectors.toList());

        if(BIUtil.isEmpty(fieldIdList) || BIUtil.isEmpty(compareFields)) {
            return excludeIdSet;
        }

        QueryContext cxt = new QueryContext();

        ExecutorService executorService = null;
        try {
            // 按表分组
            Map<String, Map<String, String>> allTableMeasureFields = new HashMap<>();
            List<MetaField> newCompareFields = new ArrayList<>();// 每个表取所有的维度，但只取一个指标字段
            for(MetaField f : compareFields){
                if(Enabled.isTrue(f.getIsMeasure())){
                    String tableId = f.getTableId();
                    if(allTableMeasureFields.containsKey(tableId)){
                        allTableMeasureFields.get(tableId).put(f.getId(), f.getId());
                    }else {
                        Map<String, String> list = new HashMap();
                        list.put(f.getId(), f.getId());
                        allTableMeasureFields.put(tableId, list);
                        newCompareFields.add(f);
                    }
                }else {
                    newCompareFields.add(f);
                }
            }

            // 多线程并行处理
            int threadFieldCount = 300;
            List<List<MetaField>> threadMetaFieldList = BIUtil.splitList(newCompareFields, threadFieldCount);//BIUtil.splitList(compareFields, threadFieldCount);
            int threadCount = threadMetaFieldList.size();
//            final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
            executorService = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<List<Map<String, String>>>> futures = new ArrayList<>();
            for(List<MetaField> threadMetaFields : threadMetaFieldList){
                CompletableFuture<List<Map<String, String>>> future = CompletableFuture.supplyAsync(()->{
                    // 此处需重新copy一份，保障线程内安全
                    Map<String, String> threadIncludeIds = new HashMap<>(500);
                    Map<String, String> threadExcludeIds = new HashMap<>(500);
                    List<Map<String, String>> result = new ArrayList<>();
                    List<QueryField> threadQueryFields = new ArrayList<>();
                    for(String fieldId : fieldIdList){
                        MetaField field = SSDMetaCacheManager.getField(fieldId);
                        if(field == null){
                            continue;
                        }
                        QueryField queryField = new QueryField(field);
                        queryField.setIsResult(true);
                        queryField.setQueryArea(Enabled.value(field.getIsMeasure()) ? QueryArea.Measure : QueryArea.RowDimension);
                        threadQueryFields.add(queryField);
                        threadIncludeIds.put(fieldId, fieldId);
                    }

                    for(MetaField metaField : threadMetaFields){
                        // 只判断有前台目录的字段
                        if(CollUtil.isEmpty(metaField.getCategoryIdList())) {
                            continue;
                        }

                        if(threadIncludeIds.containsKey(metaField.getId())) {
                            continue;
                        }


                        // 设置结果字段
                        QueryField testQueryField = new QueryField(metaField);
                        testQueryField.setIsResult(true);
                        testQueryField.setQueryArea(Enabled.value(metaField.getIsMeasure()) ? QueryArea.Measure : QueryArea.RowDimension);
                        threadQueryFields.add(testQueryField);

                        QueryConfigure cfg = new QueryConfigure();
                        cfg.getSettings().setEnableHotTableQuery(false); // 禁用热表查询：减少不必要的处理逻辑
                        cfg.getResult().setFields(threadQueryFields);

                        //String tableId = metaField.getTableId();

                        JoinModelCreator creator = new JoinModelCreator(cfg, cxt);
                        if(creator.validate(false).isSuccess()) {
                            threadIncludeIds.put(metaField.getId(), metaField.getId());
                        }else {
                            threadExcludeIds.put(metaField.getId(), metaField.getId());
                        }
                        threadQueryFields.remove(threadQueryFields.size() - 1);
                    }
//                    countDownLatch.countDown();
                    result.add(threadIncludeIds);
                    result.add(threadExcludeIds);

                    return result;
                }, executorService);

                futures.add(future);
            }

            Map<String, String> allThreadIncludeIds = new HashMap<>(50);
            Map<String, String> allThreadExcludeIds = new HashMap<>(100);
            futures.forEach(f->{
                List<Map<String, String>> threadResult = f.join();
                allThreadIncludeIds.putAll(threadResult.get(0));
                allThreadExcludeIds.putAll(threadResult.get(1));
            });

//            countDownLatch.await();
            executorService.shutdown();

            // 按表添加
            /**不互斥*/
            Map<String, String> appendSameTableFieldIds = new HashMap<>(50);
            for(String includeId : allThreadIncludeIds.keySet()){
                MetaField m = SSDMetaCacheManager.getField(includeId);
                if(m != null && Enabled.isTrue(m.getIsMeasure())){
                    String tableId = m.getTableId();
                    Map<String, String> appendIds = allTableMeasureFields.get(tableId);
                    if(appendIds != null) {
                        appendSameTableFieldIds.putAll(appendIds);
                        allTableMeasureFields.remove(tableId);
                    }
                }
            }
            allThreadIncludeIds.putAll(appendSameTableFieldIds);

            /**互斥*/
            appendSameTableFieldIds = new HashMap<>(100);
            for(String excludeId : allThreadExcludeIds.keySet()){
                MetaField m = SSDMetaCacheManager.getField(excludeId);
                if(m != null && Enabled.isTrue(m.getIsMeasure())){
                    String tableId = m.getTableId();
                    Map<String, String> appendIds = allTableMeasureFields.get(tableId);
                    if(appendIds != null) {
                        appendSameTableFieldIds.putAll(appendIds);
                        allTableMeasureFields.remove(tableId);
                    }
                }
            }
            allThreadExcludeIds.putAll(appendSameTableFieldIds);

            // 排除不互斥的id
//            allThreadExcludeIds.removeAll(allThreadIncludeIds);
            for(String id : allThreadIncludeIds.keySet()){
                allThreadExcludeIds.remove(id);
            }

           // excludeIdSet.addAll(allThreadExcludeIds);
            excludeIdSet = allThreadExcludeIds.keySet();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(executorService != null && !executorService.isShutdown()){
                executorService.shutdown();
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.println("************获取互斥字段code列表耗时：" + (t2 - t1) + "毫秒");

        return excludeIdSet;
    }

    public static Set<String> getExcludeFieldByIdAndModuleCtgIds(String datasetId, List<String> fieldIds, List<MetaField> compareFields){
        long t1 = System.currentTimeMillis();

        Set<String> excludeIdSet = new HashSet<>();

        // 去掉lod字段
        List<String> fieldIdList = fieldIds.stream().filter(f-> !f.contains(CustomFieldType.LOD.getIdentifier())).collect(Collectors.toList());

        if(BIUtil.isEmpty(fieldIdList) || BIUtil.isEmpty(compareFields)) {
            return excludeIdSet;
        }

        QueryContext cxt = new QueryContext();

        ExecutorService executorService = null;
        try {
            Map<String, String> includeFieldIdsBySameTable = new ConcurrentHashMap<>(2000);

            // 多线程并行处理
            int threadFieldCount = 300;
            List<List<MetaField>> threadCompareFieldList = BIUtil.splitList(compareFields, threadFieldCount);
            int threadCount = threadCompareFieldList.size();

            executorService = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<List<Map<String, String>>>> futures = new ArrayList<>();
            for(List<MetaField> threadCompareFields : threadCompareFieldList){
                CompletableFuture<List<Map<String, String>>> future = CompletableFuture.supplyAsync(()->{
                    // 此处需重新copy一份，保障线程内安全
                    Map<String, String> threadIncludeIds = new HashMap<>(500);
                    Map<String, String> threadExcludeIds = new HashMap<>(500);
                    List<Map<String, String>> result = new ArrayList<>();
                    List<QueryField> threadQueryFields = new ArrayList<>();

                    for(String fieldId : fieldIdList){
                        MetaField field = SSDMetaCacheManager.getField(fieldId);
                        if(field == null){
                            continue;
                        }
                        QueryField queryField = new QueryField(field);
                        queryField.setIsResult(true);
                        queryField.setQueryArea(Enabled.value(field.getIsMeasure()) ? QueryArea.Measure : QueryArea.RowDimension);
                        threadQueryFields.add(queryField);
                        threadIncludeIds.put(fieldId, fieldId);
                    }

                    for(MetaField metaField : threadCompareFields){
                        // 只判断有前台目录的字段
                        if(CollUtil.isEmpty(metaField.getCategoryIdList())) {
                            continue;
                        }

                        if(threadIncludeIds.containsKey(metaField.getId())) {
                            continue;
                        }

                        // 设置结果字段
                        QueryField testQueryField = new QueryField(metaField);
                        testQueryField.setIsResult(true);
                        testQueryField.setQueryArea(Enabled.value(metaField.getIsMeasure()) ? QueryArea.Measure : QueryArea.RowDimension);
                        threadQueryFields.add(testQueryField);

                        QueryConfigure cfg = new QueryConfigure();

                        // 禁用热表查询：减少不必要的处理逻辑
                        cfg.getSettings().setEnableHotTableQuery(false);
                        cfg.getSettings().setEnableTablePriSubRouter(false);
                        cfg.getSettings().setEnableSupplementFullMeasureOnCreateStarModel(false);
                        cfg.getSettings().setEnableWeakenDimTableOnCreateStarModel(false);
                        cfg.getSettings().setEnableCreateAllTableBySameCodeOpt(false);
                        cfg.getSettings().setDatasetId(datasetId);

                        //支持后台配置的计算维度与跨模型指标
                        List<QueryField> finalQueryFields = new ArrayList<>();
                        for(QueryField queryField :threadQueryFields) {
                            if (
                                //后台计算维度
                                //    (isCalcField(queryField) &&  !queryField.isMeasure() )
                                //            ||
                                            //跨模型指标
                                            FieldType.CROSS_MODEL_MEASURE == FieldType.get(queryField.getMeta().getFieldType())
                            ) {
                                if (CollUtil.isNotEmpty(queryField.getMeta().getCalcAtomFields())) {
                                    for (MetaField calcAtomField : queryField.getMeta().getCalcAtomFields()) {
                                        QueryField qf = new QueryField(calcAtomField);
                                        qf.setIsResult(true);
                                        qf.setQueryArea(Enabled.value(calcAtomField.getIsMeasure()) ? QueryArea.Measure : QueryArea.RowDimension);
                                        finalQueryFields.add(qf);
                                    }
                                } else {
                                    finalQueryFields.add(queryField);
                                }

                            } else {
                                finalQueryFields.add(queryField);
                            }
                        }

                        cfg.getResult().setFields(finalQueryFields);

                        normalizeConfig(cfg);

                        JoinModelCreator creator = new JoinModelCreator(cfg, cxt);
                        ModelValidateResult validateResult = creator.validate(false);
                        if(validateResult.isSuccess()) {
                            threadIncludeIds.put(metaField.getId(), metaField.getId());
                        }else {
                            threadExcludeIds.put(metaField.getId(), metaField.getId());
                        }
                        // 移除测试字段
                        threadQueryFields.remove(threadQueryFields.size() - 1);
                    }

                    result.add(threadIncludeIds);
                    result.add(threadExcludeIds);

                    return result;
                }, executorService);

                futures.add(future);
            }

            Map<String, String> allThreadIncludeIds = new HashMap<>(50);
            Map<String, String> allThreadExcludeIds = new HashMap<>(100);
            futures.forEach(f->{
                List<Map<String, String>> threadResult = f.join();
                allThreadIncludeIds.putAll(threadResult.get(0));
                allThreadExcludeIds.putAll(threadResult.get(1));
            });

            executorService.shutdown();

            // 排除不互斥的id
            for(String id : allThreadIncludeIds.keySet()){
                allThreadExcludeIds.remove(id);
            }

            excludeIdSet = allThreadExcludeIds.keySet();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(executorService != null && !executorService.isShutdown()){
                executorService.shutdown();
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.println("************获取互斥字段code列表耗时：" + (t2 - t1) + "毫秒");

        return excludeIdSet;
    }

    /**
     * 标准化配置
     * 1. 实时数据集选择最优id
     * @param cfg
     */
    public static void normalizeConfig(QueryConfigure cfg) {

        String datasetId = cfg.getSettings().getDatasetId();
        MetaDataset dataset = SSDMetaCacheManager.getDataset(datasetId);
        if(dataset ==  null){
            return;
        }

        if(DataTypeEnum.REAL_TIME != DataTypeEnum.codeOf(dataset.getDatasetType())){
            return;
        }

        //获取所有的维度
        List<String> dimCodeList = cfg.getAllFields().stream().filter(field->field.isDimension())
                .map(field->field.getCode()).collect(Collectors.toList());

        /**
         * 遍历指标，找到指标的一个有所有维度的实时表
         */
        for(QueryField field : cfg.getResult().getMeasures()){
            List<RtTableInfo> rtTableInfos =  SSDMetaCacheManager.getFieldRtTableByCode(datasetId, field.getCode());
            if(CollUtil.isEmpty(rtTableInfos)){
                continue;
            }

            String fieldId = field.getId();
            for(RtTableInfo rtTableInfo : rtTableInfos){
                if (!SSDUtil.isSuperset(rtTableInfo.getDimCodeList(), dimCodeList)) {
                    continue;
                }

                fieldId = rtTableInfo.getFieldId();
                break;
            }

            field.setMeta(SSDMetaCacheManager.getField(fieldId));
            field.setId(fieldId);
        }

    }


    /**
     * 获取指标格式化字符串
     * @param field
     * @return
     */
    public static String getFieldFormatString(QueryField field) {

        //日期粒度处理
        if(field.isCommonDate()) {
            DateGranularity dateGranularity = DateGranularity.get(field.getQueryDateGranularity());
            if (DateGranularity.WEEK == dateGranularity) {
                return IFunction.Format_Week;
            } else if (DateGranularity.MONTH == dateGranularity) {
                return IFunction.Format_Month;
            }
        }

        String formatString = field.getMeta().getShowFormatExpression();

        //20251106 支持前端设置指标的小数位
        formatString = rebuildFormatString(formatString, field.getDecimalPlaces());

        //showFormatExpression为空，根据字段类型赋值
        if (StrUtil.isEmpty(formatString)) {
            DataType dataType = DataType.getType(field.getMeta().getDataType());
            if (dataType.isInteger()) {
                formatString = "###,###,##0";
            } else if (DataType.Double == dataType) {
                formatString = "###,###,##0.00";
            }
        }

        // 若是分析字段
        if (Enabled.value(field.getIsAnalysis())) {
            AnalysisItemConfig analysisConfig = field.getAnalysisConfig();
            if (BIUtil.isNotEmpty(formatString) && !formatString.contains("%")) {
                if (AnalysisCalcType.RATIO == AnalysisCalcType.get(analysisConfig.getCalcType())) {
                    formatString = "###,###,##0.00%";
                } else if (analysisConfig.isTargetValue() && AnalysisCalcType.get(analysisConfig.getTargetConfig().getTargetCalcType()).isRatio()) {
                    formatString = "###,###,##0.00%";
                } else {
                   // formatString = DataType.getType(field.getMeta().getDataType()).isInteger() ? "###,###,##0" : "###,###,##0.00";
                    if(AggExpressionType.get(analysisConfig.getAggExpressionType()).isAvgByDay()){
                        formatString = formatString + BIConsts.AVG_FORMAT_SUFFIX;
                    }
                }
            } else {
                AnalysisCalcType calcType = AnalysisCalcType.get(analysisConfig.getCalcType());
                //目标值的计算方式，特殊处理
                if(analysisConfig.isTargetValue()){
                    calcType = AnalysisCalcType.get(analysisConfig.getTargetConfig().getTargetCalcType());
                }

                if (AnalysisCalcType.REAL_VALUE != calcType) {

                    //2025-11-06 比率类型的指标，同环比差值也需要按照设置的小数位显示
                    if (AnalysisCalcType.VALUE == AnalysisCalcType.get(analysisConfig.getCalcType())) {
                        formatString = formatString.replaceAll("pt", "");
                        formatString = formatString + "pt";
                    }else if(analysisConfig.isTargetValue() && AnalysisCalcType.get(analysisConfig.getTargetConfig().getTargetCalcType()).isRatio()){
                        formatString = "###,###,##0.00%";
                    }else{
                        formatString = "###,###,##0.00%pt";
                    }

                    //如果百分比指标设置差异率单位为% 特殊处理
                    if (AnalysisCalcType.RATIO == AnalysisCalcType.get(analysisConfig.getCalcType())) {
                        PercentFieldRatioUnitType percentFieldRatioUnitType = PercentFieldRatioUnitType.get(analysisConfig.getPercentFieldRatioUnit());
                        if (PercentFieldRatioUnitType.PERCENT == percentFieldRatioUnitType) {
                            formatString = "###,###,##0.00%";
                        }
                    }
                }
            }


            //占比-同环比处理
            if(AnalysisCalcMode.ZB_THB == AnalysisCalcMode.get(analysisConfig.getCalcMode())){
                switch (AnalysisCalcType.get(analysisConfig.getCalcType())){
                    case RATIO:
                    case VALUE:
                        formatString = "###,###,##0.00%pt";
                        break;
                    case REAL_VALUE:
                        formatString = "###,###,##0.00%";
                        break;
                }
            }


        }

        // 若是日均值，且没有保留小时位，则保留2位小数
        /**
         * 废弃：因为在构造日均meta字段时已设置格式化字符串，再最终输出时会根据其值大小动态设置格式化字符串
         *
        if(AggExpressionType.get(field.getAggExpressionType()).isAvgByDay()){
            if(BIUtil.isNotEmpty(formatString) && !formatString.contains(".")){
                formatString = "###,###,##0.00"; // 日均值特殊标记：小数最后一位是#
            }
        }
         */

        // 添加日均值格式化标识
        if(AggExpressionType.get(field.getAggExpressionType()).isAvgByDay()){
            if(BIUtil.isNotEmpty(formatString) && !formatString.contains(".") && !formatString.contains(BIConsts.AVG_FORMAT_SUFFIX)){
                formatString = formatString + BIConsts.AVG_FORMAT_SUFFIX;
            }
        }

        return formatString;
    }

    /**
     * 重新构造格式化字符串
     * 1 根据传入小数位，重新构建格式化字符串
     * 2 比率类的指标，需要添加%
     * @return
     */
    public static String rebuildFormatString(String formatString,Integer decimalPlaces) {

        if(decimalPlaces == null){
            return formatString;
        }

        boolean isPercent = false;
        if(formatString.contains("%")){
            isPercent = true;
        }

        String  newFormatString = "###,###,##0" + (decimalPlaces > 0 ? "." + StringUtil.createRepeatString("0", decimalPlaces) : "");
        newFormatString = newFormatString + (isPercent ? "%" : "");

        return newFormatString;
    }

    /**
     * 是否是百分比字段
     * @param field
     * @return
     */
    public static boolean isPercentField(QueryField field) {
        String formatString = getFieldFormatString(field);
        if(BIUtil.isEmpty(formatString)){
            return false;
        }
        return formatString.contains("%");
    }

    /**
     * 设置字段初始化信息
     */
    public static void initializeQueryField(MetaField meta, QueryField field){
        if(meta == null && Enabled.value(field.getIsAnalysis())) {
            // 分析字段无元字段信息，需在外层处理
            return;
        }
        if(meta == null) {
            throw new BIException(("字段不存在:" + field.getTitle()));
        }
        field.setMeta(meta);
        if(BIUtil.isEmpty(field.getTitle())){
            field.setTitle(meta.getTitle());
        }

        /*
        if(!BIUtil.isEmpty(meta.getFilterShowType()) && meta.getFilterShowType().indexOf("tree") != -1){
            field.setCascadeFilter(true);
        }
         */

        /**自定义字段*/
        CustomFieldConfigure customFieldConfigure = field.getCustomFieldConfigure();
        if(customFieldConfigure == null || customFieldConfigure.isEmpty()) {
            field.setCustomFieldConfigure(meta.getCustomFieldConfigure());
        }else if(customFieldConfigure.getExpression().contains(CustomFieldType.LOD.getIdentifier())){
            if(CustomFieldType.COMMON == CustomFieldType.get(customFieldConfigure.getType())) {
                // 若自定义字段包含lod标识（含四则运算），则都作为lod字段处理
                customFieldConfigure.setType(CustomFieldType.LOD_CALC.getCode());
            }
        }

        // 对日期格式的条件值转换为真实值并升序排序
        List<FieldValue> values = field.getValues();
        assignmentFieldValues(field, field.getValues());


        // 字符串过滤时，大小写不敏感
        String showType = meta.getFilterShowType();
        if(!Enabled.value(meta.getIsCaseSensitive())
                && DataType.String == DataType.getType(meta.getDataType())
                && FieldFilterType.Textarea == FieldFilterType.get(showType)
                && BIUtil.isNotEmpty(values)) {
            values.forEach(fv -> {
                if(BIUtil.isNotEmpty(fv.getId())) {
                    fv.setId(fv.getId().toLowerCase());
                }
            });
        }


        // 去掉空字符串
        if(FieldFilterType.Textarea == FieldFilterType.get(showType)) {
            values = values.stream().filter(v -> {return BIUtil.isNotEmpty(v.getId());}).collect(Collectors.toList());
        }

        // 布尔型去掉空值
        if(FieldFilterType.BooleanSelect == FieldFilterType.get(showType)) {
            values = values.stream().filter(v -> {return BIUtil.isNotEmpty(v.getId());}).collect(Collectors.toList());
        }

        // 初始化分类聚合信息
        initCategoryAggregationItems(field);

    }

    /**
     * 初始化分类聚合项
     */
    protected static void initCategoryAggregationItems(QueryField field){
        if(!field.isCategoryAggregation()) {
            return;
        }
        List<FieldCategoryAggregationItem> ctgItems = field.getCategoryAggregation().getCategories();
        for(FieldCategoryAggregationItem item : ctgItems){
            List<FieldValue> values = item.getValues();
            if(BIUtil.isEmpty(values)) {
                continue;
            }
            assignmentFieldValues(field, values);
        }
    }

    /**
     * 对range进行排序
     *
     * @param values
     * @return
     */
    public static void sortRangeValues(QueryField field, List<FieldValue> values) {
        MetaField meta = field.getMeta();
        FieldFilterType filterType = FieldUtil.getFilterType(field);
        if (filterType.isDateRange()) {
            List<FieldValue> sortedValues = new ArrayList<>();
            // 支持多时间段，需按照2个值进行分组后排序
            Map<Integer, List<FieldValue>> groupValues = new LinkedHashMap<>();
            int groupId = -1;
            for (int i = 0; i < values.size(); i++) {
                if (i % 2 == 0) {
                    groupId++;
                }
                if (groupValues.get(groupId) != null) {
                    groupValues.get(groupId).add(values.get(i));
                } else {
                    List<FieldValue> list = new ArrayList<>();
                    list.add(values.get(i));
                    groupValues.put(groupId, list);
                }
            }

            for (List<FieldValue> fvs : groupValues.values()) {
                Collections.sort(fvs, new Comparator<FieldValue>() {
                    @Override
                    public int compare(FieldValue o1, FieldValue o2) {
                        if (o1.getId() != null && o2.getId() != null) {
                            String value1 = o1.getId();
                            String value2 = o2.getId();
                            return value1.compareToIgnoreCase(value2);
                        } else {
                            return -1;
                        }
                    }
                });
                sortedValues.addAll(fvs);
            }

            values.clear();
            values.addAll(sortedValues);
        } else if (DataType.getType(meta.getDataType()).isDecimal()) {
            Collections.sort(values, new Comparator<FieldValue>() {
                @Override
                public int compare(FieldValue o1, FieldValue o2) {
                    String v1 = o1.getId();
                    String v2 = o2.getId();
                    if (BIUtil.isEmpty(v1) || BIUtil.isEmpty(v2)) {
                        return -1;
                    }
                    Double d1 = Double.valueOf(v1);
                    Double d2 = Double.valueOf(v2);
                    return d1.compareTo(d2);
                }
            });
        } else {
            Collections.sort(values);
        }
    }

    /**
     * 字段赋值：用于将系统变量的值转换为真实值
     */
    public static void assignmentFieldValues(QueryField field, List<FieldValue> values) {
        MetaField meta = field.getMeta();
        // 对日期格式的条件值排升序
        FieldFilterType fieldFilterType = FieldFilterType.get(meta.getFilterShowType());
        FieldDataType dataType = FieldDataType.getType(meta.getDataType());
        if (FieldFilterType.isDateRange(fieldFilterType)) {
            if (values != null && !values.isEmpty()) {
                for (FieldValue v : values) {
                    String vid = v.getId();
                    // 日期若不是整型格式，则将其值id=title
                    if (FieldFilterType.get(meta.getFilterShowType()) == FieldFilterType.DateRange ||
                            FieldFilterType.get(meta.getFilterShowType()) == FieldFilterType.DatetimeRange) {
                        if (EnvVariableManager.validate(vid)) {// 若是环境变量值
                            vid = EnvVariableManager.value(vid);
                            if ("true".equalsIgnoreCase(SC.v("ssm.date.integer.format", "false"))) { // 是否是integer格式的日期
                                v.setId(vid.replace("\\-", ""));
                            } else {
                                v.setId(vid);
                            }
                        } else {
                            v.setId(v.getTitle());
                        }
                    } else {
                        vid = EnvVariableManager.value(vid);
                        v.setId(vid);
                    }
                }

                // 支持多时间段，需按照2个值进行分组后排序
                sortRangeValues(field, values);
            }
        }
    }

    /**
     * 设置自动的过滤真实值
     * @param field
     * @return
     */
    public static List<FieldValue> getFilterRealValues(QueryField field) {
        List<FieldValue> values = field.getValues();
        if (values == null || values.isEmpty()) {
            return values;
        }
        values.forEach(fv -> {
            fv.setId(EnvVariableManager.value(fv.getId()));
        });
        // 字段数据类型
        FieldDataType dataType = FieldDataType.getType(field.getMeta().getDataType());
        String showType = BIUtil.nvl(values.get(0).getFilterShowType(), field.getMeta().getFilterShowType());
        // 对 textarea可输入多值字段分隔处理
        if ("textarea".equalsIgnoreCase(showType)) {
            String textareaValue = values.get(0).getId();
            if (!StringUtil.isEmpty(textareaValue)) {
                textareaValue = StringUtils.replaceEach(textareaValue, new String[]{"\n", ";", "；"}, new String[]{",", ",", ","});
                String[] strs = textareaValue.split(",");
                List<FieldValue> textareaValues = new ArrayList<FieldValue>();
                for (String s : strs) {
                    if (dataType == FieldDataType.Integer && !BIUtil.isInteger(s)) {// 字段类型为整数，但过滤值必须为整数
                        continue;
                    }
                    if (!StringUtil.isEmpty(s)) {
                        FieldValue fv = new FieldValue();

                        //如果值包含单引号，则需要转义，解决sql语法异常的问题
                        if(s.contains("'")){
                            s = s.replaceAll("'", "''");
                        }

                        fv.setId(s.trim());
                        fv.setTitle(s.trim());
                        fv.setFilterShowType(showType);
                        textareaValues.add(fv);
                    }
                }
                values = textareaValues;
                // values去重
                values = values.stream().distinct().collect(Collectors.toList());
            }
        }

        values.forEach(fv -> fv.setId(replaceSpecialDimFilterValues(fv.getId())));

        // 去掉虚拟值
        values =  values.stream().filter(fv -> !BIConsts.VIRTUAL_FILTER_VALUE.equalsIgnoreCase(fv.getId())).collect(Collectors.toList());

        return values;
    }

    // 处理特殊的维度筛选值
    private static String replaceSpecialDimFilterValues(String ori) {
        if (BIUtil.isEmpty(ori)) {
            return ori;
        }
        return BIConsts.SPECIAL_DIM_FILTER_VALUE_MAP.getOrDefault(ori, ori);
    }

    /**
     * 获取字段值排序表达式
     * @param fieldFullName
     * @return
     */
    public static String getFieldValueSortExpression(String fieldFullName, QueryField field){
        return getFieldValueSortExpression(fieldFullName, field.isCommonDate(), QueryArea.Measure == field.getQueryArea(),field);
    }

    public static String getFieldValueSortExpression(String fieldFullName) {
        return getFieldValueSortExpression(fieldFullName, false, false,new QueryField());
    }

    public static String getFieldValueSortExpression(String fieldFullName, boolean isCommonDate, boolean isMeasure,QueryField field){
        String fieldCode = fieldFullName;
        if(fieldFullName.contains(".")){
            fieldCode = fieldFullName.split("\\.")[1];
        }

        if (fieldCode.contains(BIConsts.GROUPING_VALUE)
                || fieldCode.contains(BIConsts.GROUPING_KEY)
                || isMeasure) {
            return fieldFullName;
        }

        //业务日历的日期字段特殊处理
        CalendarType queryCalendarType = CalendarType.get(field.getQueryCalendarType());
        if(isCommonDate &&  queryCalendarType == CalendarType.NATURAL ){
            return fieldFullName;
        }

        //除以上的所有维度字段加bi_decode函数
        List<MetaFieldValueSort> valueSorts = new ArrayList<>();
        if(queryCalendarType == CalendarType.BUSINESS){
            valueSorts = PromotionManager.getFieldValueSort(field);
        }else{
            valueSorts = SSDMetaCacheManager.getFieldValueSort(fieldCode);
        }

        Map<String, String> dimNumberMap = BIConsts.SPECIAL_DIM_VALUE_NUMBER_MAP;
        List<String> decodeRule = new ArrayList<>();
        List<String> decodeValue = new ArrayList<>();
        for(MetaFieldValueSort valueSort : valueSorts){
            decodeValue.add(valueSort.getFieldValue());
            decodeRule.add(valueSort.getFieldValue());
            decodeRule.add(dimNumberMap.getOrDefault(valueSort.getFieldValue(), valueSort.getFieldValueSortNum()));
        }

        for (Map.Entry<String, String> e: dimNumberMap.entrySet()) {
            if (decodeValue.contains(e.getKey())) {
                continue;
            }
            decodeRule.add(e.getKey());
            decodeRule.add(e.getValue());
        }

        IFunction fx = FunctionManager.getFunction();
        String expression = fx.decode(fieldFullName, BIUtil.listToStr(decodeRule));

        return expression;
    }


    /**
     * 获取前段传递的排序字段内容
     * @param config
     * @return
     */
    public static List<String> getOrderByFragments(QueryConfigure config,List<String> resultFieldCodes) {
        List<String> orderByFragments = new ArrayList<>();
        List<QuerySortItem> sortItems = config.getSettings().getQuerySortItems();

        if (CollUtil.isNotEmpty(sortItems)) {

            for (QuerySortItem sortItem : sortItems) {
                String fieldValueSortExpression = FieldUtil.getFieldValueSortExpression(sortItem, config, resultFieldCodes);
                if (StrUtil.isNotEmpty(fieldValueSortExpression)) {

                    //2025-11-12 修改null值排序规则，asc : nulls first, desc : nulls last
                    String nullsLast = "first";
                    if(FieldSortType.DESC == FieldSortType.getType(sortItem.getOrderType())){
                        nullsLast = "last";
                    }

                    if (Enabled.value(sortItem.getIsNullsLast())) {
                        nullsLast = "last";
                    }

                    orderByFragments.add(String.format("%s %s nulls %s ", fieldValueSortExpression, sortItem.getOrderType(), nullsLast));
                }
            }
        }
        return orderByFragments;
    }

    /**
     * 获取排序
     * @return
     */
    public static String getFieldValueSortExpression(QuerySortItem sortItem,QueryConfigure config,List<String> resultFieldCodes) {

        //是否是sql转置
        boolean isSqlPivot = SSDUtil.isQueryPivot(config);
        String sqlPivotIfExpression = "";

        //排序依据，特殊处理列维度场景
        String columnField = sortItem.getColumnField();

        //将__c__d_替换为_c__d_
        columnField = columnField.replaceAll("_" + BIConsts.COLUMN_DIM_FIELD_SUFFIX, BIConsts.COLUMN_DIM_FIELD_SUFFIX);
        String rawColumnField = columnField.split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
        //处理行总计
        rawColumnField = rawColumnField.replaceAll("_r_total", "");

        //不在结果集中的不处理
        String cfCode;
        if(isSqlPivot){
            cfCode = columnField;
        } else {
            cfCode = rawColumnField;
        }

        if (!resultFieldCodes.stream().filter(f -> f.equals(cfCode)).findAny().isPresent()) {
            return null;
        }

        //是否为维度
        boolean isDim = false;

        //维度需要格式化
        String orderByFieldCode = sortItem.getOrderBy();
        Optional<QueryField> optionalQueryField = config.getResult().getFields().stream().filter(f -> f.getCode().equals(orderByFieldCode)).findAny();
        if (optionalQueryField.isPresent()) {
            if (!optionalQueryField.get().isMeasure()) {
                isDim = true;
                QueryField queryField = optionalQueryField.get();
                if (queryField.isCommonDate() && CalendarType.BUSINESS.getCode().equals(queryField.getQueryCalendarType())) {
                    // 业务日历的排序依赖过滤值，需要特殊处理下
                    if (BIUtil.isEmpty(queryField.getValues())) {
                        queryField = config.getFilterCommonDateField();
                    }
                }
                rawColumnField = getFieldValueSortExpression(orderByFieldCode, queryField);
            }
        }

        List<String> caseWhenExpressions = new ArrayList<>();
        String orderCategory = sortItem.getOrderCategory();


        //20250624 列维度+有排序的场景，改为在sql转置，此处不再转置列维度的值
        if (StrUtil.isNotEmpty((orderCategory)) && !isDim) {

            //获取列维度
            if (!isSqlPivot) {
                List<QueryField> colDims = config.getResult()
                        .getFields()
                        .stream()
                        .filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend())
                        .collect(Collectors.toList());

                if (CollUtil.isNotEmpty(colDims)) {

                    orderCategory = normalizeOrderCategory(orderCategory, colDims.get(0), config.getSettings().getDateGranularity());

                    //不在列维度里面，不处理
                    if (!config.getResult().getPivotConfig().getColDimValues().contains(orderCategory)) {
                        return null;
                    }

                    caseWhenExpressions.add(String.format("%s = '%s'", colDims.get(0).getCode(), orderCategory));
                }
            } else {
                rawColumnField = columnField;
            }
        }

        List<String> partitionDimExpressions = new ArrayList<>();

        //处理列小计的场景
        String orderEntity = sortItem.getOrderEntity();
        if (StrUtil.isNotEmpty(orderEntity) && !isDim) {

            List<String> dimCodeList = new ArrayList<>();

            //没有列小计不处理
            if (!config.getAnalysis().getTotal().isActive()) {
                return null;
            }

            //获取列小计的维度
            AnalysisTotalItemConfig analysisTotalItemConfig = config.getAnalysis().getTotal().getItem(AnalysisTotalType.COL_SUBTOTAL);
            if (analysisTotalItemConfig != null) {
                for (String dimId : analysisTotalItemConfig.getDimIdList()) {
                    MetaField metaField = SSDMetaCacheManager.getField(dimId);
                    if (metaField == null) {

                        //兼容计算维度
                        dimCodeList.add(String.format("%s%s", "_ctm_rd_", dimId));
                        continue;
                    }

                    dimCodeList.add(metaField.getCode());
                }
            }

            //最后一层维度，不处理排序
            if (!dimCodeList.contains(orderEntity)) {
                return null;
            }

            int groupingValue = 0;
            int index = 0;
            List<QueryField> dimFields = config.getResult().getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
            List<QueryField> colDims = config.getResult().getFields().stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend())
                    .collect(Collectors.toList());
            int colDimCount = colDims.size();

            for (QueryField selectField : dimFields) {
                index++;
                String fieldGroupingBit = StringUtils.rightPad("", index, "0");
                String groupingBit = StringUtils.rightPad(fieldGroupingBit, dimFields.size(), "1") + StringUtils.rightPad("", colDimCount, "0");
                groupingValue = NumberUtil.binaryToInt(groupingBit);

                partitionDimExpressions.add(selectField.getCode());
                if (selectField.getCode().equalsIgnoreCase(orderEntity)) {
                    break;
                }
            }

            if (BIConsts.ROW_TOTAL_COLUMN_TITLE.equalsIgnoreCase(orderCategory)) {

                //sql转置后，行总计无需+1
                if (!SSDUtil.isQueryPivot(config)) {
                    groupingValue += 1;
                }

            }

            //添加列小计行的条件
            caseWhenExpressions.add(String.format(" %s = %s ", BIConsts.GROUPING_KEY, groupingValue));

            //max(if(_grp_k = 2,D_ORD_00429_r_total,null)) over(partition by AFM) desc nulls last
            sqlPivotIfExpression = String.format("max(if(%s = %s,%s,null)) over(partition by %s) ",
                    BIConsts.GROUPING_KEY,
                    groupingValue, rawColumnField, BIUtil.listToStr(partitionDimExpressions));
        }


        if (!isSqlPivot) {
            if (CollUtil.isNotEmpty(caseWhenExpressions)) {
                rawColumnField = String.format(" (case when %s  then %s else null end ) ",
                        BIUtil.listToStr(caseWhenExpressions, " and "),
                        rawColumnField
                );


                if(CollUtil.isNotEmpty(partitionDimExpressions)){
                    rawColumnField = String.format("max(%s) over(partition by %s) ",
                            rawColumnField, BIUtil.listToStr(partitionDimExpressions));
                }
            }
        } else {

            if (StrUtil.isNotEmpty(sqlPivotIfExpression)) {
                rawColumnField = sqlPivotIfExpression;
            }

        }

        return rawColumnField;
    }


    /**
     * 处理排序依据的值
     * @param orderCategory
     * @param colDim
     * @return
     */
    public static String normalizeOrderCategory(String orderCategory,QueryField colDim,String dateGranularity) {

        String result = orderCategory;

        try {

            //行总计特殊处理
            if (BIConsts.ROW_TOTAL_COLUMN_TITLE.equalsIgnoreCase(orderCategory)) {
                return BIConsts.ROW_TOTAL_COLUMN_CODE;
            }

            if (!colDim.isCommonDate()) {
                return orderCategory;
            }

            //时间格式化
            DateGranularity dg = DateGranularity.get(dateGranularity);
            switch (dg) {
                case DAY:
                    result = orderCategory.substring(0,10);
                    break;
                case WEEK:
                    result = orderCategory.substring(0,8)
                            .replaceAll("-", "")
                            .replaceAll("W","");
                    break;
                case MONTH:
                    result = orderCategory.substring(0,7)
                            .replaceAll("-","");
                    break;
                case QUARTER:
                    result = orderCategory;
                    break;
                case YEAR:
                    result = orderCategory.substring(0,4);
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取日期字段过滤值：若是
     * @param field
     * @param rawValues
     * @return
     */
    public static List<FieldValue> getDateFieldFilterValues(QueryField field, List<FieldValue> rawValues,QueryConfigure configure){
        if(BIUtil.isEmpty(rawValues) || !field.isCommonDate()){
            return rawValues;
        }

        // 通用日期，元数据=非日粒度，不处理，适配如保有量等场景
        if(DateGranularity.DAY != DateGranularity.get(field.getMeta().getDateGranularity())){
            return rawValues;
        }

        DateGranularity dateGranularity = DateGranularity.get(field.getQueryDateGranularity());

        List<FieldValue> resultValues = new ArrayList<>();
        rawValues.stream().forEach(v->resultValues.add(v.clone()));

        FieldValue v1 = resultValues.get(0);
        FieldValue v2 = resultValues.get(resultValues.size() - 1);

        Date date1 = null;
        Date date2 = null;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        // 周：转换为日期，v1=起始周的第一天，v2=结束周的最后一天
        if(dateGranularity == DateGranularity.WEEK){
            date1 = WeekDateUtil.getWeekFirstDay(v1.getId());
            date2 = WeekDateUtil.getWeekLastDay(v2.getId());
            v1.setId(format.format(date1));
            v2.setId(format.format(date2));
        }

        // 查询配置使用表的作业的最后可用时间
        DateTime lastDay = configure.getSettings().getLastAvailableDate();

        // 月：v1=起始月第一天，v2=min(t-1,结束月的最后一天)
        if(dateGranularity == DateGranularity.MONTH){
            date1 = DateUtil.parse(v1.getId(),"yyyyMM");
            date2 = DateUtil.endOfMonth(DateUtil.parse(v2.getId(),"yyyyMM"));
            if(date2.getTime() > lastDay.getTime()){
                date2 = lastDay;
            }
            v1.setId(format.format(date1));
            v2.setId(format.format(date2));
        }

        //季度：v1=起始季度的第一天，v2=min(t-1,结束月的最后一天)
        if(dateGranularity == DateGranularity.QUARTER){
            date1 = QuarterDateUtil.getQuarterFirstDay(v1.getId());
            date2 = QuarterDateUtil.getQuarterEndDay(v2.getId());
            if(date2.getTime() > lastDay.getTime()){
                date2 = lastDay;
            }
            v1.setId(format.format(date1));
            v2.setId(format.format(date2));
        }

        // 年：v1=起始年的第一天，v2=结束年的最后一天
        if(dateGranularity == DateGranularity.YEAR){
            date1 = DateUtil.parse(v1.getId(), "yyyy");
            date2 = DateUtil.endOfYear(DateUtil.parse(v2.getId(),"yyyy"));
            if(date2.getTime() > lastDay.getTime()){
                date2 = lastDay;
            }
            v1.setId(format.format(date1));
            v2.setId(format.format(date2));
        }
        return resultValues;
    }

    /**
     * 获取字段的过滤类型
     * @param field
     * @return
     */
    public static FieldFilterType getFilterType(QueryField field){
        FieldFilterType filterType = FieldFilterType.None;
        if(field == null){
            return filterType;
        }

        // 公共日期兼容处理
        if(field.isCommonDate()){
            DateGranularity dateGranularity = DateGranularity.get(field.getMeta().getDateGranularity());
            switch (dateGranularity){
                case YEAR:
                    filterType = FieldFilterType.YearRange;
                    break;
                case MONTH:
                    filterType = FieldFilterType.MonthRange;
                    break;
                case WEEK:
                    filterType = FieldFilterType.WeekRange;
                    break;
                default:
                    filterType = FieldFilterType.DateRange;
                    break;
            }
            return filterType;
        }

        // 优先从过滤值中获取过滤类型
        List<FieldValue> values = field.getValues();
        if(BIUtil.isNotEmpty(values)){
            filterType = FieldFilterType.get(values.get(0).getFilterShowType());
        }
        if(filterType == FieldFilterType.None && field.getMeta() != null){
            filterType = FieldFilterType.get(field.getMeta().getFilterShowType());
        }

        return filterType;
    }

    /**
     * 是否是日期字段
     * @param field
     * @return
     */
    public static boolean isDateField(QueryField field){
        MetaField meta = field.getMeta();
        FieldFilterType filterType = getFilterType(field);
        if (DataType.getType(meta.getDataType()) == DataType.Date
                || DataType.getType(meta.getDataType()) == DataType.Datetime
                || filterType.isDateRange()){
            return true;
        }
        return false;
    }

    /**
     * 获取code相同字段权重最大的字段列表
     * @param fields
     * @return
     */
    public static List<MetaField> getMaxWeightFields(List<MetaField> fields,String ctgId) {
        if (BIUtil.isEmpty(fields)) {
            return fields;
        }

        // 先排序：确保权重一样时每次返回的字段一致
        sortMetaField(fields, ctgId,FieldSortType.DESC);

        Map<String, MetaField> maxWeightFields = new HashMap<>();
        fields.stream().forEach(m -> {
            MetaField compareField = m.clone();
            compareField.setSameCodeFieldList(new ArrayList<>());
            MetaField maxWeightField = maxWeightFields.get(compareField.getCode());
            if (maxWeightField == null) {
                maxWeightFields.put(compareField.getCode(), compareField);
            } else {
                // 事实表字段权重加权
                int maxWeight = BIUtil.isNotEmpty(maxWeightField.getFactTableId()) ? maxWeightField.getWeight() * 10000 : maxWeightField.getWeight();
                int compareWeight = BIUtil.isNotEmpty(compareField.getFactTableId()) ? compareField.getWeight() * 10000 : compareField.getWeight();
                if (maxWeight < compareWeight) {
                    compareField.getSameCodeFieldList().add(maxWeightField);
                    compareField.getSameCodeFieldList().addAll(maxWeightField.getSameCodeFieldList());
                    maxWeightField.setSameCodeFieldList(new ArrayList<>());
                    // 若存在相同code，则取权重最大
                    maxWeightFields.put(compareField.getCode(), compareField);
                } else {
                    maxWeightField.getSameCodeFieldList().add(compareField);
                }
            }
        });

        List<MetaField> result = new ArrayList<>();
        result.addAll(maxWeightFields.values());

        sortMetaField(result, ctgId,FieldSortType.ASC);

        return result;
    }

    /**
     * 字段排序
     * @param fields
     */
    public static void sortMetaField(List<MetaField> fields,String ctgId,FieldSortType fieldSortType) {

       //1 构建字段id和字段排序的映射关系
        Map<String, Double> fieldSortIdMap = new HashMap<>();
        for (MetaField field : fields) {
            fieldSortIdMap.put(field.getId(), SSDMetaCacheManager.getFieldSortId(field.getId(), ctgId));
        }

        Collections.sort(fields, new Comparator<MetaField>() {
            @Override
            public int compare(MetaField o1, MetaField o2) {
                int flag = 0;

                Double showOrder1 = fieldSortIdMap.get(o1.getId());
                Double showOrder2 = fieldSortIdMap.get(o2.getId());

                if (FieldSortType.DESC == fieldSortType) {
                    // 降序：showOrder大的排前面，使用Double的compare方法更规范
                    flag = Double.compare(showOrder2, showOrder1);
                } else {
                    // 升序：showOrder小的排前面
                    flag = Double.compare(showOrder1, showOrder2);
                }

                // showOrder相等时，按ID排序兜底
                return flag == 0 ? o1.getId().compareTo(o2.getId()) : flag;
            }
        });
    }

    /**
     * 判断是否是计算字段：包括前端用户自定义计算字段、后端配置的聚合表达式计算字段
     * @param field
     * @return
     */
    public static boolean isCalcField(QueryField field){
        if(field.isCustom()){
            return true;
        }
        MetaField meta = field.getMeta();
        if(null != meta) {
            String expression = meta.getAggExpression();
            if (!StringUtil.isEmpty(expression) && expression.indexOf("[") != -1) {
                return true;
            }

            //兼容计算维度常量
            if(!Enabled.value(meta.getIsMeasure())){
                if(!StringUtil.isEmpty(expression)){
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * 判断是否是计算字段：包括前端用户自定义计算字段、后端配置的聚合表达式计算字段
     * @param field
     * @return
     */
    public static boolean isCalcField(MetaField field){
        if(null != field) {
            String expression = field.getAggExpression();
            if (!StringUtil.isEmpty(expression) && expression.indexOf("[") != -1) {
                return true;
            }
        }
        return false;
    }


    /**
     * 通过表达式提取字段引用key
     * @param expression
     * @return
     */
    public static Set<String> fetchFieldRefKeys(String expression){
        Set<String> keys = new HashSet<String>();
        if(BIUtil.isEmpty(expression)){
            return keys;
        }
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern); //Pattern.compile("(?<=\\[)(.+?)(?=\\])");
        Matcher matcher = pattern.matcher(expression);
        while(matcher.find()){
            keys.add(matcher.group());
        }
        return keys;
    }

    /**
     * 计算字段使用的跨模型指标，需要将跨模型计算字段的表达式展开
     */
    public static String getExtendCalcExpression(String expression) {

        if (StrUtil.isEmpty(expression)) {
            return expression;
        }

        Set<String> refIds = FieldUtil.fetchFieldRefKeys(expression);
        if (BIUtil.isEmpty(refIds)) {
            return expression;
        }

        for (String refId : refIds) {
            MetaField metaField = SSDMetaCacheManager.getField(refId);
            if (metaField == null) {
                continue;
            }

            if (FieldType.CROSS_MODEL_MEASURE != FieldType.get(metaField.getFieldType())) {
                continue;
            }

            expression = expression.replaceAll("\\[" + refId + "\\]", " ("+metaField.getAggExpression()+") ");
        }

       return expression;

    }


    /**
     * 解析计算字段并将计算字段的原子字段添加到该计算字段中，返回附加字段（非原子字段列表中的）
     * @param calcField
     * @param configQueryFields
     * @return
     */
    public static void setCalcFieldAtomFields(QueryField calcField, List<QueryField> configQueryFields) {
        if(!calcField.isCalc()) {
            return ;
        }
        calcField.getCalcAtomFields().clear();

        /*
        // 前端用户自定义计算指标表达式使用id引用字段，如：sum([id1])/count(id2])
        // 前端用户自定义字段最终查询时，会设置其元数据并将表达式转为字段code引用表达式（在CustomFieldManager.getMeta中处理），此时需要走code解析，因为存在嵌套引用的场景。转换后计算字段的MetaField会赋值
        boolean isRefCode = true;
        // 若是用户自定义字段，且没有被转换为code，则依然引用的是id
        if(calcField.isCustom() &&
                (   // 无元数据，则表达式未被转换为code引用
                    calcField.getMeta() == null
                    // 有元数据，但表达式未被转换为code引用
                   || (calcField.getMeta() != null && BIUtil.isEmpty(calcField.getMeta().getAggExpression()))
                 )
            ){
            isRefCode = false;
        }

        // 例外：lod表达式会转换为自定义字段，但此时其计算表达式中引用的是字段id
        if(calcField.isLodField()){
            isRefCode = false;
        }
        */
        // 用户自定义字段先通过id设置其原子字段：因id最精准。
        // 避免先通过code匹配不准确后，在通过id匹配时，无法精准匹配。
        // 场景：跨模块计算字段[模块1字段id1]/[模块2字段id2] ，转为code后：[字段code1]/[字段code2]，导致通过code匹配在相同表时无法精准找到，从而随机获取一个匹配code的字段
        if(calcField.isCustom()){
            // 用户自定义字段：通过id获取其原子字段
            Set<String> keys = fetchFieldRefKeys(calcField.getCustomFieldConfigure().getExpression());
            Set<QueryField> atomFields = getCalcFieldAtomFields(calcField, configQueryFields, keys, false);
            calcField.getCalcAtomFields().addAll(atomFields);
            calcField.getCusCalcDependFields().addAll(atomFields);
        }
        Set<String> keys = fetchFieldRefKeys(calcField.getMeta().getAggExpression());
        Set<QueryField> atomFields = getCalcFieldAtomFields(calcField, configQueryFields, keys, true);
        calcField.getCalcAtomFields().addAll(atomFields);
    }

    protected static Set<QueryField> getCalcFieldAtomFields(QueryField calcField, List<QueryField> configQueryFields, Set<String> keys , Boolean isRefCode) {
        Set<QueryField> atomFields = new HashSet<>(8);
        if(!calcField.isCalc() || BIUtil.isEmpty(keys)) {
            return atomFields;
        }

        // 解析分析字段的依赖
        Map<String, CustomFieldExpressionIdMapping> configMap = new HashMap<>(8);
        if (calcField.getCustomFieldConfigure() != null && CollUtil.isNotEmpty(calcField.getCustomFieldConfigure().getExpressionIdMapping())) {
            for (CustomFieldExpressionIdMapping mapping : calcField.getCustomFieldConfigure().getExpressionIdMapping()) {
                if (mapping.getId().contains(CustomFieldType.ANALYSIS.getIdentifier())) {
                    configMap.put(mapping.getId(), mapping);
                }
            }
        }

        for(String key : keys){
            QueryField qf = null;
            for(QueryField atomField : configQueryFields){
                if(key.equalsIgnoreCase(atomField.getCode()) && isRefCode) {
                    qf = atomField;
                    break;
                }
                if(key.equalsIgnoreCase(atomField.getId()) && !isRefCode) {
                    qf = atomField;
                    break;
                }
            }
            if(qf != null){ // 若已经在结果字段中，则不处理
                // 添加计算字段的原子字段
                atomFields.add(qf);
                continue;
            }
            MetaField meta = null;
            if(isRefCode) {
                // 优先从和计算字段相同的所属表中获取
                if(key.contains(".")){
                    meta = SSDMetaCacheManager.getFieldByFullName(key);
                }else {
                    meta = SSDMetaCacheManager.getFieldByCode(calcField.getMeta().getTableId(), key);
                }

                //兼容自定义聚合表达式使用field_id的场景
                if(meta == null){
                    meta = SSDMetaCacheManager.getField(key);
                }

                if(meta == null) {
                    List<MetaField> metaList = SSDMetaCacheManager.getFieldByCode(key);
                    if (!metaList.isEmpty()) {
                        Collections.sort(metaList);
                        meta = metaList.get(0); // 取权重最大的
                    }
                }
            } else if (key.contains(CustomFieldType.ANALYSIS.getIdentifier())) {
                // 依赖的是分析字段
                CustomFieldExpressionIdMapping mapping = configMap.get(key);
                if (mapping != null && mapping.getAnalysisItemConfig() != null) {
                    AnalysisItemConfig itemConfig = mapping.getAnalysisItemConfig();
                    // 先在查询中找， 如依赖lod等计算字段
                    for (QueryField atomField : configQueryFields) {
                        if (Objects.equals(atomField.getId(), itemConfig.getMeasureId())) {
                            qf = atomField;
                            break;
                        }
                        if (Objects.equals(atomField.getCode(), itemConfig.getMeasureCode())) {
                            qf = atomField;
                            break;
                        }
                    }
                    if (qf != null) { // 若已经在结果字段中，则不处理
                        atomFields.add(qf); // 添加计算字段的原子字段
                        continue;
                    }

                    // 后面从元数据中找
                    meta = SSDMetaCacheManager.getField(itemConfig.getMeasureId());
                    if (meta == null) {
                        String rawCode = itemConfig.getMeasureCode();
                        meta = SSDMetaCacheManager.getFieldByCode(calcField.getMeta().getTableId(), rawCode);
                        if (meta == null) {
                            List<MetaField> metaList = SSDMetaCacheManager.getFieldByCode(rawCode);
                            if (!metaList.isEmpty()) {
                                Collections.sort(metaList);
                                meta = metaList.get(0); // 取权重最大的
                            }
                        }
                    }
                }
            } else {
                meta = SSDMetaCacheManager.getField(key);
            }
            if(meta == null) {
                continue;
            }
            qf = new QueryField(meta);
            qf.setId(meta.getId());
            qf.setAppend(true);	// 表达式附加字段

            qf.setIsResult(calcField.getIsResult());
            qf.setIsFilter(calcField.getIsFilter());
            qf.setRawQueryArea(calcField.getRawQueryArea());
            qf.setQueryArea(calcField.getQueryArea());

            // 设置其查询时的聚合表达式
            for(AggExpressionType aggExpressionType : AggExpressionType.values()){
                if(BIUtil.isNotEmpty(aggExpressionType.getCode()) && qf.getId().endsWith(aggExpressionType.getCode())){
                    qf.setAggExpressionType(aggExpressionType.getCode());
                    qf.setDistinctByDayAggMode(calcField.getDistinctByDayAggMode());
                    break;
                }
            }


            atomFields.add(qf); // 添加计算字段的原子字段
        }

        /*
        Set<String> keys = new HashSet<String>();
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern); //Pattern.compile("(?<=\\[)(.+?)(?=\\])");
        Matcher matcher = pattern.matcher(expression);
        while(matcher.find()){
            keys.add(matcher.group());
        }
         */
        //calcField.getCalcAtomFields().clear();

        /*
        // 先按code
        setCalcFieldAtomFields(calcField, keys, atomFields, true);
        // 再按id
        setCalcFieldAtomFields(calcField, keys, atomFields, false);
         */
        //setCalcFieldAtomFields(calcField, keys, atomFields, isRefCode);

        return atomFields;
    }


    /**
     * 获取目录下的公共日期字段
     */
    public static void getCtgCommonDateField(MetaFieldCategory metaFieldCategory,List<MetaField> commonDateFieldList) {

        if (metaFieldCategory != null) {

            List<MetaField> fields = SSDMetaCacheManager.getFieldByCategory(metaFieldCategory.getId());
            fields = fields.stream().filter(f->Enabled.value(f.getIsCommonDate())).collect(Collectors.toList());
            fields = getMaxWeightFields(fields,metaFieldCategory.getId());

            for (MetaField metaField : fields) {
                if (Enabled.value(metaField.getIsCommonDate())) {
                    commonDateFieldList.add(metaField);
                    break;
                }
            }

            if (CollUtil.isEmpty(commonDateFieldList)) {
                MetaFieldCategory parent = SSDMetaCacheManager.getCategoryById(metaFieldCategory.getParentId());
                getCtgCommonDateField(parent, commonDateFieldList);
            }

        }

    }

    /**
     * 字段排序：按查询配置的字段顺序排序：行维度+列维度+指标
     * @param fields
     * @return
     */
    public static List<QueryField> sortByShowOrder(QueryConfigure config, List<QueryField> fields){
        List<QueryField> newFields = new ArrayList<>();

        List<QueryField> rowFields = new ArrayList<>();
        List<QueryField> colFields = new ArrayList<>();
        List<QueryField> measures = new ArrayList<>();

        for (QueryField f : fields) {
            if(f.getShowOrder() == -1){
                // 若字段无排序，则默认使用config中顺序
                QueryField resultField = config.getResult().getFieldByCode(f.getCode());
                if(resultField != null && resultField.getShowOrder() != -1){
                    f.setShowOrder(resultField.getShowOrder());
                }
            }
            if (f.getRawQueryArea() == QueryArea.RowDimension) {
                rowFields.add(f);
            } else if (f.getRawQueryArea() == QueryArea.ColumnDimension) {
                colFields.add(f);
            } else {
                measures.add(f);
            }
        }
        Collections.sort(rowFields);
        Collections.sort(colFields);
        Collections.sort(measures);

        newFields.addAll(rowFields);
        newFields.addAll(colFields);
        newFields.addAll(measures);

        return newFields;
    }

    /**
     * 获取字段精度
     * @param field
     * @return
     */
    public static Integer getPrecision(MetaField field){
        if(field == null){
            return 0;
        }
        if(Enabled.isFalse(field.getIsMeasure())){
            return 0;
        }
        String formatStr = field.getShowFormatExpression();
        if(BIUtil.isNotEmpty(formatStr)){
            //去掉空格
            formatStr = formatStr.trim();
            formatStr = formatStr.replace("%", "");
            if(!formatStr.contains(".")){
                return 0;
            }
            return formatStr.length() - formatStr.indexOf(".") - 1;
        }
        return FieldDataType.getType(field.getDataType()) == FieldDataType.Integer ? 0 : 2;
    }

    /**
     * 判断字段是否存在
     * @param field
     * @return
     */
    public static boolean isExists(QueryField field){
        if(field == null){
            return false;
        }
        // 自定义字段不处理
        if(field.isCustom() || field.isVirtual()){
            return true;
        }
        if(SSDMetaCacheManager.getField(field.getId()) == null){
            return false;
        }
        return true;
    }

    public static String getTitle(QueryField field){
        String fieldTitle = BIUtil.isEmpty(field.getDisplayTitle()) ? field.getMeta().getTitle() : field.getDisplayTitle();

        //设置了日均，修改字段显示
        AggExpressionType aggExpressionType = AggExpressionType.get(field.getAggExpressionType());
        if (aggExpressionType.isAvgByDay()) {
            if(AggExpressionType.get(field.getDistinctByDayAggMode()) != AggExpressionType.Sum) {
                fieldTitle = String.format("%s（%s）", fieldTitle, aggExpressionType.getTitle());
            }
        }
        return fieldTitle;
    }


    /**
     * 自定义计算字段语法修正
     * 场景：
     * bi_get_date_progress 时间进度
     * bi_get_left_days 剩余天数
     * @return
     */
    public static String rectifytCustomCalcExpression(String expression,QueryConfigure config,String commonDateFieldName) {

        if (StrUtil.isEmpty(expression)) {
            return expression;
        }

        expression = expression.trim();

        //汇总不处理
        if (config.isAggQuery()) {
            return expression;
        }

        //非月季年不处理
        DateGranularity dateGranularity = DateGranularity.get(config.getSettings().getDateGranularity());
        if (DateGranularity.MONTH != dateGranularity && DateGranularity.QUARTER != dateGranularity && DateGranularity.YEAR != dateGranularity) {
            return expression;
        }

        String dateFuncExpression = "";

        //计算日期进度
        if (expression.contains(BIConsts.GET_DATE_PROGRESS_FLAG)) {
            dateFuncExpression = getDateProgressExpression(config,commonDateFieldName);
            expression = expression.replaceAll(Pattern.quote(BIConsts.GET_DATE_PROGRESS_FLAG), dateFuncExpression);
        }

        //计算剩余日期天数
        if (expression.contains(BIConsts.GET_LEFT_DAYS_FLAG)) {
            dateFuncExpression = getDateLeftDaysExpression(config,commonDateFieldName);
            expression = expression.replaceAll(Pattern.quote(BIConsts.GET_LEFT_DAYS_FLAG), dateFuncExpression);
        }

        return expression;
    }

    public static String getDateProgressExpression(QueryConfigure config,String commonDateFieldName){
        IFunction function = FunctionManager.getFunction();
        String expression = function.getDateProgress(config.getSettings().getDateGranularity(),commonDateFieldName);
        return expression;
    }

    public static String getDateLeftDaysExpression(QueryConfigure config,String commonDateFieldName){
        IFunction function = FunctionManager.getFunction();
        String expression = function.getDateLeftDays(config.getSettings().getDateGranularity(),commonDateFieldName);
        return expression;
    }

    /**
     * 获取分析指标的四则运算表达式
     */
    public static List<String> getAnalysisCalcExpressions(List<QueryField> analysisCalcFields, List<QueryField> dimFields,
                                                          List<String> selectFragments, QueryConfigure config,
                                                          String mainAlias, String groupKey) {
        List<String> result = new ArrayList<>(8);
        if (CollUtil.isEmpty(analysisCalcFields)) {
            return result;
        }
        Map<String, CustomFieldExpressionIdMapping> configMap = new HashMap<>(8);
        for (QueryField queryField : analysisCalcFields) {
            if (queryField.getCustomFieldConfigure() == null || CollUtil.isEmpty(queryField.getCustomFieldConfigure().getExpressionIdMapping())) {
                continue;
            }
            for (CustomFieldExpressionIdMapping mapping : queryField.getCustomFieldConfigure().getExpressionIdMapping()) {
                if (mapping.getId().contains(CustomFieldType.ANALYSIS.getIdentifier())) {
                    configMap.put(mapping.getId(), mapping);
                }
            }
        }

        Map<String, String> fieldMap = new HashMap<>();
        for (String select : selectFragments) {
            String[] expression = select.split(" as | AS ");
            if (expression.length == 2) {
                fieldMap.put(expression[1], expression[0]);
            } else if (expression.length == 1) {
                //没有别名的
                expression = select.split("\\.");
                if (expression.length == 2) {
                    fieldMap.put(StringUtils.trim(expression[1]), select);
                }
            }
        }


        // 字段表达式构造方式
        Function<String, String> fieldSqlSupplier = field -> FieldUtil.getTableFiledExpression(Collections.singletonList(mainAlias), null, field);
        // 获取分组值和partition by字段的关系
        Map<Integer, List<String>> groupingPartitionMap = AnalysisTotalUtil.getGroupingPartitionMap(config, fieldSqlSupplier);

        Map<String, QueryField> measureFieldMap = config.getResult().getMeasures().stream().collect(Collectors.toMap(QueryField::getId, QueryField -> QueryField, (f1, f2) -> f1));

        for (QueryField queryField : analysisCalcFields) {
            AnalysisItemConfig itemConfig = queryField.getAnalysisConfig();
            String expression = queryField.getCustomFieldConfigure().getExpression();

            Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
            Matcher matcher = pattern.matcher(expression);
            while (matcher.find()) {
                String fieldId = matcher.group();
                QueryField calcAtomField = config.getResult().getFieldById(fieldId);
                String fieldCode;
                if (calcAtomField != null) {
                    fieldCode = calcAtomField.getCode();
                } else {
                    CustomFieldExpressionIdMapping mapping = configMap.get(fieldId);
                    fieldCode = mapping.getCode();
                }
                String fieldExpression = fieldMap.getOrDefault(fieldCode, mainAlias + "." + fieldCode);
                expression = expression.replaceAll("\\[" + fieldId + "\\]", fieldExpression);
            }

            BaseOperator operator = OperatorFactory.getOperator(itemConfig.getCalcMode());
            if (operator != null) {
                OperatorContext operatorContext = new OperatorContext();
                operatorContext.setConfig(config);
                operatorContext.setDimFields(dimFields);

                String ratioUnit = PercentFieldRatioUnitType.PERCENT.getCode();

                //从原子字段判断是否是百分比字段
                QueryField rawMeasureField = measureFieldMap.get(itemConfig.getMeasureId());
                // 百分比指标
                if (FieldUtil.isPercentField(rawMeasureField)) {
                    ratioUnit = itemConfig.getPercentFieldRatioUnit();
                }

                expression = operator.getCalcExpression(operatorContext, "(" + expression + ")", mainAlias, groupKey, ratioUnit);
            }

            if (!AnalysisTotalAggType.isDefault(queryField.getTotalAggType())) {
                // 处理汇总指标的自定义聚合方式  由于是需要从明细直接汇总，所以需要在此包一层
                expression = AnalysisTotalUtil.buildTotalMeasureAggExpression(queryField, expression, groupingPartitionMap, fieldSqlSupplier, groupKey);
            }

            if (StrUtil.isNotEmpty(expression)) {
                result.add(String.format("%s as %s", expression, queryField.getCode()));
            }
        }
        return result;
    }

    /**
     * 获取字段标题
     * @param itemField
     * @return
     */
    public static String getPivotItemFieldTitle(QueryField itemField, String itemValue, QueryEngine engine){
        String title = itemValue;
        if(isEmpty(title)) {
            return title;
        }

        //业务日历的公共日期，直接返回格式化后的值。例：2025双十一·开门红(2025-10-30-2025-11-03)
        if(engine.getConfig().getSettings().isBusinessCalendar()&& itemField.isCommonDate()){
          //  title = PromotionManager.getFormatPromotionName(title);
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
                    if(engine.getConfig().isShowLunarDate()){
                        formatString = IFunction.Format_Lunar_Date;
                    }
                    break;
            }
        }
        if(BIUtil.isNotEmpty(formatString)) {
            title = engine.formatValue(itemValue, itemField.getMeta().getDataType(), formatString, itemField.getRawCode()) + "";
        }
        return title;
    }


    /**
     * 获取门店开业月份 的统计日期字段
     * substring(vf.dt, 1, 10)
     * @return
     */
    public static String buildShopOpenMonthDateFieldExpression(QueryField field,String dateFieldAlias) {

        //构建dt字段
        MetaField meta = field.getMeta();
        String fieldFullName = String.format("%s.%s", dateFieldAlias, meta.getName());

        // 处理日期字段或被扩展的字段：如："年月"由"日期"字段扩展
        String formatStr = meta.getShowFormatExpression();

        //是否是公共日期字段，公共日期通过前端日期控件的粒度格式化
        if (field.isCommonDate()) {
            formatStr = ShowFormatExpressionType.getFormatExpressionByDateGranularity(field.getQueryDateGranularity());
        }

        if (!StringUtil.isEmpty(formatStr)) {
            IFunction function = FunctionManager.getFunction();
            fieldFullName = function.date2Char(fieldFullName, formatStr);
        }

        return fieldFullName;
    }

    public static String getTableFiledExpression(List<String> tableAlias, IFunction fx, String code) {
        if (CollUtil.isEmpty(tableAlias) || code == null) {
            return null;
        }

        if (tableAlias.size() == 1) {
            return tableAlias.get(0) + "." + code;
        } else if (fx == null) {
            return null;
        } else {
            List<String> coalesceDims = new ArrayList<>();
            for (String alias : tableAlias) {
                coalesceDims.add(alias + "." + code);
            }
            return fx.coalesce(coalesceDims);
        }
    }

    public static String getModelFiledExpression(List<StarModel> models, IFunction fx, String code) {
        if (CollUtil.isEmpty(models) || code == null || fx == null) {
            return null;
        }

        List<String> coalesceDims = new ArrayList<>();
        for (StarModel model : models) {
            if (model.getFieldByCode(code) != null) {
                coalesceDims.add(model.getAlias() + "." + code);
            }
        }
        return fx.coalesce(coalesceDims);
    }

    // 获取sortId, 把可能被依赖的字段排在前面
    public static int getSortId(QueryField a) {
        if (a == null) {
            return 0;
        }

        boolean isLod = a.isLodField();
        boolean isCalc = a.isCalc();
        boolean isAnalysisCalc = a.isAnalysisCalc();
        boolean isLodCalc = CustomFieldType.LOD_CALC == CustomFieldType.get(a.getCustomFieldConfigure().getType());

        if (Enabled.isTrue(a.getIsAnalysis())) {
            return 5;
        }

        if (isAnalysisCalc) {
            //是分析指标计算字段
            return 4;
        }

        if (isLodCalc) {
            //是LOD计算字段
            return 3;
        }

        if (isLod) {
            // lod字段
            return 2;
        }

        if (isCalc) {
            //是计算字段
            return 3;
        }

        return 0;
    }

    public static void main(String[] args) {
        Double d = 12.209;
        DecimalFormat df = new DecimalFormat("##0.00%pt");
        String str = df.format(d);
        System.out.println(str);
        System.out.println(str.replace("%pt", "pt"));
        String startDate = "202401";
        String endDate = "202401";
        String formatStr = "yyyyMM";
        Date d1 = DateUtil.parse(startDate, formatStr);
        DateTime d2 = DateUtil.endOfMonth(DateUtil.parse(endDate, formatStr));
        System.out.println(d2.toDateStr());
        long days = DateUtil.between(d1, d2, DateUnit.DAY, true);
        long removeDays = DateUtil.between(new Date(), d2, DateUnit.DAY, true)+1;
        days = days - removeDays;
        System.out.println(days+1);

        Double i = 0.0;
        String s1 = "###,###,###";
        s1 = s1.substring(0, s1.length() - 1) + 0;
        System.out.println(s1);

        String sql = "select * from ${tableName} where 1=2";
        String key = "tableName";
        String value = "bi_view.test";
        sql = sql.replaceAll(String.format("\\$\\{%s\\}", key), value);
        System.out.println(sql);
    }

}
