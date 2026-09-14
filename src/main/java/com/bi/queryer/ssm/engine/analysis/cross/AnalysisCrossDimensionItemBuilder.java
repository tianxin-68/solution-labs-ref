package com.bi.queryer.ssm.engine.analysis.cross;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.analysis.AnalysisUtil;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionItemBuilder;
import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.sys.common.KeyValuePair;
import com.bi.queryer.sys.enums.DateType;
import com.bi.queryer.sys.enums.SortType;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.period.DateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:49 2024-06-07
 * @Description 分析交叉表表格头：主要处理日期表头
 **/
public class AnalysisCrossDimensionItemBuilder extends CrossDimensionItemBuilder {

    /**
     * 分析计算方式
     */
    protected AnalysisCalcMode analysisCalcMode;

    /**
     * 自定义对比的索引
     */
    protected Integer customCompareIndex = -1;

    public AnalysisCrossDimensionItemBuilder(QueryConfigure config, QueryContext cxt) {
        super(config, cxt);
    }


    @Override
    public List<QueryField> build() {
        List<QueryField> result = super.build();
        if(BIUtil.isEmpty(colDimFields)){
            return result;
        }
        // 日期在类维度上，可以重复构建
        this.isBuilt = false;
        return result;
    }

    @Override
    protected ResultDataSet buildHeaderDataSetByParameter() {
        return super.buildHeaderDataSetByParameter();
    }

    /**
     * 重写获取日期过滤参数值列表：在同环比时，其case when条件值不一样，需按查询配置给case when赋值
     * 注：同环比时，值=同环比值，标题=原标题（即基准日期标题）
     * @param qf
     * @return
     */
    @Override
    public List<KeyValuePair> getDateFilterParameterValueAndTitles(QueryField qf){
        List<KeyValuePair> rawValueAndTitles = super.getDateFilterParameterValueAndTitles(qf);
        if(BIUtil.isEmpty(rawValueAndTitles)){
            return rawValueAndTitles;
        }

        //业务日历逻辑处理,将年份替换
        if(config.getSettings().isBusinessCalendar()){
            Integer promoYear = config.getSettings().getPromoYear();
            Integer promoYearOffset = config.getSettings().getPromoYear();
            if(analysisCalcMode!=null && analysisCalcMode.isPromoTb()){
                promoYearOffset = promoYear + analysisCalcMode.getOffset();
            }

            List<KeyValuePair> promoValueAndTitles = new ArrayList<>();
            //添加活动标识过滤
            for (KeyValuePair keyValuePair : rawValueAndTitles) {
                String key = keyValuePair.getValue().toString().replace(promoYear.toString(),promoYearOffset.toString());
                KeyValuePair kv = new KeyValuePair(key, keyValuePair.getValue());
                promoValueAndTitles.add(kv);
            }
            return promoValueAndTitles;
        }

        //农可比，取当期日期
        if(analysisCalcMode.isTblnyw()){
            return rawValueAndTitles;
        }

        List<String> compareDateRange = AnalysisUtil.getCompareDateRange(config, analysisCalcMode, customCompareIndex);
        if(BIUtil.isEmpty(compareDateRange) || compareDateRange.size() < 2){
            return rawValueAndTitles;
        }

        QueryField filterColumnField = config.getFilter().getFieldByCode(qf.getCode());
        if(filterColumnField == null){
            return rawValueAndTitles;
        }
        DateGranularity dateGranularity = DateGranularity.get(filterColumnField.getQueryDateGranularity());
        DateType dateType = DateType.get(dateGranularity.toString());
        List<String> newValues = DateUtil.getRangeList(compareDateRange.get(0), compareDateRange.get(1), dateType);

        /** 确保新值长度与原值的长度一致 **/

        // 新值多于原值：新值去掉多余部分
        if(newValues.size() > rawValueAndTitles.size()){
            newValues = ListUtil.sub(newValues, 0, rawValueAndTitles.size());
        }

        // 新值少于原值：新值添加null
//        SortType sortType = this.getDateHeaderSortType();
        int maxCount = getHeaderColumnMaxCount();
        if(newValues.size() < rawValueAndTitles.size()){
            int diff = rawValueAndTitles.size() - newValues.size();
            if(diff > maxCount) {
                diff = maxCount;
            }
            Integer year = 2900;
            for(int i = 0; i < diff; i++){
                String futureDate = "";
                switch (dateGranularity){
                    case WEEK:
                    case MONTH:
                        futureDate = year + "01";
                        break;
                    case YEAR:
                        futureDate = year + "";
                        break;
                    case QUARTER:
                        futureDate = year + "-Q1";
                        break;
                    case DAY:
                    default:
                        futureDate = year + "-01-01";
                        break;

                }
                // 此处不可添加相同值，因为用于表头时会做去重
                newValues.add(futureDate);
                year++;
            }
        }

        // 保持和字段设置的顺序一致
        Collections.sort(newValues);

        // 兼容处理：长度一致时，降序，不一致时，升序
       // if(sortType == SortType.DESC) {
            // 按降序排序
        newValues = ListUtil.reverse(newValues);
//        }

        List<KeyValuePair> newValueAndTitles = new ArrayList<>();
        for(int i = 0 ; i < newValues.size(); i++){
            String key = newValues.get(i);
            String title = "";
            if(i < rawValueAndTitles.size()) {
                // 使用旧值title，因为旧值作为基准日期，最终只显示旧值title
                title = rawValueAndTitles.get(i).getValue() + "";
            }
            KeyValuePair kv = new KeyValuePair(key, title);
            newValueAndTitles.add(kv);
        }

        return newValueAndTitles;
    }

    public AnalysisCalcMode getAnalysisCalcMode() {
        return analysisCalcMode;
    }

    public void setAnalysisCalcMode(AnalysisCalcMode analysisCalcMode) {
        this.analysisCalcMode = analysisCalcMode;
    }

    public Integer getCustomCompareIndex() {
        return customCompareIndex;
    }

    public void setCustomCompareIndex(Integer customCompareIndex) {
        this.customCompareIndex = customCompareIndex;
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");

        List<String> sub = CollectionUtil.sub(list, 0, 2);

        System.out.println(sub);
    }
}
