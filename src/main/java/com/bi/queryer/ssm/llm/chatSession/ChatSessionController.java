package com.bi.queryer.ssm.llm.chatSession;

import com.bi.queryer.ssm.llm.chatSession.req.*;
import com.bi.queryer.ssm.llm.chatSession.resp.*;
import com.bi.queryer.ssm.util.WebUtil;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@Scope("prototype")
@RequestMapping("/llm/chatSession")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<ChatSessionResp> create(@RequestBody CreateChatSessionReq req) {
        try {
            return SSMResponseMessage.success("创建会话成功", chatSessionService.createChatSession(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "details", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<ChatSessionDetailResp> details(@RequestBody ChatSessionDetailReq req) {
        try {
            return SSMResponseMessage.success("获取会话详情成功", chatSessionService.getChatSessionDetail(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "list", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<ChatSessionListResp> list(@RequestBody(required = false) ChatSessionListQueryReq req) {
        try {
            return SSMResponseMessage.success("查询会话列表成功", chatSessionService.listChatSessions(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "context", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<ChatSessionContextResp> context(@RequestBody ChatSessionContextReq req) {
        try {
            return SSMResponseMessage.success("查询会话上下文成功", chatSessionService.getChatSessionContext(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "rename", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> rename(@RequestBody RenameChatSessionReq req) {
        try {
            chatSessionService.renameChatSession(req);
            return SSMResponseMessage.success("重命名会话成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> delete(@RequestBody DeleteChatSessionReq req) {
        try {
            chatSessionService.deleteChatSession(req);
            return SSMResponseMessage.success("删除会话成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "favorite", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> favorite(@RequestBody FavoriteChatSessionReq req) {
        try {
            chatSessionService.favoriteChatSession(req);
            return SSMResponseMessage.success("收藏会话成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "unfavorite", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> unfavorite(@RequestBody FavoriteChatSessionReq req) {
        try {
            chatSessionService.unfavoriteChatSession(req);
            return SSMResponseMessage.success("取消收藏会话成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "bindView", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> bindView(@RequestBody BindChatSessionViewReq req) {
        try {
            chatSessionService.bindChatSessionView(req);
            return SSMResponseMessage.success("绑定视图成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "unbindView", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> unbindView(@RequestBody UnbindChatSessionViewReq req) {
        try {
            chatSessionService.unbindChatSessionView(req);
            return SSMResponseMessage.success("取消绑定视图成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    @RequestMapping(value = "message/list", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<ChatMessageResp>> listMessages(@RequestBody ChatMessageListReq req) {
        try {
            return SSMResponseMessage.success("查询会话的对话记录列表成功", chatSessionService.listChatMessages(req,true));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * SSE：服务端转发远程 SSE 接口内容到浏览器（OkHttp 拉流 + Spring SseEmitter 推送）
     *
     * @param req 可传 remoteUrl；为空时使用配置 llm.chat.sse.remote.url。method 默认 POST，body 为 JSON。
     */
    @RequestMapping(value = "execute", method = RequestMethod.POST,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter execute(@RequestBody ChatExecuteReq req) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        ChatExecuteContext context = new ChatExecuteContext();
        context.setEmitter(emitter);
        context.setChatId(req.getChatId());
        String token = UserManager.getToken(WebUtil.getRequest());
        context.setToken(token);
        chatSessionService.execute(req, context);
        return emitter;
    }

    /**
     * 创建会话，存储快照
     */
    @RequestMapping(value = "createWithSnapshot", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<Long> createWithSnapshot(@RequestBody ChatCreateWithSnapshotReq req) {
        try {
            return SSMResponseMessage.success("创建会话成功", chatSessionService.createWithSnapshot(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 更新快照数据
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "snapshotUpdate", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage snapshotUpdate(@RequestBody ChatUpdateWithSnapshotReq req) {
        try {
            chatSessionService.snapshotUpdate(req);
            return SSMResponseMessage.success("更新快照成功");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 判断快照是否变更
     */
    @RequestMapping(value = "checkSnapshotDataChange", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<Integer> checkSnapshotDataChange(@RequestBody CheckSnapshotDataChangeReq req) {
        try {
            return SSMResponseMessage.success("判断快照是否变更成功", chatSessionService.checkSnapshotDataChange(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 获取会话数据预览数据
     * @return
     */
    @RequestMapping(value = "getPreviewDataByChatId", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<GetPreviewDataResp>> getPreviewDataByChatId(@RequestBody GetPreviewDataByChatIdReq req) {
        try {
            return SSMResponseMessage.success("获取会话数据预览成功", chatSessionService.getPreviewDataByChatId(req));
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

}
