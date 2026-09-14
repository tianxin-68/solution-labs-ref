package com.bi.queryer.ssm.engine.view.impl;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;

import java.util.Arrays;
import java.util.List;

/**
 * 轮胎-支付平均车龄
 */
public class TirePayAvgCarYearDataSourceViewBuilder extends BaseDataSourceViewBuilder{
    public TirePayAvgCarYearDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
    }

    protected List<String> getExtendDims() {
        List<String> extendDims = Arrays.asList("vf.pay_userid", "vf.dim_tid", "vf.dim_car_year");
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
        //默认 限制轮胎业务线
        whereSQL.append(" and vf.dim_analysis_business = '轮胎' ");
        //默认 限制支付
        whereSQL.append(" and vf.pay_userid is not null   ");
        return whereSQL;
    }
}
