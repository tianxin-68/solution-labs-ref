package com.bi.queryer.ssm.changenotice.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.changenotice.req.ChangeNoticeAckReq;
import com.bi.queryer.ssm.changenotice.req.ChangeNoticeMatchReq;
import com.bi.queryer.ssm.changenotice.service.ChangeNoticeService;
import com.bi.queryer.ssm.changenotice.vo.ChangeNoticeVO;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 变更通知接口。
 * 提供分析查询场景下的变更通知匹配与已读确认能力。
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/changenotice")
@Slf4j
public class ChangeNoticeController {

    @Autowired
    private ChangeNoticeService changeNoticeService;

    /**
     * 按查询配置匹配当前生效的变更通知。
     * 请求体携带 queryConfig、可选 offlineFieldIds、viewId、checkHistoryNotify；
     * checkHistoryNotify=1（默认）时剔除当前用户在该视图下已读及达次数上限的通知。
     * @param req 匹配请求
     * @return 命中的通知列表，不会为 null
     */
    @RequestMapping(value = "match", method = RequestMethod.POST)
    public SSMResponseMessage<List<ChangeNoticeVO>> match(@RequestBody ChangeNoticeMatchReq req) {
        String queryConfig = req == null ? null : req.getQueryConfig();
        List<String> offlineFieldIds = req == null ? null : req.getOfflineFieldIds();
        String viewId = req == null ? null : req.getViewId();
        // 未传时默认 1：保持原「校验历史已通知」行为
        Integer checkHistoryNotify = req == null ? null : req.getCheckHistoryNotify();
        List<ChangeNoticeVO> hitList = changeNoticeService.matchHitNotices(
                queryConfig, offlineFieldIds, viewId, checkHistoryNotify);
        if (hitList == null) {
            hitList = Collections.emptyList();
        }
        return SSMResponseMessage.success("查询成功", hitList);
    }

    /**
     * 确认已读：写入 ssm_change_notice_log，之后同一用户在同一视图下不再重复提醒。
     * 前端在用户关闭/确认通知后调用。
     * @param req 含 viewId 与 noticeIds
     * @return 操作结果
     */
    @RequestMapping(value = "ack", method = RequestMethod.POST)
    public SSMResponseMessage<Void> acknowledge(@RequestBody ChangeNoticeAckReq req) {
        if (req == null || StrUtil.isBlank(req.getViewId()) || CollUtil.isEmpty(req.getNoticeIds())) {
            return SSMResponseMessage.operationFailed("viewId 与 noticeIds 不能为空");
        }
        changeNoticeService.acknowledgeNotices(req.getViewId(), req.getNoticeIds());
        return SSMResponseMessage.success("确认成功");
    }
}
