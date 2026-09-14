package com.bi.queryer.ssm.mgr.hot;

import com.bi.queryer.ssm.meta.accelerate.hot.HotTableDdl;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 17:09 2024/8/19
 * @Description 热表ddl增删改查
 **/
@Service
@Scope("prototype")
public class HotTableDdlService {

    @Autowired
    protected BaseDao dao = null;

    public void save(HotTableDdl ddl){
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                // 先删除
                dao.delete("ssm.hot.ddl.delete", ddl);

                // 再新增
                dao.insert("ssm.hot.ddl.insert", ddl);
            }
        });
    }

    public HotTableDdl get(String hotTableName, String dbEngine){
        Map<String, String> param = new HashMap<>();
        param.put("hotTableName", hotTableName);
        param.put("dbEngine", dbEngine);
        HotTableDdl ddl = dao.queryObject("ssm.hot.ddl.get", param, HotTableDdl.class);
        return ddl;
    }

    public List<HotTableDdl> getAll() {
        List<HotTableDdl> hotTableDdlList = (List<HotTableDdl>) dao.queryObjectList("ssm.hot.ddl.getAll", null);
        return hotTableDdlList;
    }

}
