package com.bi.queryer.ssm.api;

import com.bi.queryer.ssm.api.vo.req.AnalysisDataReq;
import com.bi.queryer.ssm.api.vo.rsp.AnalysisDataRsp;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;
import com.bi.queryer.util.BIConsts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 多维对外提供的分析api
 */
@Controller
@Scope("prototype")
@RequestMapping("/olap/api/analysis")
@Slf4j
public class OlapApiAnalysisController {

    @Autowired
    private OlapApiAnalysisService olapApiAnalysisService;

    /**
     * 通过pandas-ai进行数据分析
     * 交互方案见wiki：https://wiki.example.com/pages/viewpage.action?pageId=695021838
     * 入参：提示词 + 模型名称 + query/dataset入参
     */
    @RequestMapping(value = "pandas-ai", method = RequestMethod.POST)
    @ResponseBody
    @FreeCheckAuthority
    public SSMResponseMessage<AnalysisDataRsp> pandasAI(@RequestBody AnalysisDataReq analysisDataReq, HttpServletRequest request) throws IOException {
        String olapApiKey = request.getHeader(BIConsts.OLAP_API_KEY);
        String olapApiUserName = request.getHeader(BIConsts.OLAP_API_USER_NAME);
        AnalysisDataRsp data = olapApiAnalysisService.pandasAI(analysisDataReq, olapApiKey, olapApiUserName, request);
        return SSMResponseMessage.success("分析成功", data);
    }

}
