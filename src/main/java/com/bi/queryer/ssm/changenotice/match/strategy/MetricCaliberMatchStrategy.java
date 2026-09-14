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
 * 指标口径变更（METRIC_CALIBER）匹配策略。
 *
 * 业务含义：配置端将某些指标标记为「口径已变更」，查询用到其中任一指标时提醒用户注意口径变化。
 *
 * 命中规则（对照配置端触发场景）：
 * 1. 对象命中：查询字段 code 与 CHANGE 对象的 objectId 有交集（任一即可）
 * 2. 触发范围：由 Service 统一校验——GLOBAL 用到对象即提醒；LOCAL 还需路由命中指定表
 * 3. 本类型不做筛选条件二次校验；filters 参数仅保留接口统一签名
 *
 * 出参规则：
 * changeObjects 仅返回本次查询实际命中的 CHANGE 指标，便于前端展示受影响字段。
 *
 * @see ChangeNoticeMatchHelper#listHitByObjectCtg
 */
@Component
public class MetricCaliberMatchStrategy implements ChangeNoticeMatchStrategy {

    @Autowired
    private ChangeNoticeMatchHelper matchHelper;

    /**
     * 本策略仅处理 METRIC_CALIBER。
     *
     * @return 指标口径变更类型
     */
    @Override
    public ChangeNoticeType supportType() {
        return ChangeNoticeType.METRIC_CALIBER;
    }

    /**
     * 一次完成命中判定与出参组装：先收集命中 CHANGE，空则未命中。
     *
     * @param notice  通知主表
     * @param objects 通知对象明细
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
        return matchHelper.buildBaseVO(notice, hitObjects);
    }
}
