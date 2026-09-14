package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.constant.BizSplitDataAuthConstant;
import com.bi.queryer.ssm.migrate.bizsplit.enums.BizSplitBusinessLineAuthExpandRule;
import com.bi.queryer.ssm.migrate.bizsplit.enums.BizSplitDeptBusinessLineAuth;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitDataAuthInitReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitUserRowBizlineAuthSnapshot;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionOwnerUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务线拆分权限初始化服务。
 *
 * 目标：为 User 写入 ssm_row / ssm_dim_bizline 行级权限至 bi_portal.sys_data_auth。
 * 入口方法 initBusinessLineRowAuth 分映射组织复制/补充、非映射组织源业务线扩展两段处理。
 */
@Service
@Scope("prototype")
public class BizSplitDataAuthInitService {

    private static final Logger log = LoggerFactory.getLogger(BizSplitDataAuthInitService.class);

    @Autowired
    private BaseDao dao;

    /**
     * 初始化业务线行级权限（业务拆分迁移专用）。
     *
     * 写入目标表 bi_portal.sys_data_auth，固定 owner_type=User、module_code=ssm_row、
     * dim_code=ssm_dim_bizline、active_duration_days=9999、remark=bizsplit_init:user_businessline。
     *
     * 第一段：映射组织在职用户（枚举 BizSplitDeptBusinessLineAuth）
     *   - 按 deptIds 过滤预置组织，一次 SQL 加载全部映射组织在职员工
     *   - 按枚举顺序匹配，每个用户仅命中第一个映射组织，不做跨组织并集
     *   - 无历史业务线权限：复制该组织全部默认业务线
     *   - 有历史业务线权限：补充该组织映射中尚未持有的业务线
     *
     * 第二段：非映射组织已有权限用户（枚举 BizSplitBusinessLineAuthExpandRule）
     *   - 遍历全量已有权限用户，跳过第一段已处理用户
     *   - 跳过仍归属任一映射组织的用户
     *   - 保养 补充 保养油液、保养配件
     *   - 改装超市 补充 超市改装、电子改装、电瓶车
     *
     * 幂等：待写记录按 owner_id + item_code 去重后，分批 REPLACE INTO。
     *
     * @param req 初始化请求；deptIds 为空或 null 时处理全部预置映射组织；
     *            userNames 为空或 null 时不限定人员域账号
     */
    public void initBusinessLineRowAuth(BizSplitDataAuthInitReq req) {
        BizSplitDataAuthInitReq safeReq = req == null ? new BizSplitDataAuthInitReq() : req;
        Set<String> userNameFilter = resolveUserNameFilter(safeReq.getUserNames());

        // 1. 加载维度配置（module_name、dim_name 等 insert 所需字段）
        RowBizlineDimConfig dimConfig = loadRowBizlineDimConfig();
        // 2. 解析待处理的组织映射（deptIds 为空则取全部预置枚举）
        List<BizSplitDeptBusinessLineAuth> mappings = resolveMappings(safeReq.getDeptIds());
        if (CollUtil.isEmpty(mappings)) {
            throw new IllegalStateException("未找到可处理的组织映射");
        }

        String createdBy = UserManager.get() != null ? UserManager.get().getName() : null;

        // 3. 一次 SQL（INNER JOIN hr_employee）加载全量已有业务线权限及在职部门
        UserAuthContext authContext = loadUserAuthContext();
        Map<String, Set<String>> existingAuthMap = authContext.getExistingAuthMap();
        Map<String, HrEmployee> employeeByName = authContext.getEmployeeByName();

        // key=ownerId|itemCode，去重待写记录
        Map<String, DataAuth> pendingAuthMap = new LinkedHashMap<>();
        // 记录第一段已处理用户，第二段不再重复处理
        Set<String> processedUsers = new HashSet<>();

        // 4. 第一段：一次查全部映射组织在职员工，按枚举顺序每人仅命中首个组织
        List<HrEmployee> mappedDeptEmployees = listActiveEmployeesByDeptIds(collectMappingDeptIds(mappings));
        for (BizSplitDeptBusinessLineAuth mapping : mappings) {
            if (CollUtil.isEmpty(mappedDeptEmployees)) {
                break;
            }
            for (HrEmployee employee : mappedDeptEmployees) {
                if (employee == null || StrUtil.isEmpty(employee.getUserName())) {
                    continue;
                }
                if (!isUserInScope(employee.getUserName(), userNameFilter)) {
                    continue;
                }
                if (processedUsers.contains(employee.getUserName())) {
                    continue;
                }
                if (!MetricExpansionOwnerUtil.containsDeptId(employee, mapping.getDeptId())) {
                    continue;
                }
                processMappedDeptUser(employee, mapping, dimConfig, existingAuthMap, pendingAuthMap, createdBy);
                processedUsers.add(employee.getUserName());
            }
        }

        // 5. 第二段：非映射组织用户按源业务线扩展补充
        processNonMappedUsers(existingAuthMap, employeeByName, mappings, processedUsers,
                userNameFilter, dimConfig, pendingAuthMap, createdBy);

        // 6. 分批 REPLACE INTO sys_data_auth
        if (CollUtil.isNotEmpty(pendingAuthMap)) {
            persistInsertList(new ArrayList<>(pendingAuthMap.values()));
        }
    }

