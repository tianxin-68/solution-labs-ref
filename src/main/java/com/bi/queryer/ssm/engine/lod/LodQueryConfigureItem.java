package com.bi.queryer.ssm.engine.lod;

import com.bi.queryer.ssm.engine.QuerySqlFragments;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.AggExpressionType;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:10 2023-12-19
 * @Description lod查询配置项
 **/
public class LodQueryConfigureItem {

    private String code = "";

    private QueryConfigure config ;

    private String sql = "";

    /**
     * 聚合类型：前端可设置其聚合表达式类型（如：默认、日均值）
     */
    protected AggExpressionType aggExpressionType = AggExpressionType.None;

    private QuerySqlFragments fragments = new QuerySqlFragments();

    /**
     * 是否需要lod二次聚合：聚合到主视图的维度水平线
     */
    private boolean isAggToMainLevel = false;

    /**
     * 聚合到主维度level的维度列表
     */
    private List<QueryField> aggToMainLevelDimensions = new ArrayList<>();


    private QueryField lodField = null;

    public LodQueryConfigureItem(String code, QueryConfigure config) {
        this.code = code;
        this.config = config;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public QueryConfigure getConfig() {
        return config;
    }

    public void setConfig(QueryConfigure config) {
        this.config = config;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public QuerySqlFragments getFragments() {
        return fragments;
    }

    public void setFragments(QuerySqlFragments fragments) {
        this.fragments = fragments;
    }

    public boolean isMainConfig(){
        return LodConsts.CONFIG_CODE_MAIN.equals(this.getCode());
    }

    public boolean isAggToMainLevel() {
        return isAggToMainLevel;
    }

    public void setAggToMainLevel(boolean aggToMainLevel) {
        isAggToMainLevel = aggToMainLevel;
    }

    public List<QueryField> getAggToMainLevelDimensions() {
        return aggToMainLevelDimensions;
    }

    public void setAggToMainLevelDimensions(List<QueryField> aggToMainLevelDimensions) {
        this.aggToMainLevelDimensions = aggToMainLevelDimensions;
    }

    public QueryField getLodField() {
        return lodField;
    }

    public void setLodField(QueryField lodField) {
        this.lodField = lodField;
    }

    public AggExpressionType getAggExpressionType() {
        return aggExpressionType;
    }

    public void setAggExpressionType(AggExpressionType aggExpressionType) {
        this.aggExpressionType = aggExpressionType;
    }
}
