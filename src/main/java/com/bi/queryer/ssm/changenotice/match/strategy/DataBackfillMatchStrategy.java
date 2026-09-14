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
 * 数据回刷（DATA_BACKFILL）匹配策略。
 *
 * 业务含义：某批数据发生回刷时，查询用到变更对象表（及可选的提醒范围字段）则提示数据可能已变化。
 *
 * 命中规则（对照配置端触发场景）：
 * 1. 变更对象表命中：CHANGE.objectId 与 usedTableNames 有交集（任一即可；配置端「变更对象（表）」必填）
 * 2. 提醒范围：未配置 REMIND_FIELD 时通过；已配置则需查询字段与之有交集（任一即可；配置端选填）
 * 3. 触发范围：由 Service 统一校验——GLOBAL / LOCAL
 * 4. 本类型不做筛选条件二次校验
 *
 * 出参规则：
 * 1. changeObjects：命中的变更对象表（CHANGE），命中键为 usedTableNames
 * 2. remindFieldObjects：命中的提醒范围字段（REMIND_FIELD 与 queryFieldKeys 交集）；未配置时为空列表
 *
 * @see ChangeNoticeMatchHelper#hitByRemindFields
 * @see ChangeNoticeMatchHelper#listHitByObjectCtg
 * @see ChangeNoticeMatchHelper#buildDataBackfillVO
 */
@Component
public class DataBackfillMatchStrategy implements ChangeNoticeMatchStrategy {

    @Autowired
    private ChangeNoticeMatchHelper matchHelper;

    /**
     * 本策略仅处理 DATA_BACKFILL。
     *
     * @return 数据回刷类型
     */
    @Override
    public ChangeNoticeType supportType() {
        return ChangeNoticeType.DATA_BACKFILL;
    }

    /**
     * 一次完成命中判定与出参组装。
     *
     * @param notice  通知主表
     * @param objects 通知对象明细（含 CHANGE、可选 REMIND_FIELD）
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
                objects, ChangeNoticeObjectCtg.CHANGE, context.getUsedTableNames());
        if (CollUtil.isEmpty(hitObjects)) {
            return null;
        }
        // 提醒范围选填：未配置通过；已配置则需用到其中任一
        if (!matchHelper.hitByRemindFields(objects, context.getQueryFieldKeys())) {
            return null;
        }
        List<ChangeNoticeObject> hitRemindFields = matchHelper.listHitByObjectCtg(
                objects, ChangeNoticeObjectCtg.REMIND_FIELD, context.getQueryFieldKeys());
        return matchHelper.buildDataBackfillVO(notice, hitObjects, hitRemindFields);
    }
}
