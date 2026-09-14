package com.bi.queryer.ssm.engine.view.impl;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 保养-支付平均车龄
 */
public class MaintenancePayAvgCarYearDataSourceViewBuilder extends BaseDataSourceViewBuilder {

    private final String analysisBusinessLine;

    public MaintenancePayAvgCarYearDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt,
                                                         String analysisBusinessLine) {
        super(model, config, cxt);
        this.analysisBusinessLine = analysisBusinessLine;
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
        List<String> analysisBusinessLineList = Arrays.asList(StringUtils.split(analysisBusinessLine, ","));
        whereSQL.append(String.format(" and vf.dim_analysis_business in (%s) ", "'" + StringUtils.join(analysisBusinessLineList, "','") + "'"));
        whereSQL.append(" and vf.pay_userid is not null   ");
        return whereSQL;
    }
}
