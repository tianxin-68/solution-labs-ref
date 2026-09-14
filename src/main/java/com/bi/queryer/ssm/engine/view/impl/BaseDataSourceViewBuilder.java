package com.bi.queryer.ssm.engine.view.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilder;
import com.bi.queryer.ssm.engine.DefaultDataSourceSqlBuilderAvgWrapper;
import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.engine.view.DataSourceView;
import com.bi.queryer.ssm.engine.view.IDataSourceViewBuilder;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaseDataSourceViewBuilder extends DefaultDataSourceSqlBuilder implements IDataSourceViewBuilder {


    public BaseDataSourceViewBuilder(StarModel model, QueryConfigure config, QueryContext cxt) {
        super(model, config, cxt);
    }

    @Override
    public DataSourceView buildView(StarModel model, QueryConfigure config, QueryContext cxt) {

        super.prepare();

        StringBuilder dsSql = new StringBuilder();

        dsSql.append(buildSelectClause());
        dsSql.append(buildFromClause());
        dsSql.append(buildWhereClause());
        dsSql.append(buildGroupByClause());

        String sql = dsSql.toString();

        DataSourceView view = new DataSourceView();
        view.setSql(sql);

        return view;
    }

    @Override
    public StringBuilder buildSelectClause() {
        StringBuilder selectSQL = new StringBuilder();

        List<String> fragments = new ArrayList<String>();

        for (QueryField qf : model.getFields()) {

            if (qf.isVirtual()) {
                continue;
            }

            //计算维度
            if (qf.isCustomDimension() || (!qf.isMeasure() && qf.isCalc())) {
                fragments.add(String.format("%s as %s", getCalcFieldExpr(qf), qf.getCode()));
                continue;
            }

            if (qf.isCalc()) {
                continue;
            }
            
            fragments.add(getSelectFragment(qf));
        }

        if (CollUtil.isNotEmpty(fragments)) {
            fragments = fragments.stream().distinct().collect(Collectors.toList());
            selectSQL.append(String.format(" select %s ", BIUtil.listToStr(fragments)));
        }

        return selectSQL;
    }

    public String getTableAlias(QueryField qf){
        String tableAlias = super.getTableAlias(qf);
        return tableAlias;
    }

    public String getSelectFragment(QueryField qf) {
        String fragment = String.format("%s.%s as %s", getTableAlias(qf),qf.getName(), qf.getName());

        if(qf.isAppend()){
            fragment = String.format("null as %s", qf.getName());
        }
        return fragment;
    }

    @Override
    public StringBuilder buildWhereClause() {
        StringBuilder whereSQL = cxt.getWhereSqlBuilder();

        if (StrUtil.isEmpty(whereSQL.toString())) {
            whereSQL.append(" where 1=1 ");
        }

        return whereSQL;
    }

    @Override
    public StringBuilder buildGroupByClause() {

        StringBuilder groupBySQL = new StringBuilder();

        List<String> fragments = new ArrayList<String>();

        fragments.addAll(getExtendDims());
        for(QueryField field : model.getFields()) {
            if (fragments.contains(field.getName())) {
                continue;
            }

            if (field.isVirtual()) {
                continue;
            }

            if (field.isMeasure()) {
                continue;
            }

            //1 前端配置的计算维度  field.isCustomDimension()
            //2 后台配置的计算维度 field.isCalc()
            if(field.isCustomDimension() || field.isCalc()){
                fragments.add(getCalcFieldExpr(field));
                continue;
            }

            if (field.isAppend()) {
                continue;
            }

            fragments.add(getTableAlias(field) + "." + field.getName());
        }

        if(CollUtil.isNotEmpty(fragments)){
            fragments = fragments.stream().distinct().collect(Collectors.toList());
            groupBySQL.append(String.format(" group by %s ", BIUtil.listToStr(fragments)));
        }

        return groupBySQL;
    }

    /**
     * 不需要包装类
     * @param datasourceSql
     * @param model
     * @param config
     * @param cxt
     * @return
     */
    @Override
    protected DefaultDataSourceSqlBuilderAvgWrapper createDefaultDataSourceSqlBuilderAvgWrapper(String datasourceSql, StarModel model, QueryConfigure config, QueryContext cxt) {
        return null;
    }

    /**
     * 获取扩展的维度
     * @return
     */
    protected List<String> getExtendDims(){
        return new ArrayList<>();
    }

}
