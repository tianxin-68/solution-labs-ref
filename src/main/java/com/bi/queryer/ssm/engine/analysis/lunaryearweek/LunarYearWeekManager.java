package com.bi.queryer.ssm.engine.analysis.lunaryearweek;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.SpringContextUtil;

import java.util.*;

public class LunarYearWeekManager {

    private static Map<String, lunarYearWeekEntity> lunarYearWeekMap = new HashMap<String, lunarYearWeekEntity>();


    public static void refresh(){
        BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
        List<lunarYearWeekEntity> lunarYearWeekEntityList = dao.queryObjectList("ssm.lunar.year.week.queryAll", null, lunarYearWeekEntity.class);

        Map<String, lunarYearWeekEntity> lunarYearWeekMapTemp = new HashMap<>();
        for (lunarYearWeekEntity lunarYearWeekEntity : lunarYearWeekEntityList) {
            lunarYearWeekMapTemp.put(lunarYearWeekEntity.getCurrentDt(), lunarYearWeekEntity);
        }

        lunarYearWeekMap.clear();
        lunarYearWeekMap.putAll(lunarYearWeekMapTemp);

    }

    /**
     * 获取农历年周的日期范围
     * @param baseDateList
     * @param calcMode
     * @return
     */
    public static List<String> getLunarYearWeekDateRange(List<String> baseDateList, AnalysisCalcMode calcMode) {
        List<String> offsetDateRange = new ArrayList<>();

        for (String baseDate : baseDateList) {

            lunarYearWeekEntity lunarYearWeekEntity = lunarYearWeekMap.get(baseDate);
            if (lunarYearWeekEntity == null) {
                continue;
            }

            String offsetDate = null;
            switch (calcMode) {
                case TB_LN_YEAR_WEEK:
                    offsetDate = lunarYearWeekEntity.getLunarYearWeek1();
                    break;
                case TB_LN_YEAR_WEEK_2:
                    offsetDate = lunarYearWeekEntity.getLunarYearWeek2();
                    break;
                case TB_LN_YEAR_WEEK_3:
                    offsetDate = lunarYearWeekEntity.getLunarYearWeek3();
                    break;
            }

            offsetDateRange.add(offsetDate);
        }

        //农历年周没有数据，返回一个极大值
        if (offsetDateRange.size() == 0) {
            offsetDateRange.add(BIConsts.MAX_END_DATE);
        }

        //补全日期为范围
        if (offsetDateRange.size() == 1) {
            offsetDateRange.add(offsetDateRange.get(0));
        }

        Collections.sort(offsetDateRange);
        return offsetDateRange;
    }

    /**
     * 获取当前日期的农历年周
     * @param baseDate
     * @param calcMode
     * @return
     */
    public static String getLunarYearWeekDate(String baseDate,AnalysisCalcMode calcMode){
        String result = "";

        lunarYearWeekEntity lunarYearWeekEntity = lunarYearWeekMap.get(baseDate);
        if (lunarYearWeekEntity == null) {
            return  result ;
        }

        switch (calcMode) {
            case TB_LN_YEAR_WEEK:
                result = lunarYearWeekEntity.getLunarYearWeek1();
                break;
            case TB_LN_YEAR_WEEK_2:
                result = lunarYearWeekEntity.getLunarYearWeek2();
                break;
            case TB_LN_YEAR_WEEK_3:
                result = lunarYearWeekEntity.getLunarYearWeek3();
        }

        return result;
    }

    /**
     * 构建当前日期的过滤条件
     * @param config
     * @return
     */
    public static StringBuilder buildLunarCurrentDateFilter(QueryConfigure config){

        StringBuilder builder = new StringBuilder();
        List<String> baseDateList = AnalysisUtil.getFilterDateList(config);
        Collections.sort(baseDateList);
        if (CollUtil.isNotEmpty(baseDateList)) {
            builder.append(String.format(" and ( ln_yw_cfg.current_dt between '%s' and '%s' )",
                    baseDateList.get(0), baseDateList.get(baseDateList.size() - 1)));
        }

        return builder;
    }

}
