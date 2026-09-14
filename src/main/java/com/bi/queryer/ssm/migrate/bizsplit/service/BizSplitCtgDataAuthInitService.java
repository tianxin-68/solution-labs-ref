package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.OwnerType;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.migrate.bizsplit.constant.BizSplitCtgDataAuthConstant;
import com.bi.queryer.ssm.migrate.bizsplit.constant.BizSplitDataAuthConstant;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitCtgAuthSnapshot;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitCtgDataAuthInitReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitCtgSplitMapping;
import com.bi.queryer.ssm.migrate.bizsplit.model.DatasetCtgAuthWriteEntity;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionOwnerUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.dim.vo.DataAuth;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.model.HrEmployee;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.UpperCaseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多维目录权限初始化服务（方法2）。
 *
 * 业务拆分迁移场景：将旧目录 id 下的权限复制到新目录 id，
 * 分别写入 Portal bi_portal.sys_data_auth 与 OLAP ssm_data_auth_dataset_ctg。
 */
@Service
@Scope("prototype")
public class BizSplitCtgDataAuthInitService {

    private static final Logger log = LoggerFactory.getLogger(BizSplitCtgDataAuthInitService.class);

    @Autowired
    private BaseDao dao;

    /**
     * 多维目录权限初始化（业务拆分迁移专用，方法2）。
     *
     * 入参：新目录 id、旧目录 id、排除部门 id 集合。
     *
     * 子目录：从多维 ssd_field_ctg 获取模块下直接子目录 old_ctg_id（不递归多级），
     * 再按 old_ctg_id 查询 mgp_ctg_biz_line_split_mapping 映射并一并处理。
     *
     * 权限来源一：bi_portal.sys_data_auth
     *   - owner_type=User（不处理 Dept），module_code=ssd_ctg，dim_code=ssm_ctg
     *   - item_code=旧目录 id，且记录在有效期内
     *
     * 权限来源二：ssm_data_auth_dataset_ctg
     *   - item_value=旧目录 id，owner_type 含 user/dept
     *
     * 过滤规则：
     *   - 用户主体：HR 部门路径命中 excludeDeptIds 任一部门则跳过
     *   - 组织主体：owner_id 命中 excludeDeptIds 则跳过
     *   - excludeDeptIds 须传完整部门 id 列表，子部门也需逐一传入，不会根据父部门自动展开
     *
     * 写入规则：
     *   - 重跑前先按 newCtgId 删除本工具已写入的 Portal/数据集/MGP 权限，再插入过滤后的结果
     *   - Portal：item_code/item_value 替换为新目录 id/名称，remark=bizsplit_init:ctg
     *   - 数据集侧：item_value 替换为新目录 id，写入 ssm_data_auth_dataset_ctg（OLAP）
     *   - MGP 侧：同步写入 mgp_app_module_ctg_auth（SC.v 配置的 MGP 数据源）
     *   - 按 owner + item 去重后分批写入
     *
     * @param req 新/旧目录 id 及排除部门集合
     */
    public void initCtgDataAuth(BizSplitCtgDataAuthInitReq req) {
        // 1. 校验入参
        BizSplitCtgDataAuthInitReq safeReq = validateReq(req);

        // 2. 加载 Portal 维度配置、排除部门集合，组装模块 + 子目录映射对
        CtgDimConfig dimConfig = loadCtgDimConfig();
        Set<String> excludeDeptIds = normalizeExcludeDeptIds(safeReq.getExcludeDeptIds());
        String operator = UserManager.get() != null ? UserManager.get().getName() : null;
        List<CtgMappingPair> mappingPairs = buildCtgMappingPairs(safeReq);

        // 3. 预加载全部映射对涉及用户的 HR 部门
        Map<String, HrEmployee> employeeByName = loadEmployeeByUserNames(collectUserOwnerIdsForMappings(mappingPairs));

        Map<String, DataAuth> portalAuthMap = new LinkedHashMap<>();
        Map<String, DatasetCtgAuthWriteEntity> datasetAuthMap = new LinkedHashMap<>();

        // 4. 逐对处理：模块 + 子目录，过滤并组装待写权限
        for (CtgMappingPair mappingPair : mappingPairs) {
            processCtgMappingAuth(mappingPair, dimConfig, excludeDeptIds, employeeByName, operator,
                    portalAuthMap, datasetAuthMap);
        }

        // 5. 先按 newCtgId 删除本工具已写入权限，再分批写入过滤后的结果
        deleteExistingCtgAuth(collectNewCtgIds(mappingPairs));
        persistPortalAuth(new ArrayList<>(portalAuthMap.values()));
        persistDatasetAuth(new ArrayList<>(datasetAuthMap.values()));
    }

