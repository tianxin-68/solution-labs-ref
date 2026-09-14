package com.bi.queryer.ssm.promotion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.meta.MetaFieldValueSort;
import com.bi.queryer.ssm.promotion.model.PromotionCfg;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.period.DateDiff;

import java.util.*;
import java.util.stream.Collectors;

public class PromotionManager {

    private static LinkedHashMap<String, PromotionCfg> promotionMap = new LinkedHashMap<>();

    /**
     * 刷新大促日历配置
     */
    public static void refresh(){
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<PromotionCfg> promotionList = dao.queryObjectList("ssm.promotion.queryAll", null, PromotionCfg.class);

        LinkedHashMap<String, PromotionCfg> promotionMapTemp = new LinkedHashMap<>();

        for(PromotionCfg promotionCfg : promotionList){
            promotionMapTemp.put(promotionCfg.getPromoIdentifier(), promotionCfg);
        }

        promotionMap.clear();
        promotionMap.putAll(promotionMapTemp);
    }

    /**
     * 获取所有大促日历配置
     * @return
     */
    public static List<PromotionCfg> getAllPromotionCfgList(){
        return promotionMap.values().stream().collect(Collectors.toList());
    }

    /**
     * 通过大促标识获取大促日历配置
     * @param promotionIdentifier
     * @return
     */
    public static PromotionCfg getPromotionCfg(String promotionIdentifier){
        return promotionMap.get(promotionIdentifier);
    }

    /**
     * 通过年份和大促活动名称获取所有阶段标识列表（promoIdentifier），保持原始顺序
     * @param promoYear  年份，如 2026
     * @param promoName  大促活动名称，如 年中618大促
     * @return 按配置顺序排列的 promoIdentifier 列表
     */
    public static List<String> getStageIdentifiersByYearAndName(int promoYear, String promoName) {
        return getAllPromotionCfgList().stream()
                .filter(cfg -> cfg.getPromoYear() != null && cfg.getPromoYear() == promoYear
                        && promoName.equalsIgnoreCase(cfg.getPromoName()))
                .map(PromotionCfg::getPromoIdentifier)
                .collect(Collectors.toList());
    }

    /**
     * 获取字段值排序
     *   case
     *   dt
     *   when '2025-双十一-整体' then '1'
     *   when '2025-双十一-开门红' then '2'
     *   when '2025-双十一-预热期' then '3'
     *   when '2025-双十一-爆发期' then '4'
     *   when '2025-双十一-返场期' then '5'
     *   end
     * @param field
     * @return
     */
    public static List<MetaFieldValueSort> getFieldValueSort(QueryField  field) {
        List<MetaFieldValueSort> valueSorts = new ArrayList<>();

        for (FieldValue fieldValue : field.getValues()) {
            PromotionCfg promotionCfg = getPromotionCfg(fieldValue.getId());
            if (promotionCfg == null) {
                continue;
            }

            MetaFieldValueSort valueSort = new MetaFieldValueSort();
            valueSort.setFieldCode(field.getCode());
            valueSort.setFieldValue(promotionCfg.getPromoIdentifier());
            valueSort.setFieldValueSortNum(BIUtil.nvl(promotionCfg.getPromoPhaseSortId(), "9999"));
            valueSorts.add(valueSort);
        }

        return valueSorts;
    }

    /**
     * 获取业务日历枚举值排序
     * @return
     */
    public static List<MetaFieldValueSort> getAllFieldValueSort(){

        List<MetaFieldValueSort> valueSorts = new ArrayList<>();
        List<PromotionCfg> promotionList = getAllPromotionCfgList();

        for(PromotionCfg promotionCfg : promotionList){

            MetaFieldValueSort valueSort = new MetaFieldValueSort();
            valueSort.setFieldCode(BIConsts.DATE_CODE);
            valueSort.setFieldValue(getFormatPromotionName(promotionCfg.getPromoIdentifier()));
            valueSort.setFieldValueSortNum(BIUtil.nvl(promotionCfg.getPromoPhaseSortId(), "9999"));
            valueSorts.add(valueSort);
        }

        return valueSorts;
    }

    /**
     * 获取大促总天数
     * @param field
     * @return
     */
    public static Integer getPromotionTotalDays(QueryField  field,QueryConfigure config) {

        Integer totalDays = 0;
        List<String> dataList = getFilterDateList(field, config);

        //计算日期天数
        Long diffDays = DateDiff.getDateDiff(DateGranularity.DAY.getCode(),
                dataList.get(0),
                dataList.get(dataList.size() - 1));

        totalDays = diffDays.intValue() + 1;
        return totalDays;
    }

