package com.bi.queryer.ssm.changenotice.match.strategy;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.changenotice.entity.ChangeNotice;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObject;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObjectFilter;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeObjectCtg;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeRemindTarget;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeType;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchContext;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchHelper;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 替换/下线（REPLACE_OFFLINE）匹配策略。
 *
 * 业务背景：
 * 配置端可为「旧对象催迁移」与「新对象告知口径变」分别勾选提醒目标（remindTargets）：
 * 1. REMIND_OLD：查询仍在使用将被替换/下线的旧对象时提醒（催迁移）
 * 2. REMIND_NEW：视图已按替换任务完成字段替换后，提醒新对象口径变化
 * 两条路径可同时配置，任一命中即提醒；本类型不做筛选条件二次校验。
 * 本类型变更对象角色为 MAPPING（替换映射：旧对象 → 新对象），非 CHANGE。
 *
 * 命中路径一：提醒旧对象（REMIND_OLD）
 * 按变更对象类型分两种比对方式：
 * 1. 字段类（指标/维度等，非 METRIC_GROUP/DIM_GROUP/MODULE）：
 *    MAPPING.objectId 与 context.hitFieldKeys（query 并集 offline）求交集，任一命中即可。
 * 2. 目录类（objectType=METRIC_GROUP/DIM_GROUP/MODULE，对应配置端「指标组/维度组/多维模块」）：
 *    改为用 MAPPING.objectId 与 context.queryCtgIds 求交集。
 *
 * 命中路径二：提醒新对象（REMIND_NEW）
 * 需同时满足：
 * 1. viewId + replaceTaskId（支持逗号分隔多个，任一命中即可）出现在预加载的 template_field_replace_log 键集合中；
 * 2. 至少一个 MAPPING 新对象（relObjectId）已存在于当前查询字段/目录中。
 *
 * 出参组装与命中判定在 {@link #matchAndBuild} 内一次完成，避免重复解析 remindTargets / 重复命中计算。
 *
 * @see ChangeNoticeMatchHelper#hitByMappingObjects
 * @see ChangeNoticeMatchHelper#hitByMappingCtgObjects
 * @see ChangeNoticeMatchHelper#listHitMappingObjectsForReplace
 * @see ChangeNoticeMatchHelper#existsViewInReplaceLog
 * @see ChangeNoticeMatchHelper#isHitNewReplaceObject
 */
@Component
public class ReplaceOfflineMatchStrategy implements ChangeNoticeMatchStrategy {

    @Autowired
    private ChangeNoticeMatchHelper matchHelper;

    /**
     * 本策略仅处理 REPLACE_OFFLINE。
     *
     * @return 替换/下线类型
     */
    @Override
    public ChangeNoticeType supportType() {
        return ChangeNoticeType.REPLACE_OFFLINE;
    }

    /**
     * 一次完成：解析提醒目标 → 命中判定 → 组装出参。
     *
     * @param notice  通知主表
     * @param objects 通知对象明细
     * @param filters 筛选配置（本类型不使用）
     * @param context 匹配上下文（含预计算 hitFieldKeys）
     * @return 命中时的 VO；未命中返回 null
     */
    @Override
    public ChangeNoticeVO matchAndBuild(ChangeNotice notice, List<ChangeNoticeObject> objects,
                                        List<ChangeNoticeObjectFilter> filters,
                                        ChangeNoticeMatchContext context) {
        if (context == null) {
            return null;
        }
        // 只解析一次 remindTargets，供旧/新对象两条路径复用
        Set<ChangeNoticeRemindTarget> remindTargets = matchHelper.parseRemindTargets(notice.getRemindTargets());
        boolean remindOldConfigured = matchHelper.hasRemindTarget(
                remindTargets, ChangeNoticeRemindTarget.REMIND_OLD);
        boolean remindNewConfigured = matchHelper.hasRemindTarget(
                remindTargets, ChangeNoticeRemindTarget.REMIND_NEW);

        Set<String> hitFieldKeys = context.getHitFieldKeys();
        boolean hitRemindOld = remindOldConfigured && matchRemindOld(objects, context, hitFieldKeys);
        boolean hitRemindNew = remindNewConfigured && matchRemindNew(notice, objects, context);
        if (!hitRemindOld && !hitRemindNew) {
            return null;
        }

        List<ChangeNoticeObject> hitObjects = matchHelper.listHitMappingObjectsForReplace(
                objects, hitFieldKeys, context.getQueryFieldKeys(), context.getQueryCtgIds());
        // 仅新对象提醒命中时，查询侧往往已无旧对象交集，仍需展示完整替换清单
        if (CollUtil.isEmpty(hitObjects) && hitRemindNew) {
            hitObjects = matchHelper.listByCtg(objects, ChangeNoticeObjectCtg.MAPPING);
        }
        return matchHelper.buildReplaceOfflineVO(
                notice, hitObjects, hitRemindOld, hitRemindNew, hitFieldKeys, context.getQueryCtgIds());
    }

    /**
     * 旧对象提醒（REMIND_OLD）命中判定：字段类或目录类任一命中即可。
     *
     * @param objects      该通知下全部对象明细
     * @param context      匹配上下文
     * @param hitFieldKeys 预计算的旧对象字段命中键
     * @return true 表示旧对象侧已命中
     */
    private boolean matchRemindOld(List<ChangeNoticeObject> objects, ChangeNoticeMatchContext context,
                                   Set<String> hitFieldKeys) {
        if (matchHelper.hitByMappingObjects(objects, hitFieldKeys)) {
            return true;
        }
        return matchHelper.hitByMappingCtgObjects(objects, context.getQueryCtgIds());
    }

    /**
     * 新对象提醒（REMIND_NEW）命中判定：
     * 1. 当前视图已在替换任务日志中完成字段替换；
     * 2. 至少一个 MAPPING 新对象已出现在当前查询中。
     *
     * @param notice  通知主表（取 replaceTaskId）
     * @param objects 该通知下全部对象明细
     * @param context 匹配上下文
     * @return true 表示新对象侧已命中
     */
    private boolean matchRemindNew(ChangeNotice notice, List<ChangeNoticeObject> objects,
                                   ChangeNoticeMatchContext context) {
        if (!matchHelper.existsViewInReplaceLog(context.getViewId(), notice.getReplaceTaskId(), context)) {
            return false;
        }
        return hitByNewObjectsInQuery(objects, context);
    }

    /**
     * 判断是否存在已出现在当前查询中的 MAPPING 新对象。
     *
     * @param objects 通知对象明细
     * @param context 匹配上下文
     * @return true 表示至少一个新对象在查询字段/目录中
     */
    private boolean hitByNewObjectsInQuery(List<ChangeNoticeObject> objects, ChangeNoticeMatchContext context) {
        List<ChangeNoticeObject> mappingObjects = matchHelper.listByCtg(objects, ChangeNoticeObjectCtg.MAPPING);
        if (CollUtil.isEmpty(mappingObjects)) {
            return false;
        }
        for (ChangeNoticeObject object : mappingObjects) {
            if (matchHelper.isHitNewReplaceObject(
                    object, context.getQueryFieldKeys(), context.getQueryCtgIds())) {
                return true;
            }
        }
        return false;
    }
}
