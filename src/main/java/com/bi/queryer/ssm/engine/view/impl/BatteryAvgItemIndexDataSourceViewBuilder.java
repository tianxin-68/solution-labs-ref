package com.bi.queryer.ssm.engine.view.impl;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;

import java.util.List;

/**
 * 蓄电池列表商品曝光平均坑位
 */
public class BatteryAvgItemIndexDataSourceViewBuilder extends ItemIndexDataSourceViewBuilder{
    public BatteryAvgItemIndexDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
    }

    protected List<String> getExtendDims() {
        return super.getExtendDims();
    }

    public String getSelectFragment(QueryField qf) {
        return super.getSelectFragment(qf);
    }

    @Override
    public StringBuilder buildWhereClause() {
        return super.buildWhereClause();
    }

    /**
     * 构建扩展的where条件
     * @param whereSQL
     */
    public void buildExtendWhereClause(StringBuilder whereSQL) {
        whereSQL.append(" and vf.dim_analysis_business = '蓄电池' ");
        whereSQL.append(" and vf.dim_url = '/battery' ");
        whereSQL.append(" and vf.prd_listing_deviceid is not null ");
    }
}
