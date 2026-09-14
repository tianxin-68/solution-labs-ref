package com.bi.queryer.ssm.changenotice.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.changenotice.entity.ChangeNotice;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeLog;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeNotifyCount;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObject;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObjectFilter;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeUserStat;
import com.bi.queryer.ssm.changenotice.entity.TemplateFieldReplaceLog;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeRemindTarget;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeTriggerScope;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeType;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchContext;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchHelper;
import com.bi.queryer.ssm.changenotice.match.strategy.ChangeNoticeMatchStrategy;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeVO;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.lod.LodUIConfigure;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 变更通知匹配服务。
 * 负责公共编排：解析查询配置、加载候选通知、触发范围过滤，再按 changeType 分发到对应策略。
 */
@Service
@Slf4j
public class ChangeNoticeService {

    /** 变更通知匹配总开关配置项 */
    private static final String CHANGE_NOTICE_MATCH_ENABLE_KEY = "ssm.change.notice.match.enable";

    /** 同一通知对同一用户的最大通知次数配置项 */
    private static final String CHANGE_NOTICE_NOTIFY_MAX_TIMES_KEY = "ssm.change.notice.notify.max.times";

    /** 同一通知对同一用户的默认最大通知次数 */
    private static final int CHANGE_NOTICE_NOTIFY_MAX_TIMES_DEFAULT = 3;

    /** 已读日志异步入库最大并发数 */
    private static final int ACK_LOG_MAX_CONCURRENCY = 3;

    /** 已读日志异步写入线程池 */
    private static final ExecutorService ACK_LOG_EXECUTOR = Executors.newFixedThreadPool(ACK_LOG_MAX_CONCURRENCY);

    @Autowired
    private BaseDao dao;

    /** 匹配公共能力（字段合并、提醒目标解析等） */
    @Autowired
    private ChangeNoticeMatchHelper matchHelper;

    /** Spring 注入的全部匹配策略实现 */
    @Autowired
    private List<ChangeNoticeMatchStrategy> matchStrategies;

    /** 变更类型 -> 策略映射，启动时由 initStrategyMap 初始化 */
    private Map<ChangeNoticeType, ChangeNoticeMatchStrategy> strategyMap = new EnumMap<>(ChangeNoticeType.class);

    /**
     * 将注入的策略列表注册到 EnumMap，便于按类型 O(1) 查找
     */
    @PostConstruct
    public void initStrategyMap() {
        Map<ChangeNoticeType, ChangeNoticeMatchStrategy> map = new EnumMap<>(ChangeNoticeType.class);
        if (CollUtil.isNotEmpty(matchStrategies)) {
            for (ChangeNoticeMatchStrategy strategy : matchStrategies) {
                if (strategy == null || strategy.supportType() == null) {
                    continue;
                }
                map.put(strategy.supportType(), strategy);
            }
        }
        this.strategyMap = map;
    }

