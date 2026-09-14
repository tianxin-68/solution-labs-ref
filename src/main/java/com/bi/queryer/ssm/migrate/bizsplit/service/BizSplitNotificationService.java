package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.constant.BizSplitNotificationConstant;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitViewReminderRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.SsmMigrateBizsplitMappingEntity;
import com.bi.queryer.ssm.migrate.bizsplit.util.MetricExpansionOwnerUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.model.HrEmployee;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务线拆分通知服务。
 *
 * 负责视图失效提醒等拆分迁移后的用户提示能力。
 */
@Service
@Scope("prototype")
public class BizSplitNotificationService {

    @Autowired
    private BaseDao dao;

    /**
     * 查询业务线拆分-视图失效提醒：viewId 命中迁移映射表（ssm_migrate_bizsplit_mapping）时，
     * 返回拆分后的新视图 id 及模糊指标提示拼成的提醒文案。
     *
     * 拆分分支按当前登录用户所在部门收窄（规则见 {@link MetricExpansionOwnerUtil#resolveMatchedBusinesslines}）；
     * 未登录、查不到 HR 信息或部门未命中任一分支时，回退为展示全部分支。
     * 总开关：配置项 {@link BizSplitNotificationConstant#REMINDER_ENABLE_KEY}，关闭时直接返回 affected=false。
     *
     * @param viewId 老视图 id
     * @return 提醒结果；未命中任何拆分记录时 affected=false，message 为 null
     */
    public BizSplitViewReminderRsp queryBizSplitReminder(String viewId) {
        return queryBizSplitReminder(viewId, null);
    }

    /**
     * 查询业务线拆分-视图失效提醒。
     *
     * @param viewId   老视图 id
     * @param userName 指定用户域账号；为空时回退为 {@link UserManager} 当前用户
     * @return 提醒结果；未命中任何拆分记录时 affected=false，message 为 null
     */
    public BizSplitViewReminderRsp queryBizSplitReminder(String viewId, String userName) {
        Preconditions.checkArgument(StrUtil.isNotEmpty(viewId), "viewId 不能为空");

        if (!isBizSplitReminderEnabled()) {
            return BizSplitViewReminderRsp.builder().viewId(viewId).affected(false).build();
        }

        List<SsmMigrateBizsplitMappingEntity> mappings = dao.queryObjectList(
                "ssm.migrate.bizsplit.mapping.listViewSplitByOldId", viewId,
                SsmMigrateBizsplitMappingEntity.class);
        if (CollUtil.isEmpty(mappings)) {
            return BizSplitViewReminderRsp.builder().viewId(viewId).affected(false).build();
        }

        String oldBizLine = mappings.get(0).getOldBizLine();
        List<SsmMigrateBizsplitMappingEntity> narrowed = narrowSplitsByUserDept(mappings, oldBizLine, userName);

        List<BizSplitViewReminderRsp.SplitGroup> splitGroups = new ArrayList<>();
        for (SsmMigrateBizsplitMappingEntity mapping : narrowed) {
            splitGroups.add(BizSplitViewReminderRsp.SplitGroup.builder()
                    .newBizLine(mapping.getNewBizLine())
                    .newViewId(mapping.getNewId())
                    .build());
        }

        String message = buildBizSplitReminderMessage(viewId, oldBizLine, splitGroups);
        return BizSplitViewReminderRsp.builder()
                .viewId(viewId)
                .affected(true)
                .oldBizLine(oldBizLine)
                .message(message)
                .splits(splitGroups)
                .build();
    }

    /**
     * 命中拆分提醒时返回提醒文案，否则返回默认文案。
     *
     * @param defaultMessage 默认响应 message
     * @param viewId         老视图 id
     * @param userName       指定用户域账号；为空时回退为 {@link UserManager} 当前用户
     * @return 最终响应 message
     */
    public String resolveResponseMessage(String defaultMessage, String viewId, String userName) {
        if (StrUtil.isBlank(viewId)) {
            return defaultMessage;
        }
        BizSplitViewReminderRsp reminder = queryBizSplitReminder(viewId, userName);
        if (Boolean.TRUE.equals(reminder.getAffected()) && StrUtil.isNotBlank(reminder.getMessage())) {
            return reminder.getMessage();
        }
        return defaultMessage;
    }

