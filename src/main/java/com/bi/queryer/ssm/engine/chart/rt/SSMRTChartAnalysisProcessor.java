package com.bi.queryer.ssm.engine.chart.rt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cfg.compare.AnalysisCompareItemConfig;
import com.bi.queryer.ssm.engine.analysis.cfg.thb.AnalysisThbItemConfig;
import com.bi.queryer.ssm.engine.chart.ChartAnalysisProcessor;
import com.bi.queryer.ssm.engine.config.QueryAnalysis;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DataSliceGranularity;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SSMRTChartAnalysisProcessor extends ChartAnalysisProcessor {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 填充断点数据
     * 1 按时间切片获取完整的时间切片
     * 2 将数据集按时间切片分组（同一时间切片可有多行，如多维度）
     * 3 遍历完整的时间切片，构建结果集。不存在数据的时间切片，填充默认值
     */
    public  void fillDiscontinuousDateValues(QueryConfigure queryConfigure, ResponseMessage responseMessage) {

        if (responseMessage == null || !responseMessage.getSuccess()) {
            return;
        }

        ResultDataSet resultDataSet = (ResultDataSet) responseMessage.getData();
        if (resultDataSet == null || BIUtil.isEmpty(resultDataSet.getRows())) {
            return;
        }

        Map<String, Object> templateRow = initTemplateRow(resultDataSet.getColumns(), resultDataSet.getRows().get(0));

        String dataSliceGranularity = queryConfigure.getSettings().getDataSliceGranularity();
        DataSliceGranularity sliceGranularity = DataSliceGranularity.get(dataSliceGranularity);
        if (DataSliceGranularity.UNKNOWN == sliceGranularity) {
            return;
        }

        List<String> timeSlices = getDayTimeSlices(sliceGranularity.getGranularity());

        // key = 时间切片，value = 该切片下的所有数据行
        Map<String, List<Map<String, Object>>> rowMap = new HashMap<>();
        for (Map<String, Object> row : resultDataSet.getRows()) {

            if (row.get(BIConsts.DATE_CODE) == null) {
                continue;
            }

            String timeSlice = row.get(BIConsts.DATE_CODE).toString();
            rowMap.computeIfAbsent(timeSlice, k -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> finalRows = new ArrayList<>();
        for (String timeSlice : timeSlices) {
            List<Map<String, Object>> sliceRows = rowMap.get(timeSlice);
            if (CollUtil.isEmpty(sliceRows)) {
                Map<String, Object> row = new HashMap<>(templateRow);
                row.put(BIConsts.DATE_CODE, timeSlice);
                finalRows.add(row);
            } else {
                finalRows.addAll(sliceRows);
            }
        }

        resultDataSet.setRows(finalRows);
    }

    /**
     * 实时趋势图补齐行模板：仅保留时间切片字段，不复制其他行维度。
     */
    @Override
    protected Map<String, Object> initTemplateRow(List<ResultDataSetColumn> columns, Map<String, Object> firstRow) {
        return super.initTemplateRow(columns, firstRow);
    }

    /**
     * 核心方法：获取指定粒度的时间切片（00:00 -> 24:00）
     *
     * @param granularity 时间粒度，单位：分钟，仅支持1/5/10/60
     * @return 格式化后的时间切片列表，如[00:00, 00:05, 00:10, ..., 24:00]
     * @throws IllegalArgumentException 非法粒度时抛出异常
     */
    public List<String> getDayTimeSlices(Integer granularity) {

        List<String> timeSlices = new ArrayList<>();
        // 2. 初始化起始时间：00:00
        LocalTime currentTime = LocalTime.of(0, 0);
        // 3. 循环累加粒度，直到超过23:59
        while (currentTime.isBefore(LocalTime.MAX) || currentTime.equals(LocalTime.of(0, 0))) {
            // 格式化当前时间为HH:mm
            timeSlices.add(currentTime.format(TIME_FORMATTER));
            // 累加指定分钟数
            currentTime = currentTime.plusMinutes(granularity);
            // 4. 当累加后为00:00（即24:00），手动添加并退出循环
            if (currentTime.equals(LocalTime.of(0, 0))) {
                timeSlices.add("24:00");
                break;
            }
        }
        return timeSlices;
    }

    /**
     * 添加分析对比时间
     *
     * @param queryConfigure
     */
    public void addAnalysisCompareDate(QueryEngine engine, QueryConfigure queryConfigure,
                                              ResponseMessage responseMessage) {

        if (responseMessage == null || !responseMessage.getSuccess()) {
            return;
        }

        ResultDataSet resultDataSet = (ResultDataSet) responseMessage.getData();
        if (resultDataSet == null) {
            return;
        }

        //是否含有农历
        boolean isContainLunarDate = false;

        QueryAnalysis queryAnalysis = queryConfigure.getAnalysis();
        Map<String, LinkedHashMap<String, String>> compareDateMappings = engine.getCompareDateMappings();
        QueryField commonDateField = engine.getConfig().getFilterCommonDateField();
        String dateFieldCode = commonDateField.getCode();

        if (CollUtil.isEmpty(commonDateField.getValues())) {
            return;
        }

        String dateStr = commonDateField.getValues().get(0).getId();
        Calendar curDate = getCurDateCalendarByFullName(DateGranularity.DAY, dateStr);

        Map<String, String> dateFormatMap = new HashMap<>();

        //同环比
        if (queryAnalysis.isActive() && queryAnalysis.getThb().isActive()) {
            for (AnalysisThbItemConfig itemConfig : queryAnalysis.getThb().getItems()) {
                String calcMode = itemConfig.getCalcModes().get(0);
                String dateKey = String.format("%s_%s", calcMode, dateFieldCode);

                AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);

                if (AnalysisCalcMode.TB_LN_YEAR == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_2 == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_3 == analysisCalcMode || analysisCalcMode.isTblnyw()) {
                    isContainLunarDate = true;
                }

                String compareDate = compareDateMappings.getOrDefault(dateStr, new LinkedHashMap<>()).get(calcMode);
                //农历年同比图例显示
                if (AnalysisCalcMode.TB_LN_YEAR == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_2 == analysisCalcMode || AnalysisCalcMode.TB_LN_YEAR_3 == analysisCalcMode) {
                    dateFormatMap.put(dateKey, formatLunarDate(compareDate));
                } else if (analysisCalcMode.isTblnyw()) {
                    dateFormatMap.put(dateKey, formatLunarDate(compareDate));
                } else {
                    dateFormatMap.put(dateKey, formatDate(compareDate));
                }
            }
        }

        //农历对比时，本期值也需要标识农历
        if (isContainLunarDate) {
            dateFormatMap.put(String.format("%s_%s", "", dateFieldCode), formatLunarDate(DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT)));
        } else {
            dateFormatMap.put(String.format("%s_%s", "", dateFieldCode), formatDate(DateUtil.format(curDate.getTime(), DatePattern.NORM_DATE_FORMAT)));
        }

        if (queryAnalysis.isActive() && queryAnalysis.getCompare().isActive()) {
            for (AnalysisCompareItemConfig itemConfig : queryAnalysis.getCompare().getItems()) {
                String calcMode = itemConfig.getCalcMode();
                Calendar compareDate = getCompareRelativeDate(DateGranularity.DAY, curDate, itemConfig);
                String dateKey = String.format("%s_%d_%s", calcMode, itemConfig.getCompareIndex(), dateFieldCode);
                dateFormatMap.put(dateKey, formatCurDateCalendar(DateGranularity.DAY, compareDate));
            }
        }


        for (Map<String, Object> row : resultDataSet.getRows()) {
            for (Map.Entry<String, String> entry : dateFormatMap.entrySet()) {
                row.put(entry.getKey(), entry.getValue());
            }
        }

    }

}