    /**
     * 获取过滤的日期列表
     * @param field
     * @return
     */
    public static List<String> getFilterDateList(QueryField field, QueryConfigure configure) {

        List<String> dateValueList = new ArrayList<>();

        /**
         * 将活动阶段转化为 开始日期 - 结束日期
         */
        for (FieldValue fieldValue : field.getValues()) {
            PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(fieldValue.getId());
            if (promotionCfg == null) {
                continue;
            }

            dateValueList.add(promotionCfg.getStartDate());
            dateValueList.add(promotionCfg.getEndDate());
        }

        if (CollUtil.isEmpty(dateValueList)) {
            return dateValueList;
        }


        List<String> result = new ArrayList<>();
        Date dataSnapshotDate = DateUtil.parseDate(configure.getSettings().getDataSnapshotDate());
        //截止时间处理
        for (String dateValue : dateValueList) {
            if(DateUtil.compare(DateUtil.parseDate(dateValue),dataSnapshotDate ) > 0){
                result.add(configure.getSettings().getDataSnapshotDate());
            }else{
                result.add(dateValue);
            }
        }

        result = result.stream().distinct().collect(Collectors.toList());
        Collections.sort(result);

        return result;
    }


    /**
     * 获取业务日历对比的日期
     * @param config
     * @return
     */
    public static List<String> getPromoDateOffsetList(QueryConfigure config, AnalysisCalcMode calcMode) {
        List<String> finalFilterValues = new ArrayList<>();

        QueryField dateField = config.getFilterCommonDateField();
        Integer offset = calcMode.getOffset();

        for (FieldValue value : dateField.getValues()) {
            PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(value.getId());
            if (promotionCfg == null) {
                continue;
            }

            Date startDate = DateUtil.parseDate(promotionCfg.getStartDate());
            Date endDate = DateUtil.parseDate(promotionCfg.getEndDate());
            Date dataSnapshotDate = DateUtil.parseDate(config.getSettings().getDataSnapshotDate());

            //截止时间大于开始时间不处理
            if(DateUtil.compare(startDate,dataSnapshotDate)>0){
                continue;
            }

            //判断阶段是否过完
            boolean isOver = true;
            Long diffDays = 0L;
            if(DateUtil.compare(endDate,dataSnapshotDate) > 0) {
                isOver = false;
                diffDays = DateDiff.calculateDays(promotionCfg.getStartDate(),config.getSettings().getDataSnapshotDate());
            }

            Integer yearTb = promotionCfg.getPromoYear() + offset;
            String promoIdentifierTb = String.format("%s-%s-%s",
                    yearTb,promotionCfg.getPromoName(),promotionCfg.getPromoPhase());

            //查询同比对应的业务日历配置
            PromotionCfg promotionCfgTb = PromotionManager.getPromotionCfg(promoIdentifierTb);
            if(promotionCfgTb !=null){
                finalFilterValues.add(promotionCfgTb.getStartDate());

                String endDateTb = promotionCfgTb.getEndDate();
                if(!isOver){
                    //此阶段若未过完（阶段结束时间>数据截止时间），取对比阶段开始时间-同过去天数的对比值
                    endDateTb = DateUtil.offset(DateUtil.parseDate(promotionCfgTb.getStartDate()), DateField.DAY_OF_YEAR, diffDays.intValue()).toDateStr();
                }
                finalFilterValues.add(endDateTb);
            }
        }

        //如果没有时间范围,塞入默认时间，避免报错
        if(CollUtil.isEmpty(finalFilterValues)){
            finalFilterValues.add(BIConsts.MAX_END_DATE);
        }

        Collections.sort(finalFilterValues);
        return finalFilterValues;
    }

    /**
     * 获取格式化大促名称
     * @param promotionIdentifier
     * @return
     */
    public static String getFormatPromotionName(String promotionIdentifier) {
        PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(promotionIdentifier);
        if (promotionCfg == null) {
            return promotionIdentifier;
        }

        String result = String.format("%s%s·%s",
                promotionCfg.getPromoYear(),
                promotionCfg.getPromoName(),
                promotionCfg.getPromoPhase());

        return result;
    }

