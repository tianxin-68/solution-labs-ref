package com.bi.queryer.ssm.engine.parameter;

import com.bi.queryer.ssm.enums.FieldValueFilterType;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 组件参数
 */
public class WidgetParameter implements Comparable<WidgetParameter> {
    private String key;

    private String value;

    private String keyTitle;

    private String compareType = FieldValueFilterType.include.toString();

    /**
     * 元信息
     */
    private WidgetParameterMeta meta = new WidgetParameterMeta();

    private boolean enable = true;

    @Deprecated
    private List realValues = new ArrayList(); // 真实值，规范化后的真实值

    private List<List> realValueList = new ArrayList<>(); // 真实值列表，用于处理多段参数值

    public WidgetParameter clone()  {
        JSONObject jsonObject = BIUtil.toJSONObject(this);
        WidgetParameter wp = JSONObject.toJavaObject(jsonObject, this.getClass());
        return wp;
    }

    /**
     * 将对象按姓名字典序升序排序
     * @param o
     * @return
     */
    @Override
    public int compareTo(WidgetParameter o) {
        return this.key.compareTo(o.getKey());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKeyTitle() {
        if(BIUtil.isEmpty(keyTitle) && meta != null) {
            keyTitle = meta.getKeyTitle();
        }
        return keyTitle;
    }

    public void setKeyTitle(String keyTitle) {
        this.keyTitle = keyTitle;
    }

    public WidgetParameterMeta getMeta() {
        return meta;
    }

    public void setMeta(WidgetParameterMeta meta) {
        this.meta = meta;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public List getRealValues() {
        // 兼容处理
        if(BIUtil.isNotEmpty(realValueList)) {
            return realValueList.get(0);
        }
        return realValues;
    }

    public void setRealValues(List realValues) {
        this.realValues = realValues;
        this.realValueList.clear();
        this.addRealValues(realValues);
    }

    public void addRealValues(List realValues) {
        this.realValueList.add(realValues);
    }

    public List<List> getRealValueList() {
        return realValueList;
    }

    public void setRealValueList(List<List> realValueList) {
        this.realValueList = realValueList;
    }

    @Override
    public String toString() {
        return key + "\t" + keyTitle + "\t" + value;
    }

    public String getCompareType() {
        return compareType;
    }

    public void setCompareType(String compareType) {
        this.compareType = compareType;
    }
}
