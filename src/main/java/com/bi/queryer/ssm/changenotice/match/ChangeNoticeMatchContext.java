package com.bi.queryer.ssm.changenotice.match;

import com.bi.queryer.ssm.engine.config.field.QueryField;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 变更通知匹配上下文。
 *
 * 由 Service 在解析本次查询的 queryConfig（及请求附带参数）后一次性构建，
 * 再交给各 {@link com.bi.queryer.ssm.changenotice.match.strategy.ChangeNoticeMatchStrategy} 做命中判定与出参组装。
 * 本对象只承载「当前这次匹配」所需的只读快照，不持有业务分支逻辑。
 *
 * 字段用途概览：
 * 1. queryFieldKeys：绝大多数类型用字段 code 与 CHANGE / MAPPING / REMIND_FIELD / MUST_QUERY 等比对
 * 2. hitFieldKeys：queryFieldKeys 并集 offline 字段 code，供 REPLACE_OFFLINE 旧对象催迁移命中
 * 3. filterFieldMap：使用约束「必须同查筛选」及通用筛选二次校验时，按字段 code 取当前筛选项
 * 4. usedTableNames：Service 侧做 LOCAL 触发范围与本地路由表求交时使用
 * 5. queryCtgIds：替换/下线且 objectType=METRIC_GROUP/DIM_GROUP/MODULE 时，用目录 ID 判断「查询仍挂在该目录下」
 * 6. viewId + viewReplaceLogKeys：替换/下线「提醒新对象」路径，判断视图是否已执行替换任务
 */
@Getter
@Builder
public class ChangeNoticeMatchContext {

    /**
     * 当前查询参与命中的字段 code 集合。
     * 仅来自 queryConfig 中实际参与查询的字段，不含已下线字段。
     * 各策略用其与通知对象 objectId（字段编码）做交集判定。
     */
    private final Set<String> queryFieldKeys;

    /**
     * 旧对象字段命中键：queryFieldKeys 并集已下线字段 code。
     * 由 Service 构建上下文时预计算，REPLACE_OFFLINE 的 REMIND_OLD 路径直接使用，避免策略内重复 merge。
     */
    private final Set<String> hitFieldKeys;

    /**
     * 当前查询中 isFilter=true 的字段映射：字段 code -> QueryField。
     * 用于使用约束必须同查筛选：仅判断筛选项存在且已有取值，不比较具体值。
     */
    private final Map<String, QueryField> filterFieldMap;

    /**
     * 当前查询模型实际用到的物理表名集合。
     * 供 Service 统一做触发范围校验：LOCAL 时与通知配置的 local_route_table_names 求交集；
     * 策略层一般不再重复做表级路由判定。
     */
    private final Set<String> usedTableNames;

    /**
     * 当前查询用到的目录 ID 集合（来自 QueryField.moduleCtgId + MetaField.categoryIdList，已排除公共日期）。
     * 供 REPLACE_OFFLINE 且 objectType=METRIC_GROUP/DIM_GROUP/MODULE 时命中「目录替换/下线后节点仍存在、查询仍挂在该目录」场景。
     */
    private final Set<String> queryCtgIds;

    /**
     * 当前视图 ID，来自匹配请求。
     * 与 replaceTaskId 组合后，用于在 viewReplaceLogKeys 中判断是否已执行模板字段替换。
     */
    private final String viewId;

    /**
     * 模板字段替换日志键集合，格式为 viewId|replaceTaskId。
     * 匹配开始时由 Service 一次性预加载，避免逐条通知查库；
     * REPLACE_OFFLINE 的 REMIND_NEW 路径通过 {@link ChangeNoticeMatchHelper#existsViewInReplaceLog} 使用。
     */
    private final Set<String> viewReplaceLogKeys;
}
