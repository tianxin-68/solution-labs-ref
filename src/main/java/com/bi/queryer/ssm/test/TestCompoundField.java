package com.bi.queryer.ssm.test;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:46 2024/11/21
 * @Description TODO
 **/
public class TestCompoundField extends TestCalcField{
    private List<TestCompoundFieldOperator> operators = new ArrayList<>();
    public TestCompoundField() {
    }

    public static TestCompoundField copy(TestCalcField calcField){
        TestCompoundField f = JSONObject.parseObject(JSONObject.toJSONString(calcField), TestCompoundField.class);
        return f;
    }

    public List<TestCompoundFieldOperator> getOperators() {
        return operators;
    }

    public void setOperators(List<TestCompoundFieldOperator> operators) {
        this.operators = operators;
    }
}
