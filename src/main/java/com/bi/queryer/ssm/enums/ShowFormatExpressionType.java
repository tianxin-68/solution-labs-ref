package com.bi.queryer.ssm.enums;

import com.bi.queryer.ssm.engine.function.IFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
public enum ShowFormatExpressionType {

    None("","无"),
    Format_Decimal("###,###,##0.00","###,###,##0.00"),
    Format_Percent("###,###,##0.00%","###,###,##0.00%"),
    Format_Number("###,###,###","###,###,###"),
    Format_DateTime(IFunction.Format_DateTime, IFunction.Format_DateTime),
    Format_Date(IFunction.Format_Date, IFunction.Format_Date),
    Format_Week(IFunction.Format_Week, IFunction.Format_Week),
    Format_Month(IFunction.Format_Month, IFunction.Format_Month),
    Format_Quarter(IFunction.Format_Quarter,IFunction.Format_Quarter),
    Format_Year(IFunction.Format_Year, IFunction.Format_Year),
    Auto_Extend("Auto-Extend","Auto-Extend");

    private String code;
    private String desc;

    private ShowFormatExpressionType(String code,String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    // 转换成为 List<Map<String, String>>, 对外提供查询和遍历功能
    public static List<Map<String, String>> toListMap() {
        List<Map<String, String>> listMap = new ArrayList<>();
        for (ShowFormatExpressionType i : ShowFormatExpressionType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("value", i.getCode());
            map.put("label", i.getDesc());
            listMap.add(map);
        }
        return listMap;
    }

    /**
     * 通过日期粒度获取对应的格式化表达式
     * @param queryDateGranularity
     * @return
     */
    public static String getFormatExpressionByDateGranularity(String queryDateGranularity) {

        DateGranularity dateGranularity = DateGranularity.get(queryDateGranularity);
        ShowFormatExpressionType showFormatExpressionType = Format_Date;

        switch (dateGranularity) {
            case DAY:
                showFormatExpressionType = Format_Date;
                break;
            case WEEK:
                showFormatExpressionType = Format_Week;
                break;
            case MONTH:
                showFormatExpressionType = Format_Month;
                break;
            case QUARTER:
                showFormatExpressionType = Format_Quarter;
                break;
            case YEAR:
                showFormatExpressionType = Format_Year;
                break;
        }

        return showFormatExpressionType.getCode();
    }

}
