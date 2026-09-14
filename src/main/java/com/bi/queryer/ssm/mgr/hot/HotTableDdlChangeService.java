package com.bi.queryer.ssm.mgr.hot;

import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdlChange;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-09-09  19:25
 * @Description: 热表数据库变更记录-服务
 */
@Service
@Scope("prototype")
public class HotTableDdlChangeService {

    @Autowired
    protected BaseDao dao = null;


    public List<HotTableDdlChange> queryHotTableDdlChange(){
        String sqlId = "ssm.hot.table.ddl.change.queryHotTableDdlChange";
        List<HotTableDdlChange> hotTableDdlChangeList = (List<HotTableDdlChange>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        return hotTableDdlChangeList;
    }

    public void add(String sourceTableName,String operator){

        HotTableDdlChange hotTableDdlChange = new HotTableDdlChange();
        hotTableDdlChange.setSourceTableName(sourceTableName);
        hotTableDdlChange.setIsActive(Enabled.YES.getId());
        hotTableDdlChange.setCreatedBy(operator);

        String sqlId = "ssm.hot.table.ddl.change.add";
        dao.insert(sqlId,hotTableDdlChange);

    }

    public void update(HotTableDdlChange hotTableDdlChange){
        String sqlId = "ssm.hot.table.ddl.change.update";
        dao.update(sqlId,hotTableDdlChange);
    }

}
