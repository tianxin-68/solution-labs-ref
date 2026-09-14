package com.bi.queryer.ssm.chat;

import com.bi.queryer.ssm.chat.req.ChatCheckSnapshotDataChangeReq;
import com.bi.queryer.ssm.chat.req.ChatCreateWithSnapshotReq;
import com.bi.queryer.ssm.chat.req.ChatDataRsp;
import com.bi.queryer.ssm.chat.req.ChatSnapshotUpdateReq;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Agent 平台会话接口。
 */
@Controller
@Scope("prototype")
@RequestMapping("/agent/plat/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 创建会话并保存数据快照。
     *
     * @param req 创建会话及快照入参
     * @return 会话 ID
     */
    @RequestMapping(value = "createWithSnapshot", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> createWithSnapshot(@RequestBody ChatCreateWithSnapshotReq req) {
        try {
            return SSMResponseMessage.success("创建成功", chatService.createWithSnapshot(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 更新会话数据快照。
     *
     * @param req 快照更新入参
     */
    @RequestMapping(value = "snapshotUpdate", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<Void> snapshotUpdate(@RequestBody ChatSnapshotUpdateReq req) {
        try {
            chatService.snapshotUpdate(req);
            return SSMResponseMessage.success("更新成功", null);
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 判断快照是否变更
     */
    @RequestMapping(value = "checkSnapshotDataChange", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<Integer> checkSnapshotDataChange(@RequestBody ChatCheckSnapshotDataChangeReq req) {
        try {
            return SSMResponseMessage.success("判断快照是否变更成功", chatService.checkSnapshotDataChange(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 通过 chatId 查询最新数据快照配置及结果列表
     */
    @RequestMapping(value = "getChatData", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<ChatDataRsp> getChatData(@RequestParam("chatId") String chatId) {
        try {
            return SSMResponseMessage.success("查询成功", chatService.getChatData(chatId));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 通过 chatId 查询最新快照对应的 viewId
     */
    @RequestMapping(value = "getDataSnapshotViewId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> getDataSnapshotViewId(@RequestParam("chatId") String chatId) {
        try {
            return SSMResponseMessage.success("查询成功", chatService.getDataSnapshotViewId(chatId));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }
}