    /**
     * 命中拆分提醒时将文案写入 remark；未命中时不改动 remark。
     *
     * @param remark   查询结果 remark，可为 null（将新建）
     * @param viewId   老视图 id
     * @param userName 指定用户域账号；为空时回退为 {@link UserManager} 当前用户
     * @return 写入提醒后的 remark
     */
    public Map<String, Object> applyReminderToRemark(Map<String, Object> remark, String viewId, String userName) {
        if (StrUtil.isBlank(viewId)) {
            return remark;
        }
        BizSplitViewReminderRsp reminder = queryBizSplitReminder(viewId, userName);
        if (!Boolean.TRUE.equals(reminder.getAffected()) || StrUtil.isBlank(reminder.getMessage())) {
            return remark;
        }
        Map<String, Object> targetRemark = remark == null ? new LinkedHashMap<>() : remark;
        targetRemark.put("注意" + (targetRemark.size() + 1), reminder.getMessage());
        return targetRemark;
    }

    /**
     * 按用户所在部门收窄拆分分支；未指定用户、无 HR 信息或未命中任一分支时回退为全部分支。
     *
     * @param mappings   该视图下全部拆分分支
     * @param oldBizLine 老业务线（作为 resolveMatchedBusinesslines 的 sourceBusinessline）
     * @param userName   指定用户域账号；为空时回退为 {@link UserManager} 当前用户
     * @return 收窄后的分支；无法判定时原样返回全部分支
     */
    private List<SsmMigrateBizsplitMappingEntity> narrowSplitsByUserDept(
            List<SsmMigrateBizsplitMappingEntity> mappings, String oldBizLine, String userName) {
        String resolvedUserName = resolveOperator(userName);
        if (StrUtil.isEmpty(resolvedUserName)) {
            return mappings;
        }
        List<HrEmployee> employees = dao.queryObjectList(
                "user.listEmployeeDeptByUserNames", Collections.singletonList(resolvedUserName), HrEmployee.class);
        if (CollUtil.isEmpty(employees)) {
            return mappings;
        }
        Map<String, HrEmployee> employeeByName = new HashMap<>();
        for (HrEmployee employee : employees) {
            if (employee != null && StrUtil.isNotEmpty(employee.getUserName())) {
                employeeByName.put(employee.getUserName(), employee);
            }
        }
        Set<String> matched = MetricExpansionOwnerUtil.resolveMatchedBusinesslines(
                Collections.singletonList(resolvedUserName), employeeByName, oldBizLine);
        if (CollUtil.isEmpty(matched)) {
            return mappings;
        }
        List<SsmMigrateBizsplitMappingEntity> narrowed = new ArrayList<>();
        for (SsmMigrateBizsplitMappingEntity mapping : mappings) {
            if (matched.contains(mapping.getNewBizLine())) {
                narrowed.add(mapping);
            }
        }
        return CollUtil.isNotEmpty(narrowed) ? narrowed : mappings;
    }

    /**
     * 按约定模板拼接业务线拆分提醒文案：
     *
     * 因"业务线拆分"视图id[xxx]已不适用，当前视图所属"保养"业务线，请使用拆分后的视图和指标：
     * 【"保养" 拆分为 "保养油液"】
     * 拆分后的视图id=xxx，其中指标"保养-***"拆分为"保养油液-***"
     *
     * @param viewId      老视图 id
     * @param oldBizLine  老业务线
     * @param splitGroups 已按用户部门收窄的拆分分支
     * @return 拼接好的完整文案
     */
    private String buildBizSplitReminderMessage(String viewId, String oldBizLine,
                                                 List<BizSplitViewReminderRsp.SplitGroup> splitGroups) {
        StringBuilder sb = new StringBuilder();
        sb.append("因\"业务线拆分\"视图id[").append(viewId).append("]已不适用，当前视图所属\"")
                .append(oldBizLine).append("\"业务线，请使用拆分后的视图和指标：\n");
        for (BizSplitViewReminderRsp.SplitGroup group : splitGroups) {
            sb.append("\n【\"").append(oldBizLine).append("\" 拆分为 \"").append(group.getNewBizLine()).append("\"】\n");
            sb.append("拆分后的视图id=").append(group.getNewViewId())
                    .append("，其中指标\"").append(oldBizLine).append("-***\"拆分为\"")
                    .append(group.getNewBizLine()).append("-***\"\n");
        }
        return sb.toString().trim();
    }

    /**
     * 视图失效提醒总开关是否开启。
     *
     * @return true 表示继续走后续提醒逻辑
     */
    private boolean isBizSplitReminderEnabled() {
        return "true".equalsIgnoreCase(SC.v(
                BizSplitNotificationConstant.REMINDER_ENABLE_KEY,
                BizSplitNotificationConstant.REMINDER_ENABLE_DEFAULT));
    }

    /**
     * @param userName 指定用户域账号；为空时回退为 {@link UserManager} 当前用户
     * @return 当前操作人用户名，未登录时返回 null
     */
    private String resolveOperator(String userName) {
        if (StrUtil.isNotBlank(userName)) {
            return userName;
        }
        return UserManager.get() != null ? UserManager.get().getName() : null;
    }
}
