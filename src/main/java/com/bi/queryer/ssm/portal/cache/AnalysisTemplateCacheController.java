package com.bi.queryer.ssm.portal.cache;

import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author contributor
 * @Date 16:18 2024/12/26
 * @Description 分析模板缓存请求处理类
 **/
@RestController
@Scope("prototype")
@RequestMapping("ssm/analysisTpl/cache")
public class AnalysisTemplateCacheController {

    @Autowired
    private AnalysisTemplateCacheService service;

    /**
     * 每个15分钟提交一次缓存请求
     * @param req
     * @return
     */
    @RequestMapping("submit")
    @ResponseBody
    public ResponseMessage submit(@RequestBody AnalysisTemplateCacheRequest req) {
        ResponseMessage result = new ResponseMessage();
        String menuId = req.getMenuId();
        List<String> cacheInfos = service.cache(menuId);
        result.setData(BIUtil.listToStr(cacheInfos, "\n"));
        return result;
    }
}
