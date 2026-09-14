package com.bi.queryer.ssm.engine.result;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.QueryArea;
import com.bi.queryer.util.BIConsts;

import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

public class DataSetBuilderFactory {

    /**
     * 创建数据集构造器
     * @param rs
     * @param dataSet
     * @param engine
     * @return
     */
    public static DefaultDataSetBuilder createDataSetBuilder(ResultSet rs, ResultDataSet dataSet, QueryEngine engine) {

//        String sessionId = engine.getConfig().getSessionId();
//        if (StrUtil.isEmpty(sessionId)) {
//            return new DefaultDataSetBuilder(rs, dataSet, engine);
//        }
//
//        if (!sessionId.startsWith(BIConsts.OLAP_API_SESSION_ID_PREFIX)) {
//            return new DefaultDataSetBuilder(rs, dataSet, engine);
//        }
//
//        //如果是olap-api调用，且没有列维度，则使用olap-api数据集构造器
//        List<QueryField> colDims = engine.getConfig().getResult().getFields()
//                .stream().filter(f -> f.getRawQueryArea() == QueryArea.ColumnDimension && !f.isAppend())
//                .collect(Collectors.toList());
//        int colDimCount = colDims.size();
//        if (StrUtil.isNotEmpty(engine.getConfig().getSettings().getOlapApiKey())
//                && colDimCount == 0) {
//            return new OlapApiDataSetBuilder(rs, dataSet, engine);
//        }

        return new DefaultDataSetBuilder(rs, dataSet, engine);
    }

}
