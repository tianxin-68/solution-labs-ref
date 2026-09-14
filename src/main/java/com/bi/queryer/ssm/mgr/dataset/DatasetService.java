package com.bi.queryer.ssm.mgr.dataset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.DataAuthItemType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.MetaFieldDataAuth;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSMDataset;
import com.bi.queryer.ssm.meta.SSMDatasetCtgRel;
import com.bi.queryer.ssm.mgr.dataset.constant.DatasetConstant;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetAddReq;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetRsq;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetUpdateReq;
import com.bi.queryer.ssm.mgr.dataset.model.SSMDatasetAuthDim;
import com.bi.queryer.ssm.mgr.fieldCtg.model.DatasetCtgDataAuthRsq;
import com.bi.queryer.ssm.mgr.fieldCtg.model.DatasetCtgInheritEntity;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.query.template.model.TemplateAddReq;
import com.bi.queryer.ssm.query.template.model.TemplateEntity;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  13:48
 * @Description: 数据集管理-服务实现
 */
@Service
public class DatasetService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private QueryFieldService queryFieldService;

    /**
     * 数据集新增
     * @param datasetAddReq
     * @return
     */
    public SSMResponseMessage<String> add(DatasetAddReq datasetAddReq){

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

        String datasetId = datasetAddReq.getDatasetId();
        if(StrUtil.isEmpty(datasetId)) {
            datasetId = Guid.id();
        }

        SSMDataset ssmDataset = new SSMDataset();
        ssmDataset.setDatasetId(datasetId);
        ssmDataset.setDatasetName(datasetAddReq.getDatasetName());
        ssmDataset.setRptDevOwner(datasetAddReq.getRptDevOwner());
        ssmDataset.setDataDevOwner(datasetAddReq.getDataDevOwner());
        ssmDataset.setDataDate(datasetAddReq.getDataDate());
        ssmDataset.setDataDesc(datasetAddReq.getDataDesc());
        ssmDataset.setDatasetType(datasetAddReq.getDatasetType());
        ssmDataset.setIsActive(datasetAddReq.getIsActive());
        ssmDataset.setIsStandardDataset(datasetAddReq.getIsStandardDataset());

        if(datasetAddReq.getSortId() == null){
            //获取当前最大的排序id
            Double sortId = (Double)dao.queryObject("ssm.dataset.queryMaxSortId",null,dataEnvDataSourceType);
            ssmDataset.setSortId(sortId+1);
        }else{
            ssmDataset.setSortId(datasetAddReq.getSortId());
        }

        String userName = UserManager.get().getName();
        ssmDataset.setCreatedBy(userName);

        dao.insert("ssm.dataset.add", ssmDataset,dataEnvDataSourceType);


        //新数据集默认继承
        DatasetCtgInheritEntity datasetCtgInheritEntity = new DatasetCtgInheritEntity();
        datasetCtgInheritEntity.setItemType(DataAuthItemType.DATASET.getCode());
        datasetCtgInheritEntity.setItemValue(ssmDataset.getDatasetId());
        datasetCtgInheritEntity.setIsInherited(Enabled.YES.getId());
        datasetCtgInheritEntity.setCreatedBy(userName);
        dao.insert("ssm.dataset.ctg.inherit.add", datasetCtgInheritEntity,dataEnvDataSourceType);

        return SSMResponseMessage.success("",datasetId);
    }

    /**
     * 数据集修改
     * @param datasetUpdateReq
     * @return
     */
    public SSMResponseMessage<String> update(DatasetUpdateReq datasetUpdateReq){

        String datasetId = datasetUpdateReq.getDatasetId();
        SSMDataset ssmDataset = new SSMDataset();
        ssmDataset.setDatasetId(datasetId);
        ssmDataset.setDatasetName(datasetUpdateReq.getDatasetName());
        ssmDataset.setRptDevOwner(datasetUpdateReq.getRptDevOwner());
        ssmDataset.setDataDevOwner(datasetUpdateReq.getDataDevOwner());
        ssmDataset.setDataDate(datasetUpdateReq.getDataDate());
        ssmDataset.setDataDesc(datasetUpdateReq.getDataDesc());
        ssmDataset.setSortId(datasetUpdateReq.getSortId());
        ssmDataset.setUpdatedBy(UserManager.get().getName());
        ssmDataset.setIsActive(datasetUpdateReq.getIsActive());
        ssmDataset.setDatasetType(datasetUpdateReq.getDatasetType());
        ssmDataset.setIsStandardDataset(datasetUpdateReq.getIsStandardDataset());

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        dao.update("ssm.dataset.update", ssmDataset,dataEnvDataSourceType);

        return SSMResponseMessage.success("",datasetId);
    }

    /**
     * 数据集获取(用于前端查看)
     * @param datasetId
     * @return
     */
    public SSMResponseMessage<DatasetRsq> get(String datasetId) {

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        SSMDataset ssmDataset = (SSMDataset) dao.queryObject("ssm.dataset.getById", datasetId, dataEnvDataSourceType);
        DatasetRsq datasetRsq = new DatasetRsq(ssmDataset);

        return SSMResponseMessage.success("", datasetRsq);
    }

    /**
     * 数据集删除
     * @param datasetId
     * @return
     */
    public SSMResponseMessage deleteById(String datasetId) {

        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();

        //查询数据集下有没有目录
        Integer datasetCtgCount = (Integer) dao.queryObject("ssm.dataset.getDatasetCtgCount", datasetId,dataEnvDataSourceType);
        if (datasetCtgCount > 0) {
            return SSMResponseMessage.operationFailed("该数据集下还存在目录，无法删除！");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("datasetId", datasetId);
        map.put("updatedBy", UserManager.get().getName());

        DatasetCtgInheritEntity datasetCtgInheritEntity = new DatasetCtgInheritEntity();
        datasetCtgInheritEntity.setItemType(DataAuthItemType.DATASET.getCode());
        datasetCtgInheritEntity.setItemValue(datasetId);

        dao.executeTranscation(dataEnvDataSourceType, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.dataset.delete", map,dataEnvDataSourceType);
                dao.delete("ssm.dataset.ctg.inherit.delete",datasetCtgInheritEntity,dataEnvDataSourceType);
            }
        });

        return SSMResponseMessage.success("删除成功");
    }

    public List<DatasetRsq> getDatasetList(DataSourceType dataSourceType) {
        return getDatasetList(dataSourceType, true);
    }

    /**
     * 获取数据集列表
     * @param dataSourceType 数据源类型
     * @param checkAuth      是否判断权限；mgpDataSourceType 默认都有权限，传 false 跳过鉴权逻辑
     */
    public List<DatasetRsq> getDatasetList(DataSourceType dataSourceType, boolean checkAuth) {

        List<DatasetRsq> result = new ArrayList<>();
        String userName = UserManager.get().getName();

        result = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.list", null, dataSourceType);

        // 不需要判断权限时，直接标记所有数据集为有权限
        if (!checkAuth) {
            for (DatasetRsq datasetRsq : result) {
                datasetRsq.setHasAuth(Enabled.YES.getId());
            }
            return result;
        }

        //判断是否有权限
        if (CollUtil.isNotEmpty(result)) {

            String CtgAuthDim = SSDUtil.Ctg_Auth_Dim;

            // 目录权限列表
            List<String> aclCtgList = queryFieldService.getAuthCtgList(userName, SSDUtil.Ctg_Auth_Module, CtgAuthDim);
            List<SSMDatasetCtgRel> ssmDatasetCtgRelList = (List<SSMDatasetCtgRel>) dao.queryObjectList("ssm.dataset.queryAllDatasetCtgRel", null, dataSourceType);

            Map<String, List<String>> datasetCtgMap = new HashMap<>();
            for (SSMDatasetCtgRel datasetCtgRel : ssmDatasetCtgRelList) {
                List<String> ctgList = datasetCtgMap.get(datasetCtgRel.getDatasetId());
                if (ctgList == null) {
                    ctgList = new ArrayList<>();
                }
                ctgList.add(datasetCtgRel.getCtgId());
                datasetCtgMap.put(datasetCtgRel.getDatasetId(), ctgList);
            }

            User user = UserManager.get();
            Map<String, Object> map = new HashMap<>();
            map.put("userName", user.getName());
            map.put("deptId", user.getDeptId());

            List<String> hasAuthDatasetIds = new ArrayList<>();
            List<DatasetCtgDataAuthRsq> dataAuthRsqList = (List<DatasetCtgDataAuthRsq>) dao.queryObjectList("ssm.data.auth.dataset.ctg.selectUserAuth", map, dataSourceType);
            if (CollUtil.isNotEmpty(dataAuthRsqList)) {
                for (DatasetCtgDataAuthRsq dataAuthRsq : dataAuthRsqList) {
                    DataAuthItemType dataAuthItemType = DataAuthItemType.get(dataAuthRsq.getItemType());
                    switch (dataAuthItemType) {
                        case ALL:
                            List<String> inheritedDatasetIds = (List<String>) dao.queryObjectList("ssm.dataset.ctg.inherit.queryInheritedDatasetIds", null, dataSourceType);
                            if (CollUtil.isNotEmpty(inheritedDatasetIds)) {
                                hasAuthDatasetIds.addAll(inheritedDatasetIds);
                            }
                            break;
                        case DATASET:
                            hasAuthDatasetIds.add(dataAuthRsq.getItemValue());
                            break;
                        case CTG:
                            aclCtgList.add(dataAuthRsq.getItemValue());
                            break;
                    }
                }
            }

            for (DatasetRsq datasetRsq : result) {

                if (hasAuthDatasetIds.contains(datasetRsq.getDatasetId())) {
                    datasetRsq.setHasAuth(Enabled.YES.getId());
                    continue;
                }

                List<String> ctgList = datasetCtgMap.get(datasetRsq.getDatasetId());
                if (CollUtil.isEmpty(ctgList)) {
                    continue;
                }

                //比较两个集合中是否有相同的元素；当两个集合中没有相同元素时返回true，当有相同元素时返回false。
                if (Collections.disjoint(ctgList, aclCtgList)) {
                    continue;
                }

                datasetRsq.setHasAuth(Enabled.YES.getId());
            }

        }

        return result;
    }

    /**
     * 数据集列表
     * @return
     */
    public SSMResponseMessage<List<DatasetRsq>> list() {
        return SSMResponseMessage.success("", loadActiveDatasetList());
    }

    /**
     * 工单权限申请场景的数据集列表：在 {@link #list()} 结果基础上排除配置项中的 datasetId。
     *
     * @return 过滤后的数据集列表
     */
    public SSMResponseMessage<List<DatasetRsq>> listForAuthApply() {
        List<DatasetRsq> result = filterWorkOrderAuthApplyExcludedDatasets(loadActiveDatasetList());
        return SSMResponseMessage.success("", result);
    }

    /**
     * 加载可用数据集列表（含权限标记），合并 Default 与 MGP 数据源。
     */
    private List<DatasetRsq> loadActiveDatasetList() {
        List<DatasetRsq> result = new ArrayList<>();

        DataSourceType dataEnvDataSourceType = DataSourceType.Default;
        result.addAll(getDatasetList(dataEnvDataSourceType));

        //添加新多维数据集，mgpDataSourceType 默认都有权限，不需要判断权限
        DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
        List<DatasetRsq> mgpDatasetResult = getDatasetList(mgpDataSourceType, false);
        result.addAll(mgpDatasetResult);

        return result;
    }

    /**
     * 读取工单权限申请场景需排除的数据集 id 配置。
     */
    private Set<String> loadWorkOrderAuthApplyExcludeDatasetIds() {
        String config = SC.v(DatasetConstant.WORKORDER_AUTH_APPLY_EXCLUDE_DATASET_IDS_KEY, "");
        if (StrUtil.isBlank(config)) {
            return Collections.emptySet();
        }
        return Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 排除工单权限申请场景配置的数据集 id。
     */
    private List<DatasetRsq> filterWorkOrderAuthApplyExcludedDatasets(List<DatasetRsq> datasets) {
        if (CollUtil.isEmpty(datasets)) {
            return datasets;
        }
        Set<String> excludeIds = loadWorkOrderAuthApplyExcludeDatasetIds();
        if (CollUtil.isEmpty(excludeIds)) {
            return datasets;
        }
        List<DatasetRsq> filtered = new ArrayList<>();
        for (DatasetRsq dataset : datasets) {
            if (dataset == null || StrUtil.isBlank(dataset.getDatasetId())) {
                continue;
            }
            if (!excludeIds.contains(dataset.getDatasetId())) {
                filtered.add(dataset);
            }
        }
        return filtered;
    }

    /**
     * 获取所有数据集列表
     * @return
     */
    public List<DatasetRsq> listAll() {

        List<DatasetRsq> result = new ArrayList<>();

        result = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null, DataSourceType.Default);

        DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
        List<DatasetRsq> mgpDatasetResult = (List<DatasetRsq>) dao.queryObjectList("ssm.dataset.listAll", null, mgpDataSourceType);
        if (CollUtil.isNotEmpty(mgpDatasetResult)) {
            result.addAll(mgpDatasetResult);
        }

        return result;
    }

    /**
     * 通过模板id获取数据集
     * @param tplId
     * @return
     */
    public SSMResponseMessage<DatasetRsq> getDatasetByTplId(String tplId) {
        DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();

        String datasetId = SC.v("ssm.default.datasetId", "");
        if (StrUtil.isNotEmpty(tplId)) {
            TemplateAddReq templateAddReq = new TemplateAddReq();
            templateAddReq.setTplId(tplId);
            TemplateEntity templateEntity = (TemplateEntity) dao.queryObject("ssm.template.queryTemplateEntityById", templateAddReq);
            if (templateEntity != null && StrUtil.isNotEmpty(templateEntity.getDatasetId())) {
                datasetId = templateEntity.getDatasetId();
            }
        }

        SSMDataset ssmDataset = (SSMDataset) dao.queryObject("ssm.dataset.getById", datasetId, DataSourceType.Default);

        //没查到，从新多维查询
        if (ssmDataset == null) {
            ssmDataset = (SSMDataset) dao.queryObject("ssm.dataset.getById", datasetId, mgpDataSourceType);
        }

        DatasetRsq datasetRsq = new DatasetRsq(ssmDataset);

        return SSMResponseMessage.success("", datasetRsq);
    }

    /**
     * 数据集获取
     * @param datasetId
     * @return
     */
    public SSMDataset getById(String datasetId){
        DataSourceType dataEnvDataSourceType = SSDUtil.getDataEnvDataSourceType();
        SSMDataset ssmDataset = (SSMDataset)dao.queryObject("ssm.dataset.getById",datasetId,dataEnvDataSourceType);

        DataSourceType mgpDataSourceType = SSDUtil.getMgpDataSourceType();
        if (ssmDataset == null) {
            ssmDataset = (SSMDataset) dao.queryObject("ssm.dataset.getById", datasetId, mgpDataSourceType);
        }
        return ssmDataset;
    }

    /**
     * 同步数据集与行级权限维度的关系（全量重建）
     * <p>
     * 按数据集分组前台目录，汇总目录关联的物理表并展开全部字段，
     * 按 datasetId + moduleCode + dimCode 去重后写入 ssm_dataset_auth_dim。
     * 无 tableId 的虚拟/计算字段不处理。入库时仅写入默认数据源（DataSourceType.Default）。
     * </p>
     */
    public SSMResponseMessage syncDatasetAuthDim() {
        Map<String, MetaFieldCategory> frontCategories = SSDMetaCacheManager.getFrontCategories();
        Map<String, SSMDatasetAuthDim> dedupMap = new LinkedHashMap<>();
        String userName = UserManager.get().getName();

        // 按数据集分组目录
        Map<String, List<MetaFieldCategory>> datasetCategoryMap = new LinkedHashMap<>();
        for (MetaFieldCategory category : frontCategories.values()) {
            if (StrUtil.isEmpty(category.getDatasetId())) {
                continue;
            }
            datasetCategoryMap.computeIfAbsent(category.getDatasetId(), k -> new ArrayList<>()).add(category);
        }

        for (Map.Entry<String, List<MetaFieldCategory>> entry : datasetCategoryMap.entrySet()) {
            String datasetId = entry.getKey();
            Set<String> tableIdSet = collectTableIdsFromCategories(entry.getValue());
            collectDatasetAuthDim(datasetId, tableIdSet, dedupMap, userName);
        }

        List<SSMDatasetAuthDim> entities = new ArrayList<>(dedupMap.values());
        persistDatasetAuthDim(entities, DataSourceType.Default);

        return SSMResponseMessage.success("同步数据集行级权限维度成功，共 " + entities.size() + " 条");
    }

    /**
     * 汇总目录下关联的物理表 id（dimTableId 优先，否则 factTableId）
     *
     * @param categories 目录列表
     * @return 去重后的表 id 集合
     */
    private Set<String> collectTableIdsFromCategories(List<MetaFieldCategory> categories) {
        Set<String> tableIdSet = new LinkedHashSet<>();
        for (MetaFieldCategory category : categories) {
            if (CollUtil.isEmpty(category.getFields())) {
                continue;
            }
            for (MetaField categoryField : category.getFields()) {
                String tableId = categoryField.getTableId();
                if (StrUtil.isNotEmpty(tableId)) {
                    tableIdSet.add(tableId);
                }
            }
        }
        return tableIdSet;
    }

    /**
     * 按表展开字段并收集数据集行级权限维度
     *
     * @param datasetId  数据集 id
     * @param tableIdSet 表 id 集合
     * @param dedupMap   去重索引
     * @param userName   当前操作用户
     */
    private void collectDatasetAuthDim(String datasetId, Set<String> tableIdSet,
                                       Map<String, SSMDatasetAuthDim> dedupMap, String userName) {
        for (String tableId : tableIdSet) {
            List<MetaField> tableFields = SSDMetaCacheManager.getTableFields(tableId);
            if (CollUtil.isEmpty(tableFields)) {
                continue;
            }
            for (MetaField field : tableFields) {
                String dimCode = field.getDataAuthDimCode();
                if (StrUtil.isEmpty(dimCode)) {
                    continue;
                }
                String moduleCode = field.getDataAuthModuleCode();
                String key = datasetId + "|" + moduleCode + "|" + dimCode;
                dedupMap.computeIfAbsent(key, k -> {
                    SSMDatasetAuthDim entity = new SSMDatasetAuthDim();
                    entity.setDatasetId(datasetId);
                    entity.setModuleCode(moduleCode);
                    entity.setDimCode(dimCode);
                    entity.setIsApply(field.getDataAuthIsApply());
                    entity.setCreatedBy(userName);
                    // 从元数据缓存获取权限模块/维度展示名称
                    List<MetaFieldDataAuth> authList = SSDMetaCacheManager.getMetaFieldDataAuthCfg(field.getCode());
                    MetaFieldDataAuth matched = CollUtil.isEmpty(authList) ? null : authList.stream()
                            .filter(a -> Objects.equals(a.getModuleCode(), moduleCode) && Objects.equals(a.getDimCode(), dimCode))
                            .findFirst().orElse(authList.get(0));
                    entity.setModuleName(matched == null ? null : matched.getModuleName());
                    entity.setDimName(matched == null ? null : matched.getDimName());
                    return entity;
                });
            }
        }
    }

    /**
     * 全量重建指定数据源下的数据集行级权限维度
     */
    private void persistDatasetAuthDim(List<SSMDatasetAuthDim> entities, DataSourceType dataSourceType) {
        dao.executeTranscation(dataSourceType, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.dataset.deleteAllDatasetAuthDim", null, dataSourceType);
                if (CollUtil.isNotEmpty(entities)) {
                    dao.insert("ssm.dataset.batchInsertDatasetAuthDim", entities, dataSourceType);
                }
            }
        });
    }

    /**
     * 根据数据集id获取行级权限维度配置
     *
     * @param datasetId 数据集id
     * @return 行级权限维度列表（moduleCode + dimCode 配套）
     */
    public SSMResponseMessage<List<SSMDatasetAuthDim>> getDatasetAuthDim(String datasetId) {
        if (StrUtil.isEmpty(datasetId)) {
            return SSMResponseMessage.success("", new ArrayList<>());
        }
        List<SSMDatasetAuthDim> list = (List<SSMDatasetAuthDim>) dao.queryObjectList(
                "ssm.dataset.queryDatasetAuthDimByDatasetId", datasetId, DataSourceType.Default);
        return SSMResponseMessage.success("", list);
    }
}
