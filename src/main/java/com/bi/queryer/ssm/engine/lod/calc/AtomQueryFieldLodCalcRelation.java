package com.bi.queryer.ssm.engine.lod.calc;

import com.bi.queryer.ssm.engine.config.field.QueryField;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 14:38 2024-06-18
 * @Description 原子查询字段与lod计算字段的关系
 **/
public class AtomQueryFieldLodCalcRelation {
    private QueryField atomField;

    private List<QueryField> lodCalcFields = new ArrayList<>();

    public AtomQueryFieldLodCalcRelation(QueryField atomField) {
        this.atomField = atomField;
    }

    public void addLodCalc(QueryField field){
        if(!lodCalcFields.contains(field)){
            lodCalcFields.add(field);
        }
    }

    public QueryField getAtomField() {
        return atomField;
    }

    public void setAtomField(QueryField atomField) {
        this.atomField = atomField;
    }

    public List<QueryField> getLodCalcFields() {
        return lodCalcFields;
    }

    public void setLodCalcFields(List<QueryField> lodCalcFields) {
        this.lodCalcFields = lodCalcFields;
    }
}
