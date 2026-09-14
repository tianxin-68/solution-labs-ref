package com.bi.queryer.ssm.engine.view.impl;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;

import java.util.Arrays;
import java.util.List;

public class ItemIndexDataSourceViewBuilder extends BaseDataSourceViewBuilder{
    public ItemIndexDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
    }

    protected List<String> getExtendDims() {
        List<String> extendDims = Arrays.asList("vf.item_index");
        return extendDims;
    }

    public String getSelectFragment(QueryField qf) {
        if (qf.getName().equalsIgnoreCase("prd_listing_deviceid")) {
            String fragment = String.format("%s as %s", "count(distinct vf.prd_listing_deviceid) ", "prd_listing_deviceid");
            return fragment;
        }

        if(qf.getName().equalsIgnoreCase("item_index")){
            String fragment = String.format("%s as %s", "vf.item_index", "item_index");
            return fragment;
        }

        return super.getSelectFragment(qf);
    }

    @Override
    public StringBuilder buildWhereClause() {
        StringBuilder whereSQL = super.buildWhereClause();
        buildExtendWhereClause(whereSQL);
        return whereSQL;
    }

    /**
     * 构建扩展的where条件
     * @param whereSQL
     */
    public void buildExtendWhereClause(StringBuilder whereSQL) {
        whereSQL.append(" and vf.dim_analysis_business = '轮胎' ");
        whereSQL.append(" and vf.dim_url = '/tire' ");
        whereSQL.append(" and vf.prd_listing_deviceid is not null ");
    }

}
