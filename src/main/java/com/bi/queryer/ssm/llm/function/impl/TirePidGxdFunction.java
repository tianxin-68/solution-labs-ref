package com.bi.queryer.ssm.llm.function.impl;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;
import com.bi.queryer.ssm.llm.entity.LLMQueryResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮胎-商品ID-贡献度
 */
public class TirePidGxdFunction extends TireBrandGxdFunction{
    public TirePidGxdFunction(LLMQueryConfig config) {
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
            if ("ALL".equalsIgnoreCase(dim.getField_code()) || "ASV".equalsIgnoreCase(dim.getField_code())) {
                continue;
            }

            newDimensions.add(dim);
        }

        newDimensions.add( new LLMQueryField("ALL_", "轮胎规格"));
        newDimensions.add(new LLMQueryField("ASV", "商品ID"));

        return newDimensions;
    }

    /**
     * 附加列元信息
     * @param columns
     * @return
     */
    public  List<ResultDataSetColumn> appendColumns(List<ResultDataSetColumn> columns) {
        //添加【轮胎-商品ID-结构效应】
        ResultDataSetColumn tirePidCtrStructColumn = new ResultDataSetColumn();
        tirePidCtrStructColumn.setCode("TIRE_PID_GXD_STRUCT");
        tirePidCtrStructColumn.setTitle("轮胎_商品_结构效应");
        tirePidCtrStructColumn.setDataType("double");
        tirePidCtrStructColumn.setType("");
        columns.add(tirePidCtrStructColumn);

        //【轮胎-商品ID-转化效应】
        ResultDataSetColumn tirePidCtrTranSColumn = new ResultDataSetColumn();
        tirePidCtrTranSColumn.setCode("TIRE_PID_GXD_TRANS");
        tirePidCtrTranSColumn.setTitle("轮胎_商品_转化效应");
        tirePidCtrTranSColumn.setDataType("double");
        tirePidCtrTranSColumn.setType("");
        columns.add(tirePidCtrTranSColumn);

        //【轮胎品牌贡献度】
        ResultDataSetColumn tirePidCtrColumn = new ResultDataSetColumn();
        tirePidCtrColumn.setCode("TIRE_PID_GXD");
        tirePidCtrColumn.setTitle("轮胎_商品_贡献度");
        tirePidCtrColumn.setDataType("double");
        tirePidCtrColumn.setType("");
        columns.add(tirePidCtrColumn);

        return columns;
    }


    /**
     * 获取列名前缀
     * @return
     */
    public String getColumnPrefix() {
        return "TIRE_PID";
    }

}
