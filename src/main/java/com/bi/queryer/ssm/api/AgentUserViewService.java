package com.bi.queryer.ssm.api;

import com.bi.queryer.ssm.api.entity.AgentUserViewEntity;
import com.bi.queryer.sys.base.BaseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Scope("prototype")
public class AgentUserViewService {

    @Autowired
    private BaseDao dao;

    public List<AgentUserViewEntity> queryViewListByUserName(String userName) {
        return (List<AgentUserViewEntity>) dao.queryObjectList("ssm.agent.user.view.queryViewListByUserName", userName);
    }
}
