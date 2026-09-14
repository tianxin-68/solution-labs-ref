package com.bi.queryer.ssm.changenotice.match.strategy;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.changenotice.entity.ChangeNotice;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObject;
import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObjectFilter;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeObjectCtg;
import com.bi.queryer.ssm.changenotice.enums.ChangeNoticeType;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchContext;
import com.bi.queryer.ssm.changenotice.match.ChangeNoticeMatchHelper;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 使用约束（USAGE_CONSTRAINT）匹配策略。
 *
 * 业务含义：某些变更对象（字段）存在「必须同查」约束——查询用到该对象时，
 * 必须同时带上指定字段，且必须存在指定筛选项并已填值；否则提醒并支持前端一键补全。
 *
 * 命中规则（对照配置端触发场景）：
 * 1. 查询用到 CHANGE 中的任一
 * 2. 且「必须同查」未满足时才提醒：
 *    - 必须同查字段（MUST_QUERY）：需全部出现在当前查询中；未配置则跳过
 *    - 必须同查筛选：仅要求筛选项存在且有筛选值，不比较具体取值；未配置则跳过
 * 3. 触发范围由 Service 统一校验（预览场景多为 GLOBAL）
 * 4. 已满足必须同查时不提醒（返回 null）
 *
 * 出参规则：
 * 1. changeObjects：仅返回不符合规则的变更对象（当前查询已用到的 CHANGE；此时必须同查一定未满足）
 * 2. mustQueryObjects / mustQueryFilters：返回全部必须同查配置，供前端一键「应用」补全
 * 3. needApply 固定为 1（仅在命中后组装）
 *
 * @see ChangeNoticeMatchHelper#listHitByObjectCtg
 * @see ChangeNoticeMatchHelper#isMustQuerySatisfied
 * @see ChangeNoticeMatchHelper#buildUsageConstraintVO
 */
@Component
public class UsageConstraintMatchStrategy implements ChangeNoticeMatchStrategy {

    @Autowired
    private ChangeNoticeMatchHelper matchHelper;

    /**
     * 本策略仅处理 USAGE_CONSTRAINT。
     *
     * @return 使用约束类型
     */
    @Override
    public ChangeNoticeType supportType() {
        return ChangeNoticeType.USAGE_CONSTRAINT;
    }

    /**
     * 一次完成命中判定与出参组装：先取命中 CHANGE，再校验必须同查是否满足。
     *
     * @param notice  通知主表
     * @param objects 通知对象明细（含 CHANGE、MUST_QUERY）
     * @param filters 必须同查筛选配置
     * @param context 匹配上下文
     * @return 违规使用时的 VO；未命中或已满足同查时返回 null
     */
    @Override
    public ChangeNoticeVO matchAndBuild(ChangeNotice notice, List<ChangeNoticeObject> objects,
                                        List<ChangeNoticeObjectFilter> filters,
                                        ChangeNoticeMatchContext context) {
        if (context == null) {
            return null;
        }
        List<ChangeNoticeObject> nonCompliantChangeObjects = matchHelper.listHitByObjectCtg(
                objects, ChangeNoticeObjectCtg.CHANGE, context.getQueryFieldKeys());
        // 未用到变更对象：不提醒
        if (CollUtil.isEmpty(nonCompliantChangeObjects)) {
            return null;
        }
        // 已满足必须同查：不提醒
        if (matchHelper.isMustQuerySatisfied(objects, filters, context)) {
            return null;
        }
        return matchHelper.buildUsageConstraintVO(notice, nonCompliantChangeObjects, objects, filters);
    }
}
