package com.bi.queryer.ssm.engine.view.impl;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilder;
import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.DataSourceView;
import com.bi.queryer.ssm.engine.view.DataSourceViewBuilderFactory;
import com.bi.queryer.ssm.engine.view.IDataSourceViewBuilder;
import com.bi.queryer.ssm.enums.DataSourceViewMeasureType;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 15:51 2025/3/17
 * @Description
 * 1、车龄数据源视图构建类，继承DefaultDataSourceSqlBuilder主要用于复用sql构建逻辑
 * 2、车龄构建逻辑：按keypage_prd_listing_deviceid+dim_tid+五级车型车龄+模板中的维度，记录去重
 **/
public class CarYearDataSourceViewBuilder extends BaseDataSourceViewBuilder{

    public CarYearDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
    }

    protected List<String> getExtendDims() {
        List<String> extendDims = Arrays.asList("vf.keypage_prd_listing_deviceid", "vf.dim_tid", "vf.dim_car_year");
        return extendDims;
    }

    public String getSelectFragment(QueryField qf) {
        if (qf.getName().equalsIgnoreCase("dim_car_year")) {
            String fragment = String.format("%s as %s", "cast(vf.dim_car_year as double)", "dim_car_year");
            return fragment;
        }

        return super.getSelectFragment(qf);
    }

    @Override
    public StringBuilder buildWhereClause() {
        StringBuilder whereSQL = super.buildWhereClause();
        whereSQL.append(" and vf.dim_analysis_business = '轮胎' ");
        whereSQL.append(" and vf.keypage_prd_listing_deviceid is not null ");
        return whereSQL;
    }

}
