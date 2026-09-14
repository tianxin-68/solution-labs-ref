package com.bi.queryer.ssm.query.template.view;

import cn.hutool.core.collection.CollUtil;
import com.bi.queryer.ssm.query.template.view.model.TemplateCfgHistoryEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TemplateViewCfgHistoryService {

    @Autowired
    private BaseDao dao;

    ExecutorService cfgHistoryThreadPool = null;

    private final Integer THREAD_NUM = 3;

    public TemplateViewCfgHistoryService(){
        cfgHistoryThreadPool = Executors.newFixedThreadPool(THREAD_NUM);
    }

    public void add(TemplateCfgHistoryEntity cfgHistoryEntity) {

        cfgHistoryThreadPool.execute(new Runnable() {

            @Override
            public void run() {
                dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                    @Override
                    public void execute() {

                        //每个视图最多保存3份历史
                        List<Long> pkids = (List<Long>) dao.queryObjectList("ssm.query.template.cfg.history.listByViewId", cfgHistoryEntity.getViewId());

                        if (CollUtil.isNotEmpty(pkids)) {
                            if (pkids.size() > 2) {

                                List<Long> deletePkids = new ArrayList<>();
                                for (int i = 2; i < pkids.size(); i++) {
                                    deletePkids.add(pkids.get(i));
                                }
                                dao.delete("ssm.query.template.cfg.history.batchDelete", deletePkids);
                            }
                        }
                        dao.insert("ssm.query.template.cfg.history.add", cfgHistoryEntity);

                    }
                });
            }
        });

    }

}