    /**
     * 映射组织用户：无权限复制默认业务线，有权限补充映射中缺失项。
     */
    private void processMappedDeptUser(HrEmployee employee,
                                       BizSplitDeptBusinessLineAuth mapping,
                                       RowBizlineDimConfig dimConfig,
                                       Map<String, Set<String>> existingAuthMap,
                                       Map<String, DataAuth> pendingAuthMap,
                                       String createdBy) {
        String userName = employee.getUserName();
        List<String> deptBusinessLines = mapping.getBusinessLines();
        if (CollUtil.isEmpty(deptBusinessLines)) {
            return;
        }

        Set<String> existingLines = existingAuthMap.getOrDefault(userName, Collections.emptySet());
        Set<String> targetLines;
        if (CollUtil.isEmpty(existingLines)) {
            targetLines = new LinkedHashSet<>(deptBusinessLines);
        } else {
            targetLines = subtractBusinessLines(deptBusinessLines, existingLines);
        }

        collectTargetLines(userName, targetLines, dimConfig, pendingAuthMap, createdBy);
    }

    /**
     * 非映射组织用户：按已有源业务线扩展补充。
     * 保养 补充 保养油液、保养配件；改装超市 补充 超市改装、电子改装、电瓶车。
     */
    private void processNonMappedUsers(Map<String, Set<String>> existingAuthMap,
                                       Map<String, HrEmployee> employeeByName,
                                       List<BizSplitDeptBusinessLineAuth> mappings,
                                       Set<String> processedUsers,
                                       Set<String> userNameFilter,
                                       RowBizlineDimConfig dimConfig,
                                       Map<String, DataAuth> pendingAuthMap,
                                       String createdBy) {
        for (Map.Entry<String, Set<String>> entry : existingAuthMap.entrySet()) {
            String userName = entry.getKey();
            if (!isUserInScope(userName, userNameFilter)) {
                continue;
            }
            if (processedUsers.contains(userName)) {
                continue;
            }
            HrEmployee employee = employeeByName.get(userName);
            if (employee != null && belongsToAnyMappedDept(employee, mappings)) {
                continue;
            }

            Set<String> targetLines = resolveExpandTargetLines(entry.getValue());
            if (CollUtil.isEmpty(targetLines)) {
                continue;
            }

            collectTargetLines(userName, targetLines, dimConfig, pendingAuthMap, createdBy);
        }
    }

    /** 按已有业务线汇总非映射组织的扩展目标（剔除已持有项） */
    private Set<String> resolveExpandTargetLines(Set<String> existingLines) {
        Set<String> targetLines = new LinkedHashSet<>();
        if (CollUtil.isEmpty(existingLines)) {
            return targetLines;
        }
        for (String existingLine : existingLines) {
            BizSplitBusinessLineAuthExpandRule rule =
                    BizSplitBusinessLineAuthExpandRule.findBySourceBusinessLine(existingLine);
            if (rule == null) {
                continue;
            }
            targetLines.addAll(rule.getTargetBusinessLines());
        }
        return subtractBusinessLines(new ArrayList<>(targetLines), existingLines);
    }

