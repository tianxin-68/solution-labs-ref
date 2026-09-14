package com.bi.queryer.ssm.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.entity.FieldExpansionMappingEntity;
import com.bi.queryer.ssm.api.entity.UserCtgAuthEntity;
import com.bi.queryer.ssm.api.vo.req.*;
import com.bi.queryer.ssm.api.vo.rsp.DataCtgAuthDetailRsp;
import com.bi.queryer.ssm.api.vo.rsp.UserCtgAuthExpiringRsp;
import com.bi.queryer.ssm.enums.*;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.mgr.dataset.DatasetService;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetAddReq;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetReq;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetUpdateReq;
import com.bi.queryer.ssm.mgr.fieldCtg.FieldCtgService;
import com.bi.queryer.ssm.mgr.fieldCtg.model.CtgNeedAuthEntity;
import com.bi.queryer.ssm.mgr.fieldCtg.model.DatasetCtgDataAuthDetailEntity;
import com.bi.queryer.ssm.mgr.fieldCtg.model.DatasetCtgInheritEntity;
import com.bi.queryer.ssm.mgr.fieldCtg.model.FieldCtgRelEntity;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.InitializableModule;
import com.bi.queryer.sys.startup.SystemInitializer;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.StringUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SSMApiService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private FieldCtgService fieldCtgService;

    @Autowired
    private QueryFieldService queryFieldService;

    /** 永久授权结束日期 */
    private static final String PERMANENT_AUTH_END_DATE = "2099-12-31";

    /**
     * 发布模块
     *
     * @param publishModuleReq
     * @return
     */
    public SSMResponseMessage publishModule(PublishModuleReq publishModuleReq) {

        DataSourceType dataSourceType = SSDUtil.getDataEnvDataSourceType();

        dao.executeTranscation(dataSourceType, new AbstractTransaction() {
            @Override
            public void execute() {
                StopWatch stopWatch = new StopWatch("发布到多维：" + publishModuleReq.getCategoryModule().getId());

                stopWatch.start("添加虚拟的表和字段");
                //添加虚拟的表和字段
                appendVirtualTableAndField(publishModuleReq);
                stopWatch.stop();

                stopWatch.start("数据集处理");
                //数据集处理
                saveDataSet(publishModuleReq.getDatasetReq());
                stopWatch.stop();

                stopWatch.start("目录与字段保存");
                //目录与字段保存
                saveCtg(publishModuleReq);
                stopWatch.stop();

                stopWatch.start("表保存");
                //表保存
                saveTable(publishModuleReq);
                stopWatch.stop();

                stopWatch.start("字段保存");
                //字段保存
                saveField(publishModuleReq);
                stopWatch.stop();
                log.info("发布耗时：{}", stopWatch.prettyPrint());

            }
        });

        // 更新安全等级
        updateSecurityLevel(publishModuleReq.getFieldList());

        //刷新缓存
        long start = System.currentTimeMillis();
        refreshSSMCache();
        log.info("发布到多维刷新缓存耗时耗时：{}", System.currentTimeMillis() - start);
        return SSMResponseMessage.success("发布成功");
    }

    /**
     * 保存数据集
     * @param datasetReq
     */
    public void saveDataSet(DatasetReq datasetReq) {
        String datasetId = datasetReq.getDatasetId();

        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();

        SSMDataset ssmDataset = datasetService.getById(datasetId);
        //更新
        if (ssmDataset != null) {

            DatasetUpdateReq datasetUpdateReq = new DatasetUpdateReq();

            datasetUpdateReq.setDatasetId(datasetId);
            datasetUpdateReq.setDatasetName(datasetReq.getDatasetName());
            datasetUpdateReq.setRptDevOwner(datasetReq.getRptDevOwner());
            datasetUpdateReq.setDataDevOwner(datasetReq.getDataDevOwner());
            datasetUpdateReq.setDataDate(datasetReq.getDataDate());
            datasetUpdateReq.setDataDesc(datasetReq.getDataDesc());
            datasetUpdateReq.setDatasetType(datasetReq.getDatasetType());
            datasetUpdateReq.setIsActive(Enabled.YES.getId());
            datasetUpdateReq.setIsStandardDataset(datasetReq.getIsStandardDataset());

            datasetService.update(datasetUpdateReq);

        } else {
            //新增
            DatasetAddReq datasetAddReq = DatasetAddReq.builder()
                    .datasetName(datasetReq.getDatasetName())
                    .rptDevOwner(datasetReq.getRptDevOwner())
                    .dataDevOwner(datasetReq.getDataDevOwner())
                    .dataDate(datasetReq.getDataDate())
                    .dataDesc(datasetReq.getDataDesc())
                    .datasetType(datasetReq.getDatasetType())
                    .datasetId(datasetId)
                    .isActive(Enabled.YES.getId())
                    .isStandardDataset(datasetReq.getIsStandardDataset())
                    .build();
            datasetService.add(datasetAddReq);
        }

        if(CollUtil.isNotEmpty(datasetReq.getAuthUserNameList())){

            List<String> authUserNameList = datasetReq.getAuthUserNameList()
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());
            //授权
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("itemValue",datasetId);
            paramMap.put("itemType", DataAuthItemType.DATASET.getCode() );

            List<DatasetCtgDataAuthDetailEntity> datasetCtgDataAuthDetailList = new ArrayList<>();
            for(String userName : authUserNameList){
                DatasetCtgDataAuthDetailEntity userAuth = new DatasetCtgDataAuthDetailEntity();
                userAuth.setItemType(DataAuthItemType.DATASET.getCode());
                userAuth.setItemValue(datasetId);
                userAuth.setOwnerId(userName);
                userAuth.setOwnerType(OwnerType.USER.getCode());
                userAuth.setCreatedBy(UserManager.get().getName());
                datasetCtgDataAuthDetailList.add(userAuth);
            }

            paramMap.put("ownerIdList",datasetReq.getAuthUserNameList());
            paramMap.put("dataAuthList",datasetCtgDataAuthDetailList);

            dao.execute("ssm.data.auth.dataset.ctg.batchDeleteByOwnIdList", paramMap, dataSourceType);
            dao.execute("ssm.data.auth.dataset.ctg.batchAdd", paramMap, dataSourceType);

        }

    }

    /**
     * 保存目录与字段相关表
     */
    public void saveCtg(PublishModuleReq publishModuleReq) {

        MetaFieldCategory metaFieldCategory = publishModuleReq.getCategoryModule();
        String datasetId = publishModuleReq.getDatasetReq().getDatasetId();

        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();
        String moduleId = metaFieldCategory.getId();
        //查询模块下的子目录id （第一版只有2级）
        List<MetaFieldCategory> childCtgList = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryChildFieldCtgByParentId", moduleId, dataSourceType);

        //先删除
        List<String> deleteCtgIds = new ArrayList<>();
        deleteCtgIds.add(moduleId);
        if (CollUtil.isNotEmpty(childCtgList)) {
            childCtgList.stream().forEach(c -> deleteCtgIds.add(c.getId()));
        }


        Map<String,Object> delParamMap = new HashMap<>();
        delParamMap.put("ctgIds",deleteCtgIds);
        delParamMap.put("datasetId",datasetId);
        dao.delete("fieldCtg.batchDelete",delParamMap, dataSourceType);
        dao.delete("ssm.dataset.ctg.inherit.batchDelete", delParamMap, dataSourceType);
        dao.delete("fieldCtg.batchDeleteCtgDataAuth", delParamMap, dataSourceType);
        dao.delete("ssm.dataset.batchDeleteDatasetCtgRel",delParamMap,dataSourceType);
        dao.delete("ssm.field.ctg.rel.batchDelete", delParamMap, dataSourceType);

        //插入数据
        List<MetaFieldCategory> addCtgList = new ArrayList<>();
        addCtgList.add(metaFieldCategory);
        if(CollUtil.isNotEmpty(metaFieldCategory.getChildren())){
            addCtgList.addAll(metaFieldCategory.getChildren());
        }

        String userName = UserManager.get().getName();
        Map<String,Object> addParamMap = new HashMap<>();
        addParamMap.put("ctgList",addCtgList);
        addParamMap.put("datasetId",datasetId);
        addParamMap.put("userName", userName);
        dao.insert("fieldCtg.batchAdd",addParamMap, dataSourceType);
        dao.insert("ssm.dataset.ctg.inherit.batchAddCtgInherit",addParamMap, dataSourceType);

        //保存目录权限
        List<CtgNeedAuthEntity> ctgDataAuthList = publishModuleReq.getCtgDataAuthList();
        if (CollUtil.isNotEmpty(ctgDataAuthList)) {
            addParamMap.put("dataAuthList", ctgDataAuthList);
            dao.insert("fieldCtg.batchAddCtgDataAuth", addParamMap, dataSourceType);
        }

        //保存数据集与目录关联
        List<String> ctgIdList = addCtgList.stream().map(MetaFieldCategory::getId).collect(Collectors.toList());
        addParamMap.put("ctgIdList", ctgIdList);
        dao.insert("ssm.dataset.batchInsertDatasetCtgRel", addParamMap, dataSourceType);

        //插入字段目录关联——fieldId 如果命中 ssm_field_expansion_mapping 的 old_field_id，换成映射出来的
        //new_field_id（一对多，一条关联展开成多条，每个 new_field_id 一条，其它字段原样复制）；查不到映射的
        //fieldId 保持原样
        if (CollUtil.isNotEmpty(publishModuleReq.getFieldCtgRelList())) {
            List<FieldCtgRelEntity> expandedFieldCtgRelList =
                    expandFieldCtgRelByFieldExpansionMapping(publishModuleReq.getFieldCtgRelList(), publishModuleReq.getCategoryModule().getId());
            List<List<FieldCtgRelEntity>> splitFieldCtgRelList = BIUtil.splitList(expandedFieldCtgRelList, 2000);
            for (List<FieldCtgRelEntity> insertFieldCtgRelList : splitFieldCtgRelList) {
                if (CollUtil.isEmpty(insertFieldCtgRelList)) {
                    continue;
                }
                addParamMap.put("fieldCtgRelList", insertFieldCtgRelList);
                dao.insert("fieldCtg.batchInsertFieldCtgRel", addParamMap, dataSourceType);
            }
        }

    }

    /**
     * 按 ssm_field_expansion_mapping（old_field_id -&gt; new_field_id，一对多）展开字段目录关联：
     * fieldId 命中 old_field_id 的，这一条关联换成分别挂在每个 new_field_id 上的多条（ctgType/ctgId/
     * sortId/isActive/createdBy/updatedBy 原样复制，只换 fieldId）；查不到映射的 fieldId 保持原样、
     * 原样返回，不受影响。
     */
    private List<FieldCtgRelEntity> expandFieldCtgRelByFieldExpansionMapping(List<FieldCtgRelEntity> fieldCtgRelList, String moduleId) {
        return fieldCtgRelList;
//        if ("99ea2066d7c7cffa2bced1cb339cbbcb".equals(moduleId)) {
//            return fieldCtgRelList;
//        }
//
//        List<String> oldFieldIds = fieldCtgRelList.stream()
//                .map(FieldCtgRelEntity::getFieldId)
//                .filter(StrUtil::isNotEmpty)
//                .distinct()
//                .collect(Collectors.toList());
//        if (CollUtil.isEmpty(oldFieldIds)) {
//            return fieldCtgRelList;
//        }
//
//        List<FieldExpansionMappingEntity> mappings = dao.queryObjectList(
//                "ssm.field.expansion.mapping.queryByOldFieldIds", oldFieldIds, FieldExpansionMappingEntity.class);
//        if (CollUtil.isEmpty(mappings)) {
//            return fieldCtgRelList;
//        }
//        Map<String, List<String>> newFieldIdsByOldFieldId = mappings.stream()
//                .filter(m -> StrUtil.isNotEmpty(m.getOldFieldId()) && StrUtil.isNotEmpty(m.getNewFieldId()))
//                .collect(Collectors.groupingBy(FieldExpansionMappingEntity::getOldFieldId,
//                        Collectors.mapping(FieldExpansionMappingEntity::getNewFieldId, Collectors.toList())));
//        if (newFieldIdsByOldFieldId.isEmpty()) {
//            return fieldCtgRelList;
//        }
//
//        List<FieldCtgRelEntity> result = new ArrayList<>(fieldCtgRelList.size());
//        for (FieldCtgRelEntity rel : fieldCtgRelList) {
//            List<String> newFieldIds = newFieldIdsByOldFieldId.get(rel.getFieldId());
//            result.add(rel);
//            if (CollUtil.isEmpty(newFieldIds)) {
//                continue;
//            }
//            for (String newFieldId : newFieldIds) {
//                result.add(FieldCtgRelEntity.builder()
//                        .fieldId(newFieldId)
//                        .ctgType(rel.getCtgType())
//                        .ctgId(rel.getCtgId())
//                        .sortId(rel.getSortId())
//                        .isActive(rel.getIsActive())
//                        .createdBy(rel.getCreatedBy())
//                        .updatedBy(rel.getUpdatedBy())
//                        .build());
//            }
//        }
//        return result;
    }

    /**
     * 保存表
     */
    public void saveTable(PublishModuleReq publishModuleReq) {

        DataSourceType dataSourceType = SSDUtil.getDataEnvDataSourceType();

        List<MetaTable> tableList = publishModuleReq.getTableList();

        if(CollUtil.isEmpty(tableList)){
            return;
        }

//        String bizSplitTableNames = SC.v("ssm.biz.split.table.names", "bi_olap.ads_ord_order_detail_dtl_di,bi_olap.ads_mkt_ad_ancient_ord_device_last_conv_dtl_di,bi_olap.ads_ord_order_detail_sub_dtl_di,bi_olap.ads_mkt_ad_device_tfc_trans_dtl_di,bi_olap.ads_ord_order_detail_opencard_dtl_di,bi_view.v_ads_ord_near_real_coupon_dtl_di_v2,bi_olap.ads_shp_receive_check_category_order_trans_dtl_di,bi_olap.ads_mkt_ad_device_recall_conv_dtl_di,bi_olap.ads_olap_dsv_maintenance_outofstock_warning_df,bi_olap.ads_shp_ord_shoporder_indicator_sub_dtl_di,bi_olap.ads_ord_user_penetration_sum_mi,bi_olap.ads_spy_mnkwx_order_dtl_di,bi_olap.ads_shp_rev_shop_install_order_dtl_di,bi_olap.ads_shp_rev_pid_earning_shop_dtl_di,bi_olap.ads_spy_businessline_payment_dtl_df,bi_olap.ads_ord_pid_supplier_sale_dtl_f,bi_olap.ads_spy_purchase_predict_dtl_di,bi_olap.ads_spy_tire_sku_yhl_distribution_dtl_df,bi_olap.ads_srv_servive_multi_dtl_di,bi_olap.ads_srv_servive_tousu_dtl_di,bi_olap.ads_tfc_page_transform_dtl_di,bi_olap.ads_tfc_conv_platform_goods_sum_di,bi_olap.ads_usr_platrebuy_dtl_di,bi_olap.ads_mkt_ad_device_recall_visit_dtl_di,bi_olap.ads_mkt_act_coupon_usr_ord_receive_verify_dtl_di,bi_olap.dim_prd_product_f");
//        List<String> bizSplitTableNameList = Arrays.asList(StringUtils.split(bizSplitTableNames, ","));
//        if(CollUtil.isNotEmpty(bizSplitTableNameList)) {
//            tableList.forEach(t -> {
//                if (bizSplitTableNameList.contains(t.getTableSchema() + "." + t.getName())) {
//                    t.setName(t.getName() + "_splitbusinessline");
//                }
//            });
//        }

        List<String> tableIdList = tableList.stream().map(MetaTable::getId).collect(Collectors.toList());

        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("tableIdList",tableIdList);
        dao.delete("tableDef.batchDelete",paramMap,dataSourceType);
        dao.delete("tableRel.batchDelete",paramMap,dataSourceType);
        dao.delete("tableDef.batchDeleteTableEtljob",paramMap,dataSourceType);
        dao.delete("tableDef.batchDeleteTablePriSubCfg", paramMap,dataSourceType);

        //批量保存
        paramMap.put("tableList",tableList);
        paramMap.put("userName", UserManager.get().getName());
        dao.insert("tableDef.batchInsert", paramMap,dataSourceType);

        if(CollUtil.isNotEmpty(publishModuleReq.getTableRelList())){
            paramMap.put("tableRelList",publishModuleReq.getTableRelList());
            dao.insert("tableRel.batchInsert", paramMap,dataSourceType);
        }

        List<MetaTableEtlJob> etlJobList = new ArrayList<>();
        for(MetaTable metaTable : tableList){
            if(CollUtil.isEmpty(metaTable.getEtlJobs())){
                continue;
            }

            for(String etlJob : metaTable.getEtlJobs()){
                MetaTableEtlJob metaTableEtlJob = new MetaTableEtlJob();
                metaTableEtlJob.setTableId(metaTable.getId());
                metaTableEtlJob.setEtlJob(etlJob);
                etlJobList.add(metaTableEtlJob);
            }
        }

        if(CollUtil.isNotEmpty(etlJobList)){
            paramMap.put("etlJobList",etlJobList);
            dao.insert("tableDef.batchInsertTableEtljob", paramMap,dataSourceType);
        }

        if(CollUtil.isNotEmpty(publishModuleReq.getTablePriSubCfgList())){
            paramMap.put("tablePriSubList",publishModuleReq.getTablePriSubCfgList());
            dao.insert("tableDef.batchInsertTablePriSubCfg", paramMap,dataSourceType);
        }

    }

    /**
     * 平均车龄 + 平均车价 + 平均坑位 需要在原始物理表的基础上创建虚拟表
     * 实现查询走到各自定制化的模型
     */
    public void appendVirtualTableAndField(PublishModuleReq publishModuleReq) {

        List<MetaTable> tableList = publishModuleReq.getTableList();
        List<MetaTableRelation> tableRelList = publishModuleReq.getTableRelList();
        List<MetaField> fieldList = publishModuleReq.getFieldList();

        if (CollUtil.isEmpty(tableList)) {
            return;
        }

        List<String> virtualTableOriginalNameList = DataSourceViewMeasureType.getTableNameList();
        for(String virtualTableOriginalName : virtualTableOriginalNameList){
            Optional<MetaTable> optionalMetaTable = tableList.stream().filter(t -> t.getFullName().equalsIgnoreCase(virtualTableOriginalName.trim())).findAny();
            if (!optionalMetaTable.isPresent()) {
                continue;
            }

            MetaTable metaTable = optionalMetaTable.get();

            //表关联
            List<MetaTableRelation> virtualTableRelList = publishModuleReq.getTableRelList()
                    .stream()
                    .filter(f -> f.getPrimaryTableId().equalsIgnoreCase(metaTable.getId()))
                    .collect(Collectors.toList());

            //需要虚拟的字段基础信息
            List<MetaField> virtualFieldList = publishModuleReq.getFieldList()
                    .stream()
                    .filter(f-> metaTable.getId().equalsIgnoreCase(f.getFactTableId()))
                    .collect(Collectors.toList());

            for (DataSourceViewMeasureType dataSourceViewMeasureType : DataSourceViewMeasureType.values()) {
                if (DataSourceViewMeasureType.UNKNOWN == dataSourceViewMeasureType) {
                    continue;
                }

                List<String> dataSourceViewTableName = Arrays.asList(dataSourceViewMeasureType.getTableNames().split(","));
                if(!dataSourceViewTableName.contains(virtualTableOriginalName)) {
                    continue;
                }

                //判断表里面有没有二次计算的字段
                Long virtualFieldCount = virtualFieldList.stream()
                        .filter(f->f.getName().equalsIgnoreCase(dataSourceViewMeasureType.getName())).count();
                if(virtualFieldCount == 0){
                    continue;
                }

                MetaTable virtualMetaTable = metaTable.clone();
                String virualTableId = String.format("%s_%s", dataSourceViewMeasureType.getCode().toLowerCase(), metaTable.getId());
                virtualMetaTable.setId(virualTableId);
                virtualMetaTable.setEtlJobs(metaTable.getEtlJobs());
                tableList.add(virtualMetaTable);

                if (CollUtil.isNotEmpty(virtualTableRelList)) {

                    for (MetaTableRelation metaTableRelation : virtualTableRelList) {
                        MetaTableRelation virtualMetaTableRelation = metaTableRelation.clone();
                        virtualMetaTableRelation.setPrimaryTableId(virualTableId);
                        virtualMetaTableRelation.setPrimaryFieldId(String.format("%s_%s", dataSourceViewMeasureType.getCode().toLowerCase(), metaTableRelation.getPrimaryFieldId()));
                        tableRelList.add(virtualMetaTableRelation);
                    }
                }

                if(CollUtil.isNotEmpty(virtualFieldList)){

                    for (MetaField metaField : virtualFieldList) {

                        //添加附加字段
                        String additionalFieldNames = dataSourceViewMeasureType.getAdditionalFieldNames();
                        if(StrUtil.isNotEmpty(additionalFieldNames)){
                            List<String> additionalFieldNameList = Arrays.asList(additionalFieldNames.split(","));
                            if(additionalFieldNameList.contains(metaField.getName())){
                                MetaField virtualMetaField = metaField.clone();
                                virtualMetaField.setId(String.format("%s_%s", dataSourceViewMeasureType.getCode().toLowerCase(), virtualMetaField.getId()));
                                virtualMetaField.setFactTableId(virualTableId);
                                fieldList.add(virtualMetaField);
                                continue;
                            }

                        }

                        if (Enabled.value(metaField.getIsMeasure())) {

                            if (!metaField.getName().equalsIgnoreCase(dataSourceViewMeasureType.getName())) {
                                continue;
                            }

                            metaField.setFactTableId(virualTableId);


                        } else {
                            //添加维度
                            MetaField virtualMetaField = metaField.clone();
                            virtualMetaField.setId(String.format("%s_%s", dataSourceViewMeasureType.getCode().toLowerCase(), virtualMetaField.getId()));
                            virtualMetaField.setFactTableId(virualTableId);
                            fieldList.add(virtualMetaField);
                        }
                    }
                }

            }

        }

        publishModuleReq.setTableList(tableList);
        publishModuleReq.setTableRelList(tableRelList);
        publishModuleReq.setFieldList(fieldList);
    }

    /**
     * 保存字段
     */
    public void saveField(PublishModuleReq publishModuleReq) {

        DataSourceType dataSourceType = SSDUtil.getDataEnvDataSourceType();

        List<MetaField> fieldList = publishModuleReq.getFieldList();

        if (CollUtil.isEmpty(fieldList)) {
            return;
        }

        List<String> tableIdList = fieldList.stream().map(MetaField::getTableId).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());

        List<String> calcFieldIds = fieldList.stream().filter(v->Objects.equals(v.getFieldType(), FieldType.CROSS_MODEL_MEASURE.getCode()))
                .map(MetaField::getId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(publishModuleReq.getDeletedFieldIds())) {
            calcFieldIds.addAll(publishModuleReq.getDeletedFieldIds());
        }
        if (CollUtil.isNotEmpty(calcFieldIds)) {
            dao.delete("fieldDef.deleteByFieldIds", calcFieldIds, dataSourceType);
        }

        String userName = UserManager.get().getName();
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("tableIdList", tableIdList);
        paramMap.put("userName", userName);
        dao.delete("fieldDef.batchDeleteByTableIdList", paramMap, dataSourceType);

        List<String> fieldCodeList = new ArrayList<>();
        List<MetaFieldKpi> fieldKpiList = new ArrayList<>();
        Map<String, String> fieldCodeMap = new HashMap<String, String>();
        List<MetaFieldValueMap> fieldValueMaps = new ArrayList<>();
        for (MetaField field : fieldList) {

            if (fieldCodeMap.containsKey(field.getCode())) {
                continue;
            }

            fieldCodeMap.put(field.getCode(), "");
            fieldCodeList.add(field.getCode());

            if (StrUtil.isEmpty(field.getKpiNo())) {
                continue;
            }

            if (CollUtil.isNotEmpty(field.getDimValueMap())) {
                field.getDimValueMap().forEach(item -> {
                    Map.Entry<String, String> entry = item.entrySet().iterator().next();
                    fieldValueMaps.add(MetaFieldValueMap.of(field.getKpiNo(), entry.getKey(), entry.getValue(), userName));
                });
            }

            MetaFieldKpi metaFieldKpi = new MetaFieldKpi();
            metaFieldKpi.setFieldCode(field.getCode());
            metaFieldKpi.setKpiName(field.getKpiName());

            if (Enabled.value(field.getIsMeasure())) {
                metaFieldKpi.setMeasureCode(field.getKpiNo());
            } else {
                metaFieldKpi.setDimCode1(field.getKpiNo());
            }
            fieldKpiList.add(metaFieldKpi);

        }

        //插入字段
        if (CollUtil.isNotEmpty(fieldList)) {

            //切分插入
            List<List<MetaField>> splitFieldList = BIUtil.splitList(fieldList, 2000);
            for (List<MetaField> insertFieldList : splitFieldList) {

                if (CollUtil.isEmpty(insertFieldList)) {
                    continue;
                }

                paramMap.put("fieldList", insertFieldList);
                dao.insert("fieldDef.batchInsert", paramMap, dataSourceType);
            }

        }

        //处理字段与白皮书关联
        if (CollUtil.isNotEmpty(fieldCodeList)) {
            paramMap.put("fieldCodeList", fieldCodeList);
            dao.delete("fieldDef.batchDeleteFieldKpi", paramMap, dataSourceType);
        }

        if (CollUtil.isNotEmpty(fieldKpiList)) {

            List<List<MetaFieldKpi>> splitFieldKpiList = BIUtil.splitList(fieldKpiList, 2000);
            for (List<MetaFieldKpi> insertFieldKpiList : splitFieldKpiList) {
                if (CollUtil.isEmpty(insertFieldKpiList)) {
                    continue;
                }
                paramMap.put("fieldKpiList", insertFieldKpiList);
                dao.insert("fieldDef.batchInsertFieldKpi", paramMap, dataSourceType);
            }
        }

        if (CollUtil.isNotEmpty(fieldCodeList)) {
            dao.insert("fieldDef.deleteFieldValueMap", paramMap, dataSourceType);
            List<List<MetaFieldValueMap>> splitFieldItemMapList = BIUtil.splitList(fieldValueMaps, 2000);
            for (List<MetaFieldValueMap> insertFieldItemList : splitFieldItemMapList) {
                paramMap.put("fieldItemList", insertFieldItemList);
                dao.insert("fieldDef.batchInsertFieldValueMap", paramMap, dataSourceType);
            }
        }
    }


    /**
     * 下线模块
     * @param offlineModuleReq
     * @return
     */
    public SSMResponseMessage offlineModule(OfflineModuleReq offlineModuleReq) {

        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();

        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("ctgId", offlineModuleReq.getModuleId());
        dao.update("fieldCtg.disableFieldCtg",paramMap,dataSourceType);

        refreshSSMCache();
        return SSMResponseMessage.success("下线模块成功");

    }

    /**
     * 发布字段
     * @param metaFields
     * @return
     */
    public SSMResponseMessage publishField(List<MetaField> metaFields) {
        if (CollUtil.isEmpty(metaFields)) {
            return SSMResponseMessage.success("没有需要发布的字段");
        }

        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();
        List<MetaFieldValueMap> fieldItemMaps = new ArrayList<>();
        for (MetaField metaField : metaFields) {
            List<String> fieldCodes = Collections.singletonList(metaField.getKpiNo());
//            String whitePaperCode = metaField.getKpiNo();
//
//            String sqlId = "fieldDef.queryFieldCodeByMetricCode";
//            if (!Enabled.value(metaField.getIsMeasure())) {
//                sqlId = "fieldDef.queryFieldCodeByDimCode";
//            }
//
//            fieldCodes = (List<String>) dao.queryObjectList(sqlId, whitePaperCode, dataSourceType);
            if (CollUtil.isNotEmpty(metaField.getDimValueMap())) {
                metaField.getDimValueMap().forEach(item -> {
                    Map.Entry<String, String> entry = item.entrySet().iterator().next();
                    fieldItemMaps.add(MetaFieldValueMap.of(metaField.getKpiNo(), entry.getKey(), entry.getValue(), ""));
                });
            }

            if (CollUtil.isNotEmpty(fieldCodes)) {

                List<MetaField> cacheFieldList = SSDMetaCacheManager.getFieldByCode(fieldCodes.get(0));
                if (CollUtil.isNotEmpty(cacheFieldList)) {

                    //自定义计算字段 此处不能处理计算表达式，不同表中，字段id不一致
                    //计算字段不更新 agg_type
                    //code对应的字段，存在一个是计算字段，则不更新

                    int calcFieldCount = 0;
                    for (MetaField cacheField : cacheFieldList) {
                        if (FieldUtil.isCalcField(cacheField)) {
                            calcFieldCount++;
                        }
                    }

                    if (calcFieldCount > 0) {
                        metaField.setAggExpression("");
                    }

                    metaField.setCode(cacheFieldList.get(0).getCode());
                } else {

                    //缓存里面没有找到，不更新 agg_type，避免聚合方式异常
                    //可能存在的异常场景，发布的瞬间，字段缓存被清空
                    metaField.setAggExpression("");
                }

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("fieldCodes", fieldCodes);
                paramMap.put("userName", metaField.getUpdatedBy());
                paramMap.put("metaField", metaField);

                dao.queryObjectList("fieldDef.batchUpdateFieldProperty", paramMap, dataSourceType);

            }
        }

        List<String> fieldCodeList = metaFields.stream().map(MetaField::getKpiNo).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(fieldCodeList)) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("fieldCodeList", fieldCodeList);
            dao.insert("fieldDef.deleteFieldValueMap", paramMap, dataSourceType);
            if (CollUtil.isNotEmpty(fieldItemMaps)) {
                paramMap.put("fieldItemList", fieldItemMaps);
                dao.insert("fieldDef.batchInsertFieldValueMap", paramMap, dataSourceType);
            }
        }

        Map<String, Object> rmiParamMap = new HashMap<>();
        rmiParamMap.put("cacheFlushType", CacheFlushType.FLUSH_FIELD_BY_CODE.getCode());
        rmiParamMap.put("metaField", JSON.toJSONString(metaFields));
        rmiParamMap.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.SSD.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, rmiParamMap);

        //更新字段安全等级
        updateSecurityLevel(metaFields);

        return SSMResponseMessage.success("发布字段成功");
    }

    private synchronized void updateSecurityLevel(List<MetaField> metaFields) {
        if (CollUtil.isEmpty(metaFields)) {
            return;
        }
        List<String> fieldCodeList = metaFields.stream().map(MetaField::getKpiNo).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(fieldCodeList)) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("fieldCodeList", fieldCodeList);
            dao.delete("ssm.olap.field.core.delete", paramMap, DataSourceType.Default);
        }

        User user = UserManager.get();
        if (user == null) {
            user = new User();
        }

        List<CoreMetaField> coreMetaFields = new ArrayList<>();
        Set<String> existCodes = new HashSet<>(metaFields.size());
        for (MetaField metaField : metaFields) {
            if (DataSensitiveLevel.C3.getCode().equalsIgnoreCase(metaField.getSensitiveLevel()) ||
                    DataSensitiveLevel.C4.getCode().equalsIgnoreCase(metaField.getSensitiveLevel())) {
                if (existCodes.contains(metaField.getKpiNo())) {
                    continue;
                }
                CoreMetaField coreMetaField = new CoreMetaField();
                coreMetaField.setFieldCode(metaField.getKpiNo());
                coreMetaField.setCoreType("mgp");
                coreMetaField.setFieldTitle(metaField.getTitle());
                coreMetaField.setFieldType(Enabled.value(metaField.getIsMeasure()) ? "metric" : "dim");
                coreMetaField.setSensitiveLevel(StringUtils.lowerCase(metaField.getSensitiveLevel()));
                coreMetaField.setPkid(Guid.id());
                coreMetaField.setCreatedBy(user.getName());
                coreMetaFields.add(coreMetaField);
                existCodes.add(metaField.getKpiNo());
            }
        }
        if (CollUtil.isNotEmpty(coreMetaFields)) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("coreMetaFields", coreMetaFields);
            dao.insert("ssm.olap.field.core.batchAdd", paramMap, DataSourceType.Default);
        }
    }

    /**
     * 更新目录排序
     * @param categoryList
     * @return
     */
    public SSMResponseMessage changeCtgSortId(List<MetaFieldCategory> categoryList){

        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();

        String userName = UserManager.get().getName();
        for(MetaFieldCategory category : categoryList){
            category.setUpdatedBy(userName);
            dao.update("fieldCtg.updateSortId", category,dataSourceType);
        }

        Map<String, Object> rmiParamMap = new HashMap<>();
        rmiParamMap.put("cacheFlushType", CacheFlushType.FLUSH_CTG_SORT_ID.getCode());
        rmiParamMap.put("categoryList", JSON.toJSONString(categoryList));
        rmiParamMap.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.SSD.toString());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, rmiParamMap);

        return SSMResponseMessage.success("更新目录排序成功");
    }

    /**
     * 查询目录
     * @return
     */
    public SSMResponseMessage<MetaFieldCategory> queryCategoryById(QueryCategoryReq queryCategoryReq) {

        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();
        String categoryId = queryCategoryReq.getCategoryId();

        MetaFieldCategory category = (MetaFieldCategory) dao.queryObject("fieldCtg.queryFieldCtgById", categoryId, dataSourceType);

        if (category != null) {
            List<MetaFieldCategory> children = (List<MetaFieldCategory>) dao.queryObjectList("fieldCtg.queryChildFieldCtgByParentId", categoryId, dataSourceType);
            category.setChildren(children);

            //查询目录权限
            List<CtgNeedAuthEntity> ctgDataAuthList = (List<CtgNeedAuthEntity>) dao.queryObjectList("fieldCtg.queryCtgNeedAuthById", categoryId, dataSourceType);
            category.setCtgDataAuthList(ctgDataAuthList);
        }

        return SSMResponseMessage.success("", category);
    }

    /**
     * 同步多维目录权限
     * @return
     */
    public SSMResponseMessage syncModuleCtgAuth(List<SyncModuleCtgAuthReq> list){

       if(CollUtil.isEmpty(list)){
           return SSMResponseMessage.success("同步多维目录权限成功！");
       }

       List<DatasetCtgInheritEntity> datasetCtgInheritList = new ArrayList<>();

       List<String> ctgIds = new ArrayList<>();
       List<DatasetCtgDataAuthDetailEntity> datasetCtgDataAuthDetailList = new ArrayList<>();

       List<String> deleteDataAuthIdList = new ArrayList<>();
       String createdBy = UserManager.get().getName();

        //判断目录是否存在
       for(SyncModuleCtgAuthReq syncModuleCtgAuthReq : list) {

           String categoryId = syncModuleCtgAuthReq.getModuleId();
           ctgIds.add(categoryId);

           MetaFieldCategory category = SSDMetaCacheManager.getCategoryById(categoryId);
           if (category == null) {
               continue;
           }

           //处理目录继承关系
           DatasetCtgInheritEntity datasetCtgInheritEntity = new DatasetCtgInheritEntity();
           datasetCtgInheritEntity.setItemType(DataAuthItemType.CTG.getCode());
           datasetCtgInheritEntity.setItemValue(categoryId);
           datasetCtgInheritEntity.setCreatedBy(UserManager.get().getName());
           if (syncModuleCtgAuthReq.getIsInherited() == null) {
               datasetCtgInheritEntity.setIsInherited(Enabled.YES.getId());
           } else {
               datasetCtgInheritEntity.setIsInherited(syncModuleCtgAuthReq.getIsInherited());
           }

           datasetCtgInheritList.add(datasetCtgInheritEntity);

           if (CollUtil.isNotEmpty(syncModuleCtgAuthReq.getAuthUserList())) {
               for (String userName : syncModuleCtgAuthReq.getAuthUserList()) {
                   DatasetCtgDataAuthDetailEntity userAuth = new DatasetCtgDataAuthDetailEntity();
                   userAuth.setItemType(DataAuthItemType.CTG.getCode());
                   userAuth.setItemValue(categoryId);
                   userAuth.setOwnerId(userName);
                   userAuth.setOwnerType(OwnerType.USER.getCode());
                   userAuth.setCreatedBy(createdBy);
                   datasetCtgDataAuthDetailList.add(userAuth);
               }
           }

           if (CollUtil.isNotEmpty(syncModuleCtgAuthReq.getAuthDeptList())) {
               for (String deptId : syncModuleCtgAuthReq.getAuthDeptList()) {
                   DatasetCtgDataAuthDetailEntity deptAuth = new DatasetCtgDataAuthDetailEntity();
                   deptAuth.setItemType(DataAuthItemType.CTG.getCode());
                   deptAuth.setItemValue(categoryId);
                   deptAuth.setOwnerId(deptId);
                   deptAuth.setOwnerType(OwnerType.DEPT.getCode());
                   deptAuth.setCreatedBy(createdBy);
                   datasetCtgDataAuthDetailList.add(deptAuth);
               }
           }

           if (CollUtil.isNotEmpty(syncModuleCtgAuthReq.getDeleteDataAuthIdList())) {
               deleteDataAuthIdList.addAll(syncModuleCtgAuthReq.getDeleteDataAuthIdList());
           }

       }


        DataSourceType dataSourceType = SSDUtil.getMgpDataSourceType();

        dao.executeTranscation(dataSourceType, new AbstractTransaction() {
            @Override
            public void execute() {
                //处理权限
                Map<String,Object> paramMap = new HashMap<>();
                paramMap.put("itemValueList",ctgIds);

                //先按模块删除
                dao.delete("ssm.data.auth.dataset.ctg.batchDelete",paramMap,dataSourceType);

                if(CollUtil.isNotEmpty(datasetCtgDataAuthDetailList)){
                    paramMap.put("dataAuthList", datasetCtgDataAuthDetailList);
                    dao.insert("ssm.data.auth.dataset.ctg.batchAdd", paramMap, dataSourceType);
                }

                //处理目录继承关系
                if(CollUtil.isNotEmpty(datasetCtgInheritList)){
                    paramMap.put("inheritList", datasetCtgInheritList);
                    dao.delete("ssm.dataset.ctg.inherit.batchDeleteByList", paramMap, dataSourceType);
                    dao.insert("ssm.dataset.ctg.inherit.batchAddCtgInheritByList",paramMap, dataSourceType);
                }

            }
        });


        //删除工单申请的权限
        if(CollUtil.isNotEmpty(deleteDataAuthIdList)){
            Map<String,Object> deleteParamMap = new HashMap<>();
            deleteParamMap.put("deleteDataAuthIdList", deleteDataAuthIdList);

            String today = DateUtil.today();
            String remark = String.format("%s%s解除权限",today, createdBy);
            deleteParamMap.put("remark", remark);
            dao.update("authority.disableDataAuth", deleteParamMap);
        }

        return SSMResponseMessage.success("同步多维目录权限成功！");
    }


    public List<DataCtgAuthDetailRsp> queryDataCtgAuthList(QueryDataCtgAuthReq queryDataCtgAuthReq,DataSourceType dataSourceType) {

        List<DataCtgAuthDetailRsp> result = new ArrayList<>();

        //查询数据门户目录权限
        List<DataCtgAuthDetailRsp> portalAuthList = (List<DataCtgAuthDetailRsp>) dao.queryObjectList("authority.queryDataCtgAuth",queryDataCtgAuthReq.getCtgId());

        if(CollUtil.isNotEmpty(portalAuthList)){
            result.addAll(portalAuthList);
        }

        Map<String, Integer> inheritMap = new HashMap<>();
        fieldCtgService.buildDatasetCtgInheritMap(inheritMap, dataSourceType);

        MetaFieldCategory metaFieldCategory =  SSDMetaCacheManager.getCategoryById(queryDataCtgAuthReq.getCtgId());
        if(metaFieldCategory == null){
            return result;
        }

        //查询多维后台配置的权限
        List<DataCtgAuthDetailRsp> ssmAuthList = (List<DataCtgAuthDetailRsp>) dao.queryObjectList("ssm.data.auth.dataset.ctg.queryDataCtgAuth",queryDataCtgAuthReq.getCtgId(),dataSourceType);
        if(CollUtil.isNotEmpty(ssmAuthList)){
            result.addAll(ssmAuthList);
        }

        //查询继承的权限
        buildInheritAuth(inheritMap, result, metaFieldCategory.getId(),dataSourceType);

        //排序
        if(CollUtil.isNotEmpty(result)){
            result.sort(Comparator.comparing(DataCtgAuthDetailRsp::getUserName));
        }
        
        return result;
    }

    public void buildInheritAuth(Map<String, Integer> inheritMap,List<DataCtgAuthDetailRsp> result,String ctgId,DataSourceType dataSourceType){

        MetaFieldCategory metaFieldCategory = SSDMetaCacheManager.getCategoryById(ctgId);
        if (metaFieldCategory != null) {

            MetaFieldCategory parent = SSDMetaCacheManager.getCategoryById(metaFieldCategory.getParentId());
            if (parent != null) {

                Integer isInherited = inheritMap.get(metaFieldCategory.getId());
                if (Enabled.value(isInherited)) {

                    List<DataCtgAuthDetailRsp> ssmAuthList = (List<DataCtgAuthDetailRsp>) dao.queryObjectList("ssm.data.auth.dataset.ctg.queryDataCtgAuth",parent.getId(),dataSourceType);
                    if(CollUtil.isNotEmpty(ssmAuthList)){
                        for(DataCtgAuthDetailRsp dataCtgAuthDetailRsp : ssmAuthList){
                            String authSourceDetail = String.format("【%s-%s】%s","权限继承-目录", parent.getName(), dataCtgAuthDetailRsp.getAuthSourceDetail());
                            dataCtgAuthDetailRsp.setAuthSource("权限继承");
                            dataCtgAuthDetailRsp.setAuthSourceDetail(authSourceDetail);
                            result.add(dataCtgAuthDetailRsp);
                        }

                    }

                    List<DataCtgAuthDetailRsp> ssmPortalAuthList = (List<DataCtgAuthDetailRsp>) dao.queryObjectList("authority.queryDataCtgAuth",parent.getId());
                    if(CollUtil.isNotEmpty(ssmPortalAuthList)){
                        for(DataCtgAuthDetailRsp dataCtgAuthDetailRsp : ssmPortalAuthList){
                            String authSourceDetail = String.format("【%s-%s】%s","权限继承-目录", parent.getName(), dataCtgAuthDetailRsp.getAuthSourceDetail());
                            dataCtgAuthDetailRsp.setAuthSource("权限继承");
                            dataCtgAuthDetailRsp.setAuthSourceDetail(authSourceDetail);
                            result.add(dataCtgAuthDetailRsp);
                        }
                    }

                    buildInheritAuth( inheritMap,result, parent.getId(),dataSourceType);

                }
            }else{

                //最上层目录查询数据集
                if (BIConsts.Category_Root_Id.equalsIgnoreCase(metaFieldCategory.getParentId())) {

                    Integer isInherited = inheritMap.get(metaFieldCategory.getId());
                    if (Enabled.value(isInherited)) {

                        List<DataCtgAuthDetailRsp> ssmAuthList = (List<DataCtgAuthDetailRsp>) dao.queryObjectList("ssm.data.auth.dataset.ctg.queryDataCtgAuth",metaFieldCategory.getDatasetId(),dataSourceType);
                        SSMDataset dataset = datasetService.getById(metaFieldCategory.getDatasetId());
                        if(CollUtil.isNotEmpty(ssmAuthList)){
                            for(DataCtgAuthDetailRsp dataCtgAuthDetailRsp : ssmAuthList){
                                String authSourceDetail = String.format("【%s-%s】%s","权限继承-数据集", dataset.getDatasetName(), dataCtgAuthDetailRsp.getAuthSourceDetail());
                                dataCtgAuthDetailRsp.setAuthSource("权限继承");
                                dataCtgAuthDetailRsp.setAuthSourceDetail(authSourceDetail);;
                                result.add(dataCtgAuthDetailRsp);
                            }
                        }

                        //数据集查询全部
                        if(Enabled.value(inheritMap.get(dataset.getDatasetId()))){
                            List<DataCtgAuthDetailRsp> allAuthList = (List<DataCtgAuthDetailRsp>) dao.queryObjectList("ssm.data.auth.dataset.ctg.queryDataCtgAuth",DataAuthItemType.ALL.getValue(),dataSourceType);
                            if(CollUtil.isNotEmpty(allAuthList)){
                                for(DataCtgAuthDetailRsp dataCtgAuthDetailRsp : allAuthList){
                                    String authSourceDetail = String.format("【%s】%s","权限继承-全部", dataCtgAuthDetailRsp.getAuthSourceDetail());
                                    dataCtgAuthDetailRsp.setAuthSource("权限继承");
                                    dataCtgAuthDetailRsp.setAuthSourceDetail(authSourceDetail);;
                                    result.add(dataCtgAuthDetailRsp);
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * 构建用户目录权限
     * @return
     */
    public SSMResponseMessage buildUserCtgAuthDtl() {

        //先全量删除
        dao.delete("ssm.user.ctg.auth.dtl.delete", null);

        long t1 = System.currentTimeMillis();

        QueryDataCtgAuthReq queryDataCtgAuthReq = new QueryDataCtgAuthReq();
        int num = 0;
        int total = SSDMetaCacheManager.getFrontCategories().values().size();
        for (MetaFieldCategory mf : SSDMetaCacheManager.getFrontCategories().values()) {
            queryDataCtgAuthReq.setCtgId(mf.getId());

            DataSourceType dataSourceType = DataSourceType.Default;
            if(DataEnv.NEW_MGP ==  DataEnv.get(mf.getDataEnv())){
                dataSourceType = SSDUtil.getMgpDataSourceType();
            }
            List<DataCtgAuthDetailRsp> dataCtgAuthDetailList = queryDataCtgAuthList(queryDataCtgAuthReq,dataSourceType);

            if (CollUtil.isEmpty(dataCtgAuthDetailList)) {
                continue;
            }

            List<UserCtgAuthEntity> userCtgAuthEntityList = new ArrayList<>();
            Map<String, Object> insertMap = new HashMap<>();
            for (DataCtgAuthDetailRsp dataCtgAuthDetailRsp : dataCtgAuthDetailList) {
                UserCtgAuthEntity userCtgAuthEntity = UserCtgAuthEntity.builder()
                        .ctgId(mf.getId())
                        .ctgName(mf.getName())
                        .isModule(mf.getIsModule())
                        .userName(dataCtgAuthDetailRsp.getUserName())
                        .userRealName(dataCtgAuthDetailRsp.getUserRealName())
                        .deptName(dataCtgAuthDetailRsp.getDeptName())
                        .authSource(dataCtgAuthDetailRsp.getAuthSource())
                        .authSourceDetail(dataCtgAuthDetailRsp.getAuthSourceDetail())
                        .authStartDate(dataCtgAuthDetailRsp.getAuthBeginDate())
                        .authEndDate(dataCtgAuthDetailRsp.getAuthEndDate())
                        .build();
                userCtgAuthEntityList.add(userCtgAuthEntity);
            }

            List<List<UserCtgAuthEntity>> splitUserCtgAuthList = BIUtil.splitList(userCtgAuthEntityList, 2000);
            for (List<UserCtgAuthEntity> list : splitUserCtgAuthList) {
                insertMap.put("authList", list);
                dao.insert("ssm.user.ctg.auth.dtl.batchAdd", insertMap);
            }

            num++;
            System.out.println(String.format("构建用户目录权限进度%s/%s", num, total));

        }

        long t2 = System.currentTimeMillis();
        System.out.println("构建用户目录权限耗时: "+ (t2-t1)/1000);

        // 后置处理：角色扩展用户重建特殊权限、黑名单用户清除权限
        postProcessUserCtgAuthDtl();

        return SSMResponseMessage.success("");
    }

    /**
     * 构建完成后后置处理：
     * 1. ssm_role_user_ext 用户删除旧权限，按 getSpecialCtgAcls 重建
     * 2. 黑名单用户删除全部权限（优先级最高）
     */
    private void postProcessUserCtgAuthDtl() {
        rebuildSpecialUserCtgAuth();
        removeBlacklistedUserCtgAuth();
    }

    /**
     * ssm_role_user_ext 用户：删除旧权限，单独从 getSpecialCtgAcls 获取并写入
     */
    private void rebuildSpecialUserCtgAuth() {
        List<String> specialUserNames = (List<String>) dao.queryObjectList("ssm.role.ext.queryAllUserNames", null);
        if (CollUtil.isEmpty(specialUserNames)) {
            return;
        }
        specialUserNames = specialUserNames.stream()
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(specialUserNames)) {
            return;
        }

        dao.delete("ssm.user.ctg.auth.dtl.deleteByUserNames", specialUserNames);

        Map<String, User> userMap = loadUserMapByNames(specialUserNames);
        String today = DateUtil.today();
        List<UserCtgAuthEntity> entityList = new ArrayList<>();
        for (String userName : specialUserNames) {
            List<String> ctgIds = queryFieldService.getSpecialCtgAcls(userName);
            if (CollUtil.isEmpty(ctgIds)) {
                continue;
            }
            User user = userMap.get(userName);
            for (String ctgId : ctgIds) {
                MetaFieldCategory category = SSDMetaCacheManager.getCategoryById(ctgId);
                if (category == null) {
                    continue;
                }
                entityList.add(UserCtgAuthEntity.builder()
                        .ctgId(ctgId)
                        .ctgName(category.getName())
                        .isModule(category.getIsModule())
                        .userName(userName)
                        .userRealName(user != null ? user.getRealName() : "")
                        .deptName(user != null ? user.getDeptPathName() : "")
                        .authSource("特殊权限")
                        .authSourceDetail("特殊权限-角色扩展")
                        .authStartDate(today)
                        .authEndDate(PERMANENT_AUTH_END_DATE)
                        .build());
            }
        }
        batchInsertUserCtgAuthEntities(entityList);
    }

    /**
     * 删除黑名单用户的全部目录权限
     */
    private void removeBlacklistedUserCtgAuth() {
        List<String> blacklistUserNames = (List<String>) dao.queryObjectList(
                "ssm.system.access.blacklist.queryAll", new HashMap<>());
        if (CollUtil.isEmpty(blacklistUserNames)) {
            return;
        }
        blacklistUserNames = blacklistUserNames.stream()
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(blacklistUserNames)) {
            return;
        }
        dao.delete("ssm.user.ctg.auth.dtl.deleteByUserNames", blacklistUserNames);
    }

    /**
     * 批量加载用户信息
     */
    private Map<String, User> loadUserMapByNames(List<String> userNames) {
        if (CollUtil.isEmpty(userNames)) {
            return Collections.emptyMap();
        }
        List<User> users = (List<User>) dao.queryObjectList("user.queryUserListByNames", userNames);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(User::getName, user -> user, (a, b) -> a));
    }

    /**
     * 分批写入用户目录权限明细
     */
    private void batchInsertUserCtgAuthEntities(List<UserCtgAuthEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return;
        }
        Map<String, Object> insertMap = new HashMap<>();
        List<List<UserCtgAuthEntity>> splitList = BIUtil.splitList(entityList, 2000);
        for (List<UserCtgAuthEntity> list : splitList) {
            insertMap.put("authList", list);
            dao.insert("ssm.user.ctg.auth.dtl.batchAdd", insertMap);
        }
    }

    /**
     * 发送目录权限临期提醒邮件
     * @return 发送成功/失败统计
     */
    public SSMResponseMessage sendCtgAuthExpiringMail() {
        List<UserCtgAuthExpiringRsp> expiringList =
                (List<UserCtgAuthExpiringRsp>) dao.queryObjectList("ssm.user.ctg.auth.dtl.queryExpiringAuth", null);
        expiringList = enrichExpiringAuthList(expiringList);
        if (CollUtil.isEmpty(expiringList)) {
            return SSMResponseMessage.success("无临期权限数据，无需发送邮件");
        }

        Map<String, List<UserCtgAuthExpiringRsp>> groupByUser = expiringList.stream()
                .filter(row -> StrUtil.isNotEmpty(row.getUserName()))
                .collect(Collectors.groupingBy(UserCtgAuthExpiringRsp::getUserName));

        int successCount = 0;
        int failCount = 0;
        for (Map.Entry<String, List<UserCtgAuthExpiringRsp>> entry : groupByUser.entrySet()) {
            try {
                boolean sent = sendExpiringMailToUser(entry.getKey(), entry.getValue());
                if (sent) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("发送权限临期提醒邮件失败, userName={}", entry.getKey(), e);
            }
        }
        return SSMResponseMessage.success(String.format("发送完成，成功%d人，失败%d人", successCount, failCount));
    }

    /**
     * 补全临期权限目录全路径，并过滤缓存中不存在或已停用的目录
     * @param expiringList 原始临期权限列表
     * @return 有效临期权限列表
     */
    private List<UserCtgAuthExpiringRsp> enrichExpiringAuthList(List<UserCtgAuthExpiringRsp> expiringList) {
        if (CollUtil.isEmpty(expiringList)) {
            return Collections.emptyList();
        }
        return expiringList.stream()
                .map(row -> {
                    if (StrUtil.isEmpty(row.getCtgId())) {
                        return null;
                    }
                    MetaFieldCategory ctg = SSDMetaCacheManager.getCategoryById(row.getCtgId());
                    if (ctg == null || !Enabled.isTrue(ctg.getIsActive())) {
                        log.warn("权限临期提醒跳过无效目录, ctgId={}", row.getCtgId());
                        return null;
                    }
                    row.setCtgNamePath(SSDMetaCacheManager.getCtgNamePath(row.getCtgId()));
                    return row;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(UserCtgAuthExpiringRsp::getUserName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(UserCtgAuthExpiringRsp::getCtgNamePath, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * 向单个用户发送临期提醒邮件
     * @param userName 用户域账号
     * @param rowList 该用户临期权限列表
     * @return 是否发送成功
     */
    private boolean sendExpiringMailToUser(String userName, List<UserCtgAuthExpiringRsp> rowList) {
        String receiverMail = userName + "@example.com";
        List<String> validReceiverMailList =
                (List<String>) dao.queryObjectList("user.queryValidUserEmailList", Collections.singletonList(receiverMail));
        if (CollUtil.isEmpty(validReceiverMailList)) {
            log.warn("权限临期提醒邮件收件人无效, userName={}, mail={}", userName, receiverMail);
            return false;
        }

        String subject = "【多维分析】权限临期提醒";
        String htmlContent = buildExpiringMailHtml(userName, rowList);
        BIUtil.sendMail(subject, htmlContent, validReceiverMailList);
        return true;
    }

    /**
     * 拼接临期提醒邮件 HTML 正文
     * @param userName 用户域账号
     * @param rowList 临期权限列表
     * @return HTML 正文
     */
    private String buildExpiringMailHtml(String userName, List<UserCtgAuthExpiringRsp> rowList) {
        String displayName = userName;
        String applyUrl = SC.v("ssm.auth.apply.url");
        StringBuilder html = new StringBuilder();
        html.append("<p>").append(displayName).append(":你好 ! </p>");
        html.append("<p>以下多维分析数据集模块权限即将过期，过期后无法访问，可通过「多维分析」底部权限申请入口申请权限续期。</p>");
        html.append("<p>申请链接："+applyUrl+"</p>");

        String cellStyle = "border:1px solid #804040;padding:6px 10px;";
        html.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #804040;\">");
        html.append("<tr>");
        html.append("<th style=\"").append(cellStyle).append("\">数据集名称</th>");
        html.append("<th style=\"").append(cellStyle).append("\">模块名称(全路径)</th>");
        html.append("<th style=\"").append(cellStyle).append("\">临期时长(天)</th>");
        html.append("<th style=\"").append(cellStyle).append("\">到期时间</th>");
        html.append("</tr>");
        for (UserCtgAuthExpiringRsp row : rowList) {
            String datasetNames = resolveDatasetNames(row.getCtgId());
            html.append("<tr>");
            html.append("<td style=\"").append(cellStyle).append("\">").append(nullToEmpty(datasetNames)).append("</td>");
            html.append("<td style=\"").append(cellStyle).append("\">").append(nullToEmpty(row.getCtgNamePath())).append("</td>");
            html.append("<td style=\"").append(cellStyle).append("\">").append(row.getExpireDay() == null ? "" : row.getExpireDay()).append("</td>");
            html.append("<td style=\"").append(cellStyle).append("\">").append(nullToEmpty(row.getAuthEndDate())).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    /**
     * 根据目录ID从缓存解析数据集名称
     * @param ctgId 目录ID
     * @return 数据集名称，异常时返回空串
     */
    private String resolveDatasetNames(String ctgId) {
        if (StrUtil.isEmpty(ctgId)) {
            log.warn("权限临期提醒解析数据集失败, ctgId为空");
            return "";
        }
        MetaFieldCategory ctg = SSDMetaCacheManager.getCategoryById(ctgId);
        if (ctg == null) {
            log.warn("权限临期提醒解析数据集失败, 缓存未找到目录, ctgId={}", ctgId);
            return "";
        }
        if (BIUtil.isEmpty(ctg.getDatasetId())) {
            log.warn("权限临期提醒解析数据集失败, datasetId为空, ctgId={}", ctgId);
            return "";
        }

        MetaDataset dataset = SSDMetaCacheManager.getDataset(ctg.getDatasetId());
        if (dataset == null) {
            log.warn("权限临期提醒解析数据集失败, 缓存未找到数据集, ctgId={}, datasetId={}", ctgId, ctg.getDatasetId());
            return "";
        }
        return StrUtil.emptyToDefault(dataset.getDatasetName(), "");
    }

    /**
     * 空值转空串，避免邮件表格出现 null
     * @param value 原始值
     * @return 非空字符串
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 刷新缓存
     */
    public void refreshSSMCache() {
        Map<String, Object> rmiParamMap = new HashMap<>();
        rmiParamMap.put(SystemInitializer.INIT_MODULE_KEY, InitializableModule.SSD.toString());
        rmiParamMap.put("cacheFlushType", CacheFlushType.FLUSH_ALL_NEW_MGP.getCode());
        RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, rmiParamMap);
    }

}
