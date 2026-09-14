package com.bi.queryer.ssm.engine.view.impl;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.enums.DataSourceViewMeasureType;

import java.util.Arrays;
import java.util.List;

public class CarPriceDataSourceViewBuilder extends BaseDataSourceViewBuilder{
    public CarPriceDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
    }

    protected List<String> getExtendDims() {
        List<String> extendDims = Arrays.asList("vf.keypage_prd_listing_deviceid", "vf.dim_tid", "vf.guide_price");
        return extendDims;
    }

    public String getSelectFragment(QueryField qf) {
        if (qf.getName().equalsIgnoreCase("guide_price")) {
            String fragment = String.format("%s as %s", " cast(vf.guide_price as double)", "guide_price");
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
