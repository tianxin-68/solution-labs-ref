package com.bi.queryer.ssm.portal.ai;

import com.bi.queryer.ssm.portal.ai.vo.AiAnalysisFavReq;
import com.bi.queryer.ssm.portal.ai.vo.AiAnalysisFavRsp;
import com.bi.queryer.ssm.portal.ai.vo.TemplateRecentChatRsp;
import com.bi.queryer.ssm.portal.ai.vo.TemplateRecentVisitRsp;
import com.bi.queryer.sys.common.BizBaseResponse;
import com.bi.queryer.sys.common.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 分析相关接口
 */
@RestController
@Scope("prototype")
@RequestMapping("/ssm/portal/ai")
@Slf4j
public class AiAnalysisController {

    @Autowired
    private AiAnalysisFavService aiAnalysisFavService;

    @Autowired
    private AiAnalysisService aiAnalysisService;

    /** genAI_feature/v3.15.0_start */
    /**
     * 收藏会话（fav_sort_id 按当前用户已有收藏最大值 +1）
     */
    @RequestMapping(value = "fav/add", method = RequestMethod.POST)
    public ResponseMessage addFav(@RequestBody AiAnalysisFavReq req) {
        try {
            aiAnalysisFavService.addFav(req);
            return ResponseMessage.success(true);
        } catch (Exception e) {
            log.error("ai analysis fav add error, req: {}", req, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 取消收藏
     */
    @RequestMapping(value = "fav/remove", method = RequestMethod.POST)
    public ResponseMessage removeFav(@RequestBody AiAnalysisFavReq req) {
        try {
            aiAnalysisFavService.removeFav(req);
            return ResponseMessage.success(true);
        } catch (Exception e) {
            log.error("ai analysis fav remove error, req: {}", req, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 当前用户收藏列表（按 fav_sort_id 升序）
     */
    @RequestMapping(value = "fav/list", method = RequestMethod.GET)
    public BizBaseResponse<List<AiAnalysisFavRsp>> listFav() {
        try {
            return BizBaseResponse.success(aiAnalysisFavService.listFav());
        } catch (Exception e) {
            log.error("ai analysis fav list error", e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 当前用户最近访问的看板（分析模板）与查询模板
     */
    @RequestMapping(value = "recent/visit", method = RequestMethod.GET)
    public BizBaseResponse<List<TemplateRecentVisitRsp>> recentVisit(@RequestParam(value = "limit", required = false) Integer limit) {
        try {
            return BizBaseResponse.success(aiAnalysisService.listRecentVisits(limit));
        } catch (Exception e) {
            log.error("ai analysis recent visit error, limit: {}", limit, e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }

    /**
     * 最近会话数据
     */
    @RequestMapping(value = "recent/chat", method = RequestMethod.GET)
    public BizBaseResponse<List<TemplateRecentChatRsp>> recentConversation() {
        try {
            return BizBaseResponse.success(aiAnalysisService.listRecentChats());
        } catch (Exception e) {
            log.error("ai analysis recent chat error", e);
            return BizBaseResponse.fail(e.getMessage());
        }
    }
    /** genAI_feature/v3.15.0_end */
}
