package com.bi.queryer.ssm.engine.analysis.initializer;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalItemConfig;
import com.bi.queryer.ssm.engine.config.QueryAnalysis;
import com.bi.queryer.ssm.engine.config.QueryResult;
import com.bi.queryer.ssm.engine.config.QuerySettings;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.sys.enums.Enabled;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author: contributor
 * @CreateTime: 2023-09-13  15:23
 * @Description: 贡献度初始化
 */
public class AnalysisContributionRateConfigInitializer {

    private QueryResult result = new QueryResult();

    private QueryAnalysis analysis = new QueryAnalysis();

    private QuerySettings settings = new QuerySettings();

    public AnalysisContributionRateConfigInitializer(QueryResult result, QueryAnalysis analysis, QuerySettings settings) {
        this.result = result;
        this.analysis = analysis;
        this.settings = settings;
    }

    /**
     * 初始化贡献率
     * 配置波动贡献后，带出列小计、列总计、对比差值
     */
    public void initialize() {

        if (!isActive()) {
            return;
        }

        //addColSubtotal();

        //AnalysisTotalConfig analysisTotalConfig = analysis.getTotal();
        //if (!analysisTotalConfig.isActive()) {
        //    addColTotal();
        //}

        /*
        //20251027 如果没出汇总，贡献率也都不出了
        AnalysisTotalConfig analysisTotalConfig = analysis.getTotal();
        if (analysisTotalConfig.isActive()) {
            return;
        }
        //判断同环比是否有贡献度
        if (analysis.getThb().isActive()) {
            List<AnalysisThbItemConfig> thbItems = analysis.getThb().getItems();
            if (CollUtil.isNotEmpty(thbItems)) {
                for (AnalysisThbItemConfig thbItem : thbItems) {
                    thbItem.getCalcTypes().remove(AnalysisCalcType.CONTRIBUTION_RATE.getCode());
                }
            }
        }

        //判断自定义对比是否有贡献度
        if (analysis.getCompare().isActive()) {
            List<AnalysisCompareItemConfig> items = analysis.getCompare().getItems();
            if (CollUtil.isNotEmpty(items)) {
                for (AnalysisCompareItemConfig itemConfig : items) {
                    itemConfig.getCalcTypes().remove(AnalysisCalcType.CONTRIBUTION_RATE.getCode());
                }
            }
        }
         */
    }

    /**
     * 判断是否配置了贡献率
     * 同环比+自定义对比
     * @return
     */
    public boolean isActive() {

        //判断同环比是否有贡献度
        if (analysis.getThb().isActive()) {
            long thb_ctr_num = analysis.getThb().getItems().stream()
                    .filter(item ->
                            item.getCalcTypes().contains(AnalysisCalcType.CONTRIBUTION_RATE.getCode())
                            &&
                            CollUtil.isNotEmpty(item.getCtr().getItems())

            ).count();
            if (thb_ctr_num > 0) {
                return true;
            }
        }

        //判断自定义对比是否有贡献度
        if (analysis.getCompare().isActive()) {
            long cmp_ctr_num = analysis.getCompare().getItems().stream().filter(item -> item.getCalcTypes().contains(AnalysisCalcType.CONTRIBUTION_RATE.getCode())).count();
            if (cmp_ctr_num > 0) {
                return true;
            }
        }

        return false;
    }


    /**
     * 添加列小计
     */
    public void addColSubtotal() {

        AnalysisTotalConfig analysisTotalConfig = analysis.getTotal();

        //如果未启用，先赋空值
        if (!analysisTotalConfig.isActive()) {
            analysisTotalConfig.setItems(new ArrayList<>());
        }

        analysisTotalConfig.setIsActive(Enabled.YES.getId());

        //查询行维度(排除最后一项)
        List<String> dimIdList = new ArrayList<>();
        for (int i = 0; i < result.getRowDimensions().size() - 1; i++) {
            dimIdList.add(result.getRowDimensions().get(i).getId());
        }

        //添加列小计
        if (CollUtil.isNotEmpty(dimIdList)) {
            Optional<AnalysisTotalItemConfig> colSubTotalItemConfigOpt = analysisTotalConfig.getItems().stream()
                    .filter(atic -> AnalysisCalcMode.COL_SUBTOTAL == AnalysisCalcMode.get(atic.getCalcMode()))
                    .findAny();

            if (colSubTotalItemConfigOpt.isPresent()) {
                colSubTotalItemConfigOpt.get().setDimIdList(dimIdList);
            } else {
                AnalysisTotalItemConfig colSubTotalItemConfig = new AnalysisTotalItemConfig();
                colSubTotalItemConfig.setCalcMode(AnalysisCalcMode.COL_SUBTOTAL.getCode());
                colSubTotalItemConfig.setDimIdList(dimIdList);
                analysisTotalConfig.add(colSubTotalItemConfig);
            }
        }
    }

    /**
     * 添加列总计
     */
    public void addColTotal() {

        AnalysisTotalConfig analysisTotalConfig = analysis.getTotal();
        analysisTotalConfig.setIsActive(Enabled.YES.getId());
        //添加列总计
        Optional<AnalysisTotalItemConfig> rowTotalItemConfigOpt = analysisTotalConfig.getItems().stream()
                .filter(atic -> AnalysisCalcMode.COL_TOTAL == AnalysisCalcMode.get(atic.getCalcMode()))
                .findAny();

        if (!rowTotalItemConfigOpt.isPresent()) {
            AnalysisTotalItemConfig colTotalItemConfig = new AnalysisTotalItemConfig();
            colTotalItemConfig.setCalcMode(AnalysisCalcMode.COL_TOTAL.getCode());
            analysisTotalConfig.add(colTotalItemConfig);
        }
    }

}
