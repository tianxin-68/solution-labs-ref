package com.bi.queryer.ssm.engine.pivot.meta;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-28  16:35
 * @Description: 行维度元数据
 */
@Setter
@Getter
public class RowDimMeta {

    private String value;

    private Map<String, RowDimMeta> children = new LinkedHashMap<>();

    private RowDimMeta parent;

    public void addChild(RowDimMeta child) {
        children.put(child.getValue(), child);
    }

    @Override
    public String toString() {
        return "RowDimMeta{" +
                ", value='" + value + '\'' +
                '}';
    }
}
