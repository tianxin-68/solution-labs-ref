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
 * 维度内容变更（DIM_CONTENT）匹配策略。
 *
 * 业务含义：某维度的枚举值/内容发生变更时，查询用到该维度（及可选的受影响指标）时提醒。
 *
 * 命中规则（对照配置端触发场景）：
 * 1. 变更对象命中：查询用到 CHANGE（维度）中的任一
 * 2. 限定受影响指标：若配置了 AFFECTED_METRIC，还需查询用到其中任一；未配置则跳过本条件（视为通过）
 * 3. 触发范围：由 Service 统一校验——GLOBAL 用到对象即提醒；LOCAL 还需路由命中指定表
 * 4. 本类型不做筛选条件二次校验
 *
 * 出参规则：
 * changeObjects 仅返回本次查询命中的变更维度（CHANGE），不含 AFFECTED_METRIC 明细。
 *
 * @see ChangeNoticeMatchHelper#listHitByObjectCtg
 * @see ChangeNoticeMatchHelper#hitByAffectedMetrics
 */
@Component
public class DimContentMatchStrategy implements ChangeNoticeMatchStrategy {

    @Autowired
    private ChangeNoticeMatchHelper matchHelper;

    /**
     * 本策略仅处理 DIM_CONTENT。
     *
     * @return 维度内容变更类型
     */
    @Override
    public ChangeNoticeType supportType() {
        return ChangeNoticeType.DIM_CONTENT;
    }

    /**
     * 一次完成命中判定与出参组装。
     *
     * @param notice  通知主表
     * @param objects 通知对象明细（含 CHANGE、可选 AFFECTED_METRIC）
     * @param filters 筛选配置（本类型不使用）
     * @param context 匹配上下文
     * @return 命中时的 VO；未命中返回 null
     */
    @Override
    public ChangeNoticeVO matchAndBuild(ChangeNotice notice, List<ChangeNoticeObject> objects,
                                        List<ChangeNoticeObjectFilter> filters,
                                        ChangeNoticeMatchContext context) {
        if (context == null) {
            return null;
        }
        List<ChangeNoticeObject> hitObjects = matchHelper.listHitByObjectCtg(
                objects, ChangeNoticeObjectCtg.CHANGE, context.getQueryFieldKeys());
        if (CollUtil.isEmpty(hitObjects)) {
            return null;
        }
        // 限定受影响指标：未配置通过；已配置则需用到其中任一
        if (!matchHelper.hitByAffectedMetrics(objects, context.getQueryFieldKeys())) {
            return null;
        }
        return matchHelper.buildBaseVO(notice, hitObjects);
    }
}
