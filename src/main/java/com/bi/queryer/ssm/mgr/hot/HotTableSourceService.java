package com.bi.queryer.ssm.mgr.hot;

import com.bi.queryer.ssm.meta.accelerate.hot.HotTableInfo;
import com.bi.queryer.ssm.meta.accelerate.hot.HotTableSource;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author contributor
 * @Date 17:08 2024/8/19
 * @Description 热数据源增删改查
 **/
@Service
@Scope("prototype")
public class HotTableSourceService {
    @Autowired
    protected BaseDao dao = null;

    /**
     * 获取所有热数据源（不分页）
     * @return
     */
    public List<HotTableSource> getAllHotTableSource(){
        String sqlId = "ssm.hot.source.queryAllHotTableSource";
        List<HotTableSource> hotTableSourceList = (List<HotTableSource>) dao.queryObjectList(sqlId, null, DataSourceType.Default);
        return hotTableSourceList;
    }
}