    /** 汇总全部映射对的新目录 id，供重跑前清理既有授权 */
    private List<String> collectNewCtgIds(List<CtgMappingPair> mappingPairs) {
        List<String> newCtgIds = new ArrayList<>();
        if (CollUtil.isEmpty(mappingPairs)) {
            return newCtgIds;
        }
        Set<String> seen = new HashSet<>();
        for (CtgMappingPair mappingPair : mappingPairs) {
            if (mappingPair == null || StrUtil.isEmpty(mappingPair.getNewCtgId())) {
                continue;
            }
            if (seen.add(mappingPair.getNewCtgId())) {
                newCtgIds.add(mappingPair.getNewCtgId());
            }
        }
        return newCtgIds;
    }

    /**
     * 重跑前清理：按 newCtgId 删除本工具已写入的数据集 / MGP 目录权限。
     */
    private void deleteExistingCtgAuth(List<String> newCtgIds) {
        if (CollUtil.isEmpty(newCtgIds)) {
            return;
        }
        Map<String, Object> deleteParam = new HashMap<>();
        deleteParam.put("newCtgIds", newCtgIds);
        deleteParam.put("itemType", BizSplitCtgDataAuthConstant.DATASET_ITEM_TYPE_CTG);

        dao.delete("ssm.bizsplit.data.auth.deleteDatasetCtgAuthByNewCtgIds", deleteParam, DataSourceType.OLAP);

        DataSourceType mgpDataSourceType = resolveMgpDataSourceType();
        if (mgpDataSourceType != null) {
            dao.delete("ssm.bizsplit.data.auth.deleteMgpAppModuleCtgAuthByNewCtgIds", deleteParam, mgpDataSourceType);
            log.info("bizsplit ctg auth init deleted existing rows for {} new ctg ids on dataset/mgp",
                    newCtgIds.size());
            return;
        }
        log.info("bizsplit ctg auth init deleted existing rows for {} new ctg ids on dataset",
                newCtgIds.size());
    }

    /**
     * 组装待处理的目录映射对：入参模块 + 子目录（多维取 old_ctg_id，映射表取 new_ctg_id）。
     */
    private List<CtgMappingPair> buildCtgMappingPairs(BizSplitCtgDataAuthInitReq req) {
        List<CtgMappingPair> mappingPairs = new ArrayList<>();
        mappingPairs.add(new CtgMappingPair(req.getOldCtgId(), req.getNewCtgId(), loadCtgName(req.getNewCtgId())));

        List<BizSplitCtgSplitMapping> childMappings = listChildCtgSplitMappings(req.getOldCtgId());
        if (CollUtil.isEmpty(childMappings)) {
            log.info("bizsplit ctg auth init no child mapping for module {}", req.getOldCtgId());
            return mappingPairs;
        }
        for (BizSplitCtgSplitMapping childMapping : childMappings) {
            if (childMapping == null || StrUtil.isEmpty(childMapping.getOldCtgId())
                    || StrUtil.isEmpty(childMapping.getNewCtgId())) {
                continue;
            }
            mappingPairs.add(new CtgMappingPair(childMapping.getOldCtgId(), childMapping.getNewCtgId(),
                    childMapping.getNewCtgName()));
        }
        log.info("bizsplit ctg auth init child mapping count {} for module {}",
                childMappings.size(), req.getOldCtgId());
        return mappingPairs;
    }