    /**
     * 根据查询配置匹配命中的变更通知。
     * 处理流程：
     * 1. AES 解密 queryConfig（含 AES: 前缀时），再解析并提取字段
     * 2. 查询 ACTIVE 且在时间窗内的候选通知；无 LOCAL/DATA_BACKFILL 时跳过 createEngine 抽表名
     * 3. 按 GLOBAL/LOCAL 做触发范围过滤
     * 4. 若 checkHistoryNotify=1（默认）：按 viewId + 当前用户剔除已读日志；并按用户累计通知次数上限过滤
     * 5. 批量加载对象/筛选后，按 changeType 分发策略执行 matchAndBuild（一次完成命中与组装）
     * 6. offlineFieldIds 仅由 REPLACE_OFFLINE 策略参与命中（先转 code，并入 context.hitFieldKeys）
     * 7. 按候选通知 replaceTaskId 批量加载 template_field_replace_log，供 REPLACE_OFFLINE 新对象提醒内存匹配
     * 8. 命中结果统一批量回填 publishByRealName（publishBy 优先 updatedBy，否则 createdBy）
     * 总开关：配置项 ssm.change.notice.match.enable，关闭时直接返回空列表。
     * @param queryConfig 查询配置 JSON（AES 加密，含 AES: 前缀），空或解析失败时返回空列表
     * @param offlineFieldIds 已下线字段 ID，仅替换/下线场景使用；可为空
     * @param viewId 当前视图 ID，新对象提醒与已读过滤使用；可为空（为空时仅做次数上限过滤）
     * @param checkHistoryNotify 是否校验历史已通知：1=校验（默认），0=不校验；null 按 1 处理
     * @return 命中的通知列表，不会为 null
     */
    @SuppressWarnings("unchecked")
    public List<ChangeNoticeVO> matchHitNotices(String queryConfig, List<String> offlineFieldIds,
                                                String viewId, Integer checkHistoryNotify) {
        // 总开关关闭时不做变更通知匹配
        boolean isEnableChangeNoticeMatch = "true".equalsIgnoreCase(
                SC.v(CHANGE_NOTICE_MATCH_ENABLE_KEY, "true"));
        if (!isEnableChangeNoticeMatch) {
            return Collections.emptyList();
        }
        if (StrUtil.isBlank(queryConfig)) {
            return Collections.emptyList();
        }

        // 前端传入的 queryConfig 为 AES 加密，先解密再解析
        String plainQueryConfig = SSDUtil.decryptTplConfig(queryConfig);
        if (StrUtil.isBlank(plainQueryConfig) || "{}".equals(plainQueryConfig.trim())) {
            log.warn("变更通知匹配：queryConfig解密失败或结果为空，返回空列表");
            return Collections.emptyList();
        }

        QueryConfigure configure;
        List<QueryField> fields;
        try {
            configure = new QueryConfigure();
            configure.load(plainQueryConfig);
            // 不走 configure.getAllFields：result 与 filter 按 code 有交集，简单拼接会导致 isFilter 不正确
            fields = parseAllFieldsPreserveFilter(configure);
        } catch (Exception e) {
            log.warn("变更通知匹配：解析queryConfig失败，返回空列表", e);
            return Collections.emptyList();
        }

        List<ChangeNotice> candidates = (List<ChangeNotice>) dao.queryObjectList(
                "ssm.changenotice.listActiveNotice", null, DataSourceType.Data_Studio);
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }

        // 无 LOCAL / DATA_BACKFILL 时跳过 createEngine（表名仅这两类需要）
        Set<String> usedTableNames = needsModelTableNames(candidates)
                ? extractModelTableNames(configure)
                : Collections.emptySet();

