package com.bi.queryer.ssm.query.chart;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.chart.ChartAnalysisProcessor;
import com.bi.queryer.ssm.engine.chart.ChartAnalysisProcessorFactory;
import com.bi.queryer.ssm.engine.chart.ChartQueryConfigure;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.QueryController;
import com.bi.queryer.sys.common.ResponseMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * @Auther: contributor
 * @Date: 2024/7/10 10:40
 * @Description:
 */

@RestController
@Scope("prototype")
@RequestMapping("ssm/chart")
public class SSMChartQueryController extends QueryController {

    @RequestMapping("result")
    public ResponseMessage buildQueryResult() {
        QueryEngine engine = createEngine(true);
        ResponseMessage responseMessage = engine.execute();

        ChartAnalysisProcessor chartAnalysisProcessor = ChartAnalysisProcessorFactory.getProcessor(engine);
        chartAnalysisProcessor.doPostProcessor(engine, responseMessage);

        return responseMessage;
    }

    @RequestMapping("sql")
    public ResponseMessage buildResultSQL() {
        ResponseMessage result = new ResponseMessage();
        QueryEngine engine = createEngine(true);
        String sql = engine.buildSql();
        result.setData(sql);
        return result;
    }

    @RequestMapping("kill/query")
    public ResponseMessage killQuery() {
        ResponseMessage result = new ResponseMessage();
        try {
            QueryEngine engine = this.createEngine(false);
            engine.killQuery();
        } catch (Exception e) {
            // 不处理
        }
        return result;
    }

    @RequestMapping("query/getRowAuthFieldCodes")
    @ResponseBody
    public ResponseMessage getQueryRowAuthFieldCodes() {
        ResponseMessage result = new ResponseMessage();
        QueryEngine engine = createEngine(true);
        Set<String> codes = engine.getDataRowAuthFieldCodes();
        result.setData(codes);
        return result;
    }

    @Override
    protected QueryConfigure createQueryConfigure(SSDQueryTemplate queryTemplate) {
        return new ChartQueryConfigure(queryTemplate);
    }
}