    /**
     * 获取农历格式大促名称
     * @param promotionIdentifier
     * @return
     */
    public static String getLunarFormatPromotionName(String promotionIdentifier,Map<String, LinkedHashMap<String,String>> compareDateMappings, boolean isShowLunarDate,Integer showDateRemark) {
        PromotionCfg promotionCfg = PromotionManager.getPromotionCfg(promotionIdentifier);
        if (promotionCfg == null) {
            return promotionIdentifier;
        }

        /**
         * 2025双十一·开门红
         * (2025-10-30（农历2025年九月初十）-2025-11-03（农历2025年九月十四）) -
         * 农历年同（-1）（2024-10-12 （农历2024年九月初十）- 2024-10-16（农历2024年九月十四））	-
         * 农历年同（-2）（2023-10-24 （农历2024年九月初十）- 2023-10-28（农历2024年九月十四））
         */

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s%s·%s",
                promotionCfg.getPromoYear(),
                promotionCfg.getPromoName(),
                promotionCfg.getPromoPhase()));

        //不展示对比日期 2026春保·年货节【20251225  至 20260122】
        if(!Enabled.value(showDateRemark)){
            builder.append(String.format("【%s 至 %s】"
                            , promotionCfg.getStartDate().replaceAll("-","")
                            , promotionCfg.getEndDate().replaceAll("-","")
                    )
            );
        }else{
            if (isShowLunarDate) {
                builder.append(String.format("【%s %s (%s) 至 %s %s (%s)】"
                        , promotionCfg.getStartDate().replaceAll("-","")
                        , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(promotionCfg.getStartDate())
                        , com.bi.queryer.util.period.DateUtil.toLunarDate(promotionCfg.getStartDate())
                        , promotionCfg.getEndDate().replaceAll("-","")
                        , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(promotionCfg.getEndDate())
                        , com.bi.queryer.util.period.DateUtil.toLunarDate(promotionCfg.getEndDate()))
                );
            }else{
                builder.append(String.format("【%s %s 至 %s %s】"
                                , promotionCfg.getStartDate().replaceAll("-","")
                                , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(promotionCfg.getStartDate())
                                , promotionCfg.getEndDate().replaceAll("-","")
                                , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(promotionCfg.getEndDate())
                        )
                );
            }
        }

        if (compareDateMappings == null || compareDateMappings.isEmpty()) {
            return builder.toString();
        }

        List<String> lunarCalcModeList = new ArrayList<>(compareDateMappings.values().iterator().next().keySet());
        for (String calcMode : lunarCalcModeList) {
            AnalysisCalcMode analysisCalcMode = AnalysisCalcMode.get(calcMode);

            String startDate = compareDateMappings.getOrDefault(promotionCfg.getStartDate(), new LinkedHashMap<>()).get(calcMode);
            String endDate = compareDateMappings.getOrDefault(promotionCfg.getEndDate(), new LinkedHashMap<>()).get(calcMode);

            if (BIUtil.isEmpty(startDate) || BIUtil.isEmpty(endDate)) {
                builder.append(String.format(" || %s 【无】"
                        , analysisCalcMode.getDesc())
                );
                continue;
            }

            if (isShowLunarDate) {
                builder.append(String.format(" || %s 【%s %s (%s) 至 %s %s (%s)】"
                        , analysisCalcMode.getDesc()
                        , startDate.replaceAll("-","")
                        , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(startDate)
                        , com.bi.queryer.util.period.DateUtil.toLunarDate(startDate)
                        , endDate.replaceAll("-","")
                        , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(endDate)
                        , com.bi.queryer.util.period.DateUtil.toLunarDate(endDate))
                );
            } else {
                builder.append(String.format(" || %s 【%s %s 至 %s %s】"
                        , analysisCalcMode.getDesc()
                        , startDate.replaceAll("-","")
                        , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(startDate)
                        , endDate.replaceAll("-","")
                        , com.bi.queryer.util.period.DateUtil.getWeekNameWithSpace(endDate))
                );
            }
        }

        return builder.toString();
    }

    /**
     * 获取过滤的活动标识列表
     * @param config
     * @return
     */
    public static List<String> getPromoIdentifierList(QueryConfigure config){

        List<String> promoIdentifierList = new ArrayList<>();
        QueryField commonDateField = config.getFilterCommonDateField();
        if (commonDateField == null) {
            return promoIdentifierList;
        }

        //添加活动标识过滤
        for (FieldValue fieldValue : commonDateField.getValues()) {
            promoIdentifierList.add(fieldValue.getId());
        }

        promoIdentifierList = promoIdentifierList.stream().distinct().collect(Collectors.toList());
        return promoIdentifierList;
    }


    public static void main(String[] args) {
        Long diffDays = DateDiff.getDateDiff(DateGranularity.DAY.getCode(),
                "2025-12-16",
                "2025-12-17");

        System.out.println(diffDays);
    }
}
