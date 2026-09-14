package com.bi.queryer.ssm.portal.template.builder;

import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplWidgetEntity;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateDraftStatus;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateExecMode;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.portal.template.vo.WidgetNodeVO;

import java.util.*;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;

/**
 * @Auther: contributor
 * @Date: 2024/6/18 17:26
 * @Description:
 */
public class AnalysisTemplateBuilder {
    public static AnalysisTemplateVO buildAnalysisTemplateVO(AnalysisTemplateEntity templateEntity,
                                                             List<AnalysisTplWidgetEntity> widgetEntities) {
        return buildAnalysisTemplateVO(templateEntity, widgetEntities, Collections.emptyMap(), null);
    }

    public static AnalysisTemplateVO buildAnalysisTemplateVOWithDefault(AnalysisTemplateEntity templateEntity,
                                                                        List<WidgetNodeVO> defaultWidgetNodeVOS) {
        AnalysisTemplateVO vo = new AnalysisTemplateVO();
        vo.setAnalysisTplId(templateEntity.getAnalysisTplId());
        vo.setAnalysisTplName(templateEntity.getAnalysisTplName());
        vo.setAnalysisTplDesc(templateEntity.getAnalysisTplDesc());
        vo.setSourceDataType(templateEntity.getSourceDataType());
        vo.setHasDraft(AnalysisTemplateDraftStatus.hasDraft(templateEntity.getDraftStatus()));
        vo.setWidgetConfigs(defaultWidgetNodeVOS);
        return vo;
    }

    /**
     * 构造看板配置
     *
     * @param templateEntity      看板基础配置
     * @param widgetEntities      看板组件配置
     * @param queryTplIdMap 查询模板原始和快照的映射
     * @return
     */
    public static AnalysisTemplateVO buildAnalysisTemplateVO(AnalysisTemplateEntity templateEntity,
                                                             List<AnalysisTplWidgetEntity> widgetEntities,
                                                             Map<String, String> queryTplIdMap,
                                                             AnalysisTemplateExecMode getMode) {
        AnalysisTemplateVO vo = new AnalysisTemplateVO();
        vo.setAnalysisTplId(templateEntity.getAnalysisTplId());
        vo.setAnalysisTplName(templateEntity.getAnalysisTplName());
        vo.setAnalysisTplDesc(templateEntity.getAnalysisTplDesc());
        vo.setSourceDataType(templateEntity.getSourceDataType());
        vo.setHasDraft(AnalysisTemplateDraftStatus.hasDraft(templateEntity.getDraftStatus()));

        if (isEmpty(widgetEntities)) {
            vo.setWidgetConfigs(Collections.emptyList());
            return vo;
        }

        Map<String, List<AnalysisTplWidgetEntity>> parentIdEntityMap = widgetEntities.stream()
                .collect(Collectors.groupingBy(AnalysisTplWidgetEntity::getParentWidgetId));

        List<AnalysisTplWidgetEntity> parentWidgets = parentIdEntityMap.get(templateEntity.getAnalysisTplId());
        vo.setWidgetConfigs(buildWidgetNodeVO(parentIdEntityMap, parentWidgets, queryTplIdMap, getMode));

        return vo;
    }

    private static List<WidgetNodeVO> buildWidgetNodeVO(Map<String, List<AnalysisTplWidgetEntity>> parentIdEntityMap,
                                                        List<AnalysisTplWidgetEntity> parentWidgets,
                                                        Map<String, String> queryTplIdMap,
                                                        AnalysisTemplateExecMode getMode) {
        if (isEmpty(parentWidgets)) {
            return Collections.emptyList();
        }

        List<WidgetNodeVO> voList = new ArrayList<>();
        parentWidgets.sort(Comparator.comparingDouble(AnalysisTplWidgetEntity::getSortId));

        for (AnalysisTplWidgetEntity entity : parentWidgets) {
            WidgetNodeVO vo = new WidgetNodeVO();
            vo.setWidgetId(entity.getWidgetId());
            vo.setWidgetTitle(entity.getWidgetTitle());
            vo.setWidgetDesc(entity.getWidgetDesc());
            vo.setLocalQueryTplId(queryTplIdMap.getOrDefault(entity.getQueryTplId(), entity.getQueryTplId()));
            if (AnalysisTemplateExecMode.LOCAL.equals(getMode)) {
                vo.setQueryTplId(queryTplIdMap.getOrDefault(entity.getQueryTplId(), entity.getQueryTplId()));
            } else {
                vo.setQueryTplId(entity.getQueryTplId());
            }
            vo.setWidgetTypeCode(entity.getWidgetTypeCode());
            vo.setWidgetSettings(entity.getWidgetSettings());
            vo.setWidgetOptions(entity.getWidgetOptions());

            //只有生产环境才返回
            if(AnalysisTemplateExecMode.PROD.equals(getMode)){
                vo.setQueryTplViewIdMappingList(entity.getQueryTplViewIdMappingList());
            }

            vo.setChildren(buildWidgetNodeVO(parentIdEntityMap, parentIdEntityMap.get(entity.getWidgetId()), queryTplIdMap, getMode));
            voList.add(vo);
        }

        return voList;
    }
}
