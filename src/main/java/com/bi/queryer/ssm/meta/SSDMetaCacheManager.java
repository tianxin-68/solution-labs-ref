package com.bi.queryer.ssm.meta;

/**
 * User: contributor
 * Date: 2020/2/4
 * Time: 10:41
 * Description:
 */

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.ext.FieldAutoExtenderManager;
import com.bi.queryer.ssm.engine.ext.IFieldAutoExtender;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.event.MetadataCacheLoadedEvent;
import com.bi.queryer.ssm.meta.flush.FlushCacheExecutor;
import com.bi.queryer.ssm.meta.flush.FlushCacheExecutorFactory;
import com.bi.queryer.ssm.meta.flush.MetaCache;
import com.bi.queryer.ssm.mgr.fieldCtg.FieldCtgService;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgNeedAuthEntity;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.util.AggregatorItem;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.ssm.util.SqlExpressionUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多维分析元数据缓存管理类
 * @author contributor
 *
 */
@Component
@Qualifier("SSDMetaCacheManager")
public class SSDMetaCacheManager implements Initializable {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 字段元数据
     */
    protected static Map<String, MetaField> fieldsCache = new LinkedHashMap<String, MetaField>(5000);
    protected static Map<String, MetaField> fieldsCacheTemp = new LinkedHashMap<String, MetaField>(5000);

    /**
     * 字段数据权限
     */
    protected static Map<String, List<MetaFieldDataAuth>> fieldsDataAuthCfg = new HashMap<>(100);
    protected static Map<String, List<MetaFieldDataAuth>> fieldsDataAuthCfgTemp = new HashMap<>(100);

    /**
     * 表元数据
     */
    protected static Map<String, MetaTable> tablesCache = new LinkedHashMap<String, MetaTable>(500);
    protected static Map<String, MetaTable> tablesCacheTemp = new LinkedHashMap<String, MetaTable>(500);
    protected static Map<String,MetaTableEtlJob> tableEtljobCache = new HashMap<>(500);

    //使用的etl任务
    protected static Set<String> tableEtlJobSet = new HashSet<>(500);

    // <tableId, <fieldCode, MetaField>>
    protected static Map<String, Map<String, MetaField>> tablesFieldsCache = new HashMap<>(500);
    /**
     * 关联关系元数据
     */
    protected static List<MetaTableRelation> relationsCache = new ArrayList<MetaTableRelation>();
    protected static List<MetaTableRelation> relationsCacheTemp = new ArrayList<MetaTableRelation>();

    /**
     * 层次元数据
     */
    protected static Map<String, HierarchyInfo> hierarchies = new LinkedHashMap<String, HierarchyInfo>();
    protected static Map<String, HierarchyInfo> hierarchiesTemp = new LinkedHashMap<String, HierarchyInfo>();

    /**
     * 字段互斥元数据
     */
//    protected static CopyOnWriteArrayList<MetaFieldExclude> fieldExcludes = new CopyOnWriteArrayList<MetaFieldExclude>();
//    protected static List<MetaFieldExclude> fieldExcludesTemp = new ArrayList<MetaFieldExclude>();

    /**
     * 字段所属目录
     */
    protected static Map<String, MetaFieldCategory> categories = new HashMap<String, MetaFieldCategory>(100);
    protected static Map<String, MetaFieldCategory> categoriesTemp = new HashMap<String, MetaFieldCategory>(100);

    /**
     * 字段所属虚拟目录
     */
    protected static Map<String, MetaVirtualCategory> virtualCategories = new HashMap<String, MetaVirtualCategory>(100);
    protected static Map<String, MetaVirtualCategory> virtualCategoriesTemp = new HashMap<String, MetaVirtualCategory>(100);

    /**
     * 前台目录下权重最大的字段列表
     */
    protected static Map<String, List<MetaField>> categoryMaxWeightFields = new HashMap<>(10);
    protected static Map<String, List<MetaField>> categoryMaxWeightFieldsTemp = new HashMap<>(10);

    protected static List<MetaFieldValueSort> fieldValueSortList = new ArrayList<>(50);
    protected static List<MetaFieldValueSort> fieldValueSortListTemp = new ArrayList<>(50);

    /**
     * 字段与目录关系缓存，key = 字段id + 目录id
     */
    protected static Map<String,MetaFieldCtgRel> fieldCtgRelCache = new HashMap<>(5000);
    protected static Map<String,MetaFieldCtgRel> fieldCtgRelCacheTemp = new HashMap<>(5000);

    /**
     * 过滤tree字段缓存
     */
    private static List<MetaField> filterTreeFields = new ArrayList<MetaField>();

    /**
     * 结果tree字段缓存
     */
    private static List<MetaField> resultTreeFields = new ArrayList<MetaField>();

    /**
     * 数据集元数据缓存
     */
    private static Map<String,MetaDataset> datasetsCache = new HashMap<String, MetaDataset>(10);

    /**
     * 数据集下的表
     */
    private static Map<String, MetaDatasetTable> datasetTablesCache = new HashMap<String, MetaDatasetTable>(5);

    /**
     * 表与表之间的关系
     */
    private static Map<String, List<MetaTable>> relationTablesCache =  new HashMap<String, List<MetaTable>>(50);

    /**
     * 主子表配置
     */
    private static Map<String,List<MetaTablePriSubCfg>> tablePriSubCfgCache = new HashMap<>(50);

    /**
     * 维度枚举值映射
     */
    private static Map<String,List<MetaFieldValueMap>> fieldValueMapCache = new HashMap<>(50);

    /**
     * 第一层 key = 数据集id
     * 第二层 key = 字段编码  value = 数据表信息
     */
    private static Map<String,Map<String,List<RtTableInfo>>> fieldRtTableCache = new HashMap<>(100);

    public synchronized void initialize(Map<String, ?> initParams) throws BIException{

        // 判断是否全量刷新,非全量刷新走定制化逻辑
        CacheFlushType cacheFlushType = CacheFlushType.get(BIUtil.nvl(initParams.get("cacheFlushType"),""));
        if(CacheFlushType.ALL != cacheFlushType && CacheFlushType.FLUSH_ALL_NEW_MGP != cacheFlushType){
            flushCacheByType(cacheFlushType,initParams);
            return;
        }

        long t1 = System.currentTimeMillis();

        MetaCache metaCache = new MetaCache();
        //刷新资产管理平台的数据，先从缓存中读取老多维的数据
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            buildMetaCache(metaCache);
        }

        long t2 = System.currentTimeMillis();
        System.out.println("缓存耗时：构建老多维缓存对象。耗时"+(t2-t1)+"ms");

        clearTemp();

        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

        //新多维数据源
        DataSourceType mgpDataSourceType  = SSDUtil.getMgpDataSourceType();

