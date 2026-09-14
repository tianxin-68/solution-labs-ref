package com.bi.queryer.sys.authapply.model;

import com.bi.queryer.util.BIUtil;

public class BaseTemplate {

    private String id;

    private String value;

    public BaseTemplate(String id, String value) {
        this.id = id;
        if (BIUtil.isEmpty(value))
            this.value = "";
        else
            this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
