package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionReplaceTplReq;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionReplaceTplRsp;
import com.bi.queryer.ssm.migrate.bizsplit.model.QueryTemplateCopyResult;
import com.bi.queryer.util.Guid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link QueryTemplateCopyService} 的真实实现：委托给 {@link MetricExpansionService#replaceTemplateViews}
 * （随门户业务线拆分一起合入的现成能力：复制整个查询模板 + 全部视图并做指标膨胀，直接写正式库，不写影子表）。
 * 本类只负责在两套请求/响应模型之间搬字段，不重复实现克隆逻辑本身。
 */
@Service
@Slf4j
public class QueryTemplateCopyServiceImpl implements QueryTemplateCopyService {

    @Autowired
    private MetricExpansionService metricExpansionService;

    @Override
    public QueryTemplateCopyResult copyQueryTemplate(String tplId, String oldBizLine, String newBizLine) {
        MetricExpansionReplaceTplReq req = new MetricExpansionReplaceTplReq();
        req.setTplId(tplId);
        req.setSourceBusinessline(oldBizLine);
        req.setTargetBusinessline(newBizLine);

        try {
            // 模板复制成功，返回新模板 id 与全部视图的 oldViewId → newViewId 映射
            MetricExpansionReplaceTplRsp rsp = metricExpansionService.replaceTemplateViews(req);

            List<QueryTemplateCopyResult.ViewIdMapping> viewIdMappings = new ArrayList<>();
            if (CollUtil.isNotEmpty(rsp.getViewIdMappings())) {
                for (MetricExpansionReplaceTplRsp.ViewIdMapping vm : rsp.getViewIdMappings()) {
                    viewIdMappings.add(QueryTemplateCopyResult.ViewIdMapping.builder()
                            .oldViewId(vm.getOldViewId())
                            .newViewId(vm.getNewViewId())
                            .build());
                }
            }

            return QueryTemplateCopyResult.builder()
                    .newTplId(rsp.getNewTplId())
                    .viewIdMappings(viewIdMappings)
                    .build();
        } catch (Exception e) {
            // 模板复制失败，返回 null
            log.error("query template copy error, tplId: {}, newBizLine: {}", tplId, newBizLine, e);
        }
        return null;
    }
}