        // 字段
        String sqlId = "ssm.cache.queryAllMetaField";
        List<MetaField> fieldList = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            fieldList.addAll(metaCache.getMetaFieldList());
        }else{
            fieldList = (List<MetaField>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        long t3 = System.currentTimeMillis();
        System.out.println("缓存耗时：构建字段缓存ssm。耗时"+(t3-t2)+"ms");
        fieldList = appendMgpData(fieldList,sqlId,null,mgpDataSourceType);
        long t4 = System.currentTimeMillis();
        System.out.println("缓存耗时：构建字段缓存mgp。耗时"+(t4-t3)+"ms");

        //查询字段对应的前台目录
        List<MetaFieldCtgRel> metaFieldCtgRels = (List<MetaFieldCtgRel>) dao.queryObjectList("ssm.cache.queryAllFieldCtgRel", null, DataSourceType.Default);
        metaFieldCtgRels = appendMgpData(metaFieldCtgRels,"ssm.cache.queryAllFieldCtgRel",null,mgpDataSourceType);
        Map<String, MetaFieldCtgRel> fieldCtgRelMap = new HashMap<>();
        for(MetaFieldCtgRel rel : metaFieldCtgRels){
            fieldCtgRelMap.put(rel.getFieldId() , rel);
        }

        if(fieldList != null) {
            MetaField commonDateField = null;
            for (MetaField mf : fieldList) {
                // 若code为sql关键字，则添加下划线后缀
                String[] keywords = SC.v("field.code.keywords", "ALL").split(",");
                for(String kw : keywords){
                    if(BIUtil.isNotEmpty(mf.getCode()) && mf.getCode().equals(kw)){
                        mf.setCode(mf.getCode() + SC.v("field.code.keyword.escape.suffix", "_"));
                    }
                }

                MetaFieldCtgRel metaFieldCtgRel = fieldCtgRelMap.get(mf.getId());
                if(metaFieldCtgRel != null) {
                    mf.setCategoryId(metaFieldCtgRel.getCtgId());
                }else {
                    //没有挂在目录下不显示
                    mf.setIsShow(Enabled.NO.getId());
                }

                if (StrUtil.isNotEmpty(mf.getCategoryId())) {
                    List<String> categoryIdList = Arrays.asList(mf.getCategoryId().split(","));
                    mf.setCategoryIdList(categoryIdList);
                }

                fieldsCacheTemp.put(mf.getId(), mf);
                if (Enabled.isTrue(mf.getIsCommonDate())) {
                    commonDateField = mf;
                    // 设置公共日期字段的可用日期粒度集合
                    mf.setAvailableDateGranularityList(getAvailableDateGranularityList(commonDateField));
                }
            }
            if (commonDateField != null) {
                for (MetaField mf : fieldList) {
                    if (commonDateField.getCode().equalsIgnoreCase(mf.getCode())) {
                        mf.setIsCommonDate(commonDateField.getIsCommonDate());
                        if(CollUtil.isEmpty(mf.getAvailableDateGranularityList())){
                            mf.setAvailableDateGranularityList(getAvailableDateGranularityList(mf));
                        }

                    }
                }
            }

            if (fieldsCacheTemp != null) {
                fieldsCache.clear();
                fieldsCache.putAll(fieldsCacheTemp);
                fieldsCacheTemp.clear();
            }

            for (MetaField mf : fieldList) {
                // 自动扩展字段
                IFieldAutoExtender extender = FieldAutoExtenderManager.getExtender(mf);
                if (extender != null) {
                    List<MetaField> extendFields = extender.extend(mf);
                    if (extendFields != null) {
                        for (MetaField ef : extendFields) {
                            fieldsCache.put(ef.getId(), ef);
                        }
                    }
                }
            }


        }

        long t5 = System.currentTimeMillis();
        System.out.println("缓存耗时：格式化字段与扩展字段。耗时"+(t5-t4)+"ms");

        //表依赖作业
        sqlId = "ssm.cache.queryAllTableEtlJob";
        List<MetaTableEtlJob> metaTableEtlJobList = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            metaTableEtlJobList.addAll(metaCache.getMetaTableEtlJobList());
        }else{
            metaTableEtlJobList = (List<MetaTableEtlJob>)dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }

        long t6 = System.currentTimeMillis();
        System.out.println("缓存耗时：表依赖缓存ssm。耗时"+(t6-t5)+"ms");
        metaTableEtlJobList = appendMgpData(metaTableEtlJobList,sqlId,null,mgpDataSourceType);

        long t7 = System.currentTimeMillis();
        System.out.println("缓存耗时：表依赖缓存mgp。耗时"+(t7-t6)+"ms");

        if(CollUtil.isNotEmpty(metaTableEtlJobList)){
            for(MetaTableEtlJob metaTableEtlJob:metaTableEtlJobList){
                tableEtljobCache.put(metaTableEtlJob.getTableId(),metaTableEtlJob);
                tableEtlJobSet.add(metaTableEtlJob.getEtlJob());
            }
        }

         // 表
        sqlId = "ssm.cache.queryAllMetaTable";
        List<MetaTable> tablesList = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            tablesList.addAll(metaCache.getTablesList());
        }else{
            tablesList = (List<MetaTable>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        long t8 = System.currentTimeMillis();
        System.out.println("缓存耗时：表缓存ssm。耗时"+(t8-t7)+"ms");
        tablesList = appendMgpData(tablesList,sqlId,null,mgpDataSourceType);
        long t9 = System.currentTimeMillis();
        System.out.println("缓存耗时：表缓存mgp。耗时"+(t9-t8)+"ms");

        if(tablesList != null){
            buildTables(tablesList);
        }
        if(tablesCacheTemp != null){
            tablesCache.clear();
            tablesCache.putAll(tablesCacheTemp);
            tablesCacheTemp.clear();
        }

        // 字段数据权限
        sqlId = "ssm.cache.queryAllFieldDataAuth";
        List<MetaFieldDataAuth> authList = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            authList.addAll(metaCache.getFieldDataAuthList());
        }else{
            authList = (List<MetaFieldDataAuth>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        authList = appendMgpData(authList,sqlId,null,mgpDataSourceType);

        if(authList != null) {
            List<MetaFieldDataAuth> whitePaperAuthList = new ArrayList<>();
            authList.forEach(auth -> {
                            MetaFieldDataAuth whitePaperAuth = JSONObject.parseObject(auth.toJSON().toJSONString(), MetaFieldDataAuth.class);
                            whitePaperAuth.setFieldCode(whitePaperAuth.getWhitePaperCode());
                            whitePaperAuthList.add(whitePaperAuth);
                    });
            authList.addAll(whitePaperAuthList);

            //auth 按fieldCode去重
            List<MetaFieldDataAuth> finalAuthList =  new ArrayList<>();
            Map<String,String> authFielCodeMap = new HashMap<>();
            for(MetaFieldDataAuth dataAuth : authList){
                if(authFielCodeMap.containsKey(dataAuth.getFieldCode())){
                    continue;
                }

                finalAuthList.add(dataAuth);
                authFielCodeMap.put(dataAuth.getFieldCode(),"");
            }

            finalAuthList.forEach(auth -> {
                if(fieldsDataAuthCfgTemp.containsKey(auth.getFieldCode())) {
                    fieldsDataAuthCfgTemp.get(auth.getFieldCode()).add(auth);
                }else {
                    List<MetaFieldDataAuth> list = new ArrayList<>();
                    list.add(auth);
                    fieldsDataAuthCfgTemp.put(auth.getFieldCode(), list);
                }
            });
        }
        if(fieldsDataAuthCfgTemp != null){
            fieldsDataAuthCfg.clear();
            fieldsDataAuthCfg.putAll(fieldsDataAuthCfgTemp);
            fieldsDataAuthCfgTemp.clear();
        }

        // 表关系
        sqlId = "ssm.cache.queryAllMetaTableRel";

        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            relationsCacheTemp.addAll(metaCache.getTableRelationList());
        }else{
            relationsCacheTemp = (List<MetaTableRelation>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        relationsCacheTemp = appendMgpData(relationsCacheTemp,sqlId,null,mgpDataSourceType);

        if(relationsCacheTemp != null){
            normalizeRelationsCache();
            relationsCache.clear();
            relationsCache.addAll(relationsCacheTemp);
            relationsCacheTemp.clear();
        }

        //主子表配置
        sqlId = "ssm.cache.queryAllMetaTablePriSubCfg";
        List<MetaTablePriSubCfg> tablePriSubCfgList = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            tablePriSubCfgList.addAll(metaCache.getTablePriSubCfgList());
        }else{
            tablePriSubCfgList = (List<MetaTablePriSubCfg>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        tablePriSubCfgList = appendMgpData(tablePriSubCfgList,sqlId,null,mgpDataSourceType);
        buildTablePriSubCfg(tablePriSubCfgList);

        //字段维度枚举值映射
        sqlId = "ssm.cache.queryFieldValueMap";
        List<MetaFieldValueMap> metaFieldValueMaps = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            metaFieldValueMaps.addAll(metaCache.getFieldItemMaps());
        } else {
            metaFieldValueMaps = (List<MetaFieldValueMap>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        metaFieldValueMaps = appendMgpData(metaFieldValueMaps,sqlId,null,mgpDataSourceType);
        buildFieldItemMap(metaFieldValueMaps);


        // 字段前台目录
        sqlId = "ssm.cache.queryAllMetaFieldCategory";
        List<MetaFieldCategory> categoryList = new ArrayList<>();
        if(CacheFlushType.FLUSH_ALL_NEW_MGP == cacheFlushType){
            categoryList.addAll(metaCache.getCategoryList());
        }else{
            categoryList = (List<MetaFieldCategory>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        }
        long t10 = System.currentTimeMillis();
        System.out.println("缓存耗时：前台目录缓存ssm。耗时"+(t10-t9)+"ms");
        categoryList = appendMgpData(categoryList,sqlId,null,mgpDataSourceType);
        long t11 = System.currentTimeMillis();
        System.out.println("缓存耗时：前台目录缓存mgp。耗时"+(t11-t10)+"ms");

        buildCategories(categoryList,cacheFlushType);

        long t12 = System.currentTimeMillis();
        System.out.println("缓存耗时：前台目录构造。耗时"+(t12-t11)+"ms");
        
        if(categoriesTemp != null){
            categories.clear();
            categories.putAll(categoriesTemp);
            categoriesTemp.clear();
        }

        //设置目录的模块目录id
        buildCategoriesModuleCtgId();

        // 查询前台目录的权限配置数据
        List<CtgNeedAuthEntity> ctgNeedAuthList = (List<CtgNeedAuthEntity>) dao.queryObjectList("fieldCtg.queryAllNeedAuthList", null,DataSourceType.Default);
        ctgNeedAuthList = appendMgpData(ctgNeedAuthList,"fieldCtg.queryAllNeedAuthList",null,mgpDataSourceType);

        // 匹配目录申请所需的权限数据
        buildCtgAuthNeedData(ctgNeedAuthList);

        // 字段后台虚拟目录
        List<MetaVirtualCategory> virtualCategoryList = new ArrayList<>();
        Map<String, MetaFieldCategory> backCategories = getBackCategories();
        backCategories.values().forEach(bc -> {
            virtualCategoryList.add(new MetaVirtualCategory(bc.getId(), bc.getName(), bc.getShowOrder(), bc.getParentId()));
        });
        buildVirtualCategories(virtualCategoryList);
        if(virtualCategoriesTemp != null){
            virtualCategories.clear();
            virtualCategories.putAll(virtualCategoriesTemp);
            virtualCategoriesTemp.clear();
        }

        // 字段值排序
        sqlId = "ssm.cache.queryAllMetaFieldValueSort";
        fieldValueSortListTemp = (List<MetaFieldValueSort>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        if(fieldValueSortListTemp != null){
            fieldValueSortList.clear();
            fieldValueSortList.addAll(fieldValueSortListTemp);
            fieldValueSortListTemp.clear();
        }

        List<MetaFieldCtgRel> fieldCtgRelList = (List<MetaFieldCtgRel>) dao.queryObjectList("ssm.cache.queryAllFieldCtgRelList", null,mgpDataSourceType);
        buildFieldCtgRel(fieldCtgRelList);
        if(fieldCtgRelCacheTemp != null){
            fieldCtgRelCache.clear();
            fieldCtgRelCache.putAll(fieldCtgRelCacheTemp);
            fieldCtgRelCacheTemp.clear();
        }

        // 构建表与字段map关联，便于快速查找表中字段
        buildTableFields();

        // 递归处理计算字段的依赖
        replaceCalcFieldExpressionDependency();

        //构建数据集
        List<MetaDataset> datasetList = (List<MetaDataset>) dao.queryObjectList("ssm.cache.queryAllMetaDataset", null, DataSourceType.Default);
        datasetList = appendMgpData(datasetList,"ssm.cache.queryAllMetaDataset",null,mgpDataSourceType);
        buildDataset(datasetList);

        // 构建数据集与表的关系
        buildDatasetTables();

        // 构建表关联关系
        buildRelationTables();

        // 构建字段敏感级别
        buildFieldSensitiveLevel();

        // 构建目录敏感等级聚合
        buildCategorySensitiveLevel();

        //构建
        buildFieldRtTableCache();

        // 发布加载完成事件
        eventPublisher.publishEvent(new MetadataCacheLoadedEvent(this));

        System.out.println("*************************ssm缓存刷新完成***************");

    }

    /**
     * 部分事实表与维表需要采用特殊的关联关系
     * 例：  bi_olap.ads_whs_stk_check_abnormal_stock_user_dtl_di
     * left join [shuffle] bi_olap.dim_vcl_vehicle_f
     */
    public void normalizeRelationsCache() {

        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<MetaTableRelation> metaTableRelationList = dao.queryObjectList("ssm.table.rel.join.cfg.queryAll", null, MetaTableRelation.class);

        if(CollUtil.isEmpty(metaTableRelationList)){
            return;
        }

        for (MetaTableRelation metaTableRelation : metaTableRelationList){
           Optional<MetaTableRelation> optional = relationsCacheTemp.stream().filter(r -> r.getPrimaryTableId().equals(metaTableRelation.getPrimaryTableId())&&
                    r.getSubTableId().equalsIgnoreCase(metaTableRelation.getSubTableId())).findAny();

           if(!optional.isPresent()){
               continue;
           }

           optional.get().setJoinExpression(metaTableRelation.getJoinExpression());
        }
    }


    // 递归处理计算字段的依赖
    private void replaceCalcFieldExpressionDependency() {
        // 嵌套计算字段处理：将嵌套表达式转为非嵌套表达式
        if (fieldsCache == null) {
            return;
        }
        for (MetaField mf : fieldsCache.values()) {

            if (FieldUtil.isCalcField(mf)) {

                //跨模型字段不处理
                if (FieldType.CROSS_MODEL_MEASURE == FieldType.get(mf.getFieldType())) {
                    setCalcFieldAtomFields(mf);
                    continue;
                }

                Map<String, Integer> recursionIndexes = new HashMap<>();
                setCalcFieldExpressionAndAtomField(mf, recursionIndexes);
            }
        }
    }

    /**
     * 获取可用的日期粒度
     * @return
     */
    public List<String> getAvailableDateGranularityList(MetaField field){

        List<String> availableDateGranularityList = new ArrayList<>();

        //按日期不聚合，取当前粒度
        if(Enabled.value(field.getIsDateNonAgg())){
            availableDateGranularityList.add(field.getDateGranularity());
            return availableDateGranularityList;
        }

        //日期可以向上聚合
        DateGranularity dateGranularity = DateGranularity.get(field.getDateGranularity());
        availableDateGranularityList.add(dateGranularity.getCode());
        switch (dateGranularity) {
            case DAY:
                availableDateGranularityList.add(DateGranularity.WEEK.getCode());
                availableDateGranularityList.add(DateGranularity.MONTH.getCode());
                availableDateGranularityList.add(DateGranularity.QUARTER.getCode());
                availableDateGranularityList.add(DateGranularity.YEAR.getCode());
                break;
            case WEEK:
                availableDateGranularityList.add(DateGranularity.MONTH.getCode());
                availableDateGranularityList.add(DateGranularity.QUARTER.getCode());
                availableDateGranularityList.add(DateGranularity.YEAR.getCode());
                break;
            case MONTH:
                availableDateGranularityList.add(DateGranularity.QUARTER.getCode());
                availableDateGranularityList.add(DateGranularity.YEAR.getCode());
                break;
            case QUARTER:
                availableDateGranularityList.add(DateGranularity.YEAR.getCode());
                break;
        }

        return availableDateGranularityList;
    }

    /**
     * 获取目录的模块id
     */
    public void  buildCategoriesModuleCtgId() {

        if(CollUtil.isEmpty(categories.values())){
            return;
        }

        for(MetaFieldCategory metaFieldCategory : categories.values()){
            metaFieldCategory.setModuleCtgId(getModuleCtgIdByCategoryId(metaFieldCategory.getId()));
        }
    }

    private void buildCtgAuthNeedData(List<CtgNeedAuthEntity> ctgNeedAuthList) {
        Map<String, MetaFieldCategory> categoryMap = categories.entrySet().stream()
                .filter(map -> CategoryType.get(map.getValue().getType()) == CategoryType.Front)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        ctgNeedAuthList.forEach(item -> {
            MetaFieldCategory category = categoryMap.get(item.getCtgId());
            if (category != null) {
                DataEnv dataEnv = DataEnv.get(category.getDataEnv());
                if (hasDimRelatedField(category, item.getDimCode()) || DataEnv.NEW_MGP == dataEnv) {
                    Map<String, List<String>> resultMap = category.getAuthMap();
                    if (resultMap == null) {
                        resultMap = MapUtil.newHashMap();
                        category.setAuthMap(resultMap);
                    }
                    List<String> resultList = resultMap.get(item.getDimCode());
                    if (resultList == null) {
                        resultList = ListUtil.list(false);
                        resultMap.put(item.getDimCode(), resultList);
                    }
                    resultList.add(item.getItemCode());
                }
            }

        });
    }

    // 递归查找该目录下是否包含
    private Boolean hasDimRelatedField(MetaFieldCategory category, String dimCode) {

        if(category == null){
            return false;
        }
        List<MetaField> fieldList = category.getFields();
        if (CollectionUtil.isNotEmpty(fieldList)) {
            for (MetaField field : fieldList) {
                if (dimCode.equals(field.getDataAuthDimCode())) {
                    return true;
                }
            }
        }
        List<MetaFieldCategory> childrenList = category.getChildren();
        if (CollectionUtil.isNotEmpty(childrenList)) {
            for (MetaFieldCategory child : childrenList) {
                if (hasDimRelatedField(child, dimCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建层级
     * @param hierList
     */
    protected static void buildHierarchies(List<HierarchyInfo> hierList){
        if(hierList == null){
            return ;
        }
        for(HierarchyInfo hire : hierList){
            MetaField level1Field = fieldsCache.get(hire.getLevel1FieldId());
            if(level1Field != null) {
                hire.setLevel1FieldKeyName(level1Field.getFieldKeyName());
                hire.setLevel1FieldName(level1Field.getName());
            }
            MetaField level2Field = fieldsCache.get(hire.getLevel2FieldId());
            if(level2Field != null) {
                hire.setLevel2FieldKeyName(level2Field.getFieldKeyName());
                hire.setLevel2FieldName(level2Field.getName());
            }
            MetaField level3Field = fieldsCache.get(hire.getLevel3FieldId());
            if(level3Field != null) {
                hire.setLevel3FieldKeyName(level3Field.getFieldKeyName());
                hire.setLevel3FieldName(level3Field.getName());
            }
            MetaField level4Field = fieldsCache.get(hire.getLevel4FieldId());
            if(level4Field != null) {
                hire.setLevel4FieldKeyName(level4Field.getFieldKeyName());
                hire.setLevel4FieldName(level4Field.getName());
            }
            MetaField level5Field = fieldsCache.get(hire.getLevel5FieldId());
            if(level5Field != null) {
                hire.setLevel5FieldKeyName(level5Field.getFieldKeyName());
                hire.setLevel5FieldName(level5Field.getName());
            }

            hierarchiesTemp.put(hire.getFieldId(), hire);
        }
    }


    /**
     * 缓存表与字段的关联
     */
    protected static void buildTables(List<MetaTable> tableList) {
        Map<String, List<MetaField>> dimTableFieldsMap = fieldsCache.values().stream()
                .filter(v -> v.getDimTableId() != null).collect(Collectors.groupingBy(MetaField::getDimTableId));
        Map<String, List<MetaField>> factTableFieldsMap = fieldsCache.values().stream()
                .filter(v -> v.getFactTableId() != null).collect(Collectors.groupingBy(MetaField::getFactTableId));

        Map<String, List<String>> tableEtlJobMap = tableEtljobCache.values().stream()
                .collect(Collectors.groupingBy(MetaTableEtlJob::getTableId, Collectors.mapping(MetaTableEtlJob::getEtlJob, Collectors.toList())));

        for (MetaTable table : tableList) {
            if (BIUtil.isNotEmpty(table.getDataSliceCfg())) {
                table.setDataSliceCfgObj(JSON.parseObject(table.getDataSliceCfg(), TableDataSliceCfg.class));
            }
            // 缓存
            tablesCacheTemp.put(table.getId(), table);
            // 添加metafield节点
            Set<MetaField> fields = new HashSet<>(dimTableFieldsMap.getOrDefault(table.getId(), Collections.emptyList()));
            fields.addAll(factTableFieldsMap.getOrDefault(table.getId(), Collections.emptyList()));
            table.getFields().addAll(fields);
            /*
            // 添加其扩展字段（如：日均）
            for (MetaField mf: fields) {
                List<MetaField> extendFields = mf.getExtendFields();
                if(BIUtil.isNotEmpty(extendFields)){
                    table.getFields().addAll(extendFields);
                }
            }
            */
            //添加表依赖作业
            table.getEtlJobs().addAll(tableEtlJobMap.getOrDefault(table.getId(), Collections.emptyList()));

            if (CollUtil.isNotEmpty(table.getFields())) {
                Optional<MetaField> metaFieldOpt = table.getFields().stream().filter(f -> Enabled.value(f.getIsCommonDate())).findAny();
                if (metaFieldOpt.isPresent()) {
                    table.setDateGranularity(metaFieldOpt.get().getDateGranularity());
                    table.setAvailableDateGranularityList(metaFieldOpt.get().getAvailableDateGranularityList());
                }
            }
        }
    }

    protected static void buildTableFields() {
        Map<String, Map<String, MetaField>> tablesFieldsCacheTemp = new HashMap<>(500);
        for(MetaTable table : tablesCache.values()){
            Map<String, MetaField> tableFields = new HashMap<>(1000);
            for(MetaField f : table.getFields()){
                tableFields.put(f.getCode(), f);
            }
            tablesFieldsCacheTemp.put(table.getId(), tableFields);
        }
        tablesFieldsCache.clear();
        tablesFieldsCache.putAll(tablesFieldsCacheTemp);
    }

    /**
     * 缓存数据集
     */
    protected static void buildDataset(List<MetaDataset> datasetList) {

        Map<String, MetaDataset> datasetCacheTemp = new HashMap<>(10);
        for(MetaDataset dataset : datasetList){
            datasetCacheTemp.put(dataset.getDatasetId(), dataset);
        }

        if(datasetCacheTemp != null){
            datasetsCache.clear();
            datasetsCache.putAll(datasetCacheTemp);
        }

    }

    protected static void buildDatasetTables() {
        Map<String, MetaDatasetTable> dsTables = new HashMap<>(10);
        for(MetaTable t : tablesCache.values()){
            /** 去掉：此方法构建数据集下所有的表（事实表+维度表）
            if(Enabled.isFalse(t.getIsFactTable())){
                continue;
            }
             */
            List<MetaField> fields = t.getFields();
            if(BIUtil.isEmpty(fields)){
                continue;
            }
            Set<String> datasetIdSet = new HashSet<>();
            Set<String> moduleIdSet = new HashSet<>();
            for(MetaField f : fields){
                List<String> ctgIdList = f.getCategoryIdList();
                if(BIUtil.isEmpty(ctgIdList)){
                    continue;
                }
                for(String ctgId : ctgIdList){
                    MetaFieldCategory c = getCategoryById(ctgId);
                    if(c != null){
                        moduleIdSet.add(c.getModuleCtgId());
                        if(BIUtil.isNotEmpty(c.getDatasetId())){
                            datasetIdSet.addAll(Arrays.asList(c.getDatasetId().split(",")));
                        }
                    }
                }
            }
            if(BIUtil.isNotEmpty(datasetIdSet)) {
                for(String datasetId : datasetIdSet){
                    MetaDatasetTable datasetTable = dsTables.getOrDefault(datasetId, new MetaDatasetTable(datasetId));
                    datasetTable.addTable(t, moduleIdSet);
                    dsTables.put(datasetId, datasetTable);
                }
            }
        }
        datasetTablesCache.clear();
        datasetTablesCache.putAll(dsTables);
        dsTables.clear();
    }

    protected void buildRelationTables(){
        Map<String, List<MetaTable>> relTables = new HashMap<>(50);
        for(MetaTableRelation r : relationsCache){
           String primaryTableId = r.getPrimaryTableId();
           String subTableId = r.getSubTableId();

           MetaTable subTable = tablesCache.get(subTableId);
           if(subTable != null) {
               List<MetaTable> tables = relTables.getOrDefault(primaryTableId, new ArrayList<>(5));
               if(!tables.contains(subTable)){
                   tables.add(subTable);
               }
               relTables.put(primaryTableId, tables);
           }

            MetaTable primaryTable = tablesCache.get(primaryTableId);
            if(primaryTable != null) {
                List<MetaTable> tables = relTables.getOrDefault(subTableId, new ArrayList<>(5));
                if(!tables.contains(primaryTable)){
                    tables.add(primaryTable);
                }
                relTables.put(subTableId, tables);
            }
        }
        relationTablesCache.clear();
        relationTablesCache.putAll(relTables);
        relTables.clear();
    }

    /**
     * 构建主子表配置
     * @param tablePriSubCfgList
     */
    protected void buildTablePriSubCfg(List<MetaTablePriSubCfg> tablePriSubCfgList) {
        Map<String, List<MetaTablePriSubCfg>> privSubCfgMap = new HashMap<>(50);
        for (MetaTablePriSubCfg cfg : tablePriSubCfgList) {
            String priTableId = cfg.getPriTableId();
            List<MetaTablePriSubCfg> cfgList = privSubCfgMap.getOrDefault(priTableId, new ArrayList<>(5));
            cfgList.add(cfg);
            privSubCfgMap.put(priTableId, cfgList);
        }

        tablePriSubCfgCache.clear();
        tablePriSubCfgCache.putAll(privSubCfgMap);
        privSubCfgMap.clear();
    }


    /**
     * 构建主子表配置
     * @param metaFieldValueMaps
     */
    protected void buildFieldItemMap(List<MetaFieldValueMap> metaFieldValueMaps) {
        Map<String, List<MetaFieldValueMap>> fieldItemMap = new HashMap<>(50);
        for (MetaFieldValueMap itemMap : metaFieldValueMaps) {
            String fieldCode = itemMap.getFieldCode();
            List<MetaFieldValueMap> fieldMap = fieldItemMap.getOrDefault(fieldCode, new ArrayList<>(8));
            fieldMap.add(itemMap);
            fieldItemMap.put(fieldCode, fieldMap);
        }

        fieldValueMapCache.clear();
        fieldValueMapCache.putAll(fieldItemMap);
        fieldItemMap.clear();
    }

    public static String getDataSetId(MetaField field){
        if(field == null){
            return "";
        }
        String categoryId = field.getCategoryId();
        if(BIUtil.isEmpty(categoryId)){
            return "";
        }
        String[] ctgIds = categoryId.split(",");
        for(String ctgId : ctgIds){
            MetaFieldCategory category = getCategoryById(ctgId);
            if(category != null){
                return category.getDatasetId();
            }
        }
        return "";
    }

    /**
     * 缓存目录列表,构建父子结构
     * @param categoryList
     */
    protected static void buildCategories(List<MetaFieldCategory> categoryList,CacheFlushType cacheFlushType){

        Map<String, List<MetaField>> categoryFieldsMap = new HashMap<>();

        for(MetaField mf : fieldsCache.values()) {

            if (CollUtil.isNotEmpty(mf.getCategoryIdList())) {

                for (String ctgId : mf.getCategoryIdList()) {
                    List<MetaField> metaFieldList = categoryFieldsMap.get(ctgId);
                    if (metaFieldList == null) {
                        metaFieldList = new ArrayList<>();
                    }
                    metaFieldList.add(mf);
                    categoryFieldsMap.put(ctgId, metaFieldList);
                }
            }

        }

        //2024-07-27 后台目录没有使用，暂不处理
       // Map<String, List<MetaField>> virtualCategoryFieldsMap = fieldsCache.values().stream()
            //    .filter(v -> v.getVirtualCategoryId() != null).collect(Collectors.groupingBy(MetaField::getVirtualCategoryId));

        //分析师与开发owner
        List<String> owners = new ArrayList<String>();
        for(MetaFieldCategory category : categoryList){

            if(StrUtil.isNotEmpty(category.getRptDevOwner())){
                owners.addAll(Arrays.asList(category.getRptDevOwner().split(",")));
            }

            if(StrUtil.isNotEmpty(category.getBizOwner())){
                owners.addAll(Arrays.asList(category.getBizOwner().split(",")));
            }

            if(StrUtil.isNotEmpty(category.getDataDevOwner())){
                owners.addAll(Arrays.asList(category.getDataDevOwner().split(",")));
            }

            // 缓存
            categoriesTemp.put(category.getId(), category);
            // 添加metafield节点
            Set<MetaField> fields = new HashSet<>(categoryFieldsMap.getOrDefault(category.getId(), Collections.emptyList()));
           // fields.addAll(virtualCategoryFieldsMap.getOrDefault(category.getId(), Collections.emptyList()));
            category.getFields().addAll(fields);
        }

        //查询人员信息
        Map<String,User> userMap = new HashMap<>();
        if(CollUtil.isNotEmpty(owners)) {
            owners = owners.stream().distinct().collect(Collectors.toList());
            BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
            List<User> userList = (List<User>) dao.queryObjectList("user.queryUserListByNames", owners);
            if (CollUtil.isNotEmpty(userList)) {
                userList.stream().forEach(user -> userMap.put(user.getName(), user));
            }
        }

        for(MetaFieldCategory category : categoryList){

            //设置分析师和开发负责人显示名称
            DataEnv dataEnv = DataEnv.get(category.getDataEnv());
            //单独刷新资产管理缓存时，老多维不刷新目录owner名称
            if(CacheFlushType.FLUSH_ALL_NEW_MGP != cacheFlushType || DataEnv.NEW_MGP == dataEnv ) {
                category.setRptDevOwnerDesc(buildDisplayUserName(category.getRptDevOwner(), userMap));
                category.setBizOwnerDesc(buildDisplayUserName(category.getBizOwner(), userMap));
                category.setDataDevOwnerDesc(buildDisplayUserName(category.getDataDevOwner(), userMap));
            }

            MetaFieldCategory parent = categoriesTemp.get(category.getParentId());
            if(parent != null){
                category.setParent(parent);
                parent.getChildren().add(category);
            }
        }

        // 相同目录下，字段code相同的字段若维度表存在，去掉事实表字段
        Set<String> multipliedFields = new HashSet<>(1024);
        for(MetaFieldCategory ctg : categoriesTemp.values()){
            MetaFieldCategory parent = ctg.getParent();
            if(parent == null){
                continue;
            }
            if(!"公共维度".equals(parent.getName())){
                continue;
            }
            for(MetaField field : ctg.getFields()){
                if(BIUtil.isNotEmpty(field.getDimTableId())) {
                    if (multipliedFields.contains(field.getId())) {
                        // 有多个公共维度可能乘多次，导致权重字段溢出
                        continue;
                    }
                    field.setWeight(field.getWeight() * 9999999);
                    multipliedFields.add(field.getId());
                }
            }
            /**
            List<MetaField> removeFields = new ArrayList<>();
            for(MetaField field : ctg.getFields()){
                if(BIUtil.isNotEmpty(field.getFactTableId())){
                    for(MetaField field2 : ctg.getFields()){
                        if(field2.getCode().equals(field.getCode())){
                            if(BIUtil.isNotEmpty(field2.getDimTableId()) && getRelation(field.getFactTableId(), field2.getDimTableId()) != null){
                                removeFields.add(field);
                            }
                        }
                    }
                }
            }
            if(BIUtil.isNotEmpty(removeFields)) {
                ctg.getFields().removeAll(removeFields);
            }*/
        }
    }

    /**
     * 构建用户显示名称
     * @param names
     * @param userMap
     * @return
     */
    public static String buildDisplayUserName(String names, Map<String, User> userMap) {
        String result = "";

        if (StrUtil.isEmpty(names)) {
            return result;
        }

        String[] nameArray = names.split(",");
        List<String> nameList = new ArrayList<>();
        for (String name : nameArray) {
            User user = userMap.get(name);
            if (user != null) {
                nameList.add(String.format("%s(%s)", user.getRealName(), user.getName()));
            }
        }

        result = BIUtil.listToStr(nameList, ",");

        return result;
    }

    /**
     * 缓存虚拟目录列表,构建父子结构
     * @param virtualCategoryList
     */
    protected static void buildVirtualCategories(List<MetaVirtualCategory> virtualCategoryList){
        for(MetaVirtualCategory category : virtualCategoryList){
            // 缓存
            virtualCategoriesTemp.put(category.getId(), category);
            // 添加metafield节点
//            for(MetaField mf : fieldsCache.values()){
//                if(mf.getVirtualCategoryId() != null && mf.getVirtualCategoryId().equals(category.getId())){
//                    category.getFields().add(mf);
//                }
//            }
        }

        for(MetaVirtualCategory category : virtualCategoryList){
            MetaVirtualCategory parent = virtualCategoriesTemp.get(category.getParentId());
            if(parent != null){
                category.setParent(parent);
                parent.getChildren().add(category);
            }
        }
    }

    /**
     * 构建字段目录路径
     */
    protected static void buildFieldCategoryPath(){
//        for(MetaField field : fieldsCache.values()){
//            String ctgId = field.getCategoryId();
//            if(ctgId == null) {
//                continue;
//            }
//            MetaFieldCategory ctg = categories.get(ctgId);
//            if(ctg == null) {
//                continue;
//            }
//            String idPath = "";
//            String namePath = "";
//            while(ctg != null){
//                idPath = ctg.getId() + ";" + idPath;
//                namePath = ctg.getName() + ";" + namePath;
//                ctg = ctg.getParent();
//            }
//            field.setCategoryPath(idPath);
//            field.setCategoryNamePath(namePath);
//        }
    }


    protected static void buildFieldCtgRel(List<MetaFieldCtgRel> fieldCtgRelList) {

        if (CollUtil.isEmpty(fieldCtgRelList)) {
            return;
        }

        for (MetaFieldCtgRel fieldCtgRel : fieldCtgRelList) {
            String mapKey = String.format("%s%s", fieldCtgRel.getFieldId(), fieldCtgRel.getCtgId());
            fieldCtgRelCacheTemp.put(mapKey, fieldCtgRel);
        }

    }

    /**
     * 获取字段的排序
     * 首先从字段与目录的关联关系获取排序
     * 查不到，再从字段本身排序获取
     * @param fieldId
     * @param ctgId
     * @return
     */
    public static Double getFieldSortId(String fieldId, String ctgId) {

        Double sortId = 0.0;

        String mapKey = String.format("%s%s", fieldId, ctgId);
        MetaFieldCtgRel metaFieldCtgRel = fieldCtgRelCache.get(mapKey);
        if (metaFieldCtgRel != null && metaFieldCtgRel.getSortId() != null) {
            return metaFieldCtgRel.getSortId();
        }

        MetaField metaField = fieldsCache.get(fieldId);
        if (metaField != null) {
            sortId = metaField.getShowOrder();
        }

        if (sortId == null) {
            sortId = -1.0;
        }

        return sortId;
    }

    /**
     * 获取给定表里所有关联关系
     * @param tableId
     * @return
     */
    public static List<MetaTableRelation> getRelations(String tableId){
        List<MetaTableRelation> relList = new ArrayList<MetaTableRelation>();
        if(tableId == null){
            return relList;
        }

        for(MetaTableRelation r : relationsCache){
            if(tableId.equals(r.getPrimaryTableId())){
                relList.add(r);
            }
            if(tableId.equals(r.getSubTableId())){
                relList.add(r);
            }
        }
        return relList;
    }

    /**
     * 获取主子表配置
     * @param tableId
     * @return
     */
    public static List<MetaTablePriSubCfg> getTablePriSubCfgByTableId(String tableId){
        return tablePriSubCfgCache.get(tableId);
    }

    /**
     * 获取字段维值配置
     * @param fieldCode
     * @return
     */
    public static Map<String, String> getFieldValueMapByFieldCode(String fieldCode) {
        if (StrUtil.isEmpty(fieldCode)) {
            return Collections.emptyMap();
        }
        List<MetaFieldValueMap> fieldItemMaps = fieldValueMapCache.get(fieldCode);
        if (CollUtil.isNotEmpty(fieldItemMaps)) {
            return fieldItemMaps.stream().collect(Collectors.toMap(MetaFieldValueMap::getFieldKey,
                    MetaFieldValueMap::getFieldValue, (x, y) -> x, () -> new LinkedHashMap<>(fieldItemMaps.size())));
        }
        return Collections.emptyMap();
    }

    public static void setFieldValueMapByFieldCode(String fieldCode, List<Map<String, String>> itemMap) {
        if (CollUtil.isEmpty(itemMap)) {
            return;
        }

        List<MetaFieldValueMap> items = itemMap.stream()
                .map(e -> {
                    Map.Entry<String, String> entry = e.entrySet().iterator().next();
                    return MetaFieldValueMap.of(fieldCode, entry.getKey(), entry.getValue(), null);
                }).collect(Collectors.toList());
        fieldValueMapCache.put(fieldCode, items);
    }


    /**
     * 获取2个表之间
     * @return
     */
    public static List<MetaTableRelation> getRelations(String tableId1, String tableId2){
        List<MetaTableRelation> relList = new ArrayList<>(4);
        for(MetaTableRelation r : relationsCache){
            if(r.getPrimaryTableId().equals(tableId1) && r.getSubTableId().equals(tableId2)){
                relList.add(r);
            }
            if(r.getPrimaryTableId().equals(tableId2) && r.getSubTableId().equals(tableId1)){
                relList.add(r);
            }
        }
        return relList;
    }

    /**
     * 获取所有关联的表
     * @param tableId
     * @return
     */
    public static List<MetaTable> getRelationTables(String tableId){
        List<MetaTable> relList = new ArrayList<MetaTable>();
        if(tableId == null){
            return relList;
        }
        relList = relationTablesCache.get(tableId);
        if(relList == null){
            relList = new ArrayList<>();
        }
        /*
        for(MetaTableRelation r : relationsCache){
            if(r.getPrimaryTableId().equals(tableId)){
                relList.add(getTable(r.getSubTableId()));
            }
            if(r.getSubTableId().equals(tableId)){
                relList.add(getTable(r.getPrimaryTableId()));
            }
        }
         */
        return relList;
    }

    /**
     * 通过id获取字段元信息
     * @param fieldId
     * @return
     */
    public static MetaField getField(String fieldId){
        if(fieldId == null) return null;
        return fieldsCache.get(fieldId);
    }

    /**
     * 通过id获取数据集元信息
     * @param datasetId
     * @return
     */
    public static MetaDataset getDataset(String datasetId){
        if(datasetId == null) return null;
        return datasetsCache.get(datasetId);
    }

    /**
     * 通过ids获取字段元信息List
     * @param fieldIds
     * @return
     */
    public static List<MetaField> getFieldList(List<String> fieldIds){
        List<MetaField> list = new ArrayList<>();
        for(String fieldId : fieldIds){
            list.add(fieldsCache.get(fieldId));
        }
        return list;
    }

    /**
     * 在对应表中通过字段编码查找字段元信息
     * @param tableId
     * @param fieldCode
     * @return
     */
    public static MetaField getFieldByCode(String tableId, String fieldCode){
        if(BIUtil.isEmpty(tableId) || BIUtil.isEmpty(fieldCode)){
            return null;
        }
        MetaField result = null;
        Map<String, MetaField> fields = tablesFieldsCache.get(tableId);
        if(fields != null){
            result = fields.get(fieldCode);
        }
        /*
        MetaTable table = getTable(tableId);
        if(table != null){
            List<MetaField> tableFields = table.getFields();
            if(tableFields != null){
                for(MetaField f : tableFields){
                    if(fieldCode.equalsIgnoreCase(f.getCode())){
                        result = f;
                        break;
                    }
                }
            }
        }
         */
        return result;
    }

    public static MetaField getFieldByName(String tableId, String fieldName){
        MetaField result = null;
        for(MetaField field : fieldsCache.values()){
            if( (tableId.equals(field.getFactTableId()) || tableId.equals(field.getDimTableId())) &&
                    field.getName().equalsIgnoreCase(fieldName)){
                result = field;
                break;
            }
        }
        return result;
    }

    public static MetaField getFieldByTitle(String fieldTitle) {
        MetaField result = null;
        for (MetaField field : fieldsCache.values()) {
            if ((field.getTitle().equalsIgnoreCase(fieldTitle))) {
                result = field;
                break;
            }
        }
        return result;
    }

    /**
     * 在对应表中通过字段编码查找字段元信息
     * @param fieldCode
     * @return
     */
    public static List<MetaField> getFieldByCode(String fieldCode){
        List<MetaField> result = new ArrayList<MetaField>();
        for(MetaField field : fieldsCache.values()){
            if(field.getCode().equalsIgnoreCase(fieldCode)){
                result.add(field);
            }
        }
        return result;
    }

    /**
     * 通过字段完整编码获取字段：字段全面=表schema.表名.字段编码
     * @param fieldFullCode
     * @return
     */
    public static MetaField getFieldByFullName(String fieldFullCode){
        if(BIUtil.isEmpty(fieldFullCode) || !fieldFullCode.contains(".")){
            return null;
        }
        String[] fieldInfos = fieldFullCode.split("\\.");
        if(fieldInfos.length != 3){
            return null;
        }
        String tableFullName = fieldInfos[0] + "." + fieldInfos[1];
        String fieldCode = fieldInfos[2];
        MetaTable table = getTableByFullName(tableFullName);
        if(table == null){
            return null;
        }
        MetaField field = getFieldByName(table.getId(), fieldCode);
        if(field == null){
            field = getFieldByCode(table.getId(), fieldCode);
        }
        return field;
    }

    /**
     * 字段是否存在表中
     * @param tableId
     * @param fieldName
     * @return
     */
    public static boolean exist(String tableId, String fieldName){
        MetaTable table = getTable(tableId);
        if(table == null){
            return false;
        }
        List<MetaField> tableFields = table.getFields();
        for(MetaField f : tableFields){
            if(f.getName().equalsIgnoreCase(fieldName)){
                return true;
            }
        }
        return false;
    }

    /**
     * 字段是否存在表中
     * @param tableId
     * @param fieldCode
     * @return
     */
    public static boolean existByCode(String tableId, String fieldCode){
         /*
        MetaTable table = getTable(tableId);
        if(table == null){
            return false;
        }
        List<MetaField> tableFields = table.getFields();
        for(MetaField f : tableFields){
            if(f.getCode().equalsIgnoreCase(fieldCode)){
                return true;
            }
        }
         */
        MetaField f = getFieldByCode(tableId, fieldCode);
        return f != null;
    }

    /**
     * 通过表id获取表元信息
     * @param tableId
     * @return
     */
    public static MetaTable getTable(String tableId){
        return tablesCache.get(tableId);
    }

    public static MetaTable getTableByName(String schema, String tableName) {
        String fullName = schema + "." + tableName;
        return getTableByFullName(fullName);
    }

    public static MetaTable getTableByFullName(String tableFullName) {
        Optional<MetaTable> mt = tablesCache.values().stream().filter(t->{
            return t.getFullName().equalsIgnoreCase(tableFullName);
        }).findFirst();
        if(!mt.isPresent()){
            return null;
        }
        return mt.get();
    }

    public static MetaTable getTableByField(String fieldId){
        MetaField field = getField(fieldId);
        if(field == null){
            return null;
        }
        //if(MetaFieldType.Dimension.getId().equals(field.getType())){
        if(Enabled.value(field.getIsMeasure())){
            return tablesCache.get(field.getFactTableId());
        }else{
            return tablesCache.get(field.getDimTableId());
        }
    }

    /**
     * 获取表的最大权限
     * @param tableId
     * @return
     */
    public static Double getMaxWeight(String tableId){
        Double maxWeight = (double)0;
        for(MetaTableRelation rel : relationsCache){
            if(rel.getPrimaryTableId().equals(tableId)){
                if(maxWeight < rel.getWeight()){
                    maxWeight = rel.getWeight() + 1;
                }
            }
            if(rel.getSubTableId().equals(tableId)){
                if(maxWeight < rel.getWeight()){
                    maxWeight = rel.getWeight();
                }
            }
        }
        return maxWeight;
    }

    /**
     * 获取字段层次信息
     * @param fieldId
     * @return
     */
    public static HierarchyInfo getHierarchy(String fieldId){
        return hierarchies.get(fieldId);
    }

    /**
     * 获取层次的叶子节点字段id
     * @param fieldId
     * @return
     */
    public static String getHierarchyLeafFieldId(String fieldId){
        HierarchyInfo info = getHierarchy(fieldId);
        if(info == null){
            return "-1";
        }
        List<String> levelIds = new ArrayList<String>();
        levelIds.add(info.getLevel1FieldId());
        levelIds.add(info.getLevel2FieldId());
        levelIds.add(info.getLevel3FieldId());
        levelIds.add(info.getLevel4FieldId());
        levelIds.add(info.getLevel5FieldId());
        Integer levelNum = info.getLevelNum();
        String leafFieldId = levelIds.get(levelNum - 1);
        return leafFieldId;
    }

    /**
     * 获取表所有字段元信息
     * @param tableId
     * @return
     */
    public static List<MetaField> getTableFields(String tableId){
        List<MetaField> fields = new ArrayList<MetaField>();
        /*
        Collection<MetaField> fieldCacheCollection = fieldsCache.values();
        for(MetaField field : fieldCacheCollection){
            if(tableId.equals(field.getDimTableId()) || tableId.equals(field.getFactTableId())){
                fields.add(field);
            }
        }
         */
        MetaTable table = tablesCache.get(tableId);
        if(table != null) {
            fields = table.getFields();
        }
        return fields;
    }

    public static Map<String, MetaField> getTableFieldMap(String tableId){
        Map<String, MetaField> fieldMap = tablesFieldsCache.get(tableId);
        if(fieldMap == null){
            fieldMap = new HashMap<>();
        }
    	return fieldMap;
    }

    /**
     * 通过表ctgIds获取目录元信息List
     * @param ctgIds
     * @return
     */
    public static List<MetaFieldCategory> getFieldCategoryList(Collection<String> ctgIds){
        List<MetaFieldCategory> result = new ArrayList<>();

        if(BIUtil.isEmpty(ctgIds)){
            return result;
        }

        for (String ctgId:ctgIds) {
            MetaFieldCategory metaFieldCategory = categories.get(ctgId);
            if (metaFieldCategory != null) {
                result.add(metaFieldCategory);
            }

        }
        return result;
    }


    /**
     * 获取虚拟目录所有上级目录
     * @param categoryId
     * @return
     */
    public static Set<String> getVirtualParentCategory(String categoryId){
        Set<String> categoryList = new HashSet<String>();
        if(StringUtils.isBlank(categoryId)){
            return categoryList;
        }

        String ctgId = categoryId;
        while(true){
            categoryList.add(ctgId);
            MetaVirtualCategory ctg = virtualCategories.get(ctgId);
            if(ctg != null && !"-1".equalsIgnoreCase(ctg.getParentId())&&ctg.getParentId().length()>3){
                ctgId = ctg.getParentId();
                continue;
            }else{
                break;
            }
        }

        return categoryList;
    }


    /**
     * 通过字段id集合获取code集合
     * @param fieldIds
     * @return
     */
    public static List<String> getFieldCodesByFieldIds(String fieldIds){

        if(BIUtil.isEmpty(fieldIds)){
            return new ArrayList<>();
        }
        List<String> codes = new ArrayList<>();

        List<String> list =  Arrays.asList(fieldIds.split(","));

        for (String fieldId:list) {

            MetaField metaField = getField(fieldId);
            if (metaField != null) {
                codes.add(metaField.getCode());
            }

        }
        return codes;
    }

    /**
     * 获取目录下(包含子目录)的所有字段
     * @param categoryId
     * @return
     */
    public static List<MetaField> getFieldByCategory(String categoryId){
        List<MetaField> fields = new ArrayList<MetaField>();
        MetaFieldCategory ctg = categories.get(categoryId);
        getFieldByCategory(fields, ctg);
        return fields;
    }

    private static void getFieldByCategory(List<MetaField> result, MetaFieldCategory ctg) {
        if (ctg != null) {
            result.addAll(ctg.getFields());
            for (MetaFieldCategory child : ctg.getChildren()) {
                getFieldByCategory(result, child);
            }
        }
    }

    public static List<String> getModuleCtgByCategory(String categoryId) {
        List<String> ctgIds = new ArrayList<>();
        MetaFieldCategory ctg = categories.get(categoryId);
        getModuleCtgByCategory(ctgIds, ctg);
        return ctgIds;
    }

    public static void getModuleCtgByCategory(List<String> ctgIds,MetaFieldCategory ctg) {
        if (ctg != null) {
            if (!ctg.isLeafModule()) {
                MetaFieldCategory parent = categories.get(ctg.getParentId());
                getModuleCtgByCategory(ctgIds, parent);
            } else {
                ctgIds.add(ctg.getId());
            }
        }
    }


    /**
     * 获取目录下(包含子目录)的所有字段
     * @param categoryId
     * @return
     */
    public static List<MetaField> getFieldByCategoryWithAuth(String categoryId, Map<String, Integer> inheritMap,Map<String,String> ctgAcls){
        List<MetaField> fields = new ArrayList<MetaField>();
        MetaFieldCategory ctg = categories.get(categoryId);
        getFieldByCategoryWithAuth(fields, ctg,inheritMap,ctgAcls);
        return fields;
    }

    private static void getFieldByCategoryWithAuth(List<MetaField> result, MetaFieldCategory ctg, Map<String, Integer> inheritMap,Map<String,String> ctgAcls) {
        if (ctg != null) {
            result.addAll(ctg.getFields());
            for (MetaFieldCategory child : ctg.getChildren()) {
                Integer isInherit= inheritMap.get(child.getId());
                if (isInherit == null) {
                    isInherit = Enabled.YES.getId();
                }
                if (Enabled.value(isInherit) || ctgAcls.containsKey(child.getId())) {
                    getFieldByCategoryWithAuth(result, child, inheritMap, ctgAcls);
                }

            }
        }

    }

    /**
     * 获取虚拟目录下(包含子目录)的所有字段
     * @param categoryId
     * @return
     */
    public static List<MetaField> getFieldByVirtualCategory(String categoryId){
        List<MetaField> fields = new ArrayList<MetaField>();
        MetaVirtualCategory ctg = virtualCategories.get(categoryId);
        getFieldByVirtualCategory(fields, ctg);
        return fields;
    }

    private static void getFieldByVirtualCategory(List<MetaField> result, MetaVirtualCategory ctg){
        if(ctg != null) {
            result.addAll(ctg.getFields());
            for(MetaVirtualCategory child : ctg.getChildren()){
                getFieldByVirtualCategory(result, child);
            }
        }
    }

    /**
     * 获取目录下全路径（带子目录）
     * @param path
     * @param categoryId
     */
    public void buildCategoryPath(Map<String, MetaFieldCategory> path, String categoryId){
        MetaFieldCategory category = categories.get(categoryId);
        if(category == null){
            return;
        }
        path.put(category.getId(), category);
        List<MetaFieldCategory> children = category.getChildren();
        if(children != null && !children.isEmpty()){
            for(MetaFieldCategory child : children){
                buildCategoryPath(path, child.getId());
            }
        }
    }

    /**
     * 清理临时缓存
     */
    public static void clearTemp() {
        if (fieldsCacheTemp != null) {
            fieldsCacheTemp.clear();
        }
        if (tablesCacheTemp != null) {
            tablesCacheTemp.clear();
        }
        if (fieldsDataAuthCfgTemp != null) {
            fieldsDataAuthCfgTemp.clear();
        }
        if (relationsCacheTemp != null) {
            relationsCacheTemp.clear();
        }
        if (hierarchiesTemp != null) {
            hierarchiesTemp.clear();
        }
        if (categoriesTemp != null) {
            categoriesTemp.clear();
        }
        if (categoryMaxWeightFieldsTemp != null) {
            categoryMaxWeightFieldsTemp.clear();
        }

        if (fieldCtgRelCacheTemp != null) {
            fieldCtgRelCacheTemp.clear();
        }
    }

    /**
     * 清理缓存
     */
    public static void clear(){
        if(fieldsCache != null){
            fieldsCache.clear();
        }
        if(tablesCache != null){
            tablesCache.clear();
        }
        if(fieldsDataAuthCfg != null) {
            fieldsDataAuthCfg.clear();
        }
        if(relationsCache != null){
            relationsCache.clear();
        }
        if(hierarchies != null){
            hierarchies.clear();
        }
        if(categories != null){
            categories.clear();
        }
        if(filterTreeFields != null){
            filterTreeFields.clear();
        }
        if(resultTreeFields != null){
            resultTreeFields.clear();
        }
        if(categoryMaxWeightFields != null){
            categoryMaxWeightFields.clear();
        }

        if(fieldCtgRelCache != null) {
            fieldCtgRelCache.clear();
        }

    }

    public static Map<String, MetaField> getFieldsCache() {
        return fieldsCache;
    }

    public static void setFieldsCache(Map<String, MetaField> fieldsCache) {
        SSDMetaCacheManager.fieldsCache = fieldsCache;
    }

    public static Map<String, MetaTable> getTablesCache() {
        return tablesCache;
    }

    public static void setTablesCache(Map<String, MetaTable> tablesCache) {
        SSDMetaCacheManager.tablesCache = tablesCache;
    }

    public static List<MetaTableRelation> getRelationsCache() {
        return relationsCache;
    }

    public static void setRelationsCache(List<MetaTableRelation> relationsCache) {
        SSDMetaCacheManager.relationsCache = relationsCache;
    }

    public static Map<String, HierarchyInfo> getHierarchies() {
        return hierarchies;
    }

    public static void setHierarchies(Map<String, HierarchyInfo> hierarchies) {
        SSDMetaCacheManager.hierarchies = hierarchies;
    }

    public static Map<String, MetaFieldCategory> getCategories() {
        return categories;
    }

    public static void setCategories(Map<String, MetaFieldCategory> categories) {
        SSDMetaCacheManager.categories = categories;
    }

    public static List<MetaField> getFilterTreeFields() {
        return filterTreeFields;
    }

    public static void setFilterTreeFields(List<MetaField> filterTreeFields) {
        SSDMetaCacheManager.filterTreeFields = filterTreeFields;
    }

    public static List<MetaField> getResultTreeFields() {
        return resultTreeFields;
    }

    public static void setResultTreeFields(List<MetaField> resultTreeFields) {
        SSDMetaCacheManager.resultTreeFields = resultTreeFields;
    }

    /**
     * 获取前台目录
     * @return
     */
    public static Map<String, MetaFieldCategory> getFrontCategories(){
        Map<String, MetaFieldCategory> collect = categories.entrySet().stream()
                .filter(map -> CategoryType.get(map.getValue().getType()) == CategoryType.Front)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        return collect;
    }

    public static MetaFieldCategory getCategoryById(String ctgId){
        return categories.get(ctgId);
    }

    /**
     * 根据目录ID递归拼接目录名称全路径（兼容老多维与 OLAP 目录）
     * @param ctgId 目录ID
     * @return 目录名称全路径，格式与 v_ssd_field_ctg.ctg_name_path 一致
     */
    public static String getCtgNamePath(String ctgId) {
        if (StrUtil.isBlank(ctgId)) {
            return "";
        }
        String resultPath = buildCtgNamePathSegment("", ctgId);
        if (StrUtil.isNotBlank(resultPath) && resultPath.endsWith("/")) {
            return resultPath.substring(0, resultPath.length() - 1);
        }
        return resultPath;
    }

    /**
     * 递归拼接目录名称路径片段
     * @param resultPath 已拼接路径
     * @param ctgId 当前目录ID
     * @return 目录名称路径
     */
    private static String buildCtgNamePathSegment(String resultPath, String ctgId) {
        if (StrUtil.isBlank(ctgId) || "-1".equalsIgnoreCase(ctgId)) {
            return resultPath;
        }
        MetaFieldCategory category = categories.get(ctgId);
        if (category == null) {
            return resultPath;
        }
        resultPath = category.getName() + "/" + resultPath;
        return buildCtgNamePathSegment(resultPath, category.getParentId());
    }

    /**
     * 按目录名称查找目录（工单记录里只有目录名称、没有目录id时的降级查找方式）
     * <p>
     * 目录名称不保证全局唯一，命中多个同名目录时视为有歧义，返回第一个。
     * </p>
     */
    public static MetaFieldCategory getCategoryByName(String ctgName){
        if (StrUtil.isBlank(ctgName)) {
            return null;
        }
        List<MetaFieldCategory> matched = categories.values().stream()
                .filter(category -> ctgName.equals(category.getName()))
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            System.out.printf("目录名称[%s]未匹配到目录%n", ctgName);
            return null;
        }
        if (matched.size() > 1) {
            System.out.printf("目录名称[%s]匹配到%s个目录，存在歧义，按已返回id为%s的记录%n", ctgName, matched.size(), matched.get(0).getId());
        }
        return matched.get(0);
    }

    /**
     * 获取字段所在模块下的目录id
     * 一个字段只能在一个模块，不可能同一个模块下同一个字段存在多个子目录
     * @param fieldId
     * @param targetModuleCtgId
     * @return
     */
    public static String getCategoryIdByFieldIdAndModuleCtgId(String fieldId, String targetModuleCtgId){
        MetaField metaField = getField(fieldId);
        if(metaField == null || BIUtil.isEmpty(targetModuleCtgId)){
            return null;
        }
        List<String> ctgIdList = metaField.getCategoryIdList();
        if(BIUtil.isEmpty(ctgIdList)){
            return null;
        }
        for(String ctgId : ctgIdList){
            String moduleCtgId = getModuleCtgIdByCategoryId(ctgId);
            if(targetModuleCtgId.equalsIgnoreCase(moduleCtgId)){
                return ctgId;
            }
        }
        return null;
    }

    /**
     * 获取后台目录
     * @return
     */
    public static Map<String, MetaFieldCategory> getBackCategories(){
        Map<String, MetaFieldCategory> collect = categories.entrySet().stream()
                .filter(map -> CategoryType.get(map.getValue().getType()) == CategoryType.Back)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        return collect;
    }

    /**
     * 获取全部的表的ETL任务
     * @return
     */
    public static Set<String> getTableEtlJobSet(){
        return tableEtlJobSet;
    }

    /**
     * 获取字段的数据权限列表（仅模块和维度）
     * @param fieldCode
     * @return
     */
    public static List<MetaFieldDataAuth> getDataAuth(String fieldCode) {
        return fieldsDataAuthCfg.get(fieldCode);
    }

    /**
     * 查询表是否配置了数据权限
     * @param tableId
     * @return
     */
    public static boolean isDataAuthTable(String tableId) {
        return isDataAuthTable(getTable(tableId));
    }

    /**
     * 查询表是否配置了数据权限
     * @param table
     * @return
     */
    public static boolean isDataAuthTable(MetaTable table) {
        List<MetaField> fields = getDataAuthFieldByTable(table);
        return BIUtil.isNotEmpty(fields);
    }

    /**
     * 获取有数据权限的字段
     * @param table
     * @return
     */
    public static List<MetaField> getDataAuthFieldByTable(MetaTable table) {
        List<MetaField> acl = new ArrayList<>();
        if(table == null) {
            return acl;
        }
        List<MetaField> fields = getTableFields(table.getId());
        if(BIUtil.isEmpty(fields)) {
            return acl;
        }
        acl = fields.stream().filter(f-> {return fieldsDataAuthCfg.containsKey(f.getCode());}).collect(Collectors.toList());
        return acl;
    }

    /**
     * 获取有数据权限的数据权限配置
     * @param table
     * @return
     */
    public static List<MetaFieldDataAuth> getMetaDataAuthFieldByTable(MetaTable table) {
        List<MetaFieldDataAuth> acl = new ArrayList<>();
        if(table == null) {
            return acl;
        }
        List<MetaField> fields = getTableFields(table.getId());
        if(BIUtil.isEmpty(fields)) {
            return acl;
        }
        for(MetaField mf : fields){
            acl.addAll(getMetaFieldDataAuthCfg(mf.getCode()));
        }
        return acl;
    }

    /**
     * 获取字段的数据权限配置
     * @param fieldCode
     * @return
     */
    public static List<MetaFieldDataAuth> getMetaFieldDataAuthCfg(String fieldCode) {
        return fieldsDataAuthCfg.get(fieldCode);
    }

    /**
     * 获取所有数据管控的维度配置
     * @return
     */
    public static List<MetaFieldDataAuth> getAllMetaFieldDataAuthCfg() {
        List<MetaFieldDataAuth> allAuthList = new ArrayList<>();
        fieldsDataAuthCfg.values().forEach(l -> {allAuthList.addAll(l);});
        return allAuthList;
    }

    /**
     * 获取字段是否已配置权限
     * @param fieldCode
     * @return
     */
    public static boolean hasDataAuthCfg(String fieldCode) {
        return BIUtil.isNotEmpty(fieldsDataAuthCfg.get(fieldCode));
    }

    /**
     * 获取字段值排序列表
     * @param fieldCode
     * @return
     */
    public static List<MetaFieldValueSort> getFieldValueSort(String fieldCode){
        List<MetaFieldValueSort> valueSorts = fieldValueSortList.stream().filter(f->f.getFieldCode().equalsIgnoreCase(fieldCode)).collect(Collectors.toList());
        Collections.sort(valueSorts);
        return valueSorts;
    }

    /**
     * 构建前台目录权重最大字段列表
     */
    protected static void buildCategoryMaxWeightFields(){
        categoryMaxWeightFieldsTemp.clear();
        for(MetaFieldCategory category : categories.values()){
            List<MetaField> ctgFields = category.getFields();
            ctgFields = getMaxWeightFields(ctgFields);
            categoryMaxWeightFieldsTemp.put(category.getId(), ctgFields);
        }
    }

    /**
     * 获取code相同字段权重最大的字段列表
     * @param fields
     * @return
     */
    protected static List<MetaField> getMaxWeightFields(List<MetaField> fields) {
        if (BIUtil.isEmpty(fields)) {
            return fields;
        }

        // 先排序：确保权重一样时每次返回的字段一致
        Collections.sort(fields, new Comparator<MetaField>(){
            @Override
            public int compare(MetaField o1, MetaField o2) {
                int flag = 0;
                if(o1.getShowOrder() <  o2.getShowOrder()){
                    flag = 1;
                }
                if(o1.getShowOrder() >  o2.getShowOrder()){
                    flag = -1;
                }

                if(flag == 0){
                    flag = o1.getId().compareTo(o2.getId());
                }
                return flag;
            }
        });

        Map<String, MetaField> maxWeightFields = new HashMap<>();
        for(MetaField m : fields){
            MetaField copy = m.clone(); // ！！！注：此处需要克隆，因为修改了元字段相同字段code
            copy.setSameCodeFieldList(new ArrayList<>());
            MetaField maxWeightField = maxWeightFields.get(copy.getCode());
            if (maxWeightField == null) {
                maxWeightFields.put(copy.getCode(), copy);
            } else {
                if (maxWeightField.getWeight() < copy.getWeight()) {
                    copy.getSameCodeFieldList().add(maxWeightField);
                    copy.getSameCodeFieldList().addAll(maxWeightField.getSameCodeFieldList());
                    maxWeightField.setSameCodeFieldList(new ArrayList<>());
                    // 若存在相同code，则取权重最大
                    maxWeightFields.put(copy.getCode(), copy);
                } else {
                    maxWeightField.getSameCodeFieldList().add(copy);
                }
            }
        }
        /*
        fields.stream().forEach(m -> {
            if(m.getId().equalsIgnoreCase("e73a907c0b4e479a8a8f4e71b37b501e")){
                System.out.println("");
            }
            m.setSameCodeFieldList(new ArrayList<>());
            MetaField maxWeightField = maxWeightFields.get(m.getCode());
            if (maxWeightField == null) {
                maxWeightFields.put(m.getCode(), m);
            } else {
                if (maxWeightField.getWeight() < m.getWeight()) {
                    m.getSameCodeFieldList().add(maxWeightField);
                    m.getSameCodeFieldList().addAll(maxWeightField.getSameCodeFieldList());
                    maxWeightField.setSameCodeFieldList(new ArrayList<>());
                    // 若存在相同code，则取权重最大
                    maxWeightFields.put(m.getCode(), m);
                } else {
                    maxWeightField.getSameCodeFieldList().add(m);
                }
            }
        });
         */
        List<MetaField> result = maxWeightFields.values().stream().sorted(Comparator.comparingDouble(MetaField::getShowOrder)).collect(Collectors.toList());
        return result;
    }

    /**
     * 获取前台目录下最大权重的字段列表
     * @param categoryId
     * @return
     */
    public static List<MetaField> getCategoryMaxWeightFields(String categoryId){
        List<MetaField> fields = categoryMaxWeightFields.get(categoryId);
        if(fields == null){
            fields = new ArrayList<>();
        }
        return fields;
    }

    /**
     * 嵌套计算字段处理：将嵌套表达式转为非嵌套表达式
     * 注：此方法只支持字段code引用
     * @param calcField
     * @return
     */
    public static void setCalcFieldExpressionAndAtomField(MetaField calcField, Map<String, Integer> recursionIndexes){
        if(recursionIndexes.get(calcField.getId()) == null){
            recursionIndexes.put(calcField.getId(), 1);
        }else {
            recursionIndexes.put(calcField.getId(), recursionIndexes.get(calcField.getId()) + 1);
        }
        // 递归不能超过5层
        if(recursionIndexes.get(calcField.getId()) >= 5){
            System.out.println("递归异常字段：" + calcField);
            return;
        }
        String expression = calcField.getAggExpression();
        if(BIUtil.isEmpty(expression)) {
            return ;
        }
        Set<String> refCodes = FieldUtil.fetchFieldRefKeys(expression);
        if(BIUtil.isEmpty(refCodes)){
            return ;
        }

        Map<String, MetaField> refFields = new HashMap<>();
        for(String refCode : refCodes){
            MetaField refField = getRefFieldByRefKey(calcField.getTableId(), refCode);
            if(refField != null) {
                refFields.put(refCode, refField);
            }
        }

        // 若是复合计算表达式，则先替换引用原子字段的表达式
        if(SqlExpressionUtil.isCompound(expression)){
            expression = replaceAtomFieldRef(expression, refFields);
            calcField.setAggExpression(expression);
        }

        for(String refCode : refCodes){
            if(BIUtil.isEmpty(refCode)){
                continue;
            }
            // 优先从和计算字段相同的所属表中获取
            MetaField refField = refFields.get(refCode);
            /*
            if(refCode.contains(".")){
                refField = SSDMetaCacheManager.getFieldByFullName(refCode);
            }else {
                refField = SSDMetaCacheManager.getFieldByCode(calcField.getTableId(), refCode);
            }

            //兼容自定义聚合表达式使用field_id的场景
            if(refField == null){
                refField = SSDMetaCacheManager.getField(refCode);
            }
             */

            // 自引用不处理
            if(refField == null || refField.getId().equals(calcField.getId())){
                continue;
            }
            if(FieldUtil.isCalcField(refField)){
                expression = expression.replace("[" + refCode + "]", "(" + refField.getAggExpression() + ")");
                calcField.setAggExpression(expression);
                setCalcFieldExpressionAndAtomField(calcField, recursionIndexes);
            }else {
                calcField.addCalcAtomField(refField);
            }
        }
        return ;
    }

    /**
     * 替换表达式中的原子字段引用编码或id为聚合表达式
     * 注意：原子引用已参与了聚合，则不替换，避免重复替换
     * 场景1：复合表达式=[a]/[b],其中b是原子字段，则替换为[a]/(sum([b])
     * 场景2：复合表达式=[a]/sum([b]),其中b是原子字段，则不替换
     * 处理逻辑：
     * 1、对表达式进行聚合拆分
     * 2、拆分后再判断引用的字段是否已在聚合表达式中
     * @param expression 复合计算表达式
     * @param refFields 引用字段集合 key=refKey, value=MetaField
     * @return
     */
    protected static String replaceAtomFieldRef(String expression, Map<String, MetaField> refFields){
        if(!"true".equalsIgnoreCase(SC.v("meta.field.calc.expression.replace.atom.field", "true"))){
            return expression;
        }
        List<AggregatorItem> aggItems = SqlExpressionUtil.parseAggregators(expression);
        String newExpr = expression;
        // key=id, value=field
        Map<String, MetaField> refAtomFields = new HashMap<>();
        for(MetaField refField : refFields.values()){
            if(!FieldUtil.isCalcField(refField)){
                refAtomFields.put(refField.getId(), refField);
            }
        }

        for(AggregatorItem item : aggItems){
            String aggContent = item.getContent();
            if(BIUtil.isEmpty(aggContent)){
                continue;
            }
            Set<String> aggContentRefKeys = FieldUtil.fetchFieldRefKeys(aggContent);
            for(String aggContentRefKey : aggContentRefKeys){
                MetaField aggContentRefField = refFields.get(aggContentRefKey);
                if(aggContentRefField != null){
                    refAtomFields.remove(aggContentRefField.getId());
                }
            }
        }
        if(BIUtil.isEmpty(refAtomFields)){
            return newExpr;
        }
        for(String refKey : refFields.keySet()){
            MetaField refField = refFields.get(refKey);
            if(refField == null) {
                continue;
            }
            MetaField refAtomField = refAtomFields.get(refField.getId());
            if(refAtomField == null){
                continue;
            }
            /*
            String distinctString = AggExpressionType.get(refField.getAggExpression()) == AggExpressionType.Count_Distinct ? "distinct " : "";
            String aggExpr = String.format("%s(%s[%s])", refField.getAggExpression(), distinctString, refKey);
             */
            String aggExpr = AggExpressionType.get(refField.getAggExpression()).getExpression(String.format("[%s]", refKey));
            newExpr = newExpr.replace("[" + refKey + "]", String.format("(%s)", aggExpr));
        }

        return newExpr;
    }

    /**
     * 设置计算字段的原子字段
     * @param calcField
     */
    protected static void setCalcFieldAtomFields(MetaField calcField) {

        String expression = calcField.getAggExpression();
        if (StrUtil.isEmpty(expression)) {
            return;
        }

        Set<String> refIds = FieldUtil.fetchFieldRefKeys(expression);
        if (BIUtil.isEmpty(refIds)) {
            return;
        }

        for (String refId : refIds) {
            MetaField metaField = getField(refId);
            if (metaField == null) {
                continue;
            }

            calcField.addCalcAtomField(metaField);
        }
    }

    /**
     * 通过引用key获取引用字段
     * 引用key的格式包括：[field_code]、[table_fullName.field_code]、[table_fullName.field_name]、【field_id]
     * @param tableId
     * @param refKey
     * @return
     */
    protected static MetaField getRefFieldByRefKey(String tableId, String refKey){
        MetaField refField = null;
        if(refKey.contains(".")){
            refField = SSDMetaCacheManager.getFieldByFullName(refKey);
        }else {
            refField = SSDMetaCacheManager.getFieldByCode(tableId, refKey);
        }

        //兼容自定义聚合表达式使用field_id的场景
        if(refField == null){
            refField = SSDMetaCacheManager.getField(refKey);
        }
        return refField;
    }

    /**
     * 嵌套计算字段处理：将嵌套表达式转为非嵌套表达式
     * 注：此方法只支持字段code引用
     * @param calcField
     * @return
     */
    public static void setCalcFieldExpressionAndAtomFieldBackup(MetaField calcField, Map<String, Integer> recursionIndexes){
        if(recursionIndexes.get(calcField.getId()) == null){
            recursionIndexes.put(calcField.getId(), 1);
        }else {
            recursionIndexes.put(calcField.getId(), recursionIndexes.get(calcField.getId()) + 1);
        }
        // 递归不能超过5层
        if(recursionIndexes.get(calcField.getId()) >= 5){
            System.out.println("递归异常字段：" + calcField);
            return;
        }
        String expression = calcField.getAggExpression();
        if(BIUtil.isEmpty(expression)) {
            return ;
        }
        Set<String> refCodes = FieldUtil.fetchFieldRefKeys(expression);
        if(BIUtil.isEmpty(refCodes)){
            return ;
        }

        if(calcField.getCode().equals("platform_international_quote_rate")){
            System.out.println();
        }

        for(String refCode : refCodes){
            if(BIUtil.isEmpty(refCode)){
                continue;
            }
            // 优先从和计算字段相同的所属表中获取
            MetaField refField = null;
            if(refCode.contains(".")){
                refField = SSDMetaCacheManager.getFieldByFullName(refCode);
            }else {
                refField = SSDMetaCacheManager.getFieldByCode(calcField.getTableId(), refCode);
            }

            //兼容自定义聚合表达式使用field_id的场景
            if(refField == null){
                refField = SSDMetaCacheManager.getField(refCode);
            }

            // 自引用不处理
            if(refField == null || refField.getId().equals(calcField.getId())){
                continue;
            }
            if(FieldUtil.isCalcField(refField)){
                expression = expression.replace("[" + refCode + "]", "(" + refField.getAggExpression() + ")");
                calcField.setAggExpression(expression);
                setCalcFieldExpressionAndAtomFieldBackup(calcField, recursionIndexes);
            }else {
                calcField.addCalcAtomField(refField);
            }
        }
        return ;
    }

    /**
     * 获取目录的上级可申请id
     * @param categoryId
     * @return
     */
    public static String getModuleCtgIdByCategoryId(String categoryId) {

        MetaFieldCategory ctg = categories.get(categoryId);

        while (ctg != null) {

            if (ctg.isLeafModule()) {
                return ctg.getId();
            }

            ctg = categories.get(ctg.getParentId());
        }

        return null;
    }

    /**
     * 获取字段的负责人
     * @param metaField
     * @return
     */
    public static String getOwnerByMetaField(MetaField metaField) {

        String owner = "";
        MetaTable metaTable = SSDMetaCacheManager.getTable(metaField.getTableId());
        if (metaTable != null) {
            owner = BIUtil.isNotEmpty(metaTable.getTableOwner()) ? metaTable.getTableOwner() : metaTable.getCreatedBy();
        }
        if (BIUtil.isEmpty(owner)) {
            owner = BIUtil.isNotEmpty(metaField.getUpdatedBy()) ? metaField.getUpdatedBy() : metaField.getCreatedBy();
        }

        return owner;
    }

    /**
     * 获取所有事实表
     * @return
     */
    public static Map<String, MetaTable> getAllFactTables(){
        return tablesCache.values().stream().filter(t->Enabled.isTrue(t.getIsFactTable()))
                .collect(Collectors.toMap(MetaTable::getId, MetaTable->MetaTable, (t1,t2)->t1));
    }

    /**
     * 获取所有事实表
     * @return
     */
    public static Map<String, MetaTable> getAllFactTables(String datasetId){
        if(BIUtil.isEmpty(datasetId)){
            return getAllFactTables();
        }
        Map<String, MetaTable> allFactTables = new HashMap<>();
        MetaDatasetTable dsTable = datasetTablesCache.get(datasetId);
        if(dsTable != null){
            allFactTables = dsTable.getFactTables();
        }
        if(allFactTables == null){
            allFactTables = new HashMap<>();
        }
        /*
        if(BIUtil.isEmpty(tables)){
            return allFactTables;
        }
        for(String tableId : tables.keySet()){
            MetaTable table = tables.get(tableId);
            if(Enabled.isTrue(table.getIsFactTable())){
                allFactTables.put(tableId, table);
            }
        }
         */
        return allFactTables;
    }

    public static Map<String, MetaTable> getFactTablesByQueryField(String datasetId, Collection<QueryField> metricFields) {
        Map<String, MetaTable> allFactTables = new HashMap<>();
        MetaDatasetTable dsTable = datasetTablesCache.get(datasetId);
        if (dsTable != null) {
            allFactTables = dsTable.getFactTables();
        }
        if (allFactTables == null) {
            allFactTables = new HashMap<>();
        }

        List<String> subTableIds = tablePriSubCfgCache.values().stream().flatMap(Collection::stream)
                .map(MetaTablePriSubCfg::getSubTableId).collect(Collectors.toList());
        Set<String> queryMetricCodes = metricFields.stream().map(v -> v.getMeta().getKpiNo()).collect(Collectors.toSet());
        List<MetaField> fields = new ArrayList<>();
        for (MetaTable table : allFactTables.values()) {
            if (Enabled.isTrue(table.getIsFactTable())) {
                String tableId = table.getId();
                // 子表不参入选表
                if (subTableIds.contains(tableId)) {
                    continue;
                }

                // 二次计算指标不参入选表
                if (tableId.contains("_")) {
                    continue;
                }

                Map<String, MetaField> fieldMap = tablesFieldsCache.getOrDefault(tableId, Collections.emptyMap());
                for (String fieldCode : queryMetricCodes) {
                    MetaField field = fieldMap.get(fieldCode);
                    if (field != null) {
                        fields.add(field);
                    }
                }
            }
        }

        fields.sort((x, y) -> -1 * x.getWeight().compareTo(y.getWeight()));
        Map<String, MetaTable> factTables = new LinkedHashMap<>();
        for (MetaField field : fields) {
            String tableId = field.getFactTableId();
            MetaTable table = allFactTables.get(tableId);
            if (table != null) {
                factTables.put(tableId, table);
            }
        }

        return factTables;
    }

    public static Map<String, MetaTable> getAllFactTables(String datasetId, Set<String> moduleIds){
        if(BIUtil.isEmpty(datasetId)){
            return getAllFactTables();
        }
        Map<String, MetaTable> allFactTables = new HashMap<>();
        MetaDatasetTable dsTable = datasetTablesCache.get(datasetId);
        if(dsTable != null){
            allFactTables = dsTable.getModuleFactTables(moduleIds);
        }
        if(allFactTables == null){
            allFactTables = new HashMap<>();
        }
        return allFactTables;
    }

    /**
     * 获取所有维度表
     * @return
     */
    public static Map<String, MetaTable> getAllDimTables(){
        return getAllDimTables(false);
    }

    public static Map<String, MetaTable> getAllDimTables(boolean isOnlyJoinFact){
        Map<String, MetaTable> tables = tablesCache.values().stream()
                                .filter(t->Enabled.isFalse(t.getIsFactTable()))
                                .collect(Collectors.toMap(MetaTable::getId, MetaTable->MetaTable, (t1,t2)->t1));
        if(!isOnlyJoinFact){
            return tables;
        }

        Map<String, MetaTable> hasRelationTables = new HashMap<>();
        for(MetaTableRelation rel : relationsCache){
            MetaTable metaTable = tables.get(rel.getSubTableId());
            if(metaTable == null){
                metaTable = tables.get(rel.getPrimaryTableId());
            }
            if(metaTable != null){
                hasRelationTables.put(metaTable.getId(), metaTable);
            }
        }
        return hasRelationTables;
    }

    /**
     * 追加新多维数据
     * @param list
     * @param sqlId
     * @param param
     * @param dataSourceType
     * @param <T>
     */
    public  <T> List<T> appendMgpData(List<T> list,String sqlId,Object param,DataSourceType dataSourceType) {

        //判断是否追加资产管理平台数据
        boolean isAppendMgpData = "true".equalsIgnoreCase(SC.v("ssm.append.mgp.data.enable", "true"));
        if (!isAppendMgpData) {
            return list;
        }

        if (list == null) {
            list = new ArrayList<>();
        }

        try {
            BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
            List<T> mgpData = (List<T>) dao.queryObjectList(sqlId, param, dataSourceType);
            if (CollUtil.isNotEmpty(mgpData)) {

                //设置数据来源为新多维
                for (T t : mgpData) {
                    Field f = t.getClass().getDeclaredField("dataEnv");
                    f.setAccessible(true);
                    f.set(t, DataEnv.NEW_MGP.getCode());
                }

                list.addAll(mgpData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void buildMetaCache(MetaCache metaCache) {

        List<MetaField> metaFieldList = new ArrayList<>();
        //字段
        for (MetaField metaField : fieldsCache.values()) {
            DataEnv dataEnv = DataEnv.get(metaField.getDataEnv());
            if (DataEnv.NEW_MGP == dataEnv) {
                continue;
            }

            //扩展字段不处理
            if(StrUtil.isNotEmpty(metaField.getExtendSrcId())){
                continue;
            }

            MetaField newMetaField = metaField.clone();
            newMetaField.setExtendFields(new ArrayList<>());
            newMetaField.setSameCodeFieldList(new ArrayList<>());
            newMetaField.setCalcAtomFields(new HashSet<>());

            metaFieldList.add(newMetaField);
        }

        metaCache.getMetaFieldList().addAll(metaFieldList);

        //表依赖
        List<MetaTableEtlJob> metaTableEtlJobList = new ArrayList<>();
        for (MetaTableEtlJob metaTableEtlJob : tableEtljobCache.values()) {
            DataEnv dataEnv = DataEnv.get(metaTableEtlJob.getDataEnv());
            if (DataEnv.NEW_MGP == dataEnv) {
                continue;
            }

            metaTableEtlJobList.add(metaTableEtlJob);
        }
        metaCache.getMetaTableEtlJobList().addAll(metaTableEtlJobList);

        //表
        List<MetaTable> tablesList = new ArrayList<>();
        for (MetaTable metaTable : tablesCache.values()) {
            DataEnv dataEnv = DataEnv.get(metaTable.getDataEnv());
            if (DataEnv.NEW_MGP == dataEnv) {
                continue;
            }

            MetaTable newMetaTable = metaTable.clone();
            newMetaTable.setFields(new ArrayList<>());
            newMetaTable.setEtlJobs(new ArrayList<>());
            tablesList.add(newMetaTable);
        }
        metaCache.getTablesList().addAll(tablesList);

        //字段数据权限
        List<MetaFieldDataAuth> fieldDataAuthList = new ArrayList<>();
        for (String fieldCode : fieldsDataAuthCfg.keySet()) {

            List<MetaFieldDataAuth> authList = fieldsDataAuthCfg.get(fieldCode);
            if (CollUtil.isEmpty(authList)) {
                continue;
            }

            for (MetaFieldDataAuth metaFieldDataAuth : authList) {
                DataEnv dataEnv = DataEnv.get(metaFieldDataAuth.getDataEnv());
                if (DataEnv.NEW_MGP == dataEnv) {
                    continue;
                }
                fieldDataAuthList.add(metaFieldDataAuth);
            }
        }
        metaCache.getFieldDataAuthList().addAll(fieldDataAuthList);

        //表关联
        List<MetaTableRelation> tableRelationList = new ArrayList<>();
        for (MetaTableRelation metaTableRelation : relationsCache) {
            DataEnv dataEnv = DataEnv.get(metaTableRelation.getDataEnv());
            if (DataEnv.NEW_MGP == dataEnv) {
                continue;
            }

            tableRelationList.add(metaTableRelation);
        }
        metaCache.getTableRelationList().addAll(tableRelationList);

        //主子表配置
        List<MetaTablePriSubCfg> tablePriSubCfgList = new ArrayList<>();
        for (String priTableId : tablePriSubCfgCache.keySet()) {
            List<MetaTablePriSubCfg> priSubCfgs = tablePriSubCfgCache.get(priTableId);
            if (CollUtil.isEmpty(priSubCfgs)) {
                continue;
            }

            for(MetaTablePriSubCfg metaTablePriSubCfg : priSubCfgs){
                DataEnv dataEnv = DataEnv.get(metaTablePriSubCfg.getDataEnv());
                if (DataEnv.NEW_MGP == dataEnv) {
                    continue;
                }

                tablePriSubCfgList.add(metaTablePriSubCfg);
            }
        }
        metaCache.getTablePriSubCfgList().addAll(tablePriSubCfgList);

        //字段维值配置
        List<MetaFieldValueMap> fieldItemMaps = new ArrayList<>();
        for (String fieldCode : fieldValueMapCache.keySet()) {
            List<MetaFieldValueMap> itemMap = fieldValueMapCache.get(fieldCode);
            if (CollUtil.isEmpty(itemMap)) {
                continue;
            }

            for (MetaFieldValueMap item : itemMap) {
                DataEnv dataEnv = DataEnv.get(item.getDataEnv());
                if (DataEnv.NEW_MGP == dataEnv) {
                    continue;
                }

                fieldItemMaps.add(item);
            }
        }
        metaCache.getFieldItemMaps().addAll(fieldItemMaps);

        //目录
        List<MetaFieldCategory> categoryList = new ArrayList<>();
        for (MetaFieldCategory metaFieldCategory : categories.values()) {
            DataEnv dataEnv = DataEnv.get(metaFieldCategory.getDataEnv());
            if (DataEnv.NEW_MGP == dataEnv) {
                continue;
            }

            MetaFieldCategory newMetaFieldCategory = metaFieldCategory.clone();
            newMetaFieldCategory.setChildren(new ArrayList<>());
            newMetaFieldCategory.setFields(new ArrayList<>());
            newMetaFieldCategory.setCtgDataAuthList(new ArrayList<>());
            newMetaFieldCategory.setAuthMap(new HashMap<>());
            newMetaFieldCategory.setParent(null);
            categoryList.add(newMetaFieldCategory);
        }
        metaCache.getCategoryList().addAll(categoryList);
    }

    /**
     * 刷新缓存
     */
    public void flushCacheByType(CacheFlushType cacheFlushType,Map<String, ?> initParams) {

        try {
            FlushCacheExecutor flushCacheExecutor = FlushCacheExecutorFactory.getFlushCacheEexcutor(cacheFlushType, initParams);
            flushCacheExecutor.execute();
        } catch (Exception e) {
            System.out.println("刷新缓存异常:" + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * 构建字段的安全等级
     */
    public void buildFieldSensitiveLevel(){
        boolean flag = "true".equalsIgnoreCase(SC.v("set.field.sensitive.level.by.datastudio.enable" , "true"));
        if(!flag){
            return;
        }
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

        // 底表打标
        List<MetaFieldSensitiveLevel> sensitiveLevelList = (List<MetaFieldSensitiveLevel>) dao.queryObjectList("ssm.cache.queryAllFieldSensitiveLevel", null, DataSourceType.Default);
        sensitiveLevelList = BIUtil.isEmpty(sensitiveLevelList) ?  new ArrayList<>() : sensitiveLevelList;

        Map<String, MetaFieldSensitiveLevel> sensitiveLevelMap = new HashMap<>(100);
        sensitiveLevelList.stream().forEach(f -> sensitiveLevelMap.put(f.getDbName() + "." + f.getTableName() + "." + f.getFieldName(), f));

        // 核心字段
        List<CoreMetaField> coreMetaFieldList = (List<CoreMetaField>) dao.queryObjectList("ssm.cache.queryAllCoreField", null, DataSourceType.Default);
        coreMetaFieldList =  BIUtil.isEmpty(coreMetaFieldList) ?  new ArrayList<>() : coreMetaFieldList;
        Map<String, CoreMetaField> coreFieldMap = new HashMap<>(32);
        coreMetaFieldList.stream().forEach(f -> coreFieldMap.put(f.getFieldCode(), f));

        // 核心mgp打标字段
        Map<String, CoreMetaField> coreOlapFieldMap = new HashMap<>(32);
        if ("true".equalsIgnoreCase(SC.v("set.field.sensitive.level.by.mgp.enable" , "true"))) {
            List<CoreMetaField> coreOlaoMetaFieldList = dao.queryObjectList("ssm.cache.queryAllOlapCoreField", null, CoreMetaField.class);
            coreOlaoMetaFieldList.forEach(f -> coreOlapFieldMap.put(f.getFieldCode(), f));
        }

        for(MetaField f : fieldsCache.values()){
            CoreMetaField coreMetaField = coreFieldMap.getOrDefault(f.getCode(), coreFieldMap.get(f.getKpiNo()));
            // 优先判断核心字段
            if(coreMetaField != null){
                f.setSensitiveLevel(DataSensitiveLevel.get(coreMetaField.getSensitiveLevel()).getCode());
                f.setSensitiveDataType(coreMetaField.getCoreType());
                continue;
            }

            CoreMetaField coreOlapMetaField = coreOlapFieldMap.getOrDefault(f.getCode(), coreOlapFieldMap.get(f.getKpiNo()));
            // 判断mgp核心打标字段
            if(coreOlapMetaField != null) {
                f.setSensitiveLevel(DataSensitiveLevel.get(coreOlapMetaField.getSensitiveLevel()).getCode());
                f.setSensitiveDataType(coreOlapMetaField.getCoreType());
                continue;
            }

            MetaTable table = tablesCache.get(f.getTableId());
            if(table == null){
                continue;
            }
            // 优先通过schema + tableName
            String fieldFullName = table.getFullName() + "." + f.getName();
            MetaFieldSensitiveLevel sensitiveLevel = sensitiveLevelMap.get(fieldFullName);
            if(sensitiveLevel == null){
                // 再通过表的原始表名获取，兼容集团v1的视图
                fieldFullName = table.getOrigTableName() + "." + f.getName();
                sensitiveLevel = sensitiveLevelMap.get(fieldFullName);
            }
            if(sensitiveLevel == null){
                continue;
            }
            DataSensitiveLevel level = DataSensitiveLevel.get(sensitiveLevel.getSensitiveLevel());
            f.setSensitiveLevel(level.getCode());
            //f.setIsSensitive("c4".equalsIgnoreCase(sensitiveLevel.getSensitiveLevel()) ? Enabled.YES.getId() : Enabled.NO.getId());
            f.setIsSensitive(level == DataSensitiveLevel.C4 ? Enabled.YES.getId() : Enabled.NO.getId());
            f.setSensitiveDataType(sensitiveLevel.getSensitiveCtg1Name() + "/" + sensitiveLevel.getSensitiveCtg3Name());
        }
    }

    /**
     * 自底向上递归聚合目录敏感等级：取自身字段及继承子目录中的最高等级
     */
    private void buildCategorySensitiveLevel() {
        if (CollUtil.isEmpty(categories)) {
            return;
        }
        Map<String, Integer> inheritMap = ((FieldCtgService) SpringContextUtil.getBean("fieldCtgService")).queryAllInherit();
        List<MetaFieldCategory> rootCategories = categories.values().stream()
                .filter(category -> category.getParent() == null)
                .collect(Collectors.toList());
        for (MetaFieldCategory rootCategory : rootCategories) {
            aggregateCategorySensitiveLevel(rootCategory, inheritMap);
        }
    }

    /**
     * 递归聚合单个目录及其子孙的敏感等级，并写回当前节点
     * <p>
     * 子目录始终递归计算并写回自身 sensitiveLevel；仅当该子目录相对父目录标记为「继承」时，
     * 才将其聚合等级参与父目录 maxLevel 比较。
     * </p>
     */
    private DataSensitiveLevel aggregateCategorySensitiveLevel(MetaFieldCategory category,
                                                             Map<String, Integer> inheritMap) {
        DataSensitiveLevel maxLevel = DataSensitiveLevel.C1;
        if (CollUtil.isNotEmpty(category.getFields())) {
            for (MetaField field : category.getFields()) {
                DataSensitiveLevel fieldLevel = DataSensitiveLevel.get(field.getSensitiveLevel());
                if (fieldLevel.getLevel() > maxLevel.getLevel()) {
                    maxLevel = fieldLevel;
                }
            }
        }
        if (CollUtil.isNotEmpty(category.getChildren())) {
            for (MetaFieldCategory child : category.getChildren()) {
                DataSensitiveLevel childLevel = aggregateCategorySensitiveLevel(child, inheritMap);
                // 仅继承子目录的敏感等级参与父目录聚合，缺省视为继承
                if (Enabled.value(inheritMap.getOrDefault(child.getId(), Enabled.YES.getId()))
                        && childLevel.getLevel() > maxLevel.getLevel()) {
                    maxLevel = childLevel;
                }
            }
        }
        category.setSensitiveLevel(maxLevel.getCode());
        return maxLevel;
    }

    /**
     * 构建字段的实时表缓存
     * key = 字段编码 value = 实时表集合
     */
    public void buildFieldRtTableCache() {

        Map<String,Map<String,List<RtTableInfo>>> fieldRtTableCacheTemp = new HashMap<>(100);
        for (String datasetId : datasetsCache.keySet()) {
            MetaDataset dataset = datasetsCache.get(datasetId);
            DataTypeEnum dataType = DataTypeEnum.codeOf(dataset.getDatasetType());

            //非实时数据集不处理
            if (DataTypeEnum.REAL_TIME != dataType) {
                continue;
            }

            Map<String,List<RtTableInfo>> rtTableInfoMap = new HashMap<>();

            //获取数据集下面所有的事实表
            MetaDatasetTable datasetTables = datasetTablesCache.get(datasetId);
            if(datasetTables == null) {
                continue;
            }
            for (String tableId : datasetTables.getFactTables().keySet()) {
                MetaTable factTable = datasetTables.getFactTables().get(tableId);
                List<String> supportDimList = buildFactTableSupportDimList(factTable);
                for (MetaField field : factTable.getFields()) {
                    if (!Enabled.value(field.getIsMeasure())) {
                        continue;
                    }

                    //没有白皮书编码的字段不处理
                    if(StrUtil.isEmpty(field.getKpiNo())){
                        continue;
                    }

                    List<RtTableInfo> rtTableInfoList = rtTableInfoMap.getOrDefault(field.getCode(), new ArrayList<>());

                    RtTableInfo rtTableInfo = new RtTableInfo();
                    rtTableInfo.setDimCodeList(supportDimList);

                    if(factTable.getDataSliceCfgObj() != null){
                        rtTableInfo.setDataSliceGranularity(factTable.getDataSliceCfgObj().getSliceType());
                    }

                    rtTableInfo.setTableFullName(factTable.getFullName());
                    rtTableInfo.setFieldId(field.getId());
                    rtTableInfoList.add(rtTableInfo);

                    rtTableInfoMap.put(field.getCode(), rtTableInfoList);
                }
            }

            fieldRtTableCacheTemp.put(datasetId, rtTableInfoMap);
        }

        fieldRtTableCache.clear();
        fieldRtTableCache.putAll(fieldRtTableCacheTemp);
    }

    /**
     * 构建事实表支持的维度列表
     * @param factTable
     * @return
     */
    public List<String> buildFactTableSupportDimList(MetaTable factTable) {
        List<String> supportDimList = new ArrayList<>();

        for (MetaField field : factTable.getFields()) {

            if (Enabled.value(field.getIsMeasure())) {
                continue;
            }

            // 没有白皮书编码的字段不处理
//            if (StrUtil.isEmpty(field.getKpiNo())) {
//                continue;
//            }

            supportDimList.add(field.getCode());
        }

        //再从关联的维表中获取
        List<MetaTable> refDimTables = SSDMetaCacheManager.getRelationTables(factTable.getId());
        for (MetaTable refDimTable : refDimTables) {
            if (CollUtil.isEmpty(refDimTable.getFields())) {
                continue;
            }

            for (MetaField refDimField : refDimTable.getFields()) {
                supportDimList.add(refDimField.getCode());
            }
        }

        supportDimList = supportDimList.stream().distinct().collect(Collectors.toList());
        return supportDimList;
    }

    public static List<RtTableInfo> getFieldRtTableByCode(String datasetId,String fieldCode) {

        Map<String, List<RtTableInfo>> rtTableInfoMap = fieldRtTableCache.get(datasetId);
        if (rtTableInfoMap != null) {
            return rtTableInfoMap.get(fieldCode);
        }

        return new ArrayList<>();
    }

    public static MetaDatasetTable getMetaDatasetTableById(String datasetId) {
       return datasetTablesCache.get(datasetId);
    }


    public static Map<String, MetaDatasetTable> getDatasetTables(){
        return datasetTablesCache;
    }
}
