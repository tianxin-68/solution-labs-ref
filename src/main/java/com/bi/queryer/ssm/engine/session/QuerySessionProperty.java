package com.bi.queryer.ssm.engine.session;

import com.bi.queryer.sys.db.DataSourceType;

/**
 * @Author contributor
 * @Date 17:28 2024-03-27
 * @Description 查询session属性参数
 **/
public class QuerySessionProperty {
    private String key;

    private String value;

    private String scope;

    private String supportQueryEngines;

    public QuerySessionProperty() {

    }

    public QuerySessionProperty(String key, String value) {
        this.key = key;
        this.value = value;
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        QuerySessionProperty p = (QuerySessionProperty) obj;
        return this.key.equalsIgnoreCase(p.getKey());
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    public String getSupportQueryEngines() {
        return supportQueryEngines;
    }

    public void setSupportQueryEngines(String supportQueryEngines) {
        this.supportQueryEngines = supportQueryEngines;
    }

    public boolean isSupportTrino(){
        if(supportQueryEngines != null && supportQueryEngines.toLowerCase().contains(DataSourceType.Trino_Master.getDialect().toLowerCase())){
            return true;
        }
        return false;
    }

    public boolean isSupportDoris(){
        if(supportQueryEngines != null && supportQueryEngines.toLowerCase().contains(DataSourceType.Doris_Master.getDialect().toLowerCase())){
            return true;
        }
        return false;
    }
}
