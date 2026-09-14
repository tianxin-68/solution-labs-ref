package com.bi.queryer.ssm.engine.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.zb.AnalysisZbItemConfig;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.config.settings.style.QuerySortItem;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.enums.FieldSortType;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author: contributor
 * @CreateTime: 2023-08-23  11:25
 * @Description: 处理排序
 */
public class QuerySort {

    private QueryResult result = new QueryResult();

    private QueryAnalysis analysis = new QueryAnalysis();

    private QuerySettings settings = new QuerySettings();

    QuerySort(QueryConfigure configure) {
        this.result = configure.getResult();
        this.analysis = configure.getAnalysis();
        this.settings = configure.getSettings();
    }

    public void load() {
        // 对已经废弃得规则2状态的补充，前端把指标排序的状态存下来了
        if(CollUtil.isNotEmpty(result.getMeasures())){
            result.getMeasures().get(0).setSortType(FieldSortType.NONE);
        }

        //规则1  如果模板没有设置排序（行维度+指标），则按照日期排，如果没有日期维度，则按第一个行维度排。
        long rowSortCount = result.getRowDimensions().stream().filter(r -> FieldSortType.NONE != r.getSortType()).count();
        if (rowSortCount == 0) {
            long measureSortCount = result.getMeasures().stream().filter(m -> FieldSortType.NONE != m.getSortType()).count();
            if (measureSortCount == 0) {
                Optional<QueryField> dateFieldOpt = result.getRowDimensions()
                        .stream()
                        .filter(r -> FieldUtil.getFilterType(r).isDateRange())// FieldFilterType.isDateRange(FieldFilterType.get(r.getMeta().getFilterShowType())))
                        .findAny();

                if (dateFieldOpt.isPresent()) {
                    dateFieldOpt.get().setSortType(FieldSortType.ASC);
                } else {
                    if (CollUtil.isNotEmpty(result.getRowDimensions())) {
                        result.getRowDimensions().get(0).setSortType(FieldSortType.ASC);
                    }
                }
            }
        }

        //规则2 占比或汇总包含列小计时，如果没有设置其他指标排序时，按第一个指标倒排一下（废弃）
        int c_s_total = 0;
        AnalysisZbConfig analysisZbConfig = analysis.getZb();
        if (analysisZbConfig.isActive()) {
            for (AnalysisZbItemConfig analysisZbItemConfig : analysisZbConfig.getItems()) {
                if (AnalysisCalcMode.ZB_COL_SUBTOTAL == AnalysisCalcMode.get(analysisZbItemConfig.getCalcMode())) {
                    c_s_total++;
                }
            }
        }

        AnalysisTotalConfig analysisTotalConfig = analysis.getTotal();
        if (analysisTotalConfig.isActive()) {
            for (AnalysisTotalItemConfig analysisTotalItemConfig : analysisTotalConfig.getItems()) {

                //列小计，且维度不为空
                if (AnalysisCalcMode.COL_SUBTOTAL == AnalysisCalcMode.get(analysisTotalItemConfig.getCalcMode())
                  &&CollUtil.isNotEmpty(analysisTotalItemConfig.getDimIdList())) {
                    c_s_total++;
                }
            }

        }

        if (c_s_total > 0) {
            long measureSortCount = result.getMeasures().stream().filter(m -> FieldSortType.NONE != m.getSortType()).count();
            if (measureSortCount == 0) {
                if(CollUtil.isNotEmpty(result.getMeasures())){
                    // 维度排序：列小计时默认所有维度排序，去掉默认按第一个指标排序
                    //result.getMeasures().get(0).setSortType(FieldSortType.DESC);
                }
            }
        }

        if(c_s_total >0 ) {
            normalizeSortItem();
        }

        if(c_s_total == 0){
            normalizeLastDimSortItem();
        }

    }


    public void normalizeSortItem() {

        List<QuerySortItem> querySortItems = settings.getQuerySortItems();
        if (CollUtil.isEmpty(querySortItems)) {
            return;
        }

        List<QuerySortItem> finalSortItems = new ArrayList<>();

        //所有的行维度都需要排序
        int idx = 0;
        List<QueryField> dimFields = result.getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
        for (QueryField qf : dimFields) {

            if (!Enabled.value(qf.getIsShow())) {
                continue;
            }

            QuerySortItem querySortItem = new QuerySortItem();
            Optional<QuerySortItem> optionalQuerySortItem = querySortItems.stream().filter(f -> qf.getCode().equalsIgnoreCase(f.getOrderEntity())).findAny();
            if (optionalQuerySortItem.isPresent()) {
                querySortItem = optionalQuerySortItem.get();
            } else {
                querySortItem = new QuerySortItem();
                querySortItem.setOrderEntity(qf.getCode());
                querySortItem.setOrderBy(qf.getCode());
                querySortItem.setColumnField(qf.getCode());
                querySortItem.setOrderType(FieldSortType.ASC.getCode());
            }

            if (idx == 0) {
                if (!querySortItem.getOrderBy().equalsIgnoreCase(querySortItem.getOrderEntity())) {
                    querySortItem.setIsNullsLast(Enabled.YES.getId());
                }
            }

            finalSortItems.add(querySortItem);

            idx++;

        }

        settings.getTableStyle().getUpDownSortData().setSortItems(finalSortItems);
    }

    /**
     * 没有列小计的场景，最后一个维度排序
     * 设置了维度按指标排序时，去掉orderEntity
     */
    public void normalizeLastDimSortItem() {
        List<QuerySortItem> querySortItems = settings.getQuerySortItems();
        if (CollUtil.isEmpty(querySortItems)) {
            return;
        }
        
        List<QueryField> dimFields = result.getRowDimensions().stream().filter(f -> !f.isAppend()).collect(Collectors.toList());
        if (CollUtil.isEmpty(dimFields)) {
            return;
        }

        QueryField lastDim = dimFields.get(dimFields.size() - 1);
        for (QuerySortItem querySortItem : querySortItems) {
            if (querySortItem.getOrderBy().equalsIgnoreCase(querySortItem.getOrderEntity())) {
                continue;
            }

            if (!lastDim.getCode().equalsIgnoreCase(querySortItem.getOrderEntity())) {
                continue;
            }

            querySortItem.setIsNullsLast(Enabled.YES.getId());
            querySortItem.setOrderEntity("");

        }

    }
}
