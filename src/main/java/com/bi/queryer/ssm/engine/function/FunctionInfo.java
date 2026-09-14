package com.bi.queryer.ssm.engine.function;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:27 2025/7/1
 * @Description TODO
 **/
public class FunctionInfo {
    private String name;
    private List<String> parameters = new ArrayList<>();

    public FunctionInfo() {
    }

    public FunctionInfo(String name, List<String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
}
