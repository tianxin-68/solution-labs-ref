package com.bi.queryer.ssm.query;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.QueryFactory;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.prepare.PrepareQueryEngine;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.ssm.enums.QuerySourceType;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.JSONSerializable;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Auther: contributor
 * @Date: 2024/7/10 11:20
 * @Description:
 */
public abstract class QueryController extends BaseController implements JSONSerializable {
    @Override
    public JSONObject toJSON() {
        return null;
    }

    /**
     * 从请求中创建查询模板
     *
     * @return
     */
    protected SSDQueryTemplate createTemplateFromRequest() {
        SSDQueryTemplate template = null;
        /*
        String templateId = this.stringValue("templateId");
        if (BIUtil.isNotEmpty(templateId)) { // 通过id获取
            template = templateService.getTemplateById(templateId);
        } else { // 通过当前配置获取
            String templateConfig = stringValue("templateConfig");
            String templateJsonStr = BIUtil.isEmpty(templateConfig) ? JSONObject.toJSONString(this.params) : templateConfig;
            template = JSONObject.parseObject(templateJsonStr, SSDQueryTemplate.class);
        }
         */
        String templateConfig = stringValue("templateConfig");
        String templateJsonStr = BIUtil.isEmpty(templateConfig) ? JSONObject.toJSONString(this.params) : templateConfig;
        template = JSONObject.parseObject(templateJsonStr, SSDQueryTemplate.class);
        return template;
    }

    protected QueryEngine createEngine(boolean aclCheck) {
        SSDQueryTemplate queryTemplate = this.createTemplateFromRequest();
        QueryConfigure queryConfigure = createQueryConfigure(queryTemplate);
        queryConfigure.load();
        if(BIUtil.isNotEmpty(this.stringValue("isStressTest"))){
            // 压测特殊处理
            queryConfigure.setSessionId(Guid.id());
        }

        queryConfigure.getSettings().setAclCheck(aclCheck);
        queryConfigure.getSettings().setEnableCreateAllTableBySameCodeOpt(true);

        //判断是否是agent服务的查询
        if(QuerySourceType.AGENT == QuerySourceType.get(queryTemplate.getType())) {
            queryConfigure.getSettings().setIsAgentQuery(Enabled.YES.getId());
        }

        QueryContext cxt = new QueryContext(getRequest());
        cxt.setQueryParamString(JSONObject.toJSONString(this.params));

        // 预处理：正式数据查询前置数据查询
        PrepareQueryEngine prepareQueryEngine = QueryFactory.createPrepareEngine(queryConfigure, cxt);
        cxt.setPrepareQueryResult(prepareQueryEngine.execute());

        QueryEngine engine = QueryFactory.createEngine(queryConfigure, cxt); // new QueryEngine(queryConfigure, new QueryContext(getRequest()));
        DataSourceRouter.setEnable(queryConfigure.getSettings().isEnableDataSourceRoute());
        // 设置当前引擎查询默认数据源
        DataSourceRouter.setQueryEngineDefaultDataSource(engine);


        //int columnCountPerDimValue = getColumnCountPerDimValue(queryConfigure);
        //engine.getCxt().setColumnCountPerDimValue(columnCountPerDimValue);
        return engine;
    }

    // 计算每个列维度下有多少列
    private int getColumnCountPerDimValue(QueryConfigure config) {
        List<QueryField> measureFields = config.getResult().getFields().stream()
                .filter(v -> QueryArea.Measure.equals(v.getRawQueryArea()) && !v.isAppend() && Enabled.YES.getId().equals(v.getIsShow()))
                .collect(Collectors.toList());
        if (config.getResult().getPivotConfig().isMeasureOnRow()) {
            return (int) measureFields.stream().filter(v -> Enabled.YES.getId().equals(v.getIsAnalysis()))
                    .map(QueryField::getTitle).filter(Objects::nonNull).distinct().count() + 1;
        } else {
            return measureFields.size();
        }
    }

    protected abstract QueryConfigure createQueryConfigure(SSDQueryTemplate queryTemplate);
}
