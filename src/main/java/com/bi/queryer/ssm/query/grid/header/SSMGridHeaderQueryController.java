package com.bi.queryer.ssm.query.grid.header;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.grid.header.GridHeaderQueryConfig;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.QueryController;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.common.ResponseMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: contributor
 * @Date: 2024/8/15 13:58
 * @Description:
 */

@RestController
@Scope("prototype")
@RequestMapping("ssm/grid/header")
public class SSMGridHeaderQueryController extends QueryController {

    @RequestMapping("result")
    public ResponseMessage buildQueryResult() {
        QueryEngine engine = createEngine(true);
        ResponseMessage responseMessage = engine.execute();
        //ChartAnalysisProcessor.doPostProcessor(engine.getConfig(), responseMessage);
        return responseMessage;
    }

    @RequestMapping("result/sql")
    public ResponseMessage buildResultSQL() {
        ResponseMessage result = new ResponseMessage();
        QueryEngine engine = createEngine(true);
        String sql = engine.buildSql();

        //order by 使用开窗函数，存在语法问题，需要进行sql修正
        sql = SSDUtil.rectifyOrderBySql(sql,engine.getSqlTips());

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

    @Override
    protected QueryConfigure createQueryConfigure(SSDQueryTemplate queryTemplate) {
        String gridHeaderFilterStr = stringValue("gridHeaderFilters");
        return new GridHeaderQueryConfig(queryTemplate, gridHeaderFilterStr);
    }
}
