package com.bi.queryer.ssm.llm.function.impl;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;

import java.util.ArrayList;
import java.util.List;

public class TirePNameGxdFunction extends TireBrandGxdFunction{
    public TirePNameGxdFunction(LLMQueryConfig config) {
        super(config);
    }

    /**
     * 构建附加的维度，并调整排序
     * @param result
     * @return
     */
    public List<LLMQueryField> buildDimensions(LLMQueryResult result){

        List<LLMQueryField> newDimensions = new ArrayList<>();
        for(LLMQueryField dim : result.getDimensions()) {
            if ("ALL".equalsIgnoreCase(dim.getField_code()) || "ABG".equalsIgnoreCase(dim.getField_code())) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add( new LLMQueryField("ALL_", "轮胎规格"));
        newDimensions.add(new LLMQueryField("ABG", "商品名称"));

        return newDimensions;
    }

    /**
     * 附加列元信息
     * @param columns
     * @return
     */
    public  List<ResultDataSetColumn> appendColumns(List<ResultDataSetColumn> columns) {
        //添加【轮胎_商品名称_结构效应】
        ResultDataSetColumn tirePnameCtrStructColumn = new ResultDataSetColumn();
        tirePnameCtrStructColumn.setCode("TIRE_PNAME_GXD_STRUCT");
        tirePnameCtrStructColumn.setTitle("轮胎_商品_结构效应");
        tirePnameCtrStructColumn.setDataType("double");
        tirePnameCtrStructColumn.setType("");
        columns.add(tirePnameCtrStructColumn);

        //【轮胎_商品名称_转化效应】
        ResultDataSetColumn tirePNameCtrTranSColumn = new ResultDataSetColumn();
        tirePNameCtrTranSColumn.setCode("TIRE_PNAME_GXD_TRANS");
        tirePNameCtrTranSColumn.setTitle("轮胎_商品_转化效应");
        tirePNameCtrTranSColumn.setDataType("double");
        tirePNameCtrTranSColumn.setType("");
        columns.add(tirePNameCtrTranSColumn);

        //【轮胎_商品名称_贡献度】
        ResultDataSetColumn tirePNameCtrColumn = new ResultDataSetColumn();
        tirePNameCtrColumn.setCode("TIRE_PNAME_GXD");
        tirePNameCtrColumn.setTitle("轮胎_商品_贡献度");
        tirePNameCtrColumn.setDataType("double");
        tirePNameCtrColumn.setType("");
        columns.add(tirePNameCtrColumn);

        return columns;
    }


    /**
     * 获取列名前缀
     * @return
     */
    public String getColumnPrefix() {
        return "TIRE_PNAME";
    }
}
