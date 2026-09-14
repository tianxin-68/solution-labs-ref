package com.bi.queryer.ssm.query.template.cache;

import com.bi.queryer.ssm.engine.accelerate.cache.model.QueryTemplateCacheOperateLog;
import com.bi.queryer.ssm.engine.accelerate.cache.model.QueryTemplateCacheResult;
import com.bi.queryer.ssm.engine.accelerate.cache.model.QueryTemplateCacheSource;
import com.bi.queryer.ssm.engine.model.QueryTable;
import com.bi.queryer.ssm.engine.model.StarModel;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.google.common.collect.Sets;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author contributor
 * @Date 16:09 2025/1/9
 * @Description TODO
 **/
@Service
@Scope("prototype")
public class QueryTemplateCacheService {
    @Autowired
    private BaseDao dao ;

    public List<QueryTemplateCacheSource> queryCacheSource() {
        List<QueryTemplateCacheSource> sourceList = (List<QueryTemplateCacheSource>) dao.queryObjectList("ssm.query.template.cache.getCacheSource", new HashMap<>());
        return sourceList;
    }

    public List<QueryTemplateCacheResult> queryCacheResult() {
        List<QueryTemplateCacheResult> resultList = (List<QueryTemplateCacheResult>) dao.queryObjectList("ssm.query.template.cache.getCacheResult", new HashMap<>());
        return resultList;
    }

    /**
     * 异步添加缓存结果
     */
    public void addCacheResult(String currentUserName, String cacheKey, long cacheSize, String sql, List<StarModel> models, QueryTemplateCacheSource cacheSource){
        QueryTemplateCacheResult cacheResult = new QueryTemplateCacheResult();
        BeanUtils.copyProperties(cacheSource, cacheResult);
        cacheResult.setPkId(Guid.id());
        cacheResult.setCacheId(Guid.id());

        cacheResult.setCacheKey(cacheKey);
        cacheResult.setCacheSql(sql);
        cacheResult.setCacheSize(cacheSize);
        cacheResult.setCreatedBy(currentUserName);

        Set<String> etlJobs = new HashSet<>();
        for(StarModel model : models){
            List<QueryTable> tables = model.getTables();
            for(QueryTable table : tables){
                MetaTable mt = table.getMeta();
                if(mt == null){
                    continue;
                }
                etlJobs.addAll(mt.getEtlJobs());
            }
        }
        cacheResult.setEtlJobs(BIUtil.listToStr(etlJobs));

        // 先删除
        this.deleteByCacheKey(Sets.newHashSet(cacheKey));
        // 再插入
        dao.insert("ssm.query.template.cache.addCacheResult", cacheResult);

        // 记录操作日志
        QueryTemplateCacheOperateLog operateLog = new QueryTemplateCacheOperateLog(cacheSource.getQueryTplId(), cacheSource.getQueryTplName(), cacheKey, "add", "", "", currentUserName);
        this.addOperateLog(operateLog);
    }

    public void deleteByCacheKey(Set<String> cacheKeys){
        Map<String, Object> params = new HashMap<>();
        params.put("cacheKeys", cacheKeys);
        dao.delete("ssm.query.template.cache.deleteCacheResult", params);
    }

    public void deleteByAnalysisTemplateId(String analysisTplId){
        Map<String, Object> params = new HashMap<>();
        params.put("analysisTplId", analysisTplId);
        dao.delete("ssm.query.template.cache.deleteCacheResult", params);
    }

    public void deleteByAnalysisMenuId(String analysisMenuId){
        Map<String, Object> params = new HashMap<>();
        params.put("analysisMenuId", analysisMenuId);
        dao.delete("ssm.query.template.cache.deleteCacheResult", params);
    }

    public void deleteByQueryTemplateId(String queryTplId){
        Map<String, Object> params = new HashMap<>();
        params.put("queryTplId", queryTplId);
        dao.delete("ssm.query.template.cache.deleteCacheResult", queryTplId);
    }

    public void addOperateLog(QueryTemplateCacheOperateLog log){
        this.addOperateLog(Arrays.asList(log));
    }

    public void addOperateLog(List<QueryTemplateCacheOperateLog> logs){
        if(BIUtil.isEmpty(logs)){
            return;
        }
        new Thread(()->{
            Map<String, Object> params = new HashMap<>();
            params.put("logList", logs);
            dao.insert("ssm.query.template.cache.addOperateLog", params);
        }).start();
    }
}
