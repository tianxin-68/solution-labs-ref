package com.bi.queryer.ssm.query.template.cache;

import com.bi.queryer.ssm.engine.accelerate.cache.QueryTemplateCacheManager;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Author contributor
 * @Date 16:28 2025/1/10
 * @Description 查询模板缓存
 **/
@Controller
@Scope("prototype")
@RequestMapping("ssm/template/cache")
public class QueryTemplateCacheController extends BaseController {
    @RequestMapping("refresh/config")
    @ResponseBody
    public ResponseMessage refreshConfigCache() {
        ResponseMessage result = new ResponseMessage();
        QueryTemplateCacheManager.refreshAllServer();;
        result.setData("刷新查询模板配置缓存成功");
        return result;
    }

    @RequestMapping("clear/analysis/template")
    @ResponseBody
    public ResponseMessage clearAnalysisTemplateCache() {
        ResponseMessage result = new ResponseMessage();
        QueryTemplateCacheManager.clearAnalysisTemplateCache(stringValue("analysisTplId"));
        result.setData("当前查询模板缓存清理成功");
        return result;
    }

}
