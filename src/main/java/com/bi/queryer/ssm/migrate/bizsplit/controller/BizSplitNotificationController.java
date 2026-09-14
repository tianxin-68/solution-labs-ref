package com.bi.queryer.ssm.migrate.bizsplit.controller;

import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitViewReminderReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.BizSplitViewReminderRsp;
import com.bi.queryer.ssm.migrate.bizsplit.service.BizSplitNotificationService;
import com.bi.queryer.sys.common.SSMResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务线拆分通知 HTTP 入口。
 *
 * 请求路径前缀：/ssm/bizsplit/notification
 */
@RestController
@Scope("prototype")
@Slf4j
@RequestMapping("ssm/bizsplit/notification")
public class BizSplitNotificationController {

    @Autowired
    private BizSplitNotificationService bizSplitNotificationService;

    /**
     * 业务线拆分-视图失效提醒：viewId 命中迁移映射表时返回拆分后的新视图与模糊指标提示文案。
     * 拆分分支按当前登录用户所在部门收窄，未命中任何分支时回退为展示全部分支。
     *
     * @param req viewId 必填
     * @return 提醒结果；未命中任何拆分记录时 affected=false
     */
    @RequestMapping(value = "reminder", method = RequestMethod.POST)
    public SSMResponseMessage<BizSplitViewReminderRsp> bizSplitReminder(
            @RequestBody BizSplitViewReminderReq req) {
        try {
            String viewId = req == null ? null : req.getViewId();
            BizSplitViewReminderRsp rsp = bizSplitNotificationService.queryBizSplitReminder(viewId);
            return SSMResponseMessage.success("ok", rsp);
        } catch (Exception e) {
            log.error("bizsplit notification reminder error", e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }
}
