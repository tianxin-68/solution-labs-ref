package com.bi.queryer.sys.kafka.authApproval;

import com.bi.queryer.sys.authapply.AuthApplyService;
import com.bi.queryer.sys.authority.AuthorityService;
import com.bi.queryer.sys.user.model.SysAuthApplyRecord;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSONObject;
import com.tx.mq.consumer.ConsumeStatus;
import org.springframework.stereotype.Service;

/**
 * 工单审批完成消息处理：仅处理 sys_auth_apply_record 中存在的申请记录。
 */
@Service
public class AuthApprovalConsumer {

    public ConsumeStatus processMessage(String message) {
        if (BIUtil.isEmpty(message)) {
            return ConsumeStatus.CONSUME_DISCARD;
        }
        JSONObject jsonObject = JSONObject.parseObject(message);
        String status = jsonObject.getString("status");
        if (!"已完成".equalsIgnoreCase(status)) {
            return ConsumeStatus.CONSUME_DISCARD;
        }
        String taskId = jsonObject.getString("workOrderId");
        AuthorityService authorityService = (AuthorityService) SpringContextUtil.getBean("authorityService");
        SysAuthApplyRecord record = authorityService.queryAuthApplyRecord(taskId);
        if (record != null) {
            AuthApplyService authApplyService = (AuthApplyService) SpringContextUtil.getBean("authApplyService");
            authApplyService.authApprove(record);
        }
        return ConsumeStatus.CONSUME_SUCCESS;
    }
}