    /**
     * 查询模块下子目录拆分映射。
     * 1. 从多维 OLAP 查询 parent_ctg_id = 模块 old_ctg_id 的子目录 id 集合
     * 2. 按 old_ctg_id 批量查询 bi_mgp.mgp_ctg_biz_line_split_mapping
     */
    private List<BizSplitCtgSplitMapping> listChildCtgSplitMappings(String moduleOldCtgId) {
        List<String> childOldCtgIds = listChildOldCtgIdsFromOlap(moduleOldCtgId);
        if (CollUtil.isEmpty(childOldCtgIds)) {
            log.info("bizsplit ctg auth init no child ctg under module {}", moduleOldCtgId);
            return new ArrayList<>();
        }
        List<BizSplitCtgSplitMapping> mappingList = (List<BizSplitCtgSplitMapping>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listCtgSplitMappingByOldCtgIds", childOldCtgIds, DataSourceType.Data_Studio);
        return mappingList == null ? new ArrayList<>() : mappingList;
    }

    /** 从多维 OLAP 查询模块下子目录 old_ctg_id 集合 */
    private List<String> listChildOldCtgIdsFromOlap(String moduleOldCtgId) {
        List<MetaFieldCategory> childCategories = (List<MetaFieldCategory>) dao.queryObjectList(
                "fieldCtg.queryChildFieldCtgByParentId", moduleOldCtgId, DataSourceType.OLAP);
        List<String> oldCtgIds = new ArrayList<>();
        if (CollUtil.isEmpty(childCategories)) {
            return oldCtgIds;
        }
        for (MetaFieldCategory category : childCategories) {
            if (category != null && StrUtil.isNotEmpty(category.getId())) {
                oldCtgIds.add(category.getId());
            }
        }
        return oldCtgIds;
    }

    /** 汇总全部映射对权限快照中的用户 owner，供批量查 HR 部门 */
    private Set<String> collectUserOwnerIdsForMappings(List<CtgMappingPair> mappingPairs) {
        Set<String> userNames = new HashSet<>();
        if (CollUtil.isEmpty(mappingPairs)) {
            return userNames;
        }
        for (CtgMappingPair mappingPair : mappingPairs) {
            appendUserOwnerIds(userNames, listPortalSnapshots(mappingPair.getOldCtgId()));
            appendUserOwnerIds(userNames, listDatasetSnapshots(mappingPair.getOldCtgId()));
        }
        return userNames;
    }

    /** 处理单对 old → new 目录的权限复制与组装 */
    private void processCtgMappingAuth(CtgMappingPair mappingPair,
                                       CtgDimConfig dimConfig,
                                       Set<String> excludeDeptIds,
                                       Map<String, HrEmployee> employeeByName,
                                       String operator,
                                       Map<String, DataAuth> portalAuthMap,
                                       Map<String, DatasetCtgAuthWriteEntity> datasetAuthMap) {
        String newCtgName = StrUtil.isNotEmpty(mappingPair.getNewCtgName())
                ? mappingPair.getNewCtgName()
                : loadCtgName(mappingPair.getNewCtgId());
        List<BizSplitCtgAuthSnapshot> portalSnapshots = listPortalSnapshots(mappingPair.getOldCtgId());
        List<BizSplitCtgAuthSnapshot> datasetSnapshots = listDatasetSnapshots(mappingPair.getOldCtgId());
        collectPortalAuth(portalSnapshots, excludeDeptIds, employeeByName, dimConfig,
                mappingPair.getNewCtgId(), newCtgName, operator, portalAuthMap);
        collectDatasetAuth(datasetSnapshots, excludeDeptIds, employeeByName,
                mappingPair.getNewCtgId(), operator, datasetAuthMap);
    }

    /** 校验新/旧目录 id 必填且不能相同 */
    private BizSplitCtgDataAuthInitReq validateReq(BizSplitCtgDataAuthInitReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (StrUtil.isEmpty(req.getNewCtgId())) {
            throw new IllegalArgumentException("新目录 id 不能为空");
        }
        if (StrUtil.isEmpty(req.getOldCtgId())) {
            throw new IllegalArgumentException("旧目录 id 不能为空");
        }
        if (req.getNewCtgId().equals(req.getOldCtgId())) {
            throw new IllegalArgumentException("新目录 id 与旧目录 id 不能相同");
        }
        return req;
    }

    /** 清洗排除部门 id，去空后返回 Set */
    private Set<String> normalizeExcludeDeptIds(List<String> excludeDeptIds) {
        Set<String> result = new HashSet<>();
        if (CollUtil.isEmpty(excludeDeptIds)) {
            return result;
        }
        for (String deptId : excludeDeptIds) {
            if (StrUtil.isNotEmpty(deptId)) {
                result.add(deptId);
            }
        }
        return result;
    }

    /** 查询 Portal 侧旧目录 User 级权限（有效期内） */
    private List<BizSplitCtgAuthSnapshot> listPortalSnapshots(String oldCtgId) {
        List<BizSplitCtgAuthSnapshot> snapshotList = (List<BizSplitCtgAuthSnapshot>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listPortalUserCtgAuthByOldCtgId", oldCtgId);
        return snapshotList == null ? new ArrayList<>() : snapshotList;
    }

    /** 查询数据集侧旧目录权限（OLAP 数据源） */
    private List<BizSplitCtgAuthSnapshot> listDatasetSnapshots(String oldCtgId) {
        List<BizSplitCtgAuthSnapshot> snapshotList = (List<BizSplitCtgAuthSnapshot>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listDatasetCtgAuthByOldItemValue", oldCtgId, DataSourceType.OLAP);
        return snapshotList == null ? new ArrayList<>() : snapshotList;
    }

    /** 从 OLAP 数据源查询新目录名称 */
    private String loadCtgName(String ctgId) {
        MetaFieldCategory category = (MetaFieldCategory) dao.queryObject(
                "fieldCtg.queryFieldCtgById", ctgId, DataSourceType.OLAP);
        if (category == null || StrUtil.isEmpty(category.getName())) {
            throw new IllegalStateException("未找到目录信息：" + ctgId);
        }
        return category.getName();
    }

    /** 加载 ssd_ctg / ssm_ctg 维度配置，用于 Portal 写入 */
    private CtgDimConfig loadCtgDimConfig() {
        UpperCaseMap config = (UpperCaseMap) dao.queryObject("ssm.bizsplit.data.auth.queryCtgDimConfig", null);
        if (config == null) {
            throw new IllegalStateException("未找到 ssd_ctg / ssm_ctg 维度配置");
        }
        CtgDimConfig dimConfig = new CtgDimConfig();
        dimConfig.setModuleCode(String.valueOf(config.get("moduleCode")));
        dimConfig.setModuleName(String.valueOf(config.get("moduleName")));
        dimConfig.setDimCode(String.valueOf(config.get("dimCode")));
        dimConfig.setDimName(String.valueOf(config.get("dimName")));
        dimConfig.setDimDisplayPrefix("【" + dimConfig.getModuleName() + "/" + dimConfig.getDimName() + "】");
        return dimConfig;
    }

    private void appendUserOwnerIds(Set<String> userNames, List<BizSplitCtgAuthSnapshot> snapshots) {
        if (CollUtil.isEmpty(snapshots)) {
            return;
        }
        for (BizSplitCtgAuthSnapshot snapshot : snapshots) {
            if (snapshot == null || !isUserOwnerType(snapshot.getOwnerType())) {
                continue;
            }
            if (StrUtil.isNotEmpty(snapshot.getOwnerId())) {
                userNames.add(snapshot.getOwnerId());
            }
        }
    }

    private Map<String, HrEmployee> loadEmployeeByUserNames(Set<String> userNames) {
        Map<String, HrEmployee> employeeByName = new HashMap<>();
        if (CollUtil.isEmpty(userNames)) {
            return employeeByName;
        }
        List<HrEmployee> employees = (List<HrEmployee>) dao.queryObjectList(
                "user.listEmployeeDeptByUserNames", new ArrayList<>(userNames));
        if (CollUtil.isEmpty(employees)) {
            return employeeByName;
        }
        for (HrEmployee employee : employees) {
            if (employee != null && StrUtil.isNotEmpty(employee.getUserName())) {
                employeeByName.put(employee.getUserName(), employee);
            }
        }
        return employeeByName;
    }

    /**
     * 组装 Portal 待写权限。
     * 保留原 created_by、active_duration_days，item 替换为新目录 id/名称。
     */
    private void collectPortalAuth(List<BizSplitCtgAuthSnapshot> snapshots,
                                   Set<String> excludeDeptIds,
                                   Map<String, HrEmployee> employeeByName,
                                   CtgDimConfig dimConfig,
                                   String newCtgId,
                                   String newCtgName,
                                   String operator,
                                   Map<String, DataAuth> portalAuthMap) {
        if (CollUtil.isEmpty(snapshots)) {
            return;
        }
        for (BizSplitCtgAuthSnapshot snapshot : snapshots) {
            if (snapshot == null || StrUtil.isEmpty(snapshot.getOwnerId())) {
                continue;
            }
            if (isExcludedRecord(snapshot, excludeDeptIds, employeeByName)) {
                continue;
            }
            String createdBy = StrUtil.isNotEmpty(snapshot.getCreatedBy()) ? snapshot.getCreatedBy() : operator;
            int activeDurationDays = snapshot.getActiveDurationDays() != null
                    ? snapshot.getActiveDurationDays()
                    : BizSplitCtgDataAuthConstant.ACTIVE_DURATION_DAYS;
            DataAuth dataAuth = buildPortalDataAuth(snapshot.getOwnerId(), dimConfig, newCtgId, newCtgName,
                    createdBy, activeDurationDays);
            portalAuthMap.put(buildPortalAuthKey(dataAuth), dataAuth);
        }
    }

    /**
     * 组装数据集侧待写权限。
     * item_value 替换为新目录 id，owner_type 统一为小写 user/dept。
     */
    private void collectDatasetAuth(List<BizSplitCtgAuthSnapshot> snapshots,
                                      Set<String> excludeDeptIds,
                                      Map<String, HrEmployee> employeeByName,
                                      String newCtgId,
                                      String operator,
                                      Map<String, DatasetCtgAuthWriteEntity> datasetAuthMap) {
        if (CollUtil.isEmpty(snapshots)) {
            return;
        }
        for (BizSplitCtgAuthSnapshot snapshot : snapshots) {
            if (snapshot == null || StrUtil.isEmpty(snapshot.getOwnerId())) {
                continue;
            }
            if (isExcludedRecord(snapshot, excludeDeptIds, employeeByName)) {
                continue;
            }
            DatasetCtgAuthWriteEntity entity = new DatasetCtgAuthWriteEntity();
            entity.setOwnerId(snapshot.getOwnerId());
            entity.setOwnerType(normalizeDatasetOwnerType(snapshot.getOwnerType()));
            entity.setItemType(StrUtil.isNotEmpty(snapshot.getItemType())
                    ? snapshot.getItemType()
                    : BizSplitCtgDataAuthConstant.DATASET_ITEM_TYPE_CTG);
            entity.setItemValue(newCtgId);
            entity.setCreatedBy(StrUtil.isNotEmpty(snapshot.getCreatedBy()) ? snapshot.getCreatedBy() : operator);
            datasetAuthMap.put(buildDatasetAuthKey(entity), entity);
        }
    }

    /** 判断单条权限是否应被 excludeDeptIds 排除（用户查 HR 部门路径，组织查 owner_id 精确匹配） */
    private boolean isExcludedRecord(BizSplitCtgAuthSnapshot snapshot,
                                       Set<String> excludeDeptIds,
                                       Map<String, HrEmployee> employeeByName) {
        if (CollUtil.isEmpty(excludeDeptIds)) {
            return false;
        }
        if (isUserOwnerType(snapshot.getOwnerType())) {
            HrEmployee employee = employeeByName.get(snapshot.getOwnerId());
            return isUserInExcludedDept(employee, excludeDeptIds);
        }
        if (isDeptOwnerType(snapshot.getOwnerType())) {
            return isDeptOwnerExcluded(snapshot.getOwnerId(), excludeDeptIds);
        }
        return false;
    }

    /** 用户所属部门是否命中任一排除部门（DEPT_ID 路径包含匹配） */
    private boolean isUserInExcludedDept(HrEmployee employee, Set<String> excludeDeptIds) {
        if (employee == null || CollUtil.isEmpty(excludeDeptIds)) {
            return false;
        }
        for (String excludeDeptId : excludeDeptIds) {
            if (MetricExpansionOwnerUtil.containsDeptId(employee, excludeDeptId)) {
                return true;
            }
        }
        return false;
    }

    /** 组织主体 owner_id 是否在排除部门列表中（精确匹配，不做父子展开） */
    private boolean isDeptOwnerExcluded(String ownerDeptId, Set<String> excludeDeptIds) {
        if (StrUtil.isEmpty(ownerDeptId) || CollUtil.isEmpty(excludeDeptIds)) {
            return false;
        }
        return excludeDeptIds.contains(ownerDeptId);
    }

    private boolean isUserOwnerType(String ownerType) {
        return OwnerType.USER == OwnerType.get(ownerType)
                || BizSplitCtgDataAuthConstant.OWNER_TYPE_USER.equalsIgnoreCase(ownerType);
    }

    private boolean isDeptOwnerType(String ownerType) {
        return OwnerType.DEPT == OwnerType.get(ownerType)
                || BizSplitCtgDataAuthConstant.OWNER_TYPE_DEPT.equalsIgnoreCase(ownerType);
    }

    private String normalizeDatasetOwnerType(String ownerType) {
        if (isDeptOwnerType(ownerType)) {
            return BizSplitCtgDataAuthConstant.DATASET_OWNER_TYPE_DEPT;
        }
        return BizSplitCtgDataAuthConstant.DATASET_OWNER_TYPE_USER;
    }

    private DataAuth buildPortalDataAuth(String ownerId,
                                         CtgDimConfig dimConfig,
                                         String newCtgId,
                                         String newCtgName,
                                         String createdBy,
                                         int activeDurationDays) {
        DataAuth dataAuth = new DataAuth();
        dataAuth.setAuthId(Guid.id());
        dataAuth.setOwnerType(BizSplitCtgDataAuthConstant.OWNER_TYPE_USER);
        dataAuth.setOwnerId(ownerId);
        dataAuth.setModuleCode(dimConfig.getModuleCode());
        dataAuth.setModuleName(dimConfig.getModuleName());
        dataAuth.setDimCode(dimConfig.getDimCode());
        dataAuth.setDimName(dimConfig.getDimName());
        dataAuth.setItemCode(newCtgId);
        dataAuth.setItemValue(newCtgName);
        dataAuth.setDimDisplayName(dimConfig.getDimDisplayPrefix() + newCtgName);
        dataAuth.setRemark(BizSplitCtgDataAuthConstant.REMARK_PREFIX);
        dataAuth.setCreatedBy(createdBy);
        dataAuth.setActiveDurationDays(activeDurationDays);
        return dataAuth;
    }

    private String buildPortalAuthKey(DataAuth dataAuth) {
        return dataAuth.getOwnerType() + "|" + dataAuth.getOwnerId() + "|" + dataAuth.getItemCode();
    }

    private String buildDatasetAuthKey(DatasetCtgAuthWriteEntity entity) {
        return entity.getOwnerType() + "|" + entity.getOwnerId() + "|" + entity.getItemType() + "|" + entity.getItemValue();
    }

    /** 分批 REPLACE INTO Portal sys_data_auth */
    private void persistPortalAuth(List<DataAuth> insertList) {
        if (CollUtil.isEmpty(insertList)) {
            return;
        }
        batchReplace(DataSourceType.Default, "ssm.bizsplit.data.auth.replacePortalCtgAuth", insertList);
        log.info("bizsplit ctg auth init replaced {} portal rows", insertList.size());
    }

    /**
     * 分批 REPLACE INTO 数据集侧权限。
     * ssm_data_auth_dataset_ctg 使用 OLAP；mgp_app_module_ctg_auth 使用 SC.v 配置的 MGP 数据源。
     */
    private void persistDatasetAuth(List<DatasetCtgAuthWriteEntity> insertList) {
        if (CollUtil.isEmpty(insertList)) {
            return;
        }
        batchReplace(DataSourceType.OLAP, "ssm.bizsplit.data.auth.replaceDatasetCtgAuth", insertList);
        log.info("bizsplit ctg auth init replaced {} dataset rows", insertList.size());

        DataSourceType mgpDataSourceType = resolveMgpDataSourceType();
        if (mgpDataSourceType != null) {
            batchReplace(mgpDataSourceType, "ssm.bizsplit.data.auth.replaceMgpAppModuleCtgAuth", insertList);
            log.info("bizsplit ctg auth init replaced {} mgp_app_module_ctg_auth rows on {}", insertList.size(),
                    mgpDataSourceType.getKey());
        }
    }

    /** 从 SC.v 读取 MGP 数据源 id，未配置时返回 null */
    private DataSourceType resolveMgpDataSourceType() {
        String mgpDataSourceId = SC.v(BizSplitCtgDataAuthConstant.MGP_DATASOURCE_ID_SC_KEY, "");
        if (StrUtil.isEmpty(mgpDataSourceId)) {
            return null;
        }
        return DataSourceType.getTypeById(mgpDataSourceId);
    }

    private void batchReplace(DataSourceType dataSourceType, String sqlId, List<?> insertList) {
        int total = insertList.size();
        int batchSize = BizSplitDataAuthConstant.REPLACE_BATCH_SIZE;
        for (int from = 0; from < total; from += batchSize) {
            int to = Math.min(from + batchSize, total);
            List<?> batch = insertList.subList(from, to);
            dao.executeTranscation(dataSourceType, new AbstractTransaction() {
                @Override
                public void execute() {
                    dao.insert(sqlId, batch);
                }
            });
        }
    }

    /** 单对 old → new 目录映射（模块或子目录） */
    private static class CtgMappingPair {

        private final String oldCtgId;
        private final String newCtgId;
        private final String newCtgName;

        private CtgMappingPair(String oldCtgId, String newCtgId, String newCtgName) {
            this.oldCtgId = oldCtgId;
            this.newCtgId = newCtgId;
            this.newCtgName = newCtgName;
        }

        public String getOldCtgId() {
            return oldCtgId;
        }

        public String getNewCtgId() {
            return newCtgId;
        }

        public String getNewCtgName() {
            return newCtgName;
        }
    }

    /** Portal 维度配置及 dim_display_name 前缀 */
    private static class CtgDimConfig {

        private String moduleCode;
        private String moduleName;
        private String dimCode;
        private String dimName;
        private String dimDisplayPrefix;

        public String getModuleCode() {
            return moduleCode;
        }

        public void setModuleCode(String moduleCode) {
            this.moduleCode = moduleCode;
        }

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getDimCode() {
            return dimCode;
        }

        public void setDimCode(String dimCode) {
            this.dimCode = dimCode;
        }

        public String getDimName() {
            return dimName;
        }

        public void setDimName(String dimName) {
            this.dimName = dimName;
        }

        public String getDimDisplayPrefix() {
            return dimDisplayPrefix;
        }

        public void setDimDisplayPrefix(String dimDisplayPrefix) {
            this.dimDisplayPrefix = dimDisplayPrefix;
        }
    }
}
