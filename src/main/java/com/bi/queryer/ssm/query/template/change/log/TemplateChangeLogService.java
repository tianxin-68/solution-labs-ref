package com.bi.queryer.ssm.query.template.change.log;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.cache.RedisCacheManager;
import com.bi.queryer.ssm.query.template.change.log.model.TemplateChangeLogEntity;
import com.bi.queryer.ssm.query.template.change.log.model.TemplateChangeLogRsp;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.util.BIConsts;
import com.tx.cache.common.exception.KVException;
import com.tx.cache.redis.RedisCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
public class TemplateChangeLogService {

    @Autowired
    private BaseDao dao;

    /**
     * 根据模版ID获取模版变更记录
     * @param tplId
     * @return
     */
    public List<TemplateChangeLogRsp> list(String tplId) {

        List<TemplateChangeLogRsp> result = new ArrayList<>();

        List<TemplateChangeLogEntity> changeLogEntities = (List<TemplateChangeLogEntity>)dao.queryObjectList("ssm.template.change.log.list",tplId, DataSourceType.Default);

        if(CollUtil.isNotEmpty(changeLogEntities)){
            for (TemplateChangeLogEntity changeLogEntity : changeLogEntities){
                TemplateChangeLogRsp changeLogRsp = new TemplateChangeLogRsp();
                changeLogRsp.setTplId(changeLogEntity.getTplId());
                changeLogRsp.setCreatedBy(changeLogEntity.getCreatedBy());
                changeLogRsp.setCreatedTime(changeLogEntity.getCreatedTime());
                changeLogRsp.setChangeType(changeLogEntity.getChangeType());
                changeLogRsp.setChangeName(changeLogEntity.getChangeName());
                changeLogRsp.setChangeContent(changeLogEntity.getChangeContent());
                result.add(changeLogRsp);
            }
        }

        return result;
    }

    /**
     * 获取模版最后变更时间
     * @param tplId
     * @return
     */
    public String getTemplateLastChangeTime(String tplId) {
        String changeTime = (String) dao.queryObject("ssm.template.change.log.getTemplateLastChangeTimeByTplId", tplId, DataSourceType.Default);
        return changeTime;
    }

    /**
     * 新增模版变更记录
     * @param templateChangeLogEntity
     */
    public void add(TemplateChangeLogEntity templateChangeLogEntity) {
        dao.insert("ssm.template.change.log.add", templateChangeLogEntity);

        String keyPrefix = BIConsts.TEMPLATE_ASSET_UPDATETIME_REDIS_KEY;
        try {

            if (StrUtil.isEmpty(templateChangeLogEntity.getTplId())) {
                return;
            }

            String now = DateUtil.now();
            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            Integer expireMinute = Integer.valueOf(SC.v("ssm.template.asset.change.redis.expire.minute", "2880"));
            redisCacheClient.hset(keyPrefix, templateChangeLogEntity.getTplId(), now, expireMinute * 60);
        } catch (KVException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化模版最后变更时间到Redis
     * 用于定时任务，将近一个月变更过得模板信息同步到redis
     */
    public void initTemplateLastChangeTimeToRedis() {

        List<TemplateChangeLogEntity> changeLogEntities = (List<TemplateChangeLogEntity>) dao.queryObjectList("ssm.template.change.log.getRecentTemplateLastChangeTime", null, DataSourceType.Default);

        if (CollUtil.isEmpty(changeLogEntities)) {
            return;
        }

        String keyPrefix = BIConsts.TEMPLATE_ASSET_UPDATETIME_REDIS_KEY;
        try {
            RedisCacheClient<String, Object> redisCacheClient = RedisCacheManager.getRedisClient();
            Integer expireMinute = Integer.valueOf(SC.v("ssm.template.asset.change.redis.expire.minute", "2880"));
            for (TemplateChangeLogEntity changeLogEntity : changeLogEntities) {

                if (StrUtil.isEmpty(changeLogEntity.getTplId())) {
                    continue;
                }

                redisCacheClient.hset(keyPrefix, changeLogEntity.getTplId(), changeLogEntity.getCreatedTime(), expireMinute*60);
            }

        } catch (KVException e) {
            e.printStackTrace();
        }

    }
}
