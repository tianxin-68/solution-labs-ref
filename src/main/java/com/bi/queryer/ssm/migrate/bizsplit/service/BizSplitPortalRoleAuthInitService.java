package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.constant.BizSplitDataAuthConstant;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitPortalRoleAuthInitReq;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionOwnerUtil;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleDept;
import com.bi.queryer.ssm.portal.auth.entity.PortalRoleUser;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.model.HrEmployee;
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
 * 多维门户角色权限初始化服务（方法3）。
 *
 * 业务拆分迁移场景：将旧角色 id 下的用户/组织授权复制到新角色 id，
 * 分别写入 ssm_portal_role_user 与 ssm_portal_role_dept。
 */
@Service
@Scope("prototype")
public class BizSplitPortalRoleAuthInitService {

    private static final Logger log = LoggerFactory.getLogger(BizSplitPortalRoleAuthInitService.class);

    @Autowired
    private BaseDao dao;

    /**
     * 多维门户角色权限初始化（业务拆分迁移专用，方法3）。
     *
     * 入参：新角色 id、旧角色 id、排除部门 id 集合。
     *
     * 权限来源一：ssm_portal_role_user
     * 权限来源二：ssm_portal_role_dept
     *
     * 过滤规则：
     *   - 用户：HR 部门路径命中 excludeDeptIds 任一部门则跳过
     *   - 组织：dept_id 命中 excludeDeptIds 则跳过
     *   - excludeDeptIds 须传完整部门 id 列表，子部门也需逐一传入，不会根据父部门自动展开
     *
     * 写入规则：
     *   - 重跑前先按 newRoleId 删除 ssm_portal_role_user / ssm_portal_role_dept 既有行，再插入过滤后的结果
     *   - role_id 替换为新角色 id
     *   - 按 role_id + user_name / dept_id 去重后分批写入
     *
     * @param req 新/旧角色 id 及排除部门集合
     */
    public void initPortalRoleAuth(BizSplitPortalRoleAuthInitReq req) {
        // 1. 校验入参
        BizSplitPortalRoleAuthInitReq safeReq = validateReq(req);

        // 2. 加载排除部门集合、操作人
        Set<String> excludeDeptIds = normalizeExcludeDeptIds(safeReq.getExcludeDeptIds());
        String operator = resolveOperator();

        // 3. 查询旧角色下的用户/组织授权
        List<PortalRoleUser> roleUserSnapshots = listRoleUsersByOldRoleId(safeReq.getOldRoleId());
        List<PortalRoleDept> roleDeptSnapshots = listRoleDeptsByOldRoleId(safeReq.getOldRoleId());

        // 4. 批量加载用户 HR 部门，供 excludeDeptIds 过滤
        Map<String, HrEmployee> employeeByName = loadEmployeeByUserNames(collectUserNames(roleUserSnapshots));

        Map<String, PortalRoleUser> roleUserMap = new LinkedHashMap<>();
        Map<String, PortalRoleDept> roleDeptMap = new LinkedHashMap<>();

        // 5. 过滤并组装待写用户授权（role_id 替换为新角色 id）
        collectRoleUsers(roleUserSnapshots, excludeDeptIds, employeeByName, safeReq.getNewRoleId(), operator, roleUserMap);
        // 6. 过滤并组装待写组织授权
        collectRoleDepts(roleDeptSnapshots, excludeDeptIds, safeReq.getNewRoleId(), operator, roleDeptMap);

        // 7. 先删新角色下既有授权，再写入过滤后的结果（重跑时可撤销上次误授权）
        deleteExistingRoleAuth(safeReq.getNewRoleId());
        persistRoleUsers(new ArrayList<>(roleUserMap.values()));
        persistRoleDepts(new ArrayList<>(roleDeptMap.values()));
    }