        // 触发范围过滤后，再按候选通知的 replaceTaskId 加载替换日志，避免全表扫描
        List<ChangeNotice> scopeMatched = candidates.stream()
                .filter(n -> matchTriggerScope(n, usedTableNames))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(scopeMatched)) {
            return Collections.emptyList();
        }

        String normalizedViewId = StrUtil.blankToDefault(viewId, null);
        // 未传时默认 YES：已读视图不再通知 + 同一用户对同一通知次数上限
        if (Enabled.value(checkHistoryNotify == null ? Enabled.YES.getId() : checkHistoryNotify)) {
            scopeMatched = filterNotifiedNotices(scopeMatched, normalizedViewId);
            if (CollUtil.isEmpty(scopeMatched)) {
                return Collections.emptyList();
            }
        }

        Set<String> queryFieldKeys = buildQueryFieldKeys(fields);
        List<String> offlineFieldCodes = toOfflineFieldCodes(offlineFieldIds);
        ChangeNoticeMatchContext context = ChangeNoticeMatchContext.builder()
                .queryFieldKeys(queryFieldKeys)
                .hitFieldKeys(matchHelper.mergeOfflineFieldKeys(queryFieldKeys, offlineFieldCodes))
                .filterFieldMap(buildFilterFieldMap(fields))
                .usedTableNames(usedTableNames)
                .queryCtgIds(buildQueryCtgIds(fields))
                .viewId(normalizedViewId)
                .viewReplaceLogKeys(loadViewReplaceLogKeys(scopeMatched, normalizedViewId))
                .build();

        List<Long> noticeIds = scopeMatched.stream().map(ChangeNotice::getId).collect(Collectors.toList());
        Map<Long, List<ChangeNoticeObject>> objectMap = groupObjectsByNoticeId(
                (List<ChangeNoticeObject>) dao.queryObjectList(
                        "ssm.changenotice.listNoticeObject", noticeIds, DataSourceType.Data_Studio));
        Map<Long, List<ChangeNoticeObjectFilter>> filterMap = groupFiltersByNoticeId(
                (List<ChangeNoticeObjectFilter>) dao.queryObjectList(
                        "ssm.changenotice.listNoticeObjectFilter", noticeIds, DataSourceType.Data_Studio));
        List<ChangeNoticeVO> hitNotices = dispatchByChangeType(scopeMatched, objectMap, filterMap, context);
        fillPublishByRealName(hitNotices);
        return hitNotices;
    }

    /**
     * 对命中结果统一批量回填发布人真实姓名（只查一次用户表）。
     * @param hitNotices 命中的通知 VO 列表
     */
    @SuppressWarnings("unchecked")
    private void fillPublishByRealName(List<ChangeNoticeVO> hitNotices) {
        if (CollUtil.isEmpty(hitNotices)) {
            return;
        }
        List<String> publishByList = hitNotices.stream()
                .map(ChangeNoticeVO::getPublishBy)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(publishByList)) {
            return;
        }
        List<User> userList = (List<User>) dao.queryObjectList("user.queryUserListByNames", publishByList);
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        Map<String, String> realNameMap = userList.stream()
                .filter(user -> user != null && StrUtil.isNotBlank(user.getName()))
                .collect(Collectors.toMap(User::getName, User::getRealName, (a, b) -> a));
        for (ChangeNoticeVO vo : hitNotices) {
            if (vo == null || StrUtil.isBlank(vo.getPublishBy())) {
                continue;
            }
            vo.setPublishByRealName(realNameMap.get(vo.getPublishBy()));
        }
    }

    /**
     * 确认已读：异步写入 ssm_change_notice_log，并累加 ssm_change_notice_user_stat 通知次数。
     * 不校验历史是否已存在，直接入库；入库走独立线程池，最大并发 3。
     * @param viewId 视图 ID，不能为空
     * @param noticeIds 已确认的通知 ID 列表
     */
    public void acknowledgeNotices(String viewId, List<Long> noticeIds) {
        if (StrUtil.isBlank(viewId) || CollUtil.isEmpty(noticeIds)) {
            return;
        }
        // 主线程取用户名，避免异步线程丢失登录上下文
        String createdBy = UserManager.get().getName();
        String normalizedViewId = viewId.trim();
        List<ChangeNoticeLog> toInsert = new ArrayList<>();
        List<ChangeNoticeUserStat> toUpsertStat = new ArrayList<>();
        Set<Long> distinctNoticeIds = new HashSet<>();
        for (Long noticeId : noticeIds) {
            if (noticeId == null || !distinctNoticeIds.add(noticeId)) {
                continue;
            }
            ChangeNoticeLog logEntity = new ChangeNoticeLog();
            logEntity.setViewId(normalizedViewId);
            logEntity.setNoticeId(noticeId);
            logEntity.setCreatedBy(createdBy);
            toInsert.add(logEntity);

            ChangeNoticeUserStat stat = new ChangeNoticeUserStat();
            stat.setCreatedBy(createdBy);
            stat.setUpdatedBy(createdBy);
            stat.setNoticeId(noticeId);
            toUpsertStat.add(stat);
        }
        if (CollUtil.isEmpty(toInsert)) {
            return;
        }
        ACK_LOG_EXECUTOR.execute(() -> {
            try {
                dao.insert("ssm.changenotice.batchInsertNoticeLog", toInsert);
                dao.insert("ssm.changenotice.batchUpsertUserStat", toUpsertStat);
            } catch (Exception e) {
                log.warn("变更通知已读日志/次数统计异步入库失败, viewId={}, noticeSize={}",
                        normalizedViewId, toInsert.size(), e);
            }
        });
    }

    /**
     * 剔除不再通知的候选通知：
     * 1. 当前用户在指定视图下已读过的通知（viewId 为空时跳过本条）
     * 2. 当前用户对该通知累计通知次数已达上限（配置 ssm.change.notice.notify.max.times，默认 5）
     * @param notices 候选通知
     * @param viewId 视图 ID，可为空
     * @return 过滤后的列表
     */
    private List<ChangeNotice> filterNotifiedNotices(List<ChangeNotice> notices, String viewId) {
        if (CollUtil.isEmpty(notices)) {
            return notices;
        }
        String createdBy = UserManager.get().getName();
        List<Long> noticeIds = notices.stream()
                .filter(n -> n != null && n.getId() != null)
                .map(ChangeNotice::getId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(noticeIds)) {
            return notices;
        }

        // 1）本视图已读：同 viewId + 用户不再通知
        Set<Long> viewNotifiedIds = Collections.emptySet();
        if (StrUtil.isNotBlank(viewId)) {
            viewNotifiedIds = loadNotifiedNoticeIds(viewId, createdBy, noticeIds);
        }

        // 2）用户累计次数：同一通知对同一用户最多通知 N 次
        int maxTimes = resolveNotifyMaxTimes();
        Set<Long> overLimitIds = loadOverLimitNoticeIds(createdBy, noticeIds, maxTimes);

        if (CollUtil.isEmpty(viewNotifiedIds) && CollUtil.isEmpty(overLimitIds)) {
            return notices;
        }
        Set<Long> finalViewNotifiedIds = viewNotifiedIds;
        return notices.stream()
                .filter(n -> n != null && n.getId() != null)
                .filter(n -> !finalViewNotifiedIds.contains(n.getId()))
                .filter(n -> !overLimitIds.contains(n.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 解析同一通知对同一用户的最大通知次数，配置异常时回退默认值 5。
     * @return 最大次数，至少为 1
     */
    private int resolveNotifyMaxTimes() {
        String configValue = SC.v(CHANGE_NOTICE_NOTIFY_MAX_TIMES_KEY,
                String.valueOf(CHANGE_NOTICE_NOTIFY_MAX_TIMES_DEFAULT));
        try {
            int maxTimes = Integer.parseInt(configValue.trim());
            return maxTimes > 0 ? maxTimes : CHANGE_NOTICE_NOTIFY_MAX_TIMES_DEFAULT;
        } catch (Exception e) {
            log.warn("变更通知最大次数配置非法，使用默认值{}，config={}",
                    CHANGE_NOTICE_NOTIFY_MAX_TIMES_DEFAULT, configValue);
            return CHANGE_NOTICE_NOTIFY_MAX_TIMES_DEFAULT;
        }
    }

    /**
     * 查询已达到通知次数上限的 notice_id 集合
     * @param createdBy 用户域账号
     * @param noticeIds 候选通知 ID
     * @param maxTimes 最大通知次数
     * @return 已达上限的 notice_id
     */
    @SuppressWarnings("unchecked")
    private Set<Long> loadOverLimitNoticeIds(String createdBy, List<Long> noticeIds, int maxTimes) {
        Map<String, Object> params = new HashMap<>();
        params.put("createdBy", createdBy);
        params.put("noticeIds", noticeIds);
        List<ChangeNoticeNotifyCount> countList = (List<ChangeNoticeNotifyCount>) dao.queryObjectList(
                "ssm.changenotice.listNotifyCountByUser", params);
        if (CollUtil.isEmpty(countList)) {
            return Collections.emptySet();
        }
        Set<Long> overLimitIds = new HashSet<>();
        for (ChangeNoticeNotifyCount item : countList) {
            if (item == null || item.getNoticeId() == null || item.getNotifyCount() == null) {
                continue;
            }
            if (item.getNotifyCount() >= maxTimes) {
                overLimitIds.add(item.getNoticeId());
            }
        }
        return overLimitIds;
    }

    /**
     * 查询已通知过的 notice_id 集合
     * @param viewId 视图 ID
     * @param createdBy 用户域账号
     * @param noticeIds 候选通知 ID
     * @return 已读 notice_id 集合
     */
    @SuppressWarnings("unchecked")
    private Set<Long> loadNotifiedNoticeIds(String viewId, String createdBy, List<Long> noticeIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("viewId", viewId);
        params.put("createdBy", createdBy);
        params.put("noticeIds", noticeIds);
        List<Long> notified = (List<Long>) dao.queryObjectList("ssm.changenotice.listNotifiedNoticeIds", params);
        if (CollUtil.isEmpty(notified)) {
            return Collections.emptySet();
        }
        return new HashSet<>(notified);
    }

    /**
     * 按变更类型分发到对应策略执行 matchAndBuild；未知类型或无策略实现则跳过
     * @param scopeMatched 已通过触发范围过滤的通知
     * @param objectMap noticeId -> 对象明细
     * @param filterMap noticeId -> 筛选明细
     * @param context 匹配上下文
     * @return 命中后组装的 VO 列表
     */
    private List<ChangeNoticeVO> dispatchByChangeType(List<ChangeNotice> scopeMatched,
                                                      Map<Long, List<ChangeNoticeObject>> objectMap,
                                                      Map<Long, List<ChangeNoticeObjectFilter>> filterMap,
                                                      ChangeNoticeMatchContext context) {
        List<ChangeNoticeVO> result = new ArrayList<>();
        for (ChangeNotice notice : scopeMatched) {
            ChangeNoticeType type = ChangeNoticeType.fromCode(notice.getChangeType());
            // 无法识别的 changeType 直接跳过
            if (type == null) {
                continue;
            }
            ChangeNoticeMatchStrategy strategy = strategyMap.get(type);
            if (strategy == null) {
                continue;
            }
            List<ChangeNoticeObject> objects = objectMap.getOrDefault(notice.getId(), Collections.emptyList());
            List<ChangeNoticeObjectFilter> filters = filterMap.getOrDefault(notice.getId(), Collections.emptyList());
            ChangeNoticeVO vo = strategy.matchAndBuild(notice, objects, filters, context);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 候选通知是否需要模型表名（从而触发 createEngine）。
     * LOCAL：触发范围表名交集；DATA_BACKFILL：出参 CHANGE 按表名过滤。
     * @param notices 候选通知
     * @return true 表示需要抽表名
     */
    private boolean needsModelTableNames(List<ChangeNotice> notices) {
        if (CollUtil.isEmpty(notices)) {
            return false;
        }
        for (ChangeNotice notice : notices) {
            if (notice == null) {
                continue;
            }
            if (ChangeNoticeTriggerScope.fromCode(notice.getTriggerScope())
                    == ChangeNoticeTriggerScope.LOCAL) {
                return true;
            }
            if (ChangeNoticeType.fromCode(notice.getChangeType())
                    == ChangeNoticeType.DATA_BACKFILL) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过 QueryFactory.createEngine 构建引擎，从引擎模型中提取物理表名。
     * @param configure 已 load 的查询配置
     * @return 表名集合，不会为 null；构建失败时返回空集合
     */
    private Set<String> extractModelTableNames(QueryConfigure configure) {
        Set<String> tableNames = new HashSet<>();
        try {
            // 匹配场景不做权限拦截，并启用同 code 全表构建优化（与 createEngine 入口一致）
            if (configure.getSettings() != null) {
                configure.getSettings().setAclCheck(false);
                configure.getSettings().setEnableCreateAllTableBySameCodeOpt(true);
            }
            // LOD：LodQueryEngine.createModels / LodQueryConfigureCreator 会 clone()+load()，依赖 templateEntity；
            // match 入口仅 load(string) 时若不补齐，clone 后为空配置，最终落到虚拟事实表 (select 1)
            ensureTemplateEntityForClone(configure);

            QueryEngine engine = QueryFactory.createEngine(configure, new QueryContext());
            List<StarModel> models = engine.getModels();
            if (CollUtil.isNotEmpty(models)) {
                for (StarModel model : models) {
                    if (model == null) {
                        continue;
                    }
                    for (QueryTable table : model.getTables()) {
                        appendTableName(tableNames, table);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("变更通知匹配：createEngine获取模型表失败", e);
        }
        return tableNames;
    }

    /**
     * 为无 templateEntity 的 configure 补齐模板实体，保证 clone()+load() 能还原配置。
     * 正常查询入口多为 new QueryConfigure(template)，match 入口是 load(明文 JSON)，二者不一致。
     *
     * @param configure 已 load 的查询配置
     */
    private void ensureTemplateEntityForClone(QueryConfigure configure) {
        if (configure == null || configure.getTemplateEntity() != null) {
            return;
        }
        if (StrUtil.isBlank(configure.getConfig())) {
            return;
        }
        SSDQueryTemplate template = new SSDQueryTemplate();
        template.setConfig(configure.getConfig());
        configure.setTemplateEntity(template);
    }

    /**
     * 将 QueryTable 物理表全名写入集合（含 schema，与 local_route_table_names 对齐，如 bi_olap.xxx）。
     * 跳过虚拟表（如 LOD 无事实表时的占位表 (select 1)）。
     * @param tableNames 目标集合
     * @param table 查询表
     */
    private void appendTableName(Set<String> tableNames, QueryTable table) {
        if (table == null || table.getMeta() == null) {
            return;
        }
        // 虚拟事实表名固定为 (select 1)，不参与 LOCAL 路由匹配
        if (Boolean.TRUE.equals(table.getVirtual())) {
            return;
        }
        MetaTable meta = table.getMeta();
        String fullName = meta.getFullName();
        if (StrUtil.isNotBlank(fullName)) {
            tableNames.add(fullName.trim());
            return;
        }
        if (StrUtil.isNotBlank(meta.getName())) {
            tableNames.add(meta.getName().trim());
        }
    }

    /**
     * 根据 queryConfig 字段构建命中用字段 code 集合（不含已下线字段；与 offline 合并见 hitFieldKeys）
     * @param fields 查询字段
     * @return 字段 code 集合
     */
    private Set<String> buildQueryFieldKeys(List<QueryField> fields) {
        Set<String> keys = new HashSet<>();
        if (CollUtil.isEmpty(fields)) {
            return keys;
        }
        for (QueryField field : fields) {
            String code = resolveFieldCode(field);
            if (StrUtil.isNotBlank(code)) {
                keys.add(code);
            }
        }
        return keys;
    }

    /**
     * 解析字段对应的原始指标 code：
     * LOD 字段本身 code 不可靠（见 UILodNormalizer 会清空其 code/name），需取 customFieldConfigure.lodConfig
     * 中记录的原始指标 measureCode（为空时按 measureId 查 MetaField 兜底）；非 LOD 字段直接取 field.getCode()。
     * 最终统一去除日均（_avg_by_d）/非空日均（_avg_by_d_r）聚合后缀，还原为原始指标 code。
     * @param field 查询字段
     * @return 原始指标 code；无法解析时返回空串
     */
    private String resolveFieldCode(QueryField field) {
        if (field == null) {
            return "";
        }
        String code;
        if (field.isLodField() && field.getCustomFieldConfigure() != null) {
            LodUIConfigure lodConfig = field.getCustomFieldConfigure().getLodConfig();
            code = lodConfig == null ? "" : lodConfig.getMeasureCode();
            if (StrUtil.isBlank(code) && lodConfig != null && StrUtil.isNotBlank(lodConfig.getMeasureId())) {
                MetaField measureMeta = SSDMetaCacheManager.getField(lodConfig.getMeasureId().trim());
                code = measureMeta == null ? "" : measureMeta.getCode();
            }
        } else {
            code = field.getCode();
        }
        if (StrUtil.isBlank(code)) {
            return "";
        }
        code = code.trim()
                .replace("_" + AggExpressionType.Avg_By_Day.getCode(), "")
                .replace("_" + AggExpressionType.Avg_By_Day_Real.getCode(), "");
        return code.trim();
    }

    /**
     * 从查询字段收集目录 ID，供 REPLACE_OFFLINE 的 METRIC_GROUP/DIM_GROUP/MODULE 对象命中。
     * 来源：QueryField.moduleCtgId + MetaField.categoryIdList（不再使用 QueryField.ctgId）。
     * 排除公共日期字段，避免其目录干扰命中。
     * @param fields 查询字段
     * @return 目录 ID 集合，不会为 null
     */
    private Set<String> buildQueryCtgIds(List<QueryField> fields) {
        Set<String> ctgIds = new HashSet<>();
        if (CollUtil.isEmpty(fields)) {
            return ctgIds;
        }
        for (QueryField field : fields) {
            if (field == null) {
                continue;
            }
            MetaField metaField = field.getMeta();
            if (metaField == null && StrUtil.isNotBlank(field.getId())) {
                metaField = SSDMetaCacheManager.getField(field.getId().trim());
            }
            // 公共日期不参与目录收集
            if (field.isCommonDate()
                    || (metaField != null && Enabled.isTrue(metaField.getIsCommonDate()))) {
                continue;
            }
            if (StrUtil.isNotBlank(field.getModuleCtgId())) {
                ctgIds.add(field.getModuleCtgId().trim());
            }
            if (metaField == null || CollUtil.isEmpty(metaField.getCategoryIdList())) {
                continue;
            }
            for (String categoryId : metaField.getCategoryIdList()) {
                if (StrUtil.isNotBlank(categoryId)) {
                    ctgIds.add(categoryId.trim());
                }
            }
        }
        return ctgIds;
    }

    /**
     * 将已下线字段 ID 转为字段 code，供 REPLACE_OFFLINE 命中使用
     * @param offlineFieldIds 字段 ID 列表
     * @return 字段 code 列表；无法映射的 id 将被忽略
     */
    private List<String> toOfflineFieldCodes(List<String> offlineFieldIds) {
        if (CollUtil.isEmpty(offlineFieldIds)) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<>();
        for (String fieldId : offlineFieldIds) {
            if (StrUtil.isBlank(fieldId)) {
                continue;
            }
            MetaField metaField = SSDMetaCacheManager.getField(fieldId.trim());
            if (metaField == null || StrUtil.isBlank(metaField.getCode())) {
                continue;
            }
            codes.add(metaField.getCode().trim());
        }
        return codes;
    }

    /**
     * 按候选通知的 replaceTaskId 批量加载替换日志，构建 viewId|replaceTaskId 键集合。
     * 仅收集 REPLACE_OFFLINE 且配置了 REMIND_NEW 的任务 ID，避免全表扫描。
     * @param notices 已通过触发范围过滤的候选通知
     * @param viewId 当前视图 ID，非空时进一步按 view_id 过滤
     * @return 替换日志键集合，不会为 null
     */
    @SuppressWarnings("unchecked")
    private Set<String> loadViewReplaceLogKeys(List<ChangeNotice> notices, String viewId) {
        Set<String> keys = new HashSet<>();
        List<String> replaceTaskIds = collectRemindNewReplaceTaskIds(notices);
        if (CollUtil.isEmpty(replaceTaskIds)) {
            return keys;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("replaceTaskIds", replaceTaskIds);
        if (StrUtil.isNotBlank(viewId)) {
            params.put("viewId", viewId.trim());
        }
        List<TemplateFieldReplaceLog> logs = (List<TemplateFieldReplaceLog>) dao.queryObjectList(
                "ssm.changenotice.listReplaceLogByTaskIds", params);
        if (CollUtil.isEmpty(logs)) {
            return keys;
        }
        for (TemplateFieldReplaceLog replaceLog : logs) {
            if (replaceLog == null
                    || StrUtil.isBlank(replaceLog.getViewId())
                    || StrUtil.isBlank(replaceLog.getReplaceTaskId())) {
                continue;
            }
            keys.add(ChangeNoticeMatchHelper.buildReplaceLogKey(
                    replaceLog.getViewId(), replaceLog.getReplaceTaskId()));
        }
        return keys;
    }

    /**
     * 收集需做新对象提醒校验的替换任务 ID。
     * @param notices 候选通知
     * @return 去重后的 replaceTaskId 列表
     */
    private List<String> collectRemindNewReplaceTaskIds(List<ChangeNotice> notices) {
        Set<String> taskIds = new HashSet<>();
        if (CollUtil.isEmpty(notices)) {
            return Collections.emptyList();
        }
        for (ChangeNotice notice : notices) {
            if (notice == null) {
                continue;
            }
            if (ChangeNoticeType.fromCode(notice.getChangeType()) != ChangeNoticeType.REPLACE_OFFLINE) {
                continue;
            }
            // 未配置提醒新对象：无需加载该通知的替换日志
            if (!matchHelper.hasRemindTarget(notice.getRemindTargets(), ChangeNoticeRemindTarget.REMIND_NEW)) {
                continue;
            }
            if (StrUtil.isBlank(notice.getReplaceTaskId())) {
                continue;
            }
            // replaceTaskId 可能为逗号分隔的多个任务 ID
            taskIds.addAll(matchHelper.parseReplaceTaskIds(notice.getReplaceTaskId()));
        }
        return new ArrayList<>(taskIds);
    }

    /**
     * 从已 load 的 QueryConfigure 按 code 去重合并 result + filter 字段。
     * 不调用 {@link QueryConfigure#getAllFields()}：result.getFields() 与 filter.getFields() 存在 code 交集时，
     * 简单 list 拼接会出现同一 code 两条记录，后续按 code 取字段时 isFilter 可能取到 result 侧的 false。
     * 合并规则：同一 code 只保留一条；只要出现在 filter 区则 isFilter=true，并以筛选区字段覆盖。
     *
     * @param configure 已 load 的查询配置
     * @return 按 code 去重后的字段列表；不会为 null
     */
    private List<QueryField> parseAllFieldsPreserveFilter(QueryConfigure configure) {
        Map<String, QueryField> fieldByCode = new LinkedHashMap<>();
        if (configure == null) {
            return new ArrayList<>();
        }
        // 先放入 result（默认非筛选）
        putFieldsByCode(fieldByCode,
                configure.getResult() == null ? null : configure.getResult().getFields(),
                false);
        // 再合并 filter：同 code 覆盖，并强制 isFilter=true
        putFieldsByCode(fieldByCode,
                configure.getFilter() == null ? null : configure.getFilter().getFields(),
                true);
        return new ArrayList<>(fieldByCode.values());
    }

    /**
     * 将字段列表按 code 写入 map；后写入的同 code 覆盖先前记录。
     *
     * @param fieldByCode code -> 字段
     * @param fields      字段列表
     * @param isFilter    是否筛选区字段
     */
    private void putFieldsByCode(Map<String, QueryField> fieldByCode, List<QueryField> fields, boolean isFilter) {
        if (fieldByCode == null || CollUtil.isEmpty(fields)) {
            return;
        }
        for (QueryField field : fields) {
            if (field == null || StrUtil.isBlank(field.getCode())) {
                continue;
            }
            // 附加字段（isAppend=1）不参与变更通知字段解析
            if (field.isAppend()) {
                continue;
            }
            field.setIsFilter(isFilter);
            fieldByCode.put(field.getCode().trim(), field);
        }
    }

    /**
     * 构建当前查询中筛选字段映射，供通知侧 filter 二次校验使用
     * @param fields 全部查询字段
     * @return 字段 code -> QueryField（仅 isFilter=true）
     */
    private Map<String, QueryField> buildFilterFieldMap(List<QueryField> fields) {
        Map<String, QueryField> map = new HashMap<>();
        if (CollUtil.isEmpty(fields)) {
            return map;
        }
        for (QueryField field : fields) {
            if (field == null || !Boolean.TRUE.equals(field.getIsFilter()) || StrUtil.isBlank(field.getCode())) {
                continue;
            }
            map.put(field.getCode().trim(), field);
        }
        return map;
    }

    /**
     * 判断通知触发范围是否与当前查询匹配。
     * GLOBAL 或空范围直接通过；LOCAL 要求 local_route_table_names 与模型表名有交集。
     * @param notice 通知
     * @param usedTableNames 当前查询用到的表名
     * @return true 表示通过触发范围过滤
     */
    private boolean matchTriggerScope(ChangeNotice notice, Set<String> usedTableNames) {
        ChangeNoticeTriggerScope scope = ChangeNoticeTriggerScope.fromCode(notice.getTriggerScope());
        // 空、全局或未能识别：直接通过；仅 LOCAL 校验表名交集
        if (scope != ChangeNoticeTriggerScope.LOCAL) {
            return true;
        }
        Set<String> routeTables = splitCsvToSet(notice.getLocalRouteTableNames());
        if (routeTables.isEmpty()) {
            return false;
        }
        for (String tableName : routeTables) {
            if (usedTableNames.contains(tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 逗号分隔字符串转集合（trim，忽略空段）
     * @param csv 逗号分隔文本
     * @return 去重集合
     */
    private Set<String> splitCsvToSet(String csv) {
        Set<String> result = new HashSet<>();
        if (StrUtil.isBlank(csv)) {
            return result;
        }
        for (String part : csv.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * 按通知 ID 分组对象明细
     * @param objects 对象明细列表
     * @return noticeId -> 对象列表
     */
    private Map<Long, List<ChangeNoticeObject>> groupObjectsByNoticeId(List<ChangeNoticeObject> objects) {
        if (CollUtil.isEmpty(objects)) {
            return Collections.emptyMap();
        }
        return objects.stream()
                .filter(o -> o != null && o.getNoticeId() != null)
                .collect(Collectors.groupingBy(ChangeNoticeObject::getNoticeId));
    }

    /**
     * 按通知 ID 分组筛选明细
     * @param filters 筛选明细列表
     * @return noticeId -> 筛选列表
     */
    private Map<Long, List<ChangeNoticeObjectFilter>> groupFiltersByNoticeId(List<ChangeNoticeObjectFilter> filters) {
        if (CollUtil.isEmpty(filters)) {
            return Collections.emptyMap();
        }
        return filters.stream()
                .filter(f -> f != null && f.getNoticeId() != null)
                .collect(Collectors.groupingBy(ChangeNoticeObjectFilter::getNoticeId));
    }
}
