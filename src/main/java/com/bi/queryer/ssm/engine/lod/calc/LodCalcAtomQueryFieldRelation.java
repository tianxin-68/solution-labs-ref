package com.bi.queryer.ssm.engine.lod.calc;

import com.bi.queryer.ssm.engine.config.field.QueryField;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 10:29 2024-06-18
 * @Description lod二次四则计算查询字段与原子字段的关系
 **/
public class LodCalcAtomQueryFieldRelation {
    private QueryField lodField;

    private List<QueryField> mainAtomQueryFields = new ArrayList<>();

    private List<QueryField> lodAtomQueryFields = new ArrayList<>();

    public LodCalcAtomQueryFieldRelation(QueryField lodField) {
        this.lodField = lodField;
    }

    public QueryField getLodField() {
        return lodField;
    }

    public void setLodField(QueryField lodField) {
        this.lodField = lodField;
    }


    public void addMainAtomQueryField(QueryField field){
        if(!this.mainAtomQueryFields.contains(field)){
            this.mainAtomQueryFields.add(field);
        }
    }

    public void addLodAtomQueryField(QueryField field){
        if(!this.lodAtomQueryFields.contains(field)){
            this.lodAtomQueryFields.add(field);
        }
    }

    public List<QueryField> getMainAtomQueryFields() {
        return mainAtomQueryFields;
    }

    public void setMainAtomQueryFields(List<QueryField> mainAtomQueryFields) {
        this.mainAtomQueryFields = mainAtomQueryFields;
    }

    public List<QueryField> getLodAtomQueryFields() {
        return lodAtomQueryFields;
    }

    public void setLodAtomQueryFields(List<QueryField> lodAtomQueryFields) {
        this.lodAtomQueryFields = lodAtomQueryFields;
    }
}
