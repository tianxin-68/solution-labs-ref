package com.bi.queryer.ssm.changenotice.match.strategy;

import com.bi.queryer.ssm.changenotice.entity.ChangeNotice;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObject;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObjectFilter;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeType;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchContext;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeVO;

import java.util.List;

/**
 * 变更通知按类型匹配策略接口。
 *
 * 每种 {@link ChangeNoticeType} 对应一个实现类，由 Service 按 changeType 路由调用。
 * 约定：一次调用 {@link #matchAndBuild} 完成命中判定与出参组装，避免 match/build 重复计算。
 *
 * 职责边界：
 * 1. 实现类只负责本类型的对象命中、筛选/同查等业务判定，以及命中后的 VO 组装
 * 2. 触发范围（GLOBAL / LOCAL 与路由表）由 Service 统一校验，策略内一般不再重复
 * 3. 公共命中与 VO 组装能力落在 {@link com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchHelper}
 */
public interface ChangeNoticeMatchStrategy {

    /**
     * 本策略支持的变更类型。
     * Service 用该方法将通知路由到对应策略实现。
     *
     * @return 变更类型枚举，不可为 null
     */
    ChangeNoticeType supportType();

    /**
     * 判定是否命中，命中则组装出参；未命中返回 null。
     *
     * 实现应在一次遍历/计算中完成命中判定与 VO 组装，避免先 match 再 build 的重复开销。
     * context 为空时，各实现通常直接返回 null。
     * filters 是否参与判定取决于类型：如替换/下线、指标口径等不使用筛选二次校验。
     *
     * @param notice  通知主表
     * @param objects 该通知下全部对象明细（含 CHANGE、MAPPING、MUST_QUERY、REMIND_FIELD 等角色）
     * @param filters 该通知下筛选配置，无配置时可为 empty
     * @param context 匹配上下文（字段、目录、筛选、替换日志等快照）
     * @return 命中时的前端展示 VO；未命中或无法组装时返回 null
     */
    ChangeNoticeVO matchAndBuild(ChangeNotice notice, List<ChangeNoticeObject> objects,
                                 List<ChangeNoticeObjectFilter> filters, ChangeNoticeMatchContext context);
}
