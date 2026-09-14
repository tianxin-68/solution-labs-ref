package com.bi.queryer.ssm.migrate.bizsplit.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.enums.MetricExpansionOwnerDept;
import com.bi.queryer.sys.user.model.HrEmployee;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 指标膨胀 owner 组织过滤工具。
 *
 * 使用场景：入参未指定 targetBusinessline 时，按视图 owner 的 DEPT_ID 收窄映射。
 *
 * 规则摘要（部门/业务线取值见 {@link MetricExpansionOwnerDept}）：
 * 1. 源业务线=保养 时：油液/配件部门 → 仅保留对应 target_businessline
 * 2. 源业务线=改装超市 时：超市改装/电子改装/电瓶车部门 → 仅保留对应 target
 * 3. 其余（含未命中、或同组内命中多个）→ 保留全部 target，膨胀成多个指标
 * 4. 公共视图 tplOwner 中 DEPT_ID 包含 BI 的人先剔除，再按剩余 owner 判断
 *
 * 匹配方式：两侧 DEPT_ID 末尾补 "/" 后再做 contains（非 equals），降低前缀误匹配。
 */
public final class MetricExpansionOwnerUtil {

    private MetricExpansionOwnerUtil() {
    }

    /**
     * 解析逗号分隔的 tplOwner 字符串为用户名列表。
     * 模板 owner 存库格式为多用户名逗号拼接，如 "zhangsan,lisi"。
     *
     * @param tplOwner 模板 owner 原始串，允许为空
     * @return 去空白后的非空用户名列表；入参为空时返回空列表
     */
    public static List<String> splitOwnerNames(String tplOwner) {
        List<String> names = new ArrayList<>();
        if (StrUtil.isEmpty(tplOwner)) {
            return names;
        }
        for (String name : tplOwner.split(",")) {
            if (StrUtil.isNotEmpty(name) && StrUtil.isNotEmpty(name.trim())) {
                names.add(name.trim());
            }
        }
        return names;
    }

    /**
     * 从公共视图 tplOwner 中剔除 DEPT_ID 包含 BI 部门的人。
     * 查不到 HR 信息的 owner 保留（无法判定是否 BI，交给后续业务线匹配处理）。
     *
     * @param ownerNames     原始 owner 用户名
     * @param employeeByName 用户名 → HR 员工（至少含 DEPT_ID）
     * @return 剔除 BI 后的 owner 列表；入参为空时返回空列表
     */
    public static List<String> removeBiOwners(List<String> ownerNames,
                                              Map<String, HrEmployee> employeeByName) {
        List<String> result = new ArrayList<>();
        if (CollUtil.isEmpty(ownerNames)) {
            return result;
        }
        for (String ownerName : ownerNames) {
            HrEmployee employee = employeeByName == null ? null : employeeByName.get(ownerName);
            // DEPT_ID 包含 BI 部门 id 则跳过，不参与后续油液/配件判断
            if (employee != null && containsDeptId(employee, MetricExpansionOwnerDept.BI.getDeptId())) {
                continue;
            }
            result.add(ownerName);
        }
        return result;
    }

    /**
     * 根据 owner 的 DEPT_ID 汇总应保留的 target_businessline。
     *
     * 仅处理与入参 sourceBusinessline 一致的枚举项：
     * - 保养 → 保养油液 / 保养配件
     * - 改装超市 → 超市改装 / 电子改装 / 电瓶车
     *
     * 返回约定：
     * - 空集合：无人命中当前源业务线下任一部门 → 调用方应保留全部映射（膨胀成多个指标）
     * - 单元素：仅命中一个 target → 调用方按该业务线过滤
     * - 多元素：同组内命中多个 target → 调用方同样保留全部（不收窄）
     *
     * @param ownerNames         用于判断的 owner（公共视图应已去 BI）
     * @param employeeByName     用户名 → HR 员工
     * @param sourceBusinessline 入参源业务线（如「保养」「改装超市」）
     * @return 命中的目标业务线集合
     */
    public static Set<String> resolveMatchedBusinesslines(List<String> ownerNames,
                                                          Map<String, HrEmployee> employeeByName,
                                                          String sourceBusinessline) {
        Set<String> matched = new LinkedHashSet<>();
        if (CollUtil.isEmpty(ownerNames) || employeeByName == null
                || StrUtil.isEmpty(sourceBusinessline)) {
            return matched;
        }
        for (String ownerName : ownerNames) {
            HrEmployee employee = employeeByName.get(ownerName);
            if (employee == null) {
                continue;
            }
            for (MetricExpansionOwnerDept ownerDept : MetricExpansionOwnerDept.values()) {
                if (!ownerDept.hasTargetBusinessline()) {
                    continue;
                }
                if (!sourceBusinessline.equals(ownerDept.getSourceBusinessline())) {
                    continue;
                }
                if (containsDeptId(employee, ownerDept.getDeptId())) {
                    matched.add(ownerDept.getTargetBusinessline());
                }
            }
        }
        return matched;
    }

    /**
     * 按命中的目标业务线过滤映射行。
     * 仅当 matched 恰好 1 个业务线时收窄；为空或同组内命中多个时原样返回，保留全部 target。
     *
     * @param mappings 源业务线下全部映射
     * @param matched  {@link #resolveMatchedBusinesslines} 的结果
     * @return 过滤后的映射；无需过滤时返回入参本身
     */
    public static List<MetricExpansionMappingEntity> filterByBusinesslines(
            List<MetricExpansionMappingEntity> mappings, Set<String> matched) {
        // 0 个或超过 1 个命中：不做收窄，膨胀成全部 target
        if (CollUtil.isEmpty(mappings) || CollUtil.isEmpty(matched) || matched.size() != 1) {
            return mappings;
        }
        String businessline = matched.iterator().next();
        List<MetricExpansionMappingEntity> filtered = new ArrayList<>();
        for (MetricExpansionMappingEntity mapping : mappings) {
            if (mapping != null && businessline.equals(mapping.getTargetBusinessline())) {
                filtered.add(mapping);
            }
        }
        return filtered;
    }

    /**
     * 判断员工所属部门是否包含指定部门 id。
     * 比较前在 DEPT_ID 与目标 deptId 末尾都补上 "/"，避免短 id 误匹配长 id 前缀。
     *
     * @param employee 员工，允许为空
     * @param deptId   部门 id，允许为空
     * @return true 表示补斜杠后的 DEPT_ID 包含补斜杠后的 deptId
     */
    public static boolean containsDeptId(HrEmployee employee, String deptId) {
        if (employee == null || StrUtil.isEmpty(deptId) || StrUtil.isEmpty(employee.getDeptId())) {
            return false;
        }
        // 末尾统一加 "/" 再 contains，降低前缀误伤（如 701 误匹配 701485）
        return withTrailingSlash(employee.getDeptId()).contains(withTrailingSlash(deptId));
    }

    /**
     * 在部门 id 末尾追加 "/"，便于路径包含比较。
     *
     * @param deptId 原始部门 id
     * @return 以 "/" 结尾的字符串
     */
    private static String withTrailingSlash(String deptId) {
        return deptId + "/";
    }
}
