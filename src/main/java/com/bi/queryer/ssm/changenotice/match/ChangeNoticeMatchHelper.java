package com.bi.queryer.ssm.changenotice.match;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.changenotice.entity.ChangeNotice;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObject;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObjectFilter;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeObjectCtg;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeObjectType;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeRemindTarget;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeObjectFilterVO;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeObjectVO;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeVO;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 变更通知匹配公共能力。
 *
 * 提供各类型策略可复用的命中判定、筛选校验与 VO 组装方法，不承载按 changeType 的业务分支。
 * 策略层通过本类完成：
 * 1. 按 {@link ChangeNoticeObjectCtg} 与查询字段求交
 * 2. 替换/下线场景的旧/新对象、目录（{@link ChangeNoticeObjectType#METRIC_GROUP}/{@link ChangeNoticeObjectType#DIM_GROUP}/{@link ChangeNoticeObjectType#MODULE}）命中与出参组装
 * 3. 使用约束的必须同查（字段 + 筛选存在性）判定
 * 4. 替换日志键判断与基础 / 专用 VO 组装
 */
@Component
public class ChangeNoticeMatchHelper {

    /** 替换日志匹配键分隔符，拼接为 viewId|replaceTaskId */
    private static final String REPLACE_LOG_KEY_SEPARATOR = "|";

    /**
     * 判断替换/下线（MAPPING）对象是否与当前查询字段 code 有交集（任一即可）。
     * 未配置 MAPPING 时返回 false。
     *
     * @param objects        通知对象明细
     * @param queryFieldKeys 查询字段 code 集合（旧对象路径可能已合并 offline）
     * @return true 表示至少命中一个 MAPPING 对象
     */
    public boolean hitByMappingObjects(List<ChangeNoticeObject> objects, Set<String> queryFieldKeys) {
        return hitByObjectCtg(objects, ChangeNoticeObjectCtg.MAPPING, queryFieldKeys, false);
    }

    /**
     * 判断替换/下线中 objectType 为 METRIC_GROUP/DIM_GROUP/MODULE 的 MAPPING 对象是否命中。
     *
     * 业务背景：目录下线/替换后，目录节点本身往往仍存在，不能再按字段 code 比对。
     * 改为用查询字段的 moduleCtgId / MetaField.categoryIdList（queryCtgIds）与 MAPPING.objectId 比对，
     * 表示「当前查询仍挂在该被替换/下线的目录下」。
     *
     * @param objects     通知对象明细
     * @param queryCtgIds 当前查询用到的目录 ID 集合
     * @return true 表示至少命中一个 METRIC_GROUP/DIM_GROUP/MODULE 类型 MAPPING 对象
     */
    public boolean hitByMappingCtgObjects(List<ChangeNoticeObject> objects, Set<String> queryCtgIds) {
        if (CollUtil.isEmpty(queryCtgIds)) {
            return false;
        }
        List<ChangeNoticeObject> mappingObjects = listByCtg(objects, ChangeNoticeObjectCtg.MAPPING);
        if (CollUtil.isEmpty(mappingObjects)) {
            return false;
        }
        for (ChangeNoticeObject object : mappingObjects) {
            if (object == null || !isCtgObjectType(object.getObjectType())) {
                continue;
            }
            if (StrUtil.isNotBlank(object.getObjectId())
                    && queryCtgIds.contains(object.getObjectId().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为目录类对象类型（objectType 为 METRIC_GROUP、DIM_GROUP 或 MODULE，忽略大小写）。
     *
     * @param objectType 对象类型
     * @return true 表示 METRIC_GROUP、DIM_GROUP 或 MODULE
     */
    public boolean isCtgObjectType(String objectType) {
        return ChangeNoticeObjectType.isCtg(objectType);
    }

    /**
     * 判断限定受影响指标是否命中（维度内容变更专用）。
     *
     * 未配置 AFFECTED_METRIC 时视为通过（passWhenEmpty=true）；
     * 已配置时需与查询字段 code 有交集（任一即可）。
     *
     * @param objects        通知对象明细
     * @param queryFieldKeys 查询字段 code 集合
     * @return true 表示通过受影响指标校验
     */
    public boolean hitByAffectedMetrics(List<ChangeNoticeObject> objects, Set<String> queryFieldKeys) {
        return hitByObjectCtg(objects, ChangeNoticeObjectCtg.AFFECTED_METRIC, queryFieldKeys, true);
    }

    /**
     * 判断回刷提醒范围字段是否命中（任一即可）。
     * 未配置 REMIND_FIELD 时视为通过（配置端提醒范围选填）。
     *
     * @param objects        通知对象明细
     * @param queryFieldKeys 查询字段 code 集合
     * @return true 表示通过提醒范围校验
     */
    public boolean hitByRemindFields(List<ChangeNoticeObject> objects, Set<String> queryFieldKeys) {
        return hitByObjectCtg(objects, ChangeNoticeObjectCtg.REMIND_FIELD, queryFieldKeys, true);
    }

    /**
     * 判断必须同查是否已满足（使用约束场景：满足则不再提醒）。
     *
     * 同时满足以下两点才算「已满足」：
     * 1. 必须同查字段（MUST_QUERY）全部出现在当前查询中；未配置字段则跳过本项
     * 2. 必须同查筛选：筛选项存在且已有筛选值，不比较具体取值；未配置筛选则跳过本项
     *
     * @param objects 通知对象明细
     * @param filters 通知筛选配置（此处作必须同查筛选）
     * @param context 匹配上下文
     * @return true 表示必须同查已满足（此时不应再提醒）
     */
    public boolean isMustQuerySatisfied(List<ChangeNoticeObject> objects,
                                        List<ChangeNoticeObjectFilter> filters,
                                        ChangeNoticeMatchContext context) {
        if (context == null) {
            return false;
        }
        return containsAllMustQueryFields(objects, context.getQueryFieldKeys())
                && hasRequiredFiltersPresent(filters, context);
    }

    /**
     * 判断必须同查筛选是否满足：筛选项存在且已有筛选值，不比较具体取值。
     * 未配置筛选时视为通过。
     *
     * @param filters 通知筛选配置
     * @param context 匹配上下文
     * @return true 表示筛选要求已满足
     */
    public boolean hasRequiredFiltersPresent(List<ChangeNoticeObjectFilter> filters,
                                             ChangeNoticeMatchContext context) {
        if (CollUtil.isEmpty(filters)) {
            return true;
        }
        if (context == null || context.getFilterFieldMap() == null) {
            return false;
        }
        for (ChangeNoticeObjectFilter filter : filters) {
            if (filter == null || StrUtil.isBlank(filter.getObjectId())) {
                return false;
            }
            QueryField field = context.getFilterFieldMap().get(filter.getObjectId().trim());
            // 筛选项不存在，或存在但无筛选值：不满足
            if (field == null || CollUtil.isEmpty(extractFieldValueIds(field.getValues()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断当前查询是否包含全部必须同查字段。
     * 未配置 MUST_QUERY 时视为通过；任一 MUST_QUERY.objectId 不在 queryFieldKeys 中则不通过。
     *
     * @param objects        通知对象明细
     * @param queryFieldKeys 查询字段 code 集合
     * @return true 表示字段要求已满足
     */
    public boolean containsAllMustQueryFields(List<ChangeNoticeObject> objects, Set<String> queryFieldKeys) {
        List<ChangeNoticeObject> mustQueryObjects = listByCtg(objects, ChangeNoticeObjectCtg.MUST_QUERY);
        if (CollUtil.isEmpty(mustQueryObjects)) {
            return true;
        }
        if (CollUtil.isEmpty(queryFieldKeys)) {
            return false;
        }
        for (ChangeNoticeObject object : mustQueryObjects) {
            if (object == null || StrUtil.isBlank(object.getObjectId())) {
                continue;
            }
            if (!queryFieldKeys.contains(object.getObjectId().trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按对象角色判断与查询字段 code 是否有交集（通用命中内核）。
     *
     * @param objects        通知对象明细
     * @param objectCtg      对象角色
     * @param queryFieldKeys 查询字段 code 集合
     * @param passWhenEmpty  该角色无配置时是否视为通过（如 AFFECTED_METRIC 未配则通过）
     * @return true 表示命中（或无配置且允许通过）
     */
    public boolean hitByObjectCtg(List<ChangeNoticeObject> objects, ChangeNoticeObjectCtg objectCtg,
                                  Set<String> queryFieldKeys, boolean passWhenEmpty) {
        List<ChangeNoticeObject> targetObjects = listByCtg(objects, objectCtg);
        if (CollUtil.isEmpty(targetObjects)) {
            return passWhenEmpty;
        }
        if (CollUtil.isEmpty(queryFieldKeys)) {
            return false;
        }
        for (ChangeNoticeObject object : targetObjects) {
            if (object == null || StrUtil.isBlank(object.getObjectId())) {
                continue;
            }
            // objectId 存对象编码（字段 code），与查询侧 code 直接比对
            if (queryFieldKeys.contains(object.getObjectId().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按对象角色过滤出与命中字段有交集的对象明细（objectId 在 hitFieldKeys 中）。
     * 常用于 build 阶段「只返回实际命中的 CHANGE」。
     *
     * @param objects      通知对象明细
     * @param objectCtg    对象角色
     * @param hitFieldKeys 命中字段 code（或表名等）集合，与 objectId 比对
     * @return 命中的对象列表，不会为 null
     */
    public List<ChangeNoticeObject> listHitByObjectCtg(List<ChangeNoticeObject> objects,
                                                       ChangeNoticeObjectCtg objectCtg,
                                                       Set<String> hitFieldKeys) {
        List<ChangeNoticeObject> result = new ArrayList<>();
        List<ChangeNoticeObject> targetObjects = listByCtg(objects, objectCtg);
        if (CollUtil.isEmpty(targetObjects) || CollUtil.isEmpty(hitFieldKeys)) {
            return result;
        }
        for (ChangeNoticeObject object : targetObjects) {
            if (object == null || StrUtil.isBlank(object.getObjectId())) {
                continue;
            }
            if (hitFieldKeys.contains(object.getObjectId().trim())) {
                result.add(object);
            }
        }
        return result;
    }

    /**
     * 替换/下线场景：收集命中的 MAPPING 对象。
     *
     * 命中规则：
     * 1. 字段类旧对象：objectId 属于 hitFieldKeys（query 并集 offline）
     * 2. 字段类新对象：relObjectId 属于 queryFieldKeys
     * 3. 目录类（objectType 或 relObjectType 为 METRIC_GROUP/DIM_GROUP/MODULE）：旧/新对象 ID 属于 queryCtgIds
     *
     * @param objects        通知对象明细
     * @param hitFieldKeys   旧对象命中字段（query 并集 offline）
     * @param queryFieldKeys 当前查询字段
     * @param queryCtgIds    当前查询用到的目录 ID
     * @return 命中的 MAPPING 对象，不会为 null
     */
    public List<ChangeNoticeObject> listHitMappingObjectsForReplace(List<ChangeNoticeObject> objects,
                                                                    Set<String> hitFieldKeys,
                                                                    Set<String> queryFieldKeys,
                                                                    Set<String> queryCtgIds) {
        List<ChangeNoticeObject> result = new ArrayList<>();
        List<ChangeNoticeObject> mappingObjects = listByCtg(objects, ChangeNoticeObjectCtg.MAPPING);
        if (CollUtil.isEmpty(mappingObjects)) {
            return result;
        }
        for (ChangeNoticeObject object : mappingObjects) {
            if (object == null) {
                continue;
            }
            if (isHitReplaceMappingObject(object, hitFieldKeys, queryFieldKeys, queryCtgIds)) {
                result.add(object);
            }
        }
        return result;
    }

    /**
     * 判断单条 MAPPING 是否在替换/下线场景命中（旧对象侧或新对象侧任一即可）。
     *
     * @param object         替换映射对象
     * @param hitFieldKeys   旧对象字段命中集
     * @param queryFieldKeys 查询字段集
     * @param queryCtgIds    查询目录集
     * @return true 表示命中
     */
    private boolean isHitReplaceMappingObject(ChangeNoticeObject object, Set<String> hitFieldKeys,
                                              Set<String> queryFieldKeys, Set<String> queryCtgIds) {
        return isHitOldReplaceObject(object, hitFieldKeys, queryCtgIds)
                || isHitNewReplaceObject(object, queryFieldKeys, queryCtgIds);
    }

    /**
     * 判断旧对象侧是否命中（催迁移：查询仍在用旧字段/旧目录）。
     *
     * METRIC_GROUP/DIM_GROUP/MODULE：objectId 属于 queryCtgIds；其他类型：objectId 属于 hitFieldKeys。
     *
     * @param object       变更对象
     * @param hitFieldKeys 旧对象字段命中集（query 并集 offline）
     * @param queryCtgIds  查询目录集
     * @return true 表示旧对象命中
     */
    public boolean isHitOldReplaceObject(ChangeNoticeObject object, Set<String> hitFieldKeys,
                                         Set<String> queryCtgIds) {
        if (object == null || StrUtil.isBlank(object.getObjectId())) {
            return false;
        }
        String objectId = object.getObjectId().trim();
        if (isCtgObjectType(object.getObjectType())) {
            return CollUtil.isNotEmpty(queryCtgIds) && queryCtgIds.contains(objectId);
        }
        return CollUtil.isNotEmpty(hitFieldKeys) && hitFieldKeys.contains(objectId);
    }

    /**
     * 判断新对象侧是否命中（查询已在用新字段/新目录）。
     *
     * 使用 relObjectId；若 objectType 或 relObjectType 为 METRIC_GROUP/DIM_GROUP/MODULE，则按目录 ID 比对，否则按字段 code。
     *
     * @param object         变更对象
     * @param queryFieldKeys 查询字段集
     * @param queryCtgIds    查询目录集
     * @return true 表示新对象命中
     */
    public boolean isHitNewReplaceObject(ChangeNoticeObject object, Set<String> queryFieldKeys,
                                         Set<String> queryCtgIds) {
        if (object == null || StrUtil.isBlank(object.getRelObjectId())) {
            return false;
        }
        String relObjectId = object.getRelObjectId().trim();
        if (isCtgObjectType(object.getObjectType())
                || isCtgObjectType(object.getRelObjectType())) {
            return CollUtil.isNotEmpty(queryCtgIds) && queryCtgIds.contains(relObjectId);
        }
        return CollUtil.isNotEmpty(queryFieldKeys) && queryFieldKeys.contains(relObjectId);
    }

    /**
     * 组装替换/下线出参：命中对象 + 提醒路径标记 + 是否需要一键替换。
     *
     * 标记说明：
     * 1. hitRemindOld / hitRemindNew：本次是否命中对应提醒路径（0/1）
     * 2. needReplace（对象级）：字段类旧对象命中且配置了新对象（relObjectId 非空）时为 1；
     *    METRIC_GROUP/DIM_GROUP/MODULE 目录类固定为 0（不支持一键替换）
     * 3. needApply（通知级）：任一 changeObject.needReplace=1 时为 1，否则为 0
     *
     * @param notice       通知主表
     * @param hitObjects   命中的 MAPPING 对象
     * @param hitRemindOld 是否命中提醒旧对象
     * @param hitRemindNew 是否命中提醒新对象
     * @param hitFieldKeys 旧对象字段命中集
     * @param queryCtgIds  查询目录集
     * @return 替换/下线 VO
     */
    public ChangeNoticeVO buildReplaceOfflineVO(ChangeNotice notice, List<ChangeNoticeObject> hitObjects,
                                                boolean hitRemindOld, boolean hitRemindNew,
                                                Set<String> hitFieldKeys, Set<String> queryCtgIds) {
        ChangeNoticeVO vo = ChangeNoticeVO.from(notice);
        vo.setHitRemindOld(hitRemindOld ? 1 : 0);
        vo.setHitRemindNew(hitRemindNew ? 1 : 0);
        List<ChangeNoticeObjectVO> objectVOList = new ArrayList<>();
        int needApply = 0;
        if (CollUtil.isNotEmpty(hitObjects)) {
            for (ChangeNoticeObject object : hitObjects) {
                if (object == null) {
                    continue;
                }
                // 字段类：旧对象仍在用且配置了新对象 → 需一键替换；METRIC_GROUP/DIM_GROUP/MODULE 目录类固定不替换
                int needReplace = (!isCtgObjectType(object.getObjectType())
                        && hitRemindOld
                        && isHitOldReplaceObject(object, hitFieldKeys, queryCtgIds)
                        && StrUtil.isNotBlank(object.getRelObjectId())) ? 1 : 0;
                if (needReplace == 1) {
                    needApply = 1;
                }
                objectVOList.add(ChangeNoticeObjectVO.from(object, needReplace));
            }
        }
        vo.setChangeObjects(objectVOList);
        vo.setNeedApply(needApply);
        return vo;
    }

    /**
     * 组装基础出参：主表信息 + 命中的对象明细。
     * 适用于指标口径、维度内容、数据回刷等只需展示命中 CHANGE 的类型。
     *
     * @param notice     通知主表
     * @param hitObjects 命中规则的对象明细（非全部配置）
     * @return 基础 VO
     */
    public ChangeNoticeVO buildBaseVO(ChangeNotice notice, List<ChangeNoticeObject> hitObjects) {
        ChangeNoticeVO vo = ChangeNoticeVO.from(notice);
        vo.setChangeObjects(toObjectVOList(hitObjects));
        return vo;
    }

    /**
     * 组装数据回刷出参：命中的变更对象表 + 命中的提醒范围字段。
     *
     * @param notice           通知主表
     * @param hitChangeObjects 命中的 CHANGE（表名与 usedTableNames 交集）
     * @param hitRemindFields  命中的 REMIND_FIELD（与 queryFieldKeys 交集；未配置时为空）
     * @return 含提醒范围的 VO
     */
    public ChangeNoticeVO buildDataBackfillVO(ChangeNotice notice,
                                              List<ChangeNoticeObject> hitChangeObjects,
                                              List<ChangeNoticeObject> hitRemindFields) {
        ChangeNoticeVO vo = buildBaseVO(notice, hitChangeObjects);
        vo.setRemindFieldObjects(toObjectVOList(hitRemindFields));
        return vo;
    }

    /**
     * 组装使用约束出参：不符合规则的变更对象 + 全部必须同查配置（供前端一键应用）。
     *
     * 仅在命中时调用，故 needApply 固定为 1。
     * mustQueryObjects / mustQueryFilters 返回通知侧全部配置，而非仅未满足的部分。
     *
     * @param notice                    通知主表
     * @param nonCompliantChangeObjects 不符合规则的 CHANGE（查询已用到且必须同查未满足）
     * @param objects                   通知全部对象明细（用于取 MUST_QUERY）
     * @param filters                   通知筛选配置
     * @return 含同查信息的 VO
     */
    public ChangeNoticeVO buildUsageConstraintVO(ChangeNotice notice,
                                                 List<ChangeNoticeObject> nonCompliantChangeObjects,
                                                 List<ChangeNoticeObject> objects,
                                                 List<ChangeNoticeObjectFilter> filters) {
        ChangeNoticeVO vo = buildBaseVO(notice, nonCompliantChangeObjects);
        vo.setMustQueryObjects(toObjectVOList(listByCtg(objects, ChangeNoticeObjectCtg.MUST_QUERY)));
        vo.setMustQueryFilters(toFilterVOList(filters));
        // 使用约束命中即可一键应用必须同查
        vo.setNeedApply(1);
        return vo;
    }

    /**
     * 按对象角色过滤明细列表（与实体 objectCtg 忽略大小写比较）。
     *
     * @param objects   通知对象明细
     * @param objectCtg 对象角色枚举
     * @return 匹配该角色的对象列表，不会为 null
     */
    public List<ChangeNoticeObject> listByCtg(List<ChangeNoticeObject> objects, ChangeNoticeObjectCtg objectCtg) {
        List<ChangeNoticeObject> result = new ArrayList<>();
        if (CollUtil.isEmpty(objects) || objectCtg == null) {
            return result;
        }
        String ctgCode = objectCtg.getCode();
        for (ChangeNoticeObject object : objects) {
            if (object != null && ctgCode.equalsIgnoreCase(object.getObjectCtg())) {
                result.add(object);
            }
        }
        return result;
    }

    /**
     * 将 remindTargets（逗号分隔）解析为提醒目标枚举集合，只解析一次供多处判断复用。
     *
     * @param remindTargets 提醒目标配置，逗号分隔
     * @return 已勾选的目标集合，不会为 null；空配置返回 emptySet
     */
    public Set<ChangeNoticeRemindTarget> parseRemindTargets(String remindTargets) {
        if (StrUtil.isBlank(remindTargets)) {
            return Collections.emptySet();
        }
        Set<ChangeNoticeRemindTarget> targets = EnumSet.noneOf(ChangeNoticeRemindTarget.class);
        for (String part : remindTargets.split(",")) {
            ChangeNoticeRemindTarget target = ChangeNoticeRemindTarget.fromCode(part.trim());
            if (target != null) {
                targets.add(target);
            }
        }
        return targets;
    }

    /**
     * 判断已解析的提醒目标集合是否包含指定目标。
     *
     * @param remindTargets 已解析的目标集合
     * @param target        提醒目标枚举
     * @return true 表示已勾选
     */
    public boolean hasRemindTarget(Set<ChangeNoticeRemindTarget> remindTargets, ChangeNoticeRemindTarget target) {
        return CollUtil.isNotEmpty(remindTargets) && target != null && remindTargets.contains(target);
    }

    /**
     * 判断通知是否勾选了指定提醒目标（内部先 parse 再判断；多次判断请优先用 {@link #parseRemindTargets}）。
     *
     * @param remindTargets 提醒目标配置，逗号分隔
     * @param target        提醒目标枚举
     * @return true 表示已勾选
     */
    public boolean hasRemindTarget(String remindTargets, ChangeNoticeRemindTarget target) {
        return hasRemindTarget(parseRemindTargets(remindTargets), target);
    }

    /**
     * 解析通知上的替换任务 ID（支持逗号分隔多个）。
     *
     * @param replaceTaskId 替换任务 ID，多个逗号分隔
     * @return 去重后的任务 ID 集合，不会为 null
     */
    public Set<String> parseReplaceTaskIds(String replaceTaskId) {
        if (StrUtil.isBlank(replaceTaskId)) {
            return Collections.emptySet();
        }
        Set<String> taskIds = new HashSet<>();
        for (String part : replaceTaskId.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                taskIds.add(part.trim());
            }
        }
        return taskIds;
    }

    /**
     * 判断当前视图是否已在指定替换任务下执行过模板替换。
     *
     * 基于上下文预加载的 viewReplaceLogKeys，避免逐条通知查库。
     * replaceTaskId 支持逗号分隔多个，任一任务命中即返回 true。
     * 匹配键格式见 {@link #buildReplaceLogKey}。
     *
     * @param viewId        视图 ID
     * @param replaceTaskId 通知关联的替换任务 ID，多个逗号分隔
     * @param context       匹配上下文
     * @return true 表示任一 viewId|replaceTaskId 在预加载日志中存在
     */
    public boolean existsViewInReplaceLog(String viewId, String replaceTaskId, ChangeNoticeMatchContext context) {
        if (context == null || StrUtil.isBlank(viewId) || StrUtil.isBlank(replaceTaskId)) {
            return false;
        }
        Set<String> keys = context.getViewReplaceLogKeys();
        if (CollUtil.isEmpty(keys)) {
            return false;
        }
        for (String taskId : parseReplaceTaskIds(replaceTaskId)) {
            if (keys.contains(buildReplaceLogKey(viewId, taskId))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建替换日志匹配键：viewId|replaceTaskId（两侧均 trim）。
     *
     * @param viewId        视图 ID
     * @param replaceTaskId 单个替换任务 ID（非逗号串）
     * @return 组合键
     */
    public static String buildReplaceLogKey(String viewId, String replaceTaskId) {
        return viewId.trim() + REPLACE_LOG_KEY_SEPARATOR + replaceTaskId.trim();
    }

    /**
     * 将查询字段 code 与已下线字段 code 合并（仅替换/下线 REMIND_OLD 场景使用）。
     * 用于覆盖「字段已下线但仍被引用、前端携带 offlineFieldIds」的催迁移提醒。
     *
     * @param queryFieldKeys    查询配置字段 code 集合
     * @param offlineFieldCodes 已下线字段 code 集合
     * @return 合并后的命中字段集合（新建 Set，不修改入参）
     */
    public Set<String> mergeOfflineFieldKeys(Set<String> queryFieldKeys, List<String> offlineFieldCodes) {
        Set<String> merged = new HashSet<>();
        if (CollUtil.isNotEmpty(queryFieldKeys)) {
            merged.addAll(queryFieldKeys);
        }
        if (CollUtil.isEmpty(offlineFieldCodes)) {
            return merged;
        }
        for (String fieldCode : offlineFieldCodes) {
            if (StrUtil.isBlank(fieldCode)) {
                continue;
            }
            merged.add(fieldCode.trim());
        }
        return merged;
    }

    /**
     * 提取字段筛选值 id 集合（取 FieldValue.id，trim 后去重）。
     *
     * @param values 引擎字段取值
     * @return id 集合，不会为 null
     */
    private Set<String> extractFieldValueIds(List<FieldValue> values) {
        Set<String> ids = new HashSet<>();
        if (CollUtil.isEmpty(values)) {
            return ids;
        }
        for (FieldValue value : values) {
            if (value != null && StrUtil.isNotBlank(value.getId())) {
                ids.add(value.getId().trim());
            }
        }
        return ids;
    }

    /**
     * 对象明细转 VO 列表（跳过 null 元素）。
     *
     * @param objects 实体列表
     * @return VO 列表，不会为 null
     */
    private List<ChangeNoticeObjectVO> toObjectVOList(List<ChangeNoticeObject> objects) {
        List<ChangeNoticeObjectVO> result = new ArrayList<>();
        if (CollUtil.isEmpty(objects)) {
            return result;
        }
        for (ChangeNoticeObject object : objects) {
            if (object != null) {
                result.add(ChangeNoticeObjectVO.from(object));
            }
        }
        return result;
    }

    /**
     * 筛选实体转 VO 列表（跳过 null 元素）。
     *
     * @param filters 实体列表
     * @return VO 列表，不会为 null
     */
    private List<ChangeNoticeObjectFilterVO> toFilterVOList(List<ChangeNoticeObjectFilter> filters) {
        List<ChangeNoticeObjectFilterVO> result = new ArrayList<>();
        if (CollUtil.isEmpty(filters)) {
            return result;
        }
        for (ChangeNoticeObjectFilter filter : filters) {
            if (filter != null) {
                result.add(ChangeNoticeObjectFilterVO.from(filter));
            }
        }
        return result;
    }
}