    /** 重跑前清理：删除新角色下全部用户/组织授权 */
    private void deleteExistingRoleAuth(String newRoleId) {
        if (StrUtil.isEmpty(newRoleId)) {
            return;
        }
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.portal.role.user.deleteRoleUserByRoleId", newRoleId);
                dao.delete("ssm.portal.role.dept.deleteRoleDeptByRoleId", newRoleId);
            }
        });
        log.info("bizsplit portal role auth init deleted existing rows for role {}", newRoleId);
    }

    /** 校验新/旧角色 id 必填且不能相同 */
    private BizSplitPortalRoleAuthInitReq validateReq(BizSplitPortalRoleAuthInitReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (StrUtil.isEmpty(req.getNewRoleId())) {
            throw new IllegalArgumentException("新角色 id 不能为空");
        }
        if (StrUtil.isEmpty(req.getOldRoleId())) {
            throw new IllegalArgumentException("旧角色 id 不能为空");
        }
        if (req.getNewRoleId().equals(req.getOldRoleId())) {
            throw new IllegalArgumentException("新角色 id 与旧角色 id 不能相同");
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

    private String resolveOperator() {
        return UserManager.get() != null ? UserManager.get().getName() : null;
    }

    /** 查询旧角色用户授权 */
    private List<PortalRoleUser> listRoleUsersByOldRoleId(String oldRoleId) {
        List<PortalRoleUser> roleUserList = (List<PortalRoleUser>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listPortalRoleUserByOldRoleId", oldRoleId);
        return roleUserList == null ? new ArrayList<>() : roleUserList;
    }

    /** 查询旧角色组织授权 */
    private List<PortalRoleDept> listRoleDeptsByOldRoleId(String oldRoleId) {
        List<PortalRoleDept> roleDeptList = (List<PortalRoleDept>) dao.queryObjectList(
                "ssm.bizsplit.data.auth.listPortalRoleDeptByOldRoleId", oldRoleId);
        return roleDeptList == null ? new ArrayList<>() : roleDeptList;
    }

    private Set<String> collectUserNames(List<PortalRoleUser> roleUserSnapshots) {
        Set<String> userNames = new HashSet<>();
        if (CollUtil.isEmpty(roleUserSnapshots)) {
            return userNames;
        }
        for (PortalRoleUser roleUser : roleUserSnapshots) {
            if (roleUser != null && StrUtil.isNotEmpty(roleUser.getUserName())) {
                userNames.add(roleUser.getUserName());
            }
        }
        return userNames;
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

    /** 组装待写用户授权，role_id 替换为新角色 id */
    private void collectRoleUsers(List<PortalRoleUser> snapshots,
                                  Set<String> excludeDeptIds,
                                  Map<String, HrEmployee> employeeByName,
                                  String newRoleId,
                                  String operator,
                                  Map<String, PortalRoleUser> roleUserMap) {
        if (CollUtil.isEmpty(snapshots)) {
            return;
        }
        for (PortalRoleUser snapshot : snapshots) {
            if (snapshot == null || StrUtil.isEmpty(snapshot.getUserName())) {
                continue;
            }
            if (isUserExcluded(snapshot.getUserName(), excludeDeptIds, employeeByName)) {
                continue;
            }
            PortalRoleUser roleUser = new PortalRoleUser();
            roleUser.setRoleId(newRoleId);
            roleUser.setUserName(snapshot.getUserName());
            roleUser.setCreatedBy(StrUtil.isNotEmpty(snapshot.getCreatedBy()) ? snapshot.getCreatedBy() : operator);
            roleUserMap.put(buildRoleUserKey(roleUser), roleUser);
        }
    }

    /** 组装待写组织授权，role_id 替换为新角色 id */
    private void collectRoleDepts(List<PortalRoleDept> snapshots,
                                  Set<String> excludeDeptIds,
                                  String newRoleId,
                                  String operator,
                                  Map<String, PortalRoleDept> roleDeptMap) {
        if (CollUtil.isEmpty(snapshots)) {
            return;
        }
        for (PortalRoleDept snapshot : snapshots) {
            if (snapshot == null || StrUtil.isEmpty(snapshot.getDeptId())) {
                continue;
            }
            if (isDeptExcluded(snapshot.getDeptId(), excludeDeptIds)) {
                continue;
            }
            PortalRoleDept roleDept = new PortalRoleDept();
            roleDept.setRoleId(newRoleId);
            roleDept.setDeptId(snapshot.getDeptId());
            roleDept.setCreatedBy(StrUtil.isNotEmpty(snapshot.getCreatedBy()) ? snapshot.getCreatedBy() : operator);
            roleDeptMap.put(buildRoleDeptKey(roleDept), roleDept);
        }
    }

    /** 用户所属部门是否命中任一排除部门 */
    private boolean isUserExcluded(String userName,
                                     Set<String> excludeDeptIds,
                                     Map<String, HrEmployee> employeeByName) {
        if (CollUtil.isEmpty(excludeDeptIds)) {
            return false;
        }
        HrEmployee employee = employeeByName.get(userName);
        if (employee == null) {
            return false;
        }
        for (String excludeDeptId : excludeDeptIds) {
            if (MetricExpansionOwnerUtil.containsDeptId(employee, excludeDeptId)) {
                return true;
            }
        }
        return false;
    }

    /** 组织 dept_id 是否在排除部门列表中（精确匹配，不做父子展开） */
    private boolean isDeptExcluded(String deptId, Set<String> excludeDeptIds) {
        if (StrUtil.isEmpty(deptId) || CollUtil.isEmpty(excludeDeptIds)) {
            return false;
        }
        return excludeDeptIds.contains(deptId);
    }

    private String buildRoleUserKey(PortalRoleUser roleUser) {
        return roleUser.getRoleId() + "|" + roleUser.getUserName();
    }

    private String buildRoleDeptKey(PortalRoleDept roleDept) {
        return roleDept.getRoleId() + "|" + roleDept.getDeptId();
    }

    /** 分批 REPLACE INTO ssm_portal_role_user */
    private void persistRoleUsers(List<PortalRoleUser> insertList) {
        if (CollUtil.isEmpty(insertList)) {
            return;
        }
        batchReplace("ssm.bizsplit.data.auth.replacePortalRoleUser", insertList);
        log.info("bizsplit portal role auth init replaced {} role_user rows", insertList.size());
    }

    /** 分批 REPLACE INTO ssm_portal_role_dept */
    private void persistRoleDepts(List<PortalRoleDept> insertList) {
        if (CollUtil.isEmpty(insertList)) {
            return;
        }
        batchReplace("ssm.bizsplit.data.auth.replacePortalRoleDept", insertList);
        log.info("bizsplit portal role auth init replaced {} role_dept rows", insertList.size());
    }

    private void batchReplace(String sqlId, List<?> insertList) {
        int total = insertList.size();
        int batchSize = BizSplitDataAuthConstant.REPLACE_BATCH_SIZE;
        for (int from = 0; from < total; from += batchSize) {
            int to = Math.min(from + batchSize, total);
            List<?> batch = insertList.subList(from, to);
            dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                @Override
                public void execute() {
                    dao.insert(sqlId, batch);
                }
            });
        }
    }
}
