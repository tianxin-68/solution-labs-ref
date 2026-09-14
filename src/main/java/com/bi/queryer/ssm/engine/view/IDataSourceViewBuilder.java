package com.bi.queryer.ssm.engine.view;

import com.bi.queryer.ssm.engine.QueryContext;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.model.StarModel;

public interface IDataSourceViewBuilder {
    public DataSourceView buildView(StarModel model, QueryConfigure config, QueryContext cxt);
}
