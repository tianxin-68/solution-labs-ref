package com.bi.queryer.ssm.engine.parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件参数元数据
 */
public class WidgetParameterMeta {
    private List<String> valueTitle = new ArrayList<>();
    private Map<String, Object> ext = new HashMap<>(); // 扩展信息

    private String __$title = "";

    public List<String> getValueTitle() {
        return valueTitle;
    }

    public void setValueTitle(List<String> valueTitle) {
        this.valueTitle = valueTitle;
    }

    public String get__$title() {
        return __$title;
    }

    public void set__$title(String __$title) {
        this.__$title = __$title;
    }

    public String getKeyTitle(){
        return this.get__$title();
    }

    public Map<String, Object> getExt() {
        return ext;
    }

    public void setExt(Map<String, Object> ext) {
        this.ext = ext;
    }
}
