package com.bi.queryer.ssm.engine;

/**
 * @Author contributor
 * @Date 11:47 2023-12-21
 * @Description 查询引擎类型
 **/
public enum QueryEngineType {
    Common("常规（明细表）"),
    Cross("交叉表（有列维度）"),
    Analysis("分析（同环比、汇总等）"),
    Lod("lod表达式（自定义lod字段）"),
    Pivot("行列转置");

    private String desc;

    private QueryEngineType(String desc) {
        this.desc = desc;
    }
}
