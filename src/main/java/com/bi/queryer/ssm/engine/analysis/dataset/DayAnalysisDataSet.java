package com.bi.queryer.ssm.engine.analysis.dataset;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.QueryConfigureType;
import com.bi.queryer.util.BIUtil;

import java.util.Collections;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:27 2023-06-15
 * @Description 日数据集
 **/
public class DayAnalysisDataSet extends AnalysisDataSet {
    public DayAnalysisDataSet(String dateField, AnalysisCalcMode calcMode) {
        super(dateField, calcMode);
    }

    @Override
    public String getDateFieldExpression(String defaultValue) {

        //实时趋势图
        if(config.getSettings().isRtDataset() && QueryConfigureType.Chart == config.getType()){
            String fieldExpression = this.getDateFieldFullName();
            return fieldExpression;
        }

        // 年周同比特殊处理
        switch (calcMode){
            case TB_YEAR_WEEK:
            case TB_YEAR_WEEK_2:
            case TB_YEAR_WEEK_3:
                String fieldExpression = fx.getYearWeekDay(this.getDateFieldFullName(), calcMode.getOffset() * -1);
                if(defaultValue != null) {
                    fieldExpression = fx.coalesce(fieldExpression, "'" + defaultValue + "'");
                }
                return fieldExpression;
        }

        //业务日历同阶段同比-处理
        switch (calcMode){
            case TB_PROMO_YEAR:
            case TB_PROMO_YEAR_2:
            case TB_PROMO_YEAR_3:

                Integer promoYear =  this.config.getSettings().getPromoYear();
                Integer offsetYear =  this.config.getSettings().getPromoYear() + calcMode.getOffset() ;

                String fieldExpression =String.format("REPLACE(%s,'%s','%s')",
                        this.getDateFieldFullName(),
                        offsetYear,
                        promoYear
                        );
                return fieldExpression;
        }

        //农历年周处理
        switch (calcMode) {
            case TB_LN_YEAR_WEEK:
            case TB_LN_YEAR_WEEK_2:
            case TB_LN_YEAR_WEEK_3:
                String fieldExpression = this.getDateFieldFullName();
                return fieldExpression;
        }
        
        DateGranularity offsetGranularity = null;
        Integer interval = 0;
        switch (calcMode){
            case HB:
                offsetGranularity = DateGranularity.DAY;
                interval = 1;
                break;
            case TB_WEEK:
                offsetGranularity = DateGranularity.DAY;
                interval = 7;
                break;
            case TB_MONTH:
                offsetGranularity = DateGranularity.MONTH;
                interval = 1;
                break;
            case TB_YEAR:
                interval = 1;
                offsetGranularity = DateGranularity.YEAR;
                break;
            case TB_YEAR_2:
                interval = 2;
                offsetGranularity = DateGranularity.YEAR;
                break;
            case TB_YEAR_3:
                interval = 3;
                offsetGranularity = DateGranularity.YEAR;
                break;
            /*
            case TB_YEAR_WEEK:
            case TB_YEAR_WEEK_2:
            case TB_YEAR_WEEK_3:
                // interval = buildYearWeekInterval(calcMode);
                //String fieldExpression = fx.getYearWeekDay(this.getName()+ "." + dateField,interval);
                String fieldExpression = this.getDateFieldFullName();
                if(defaultValue != null) {
                    fieldExpression = fx.coalesce(fieldExpression, "'" + defaultValue + "'");
                }
                return fieldExpression;
             */
        }

        // 农历日期：年同比时通过日偏移
        if(isLunarDate() && calcMode.isTblny()){
            interval = getLunarTbYearInterval(calcMode);
            offsetGranularity = DateGranularity.DAY;
        }

        String fieldExpression = fx.formatDate(
                fx.addDate(
                        fx.parseDate(this.getDateFieldFullName(), IFunction.Format_Date),
                        offsetGranularity,
                        interval
                )
                , IFunction.Format_Date);
        if(defaultValue != null) {
            fieldExpression = fx.coalesce(fieldExpression, "'" + defaultValue + "'");
        }
        return fieldExpression;
    }

    /**
     * 获取年周同步偏移量
     * @param calcMode
     * @return
     */
    public Integer buildYearWeekInterval(AnalysisCalcMode calcMode) {
        Integer interval = 1;
        if (AnalysisCalcMode.TB_YEAR_WEEK_2 == calcMode) {
            interval = 2;
        } else if (AnalysisCalcMode.TB_YEAR_WEEK_3 == calcMode) {
            interval = 3;
        }
        return interval;
    }

    /**
     * 获取农历年同比的interval
     * @return
     */
    protected Integer getLunarTbYearInterval(AnalysisCalcMode calcMode){
        Integer interval = 0;
        List<String> baseDateList = AnalysisUtil.getFilterDateList(this.config);
        if(BIUtil.isEmpty(baseDateList)){
            return interval;
        }

        List<String> offsetDateList = AnalysisUtil.getCompareDateRange(this.config, calcMode, -1);
        if(BIUtil.isEmpty(offsetDateList)){
            return interval;
        }
//        List<String> offsetDateList = offsetDateLists.get(0);
        // 排序
        Collections.sort(baseDateList);
        Collections.sort(offsetDateList);

        String baseDate = baseDateList.get(0);
        String offsetDate = offsetDateList.get(0);

        interval = new Long(DateUtil.between(DateUtil.parseDate(baseDate), DateUtil.parseDate(offsetDate), DateUnit.DAY, true)).intValue();

        return interval;
    }

    @Override
    public String getName() {
        return this.namePrefix + this.calcMode.getCode();
    }
}