    /** 判断用户是否归属任一预置映射组织 */
    private boolean belongsToAnyMappedDept(HrEmployee employee, List<BizSplitDeptBusinessLineAuth> mappings) {
        if (employee == null || CollUtil.isEmpty(mappings)) {
            return false;
        }
        for (BizSplitDeptBusinessLineAuth mapping : mappings) {
            if (MetricExpansionOwnerUtil.containsDeptId(employee, mapping.getDeptId())) {
                return true;
            }
        }
        return false;
    }

    /** 收集待写入的目标业务线（按 owner_id + item_code 去重） */
    private void collectTargetLines(String userName,
                                    Set<String> targetLines,
                                    RowBizlineDimConfig dimConfig,
                                    Map<String, DataAuth> pendingAuthMap,
                                    String createdBy) {
        if (CollUtil.isEmpty(targetLines)) {
            return;
        }
        for (String businessLine : targetLines) {
            if (StrUtil.isEmpty(businessLine)) {
                continue;
            }
            pendingAuthMap.put(buildAuthKey(userName, businessLine),
                    buildUserDataAuth(userName, dimConfig, businessLine, createdBy));
        }
    }

    private String buildAuthKey(String userName, String businessLine) {
        return userName + "|" + businessLine;
    }

    /**
     * 一次 SQL 加载全量用户已有业务线权限及在职部门。
     *
     * @return 已有权限 map 与 HR 员工 map
     */
    private UserAuthContext loadUserAuthContext() {
        Map<String, Set<String>> existingAuthMap = new HashMap<>();
        Map<String, HrEmployee> employeeByName = new HashMap<>();
        List<BizSplitUserRowBizlineAuthSnapshot> snapshotList = (List<BizSplitUserRowBizlineAuthSnapshot>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listUserRowBizlineAuthWithDept", null);
        if (CollUtil.isEmpty(snapshotList)) {
            return new UserAuthContext(existingAuthMap, employeeByName);
        }
        for (BizSplitUserRowBizlineAuthSnapshot snapshot : snapshotList) {
            if (snapshot == null || StrUtil.isEmpty(snapshot.getUserName())) {
                continue;
            }
            if (StrUtil.isNotEmpty(snapshot.getItemValue())) {
                existingAuthMap.computeIfAbsent(snapshot.getUserName(), key -> new LinkedHashSet<>())
                        .add(snapshot.getItemValue());
            }
            if (StrUtil.isNotEmpty(snapshot.getDeptId()) && !employeeByName.containsKey(snapshot.getUserName())) {
                HrEmployee employee = new HrEmployee();
                employee.setUserName(snapshot.getUserName());
                employee.setDeptId(snapshot.getDeptId());
                employeeByName.put(snapshot.getUserName(), employee);
            }
        }
        return new UserAuthContext(existingAuthMap, employeeByName);
    }

    private List<String> collectMappingDeptIds(List<BizSplitDeptBusinessLineAuth> mappings) {
        List<String> deptIds = new ArrayList<>();
        for (BizSplitDeptBusinessLineAuth mapping : mappings) {
            deptIds.add(mapping.getDeptId());
        }
        return deptIds;
    }

    private List<HrEmployee> listActiveEmployeesByDeptIds(List<String> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        List<HrEmployee> employees = (List<HrEmployee>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listActiveEmployeesByDeptIds", deptIds);
        return employees == null ? Collections.emptyList() : employees;
    }

    private Set<String> subtractBusinessLines(List<String> businessLines, Set<String> existingLines) {
        Set<String> result = new LinkedHashSet<>();
        if (CollUtil.isEmpty(businessLines) || CollUtil.isEmpty(existingLines)) {
            if (CollUtil.isNotEmpty(businessLines) && CollUtil.isEmpty(existingLines)) {
                result.addAll(businessLines);
            }
            return result;
        }
        for (String businessLine : businessLines) {
            if (StrUtil.isNotEmpty(businessLine) && !existingLines.contains(businessLine)) {
                result.add(businessLine);
            }
        }
        return result;
    }

    private RowBizlineDimConfig loadRowBizlineDimConfig() {
        UpperCaseMap config = (UpperCaseMap) dao.queryObject("ssm.bizsplit.data.auth.queryRowBizlineDimConfig", null);
        if (config == null) {
            throw new IllegalStateException("未找到 ssm_row / ssm_dim_bizline 维度配置");
        }
        RowBizlineDimConfig dimConfig = new RowBizlineDimConfig();
        dimConfig.setModuleCode(String.valueOf(config.get("moduleCode")));
        dimConfig.setModuleName(String.valueOf(config.get("moduleName")));
        dimConfig.setDimCode(String.valueOf(config.get("dimCode")));
        dimConfig.setDimName(String.valueOf(config.get("dimName")));
        dimConfig.setDimDisplayPrefix("【" + dimConfig.getModuleName() + "/" + dimConfig.getDimName() + "】");
        return dimConfig;
    }

    /** 解析人员域账号过滤集合；未传或全空时返回 null 表示不限定 */
    private Set<String> resolveUserNameFilter(List<String> userNames) {
        if (CollUtil.isEmpty(userNames)) {
            return null;
        }
        Set<String> filter = new LinkedHashSet<>();
        for (String userName : userNames) {
            if (StrUtil.isNotEmpty(userName)) {
                filter.add(userName.trim());
            }
        }
        return CollUtil.isEmpty(filter) ? null : filter;
    }

    /** 判断用户是否在指定域账号范围内；filter 为空表示不限定 */
    private boolean isUserInScope(String userName, Set<String> userNameFilter) {
        if (CollUtil.isEmpty(userNameFilter)) {
            return true;
        }
        return StrUtil.isNotEmpty(userName) && userNameFilter.contains(userName);
    }

    private List<BizSplitDeptBusinessLineAuth> resolveMappings(List<String> deptIds) {
        List<BizSplitDeptBusinessLineAuth> result = new ArrayList<>();
        for (BizSplitDeptBusinessLineAuth mapping : BizSplitDeptBusinessLineAuth.values()) {
            if (CollUtil.isNotEmpty(deptIds) && !deptIds.contains(mapping.getDeptId())) {
                continue;
            }
            result.add(mapping);
        }
        return result;
    }

    private DataAuth buildUserDataAuth(String userName,
                                       RowBizlineDimConfig dimConfig,
                                       String businessLine,
                                       String createdBy) {
        DataAuth dataAuth = new DataAuth();
        dataAuth.setAuthId(Guid.id());
        dataAuth.setOwnerType(BizSplitDataAuthConstant.OWNER_TYPE_USER);
        dataAuth.setOwnerId(userName);
        dataAuth.setModuleCode(dimConfig.getModuleCode());
        dataAuth.setModuleName(dimConfig.getModuleName());
        dataAuth.setDimCode(dimConfig.getDimCode());
        dataAuth.setDimName(dimConfig.getDimName());
        dataAuth.setItemCode(businessLine);
        dataAuth.setItemValue(businessLine);
        dataAuth.setDimDisplayName(dimConfig.getDimDisplayPrefix() + businessLine);
        dataAuth.setRemark(BizSplitDataAuthConstant.REMARK_PREFIX);
        dataAuth.setCreatedBy(createdBy);
        dataAuth.setActiveDurationDays(BizSplitDataAuthConstant.ACTIVE_DURATION_DAYS);
        return dataAuth;
    }

    private void persistInsertList(List<DataAuth> insertList) {
        int total = insertList.size();
        int batchSize = BizSplitDataAuthConstant.REPLACE_BATCH_SIZE;
        for (int from = 0; from < total; from += batchSize) {
            int to = Math.min(from + batchSize, total);
            List<DataAuth> batch = insertList.subList(from, to);
            dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                @Override
                public void execute() {
                    dao.insert("ssm.bizsplit.data.auth.replaceUserRowBizlineAuth", batch);
                }
            });
        }
        log.info("bizsplit user businessline auth init replaced {} rows in {} batches",
                total, (total + batchSize - 1) / batchSize);
    }

    private static class UserAuthContext {

        private final Map<String, Set<String>> existingAuthMap;
        private final Map<String, HrEmployee> employeeByName;

        private UserAuthContext(Map<String, Set<String>> existingAuthMap, Map<String, HrEmployee> employeeByName) {
            this.existingAuthMap = existingAuthMap;
            this.employeeByName = employeeByName;
        }

        public Map<String, Set<String>> getExistingAuthMap() {
            return existingAuthMap;
        }

        public Map<String, HrEmployee> getEmployeeByName() {
            return employeeByName;
        }
    }

    private static class RowBizlineDimConfig {

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
