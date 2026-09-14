package com.bi.queryer.ssm.llm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.AnalysisCalcMode;
import com.bi.queryer.ssm.enums.AnalysisCalcType;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class LLMUtil {

    public  static Double toDouble(Object value){
        if(value == null){
            return null;
        }
        String valueStr = value + "";
        valueStr = valueStr.replace(",","").replace("pt", "");
        Double doubleValue = null;
        if (valueStr.contains("%")){
            // compareValue = Double.valueOf(compareValue0.replace("%", "")) / 100.0;
            doubleValue = Double.valueOf(valueStr.replace("%", ""));
        }else {
            doubleValue = Double.valueOf(valueStr);
        }
        return doubleValue;
    }

    public static Double getColumnDoubleValue(Map<String, Object> dataMap, String key) {
        Object value = dataMap.get(key);
        if (value == null) {
            return null;
        }

        if (StrUtil.isEmpty(value.toString())) {
            return null;
        }

        boolean isRatio = false;
        if (value.toString().contains("%") || value.toString().contains("pt")) {
            isRatio = true;
        }

        Double result = Double.valueOf(value.toString().replaceAll("%", "")
                .replaceAll("pt", "")
                .replaceAll(",", ""));

        if (isRatio) {
            result = result / 100;
        }

        return result;
    }

    public static String doubleFormat(Double value,String formatStr) {

        if (value == null) {
            return null;
        }

        DecimalFormat df = new DecimalFormat(formatStr);
        String result = df.format(value);
        return result;
    }

    public static Double doubleMultiply(Double d1,Double d2) {

        if (d1 == null || d2 == null) {
            return null;
        }

        BigDecimal b1 = new BigDecimal(d1);
        BigDecimal b2 = new BigDecimal(d2);
        return b1.multiply(b2).doubleValue();
    }

    public static Double doubleDivision(Double d1,Double d2) {

        if (d1 == null || d2 == null) {
            return null;
        }

        if (d2.compareTo(0.0) == 0) {
            return null;
        }

        BigDecimal b1 = new BigDecimal(d1);
        BigDecimal b2 = new BigDecimal(d2);
        return b1.divide(b2, 6, RoundingMode.HALF_UP).doubleValue();
    }

    public static Double doubleAdd(Double d1,Double d2) {

        if (d1 == null && d2 == null) {
            return null;
        }

        if (d1 == null) {
            return d2;
        }

        if (d2 == null) {
            return d1;
        }

        BigDecimal b1 = new BigDecimal(d1);
        BigDecimal b2 = new BigDecimal(d2);
        return b1.add(b2).doubleValue();
    }

    public static Double doubleSub(Double d1,Double d2) {

        if (d1 == null && d2 == null) {
            return null;
        }

        if (d1 == null) {
            return -d2;
        }

        if (d2 == null) {
            return d1;
        }

        BigDecimal b1 = new BigDecimal(d1);
        BigDecimal b2 = new BigDecimal(d2);
        return b1.subtract(b2).doubleValue();
    }

    /**
     * 获取中位数
     * @return
     */
    public static Double calculateMedian(List<Double> inputData) {

        List<Double> dataList = new ArrayList<>();

        //去掉空值
         for(Double d : inputData) {
             if (d == null) {
                 continue;
             }
             dataList.add(d);
         }

         if(CollUtil.isEmpty(dataList)){
             return null;
         }

        Double[] data = dataList.toArray(new Double[0]);

        // 对数组进行排序
        Arrays.sort(data);
        int length = data.length;

        // 检查数组长度是奇数还是偶数
        if (length % 2 == 1) {
            // 如果是奇数，中位数是位于中间位置的元素
            return data[length / 2];
        } else {
            // 如果是偶数，中位数是位于中间两个元素的平均值
            return (data[(length - 1) / 2] + data[length / 2]) / 2.0;
        }

    }

    public static String getFieldCode(LLMQueryField field){
        return getFieldCode(field.getField_code());
    }

    public static String getFieldCode(String code){
        String fieldCode = code;
        String[] keywords = SC.v("field.code.keywords", "ALL").split(",");
        for(String kw : keywords){
            if(BIUtil.isNotEmpty(fieldCode) && fieldCode.equalsIgnoreCase(kw)){
                fieldCode = fieldCode + SC.v("field.code.keyword.escape.suffix", "_");
            }
        }
        return fieldCode;
    }


    public static String getSourceFieldCodeFromAnalysisFieldCode(String analysisFieldCode) {
        String sourceFieldCode = analysisFieldCode;
        for (AnalysisCalcMode calcMode : AnalysisCalcMode.values()) {
            for (AnalysisCalcType calcType : AnalysisCalcType.values()) {
                sourceFieldCode = sourceFieldCode.replaceAll(String.format("_%s_%s", calcMode.getCode(), calcType.getCode()), "");
            }
        }

        //兼容贡献率的场景
        for (AnalysisCalcType calcType : AnalysisCalcType.values()) {
            if(AnalysisCalcType.UNKNOW == calcType){
                continue;
            }
            sourceFieldCode = sourceFieldCode.replaceAll(String.format("_%s", calcType.getCode()), "");
        }

        return sourceFieldCode;
    }


}
