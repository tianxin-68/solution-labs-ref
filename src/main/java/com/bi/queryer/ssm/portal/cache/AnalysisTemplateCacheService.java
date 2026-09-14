package com.bi.queryer.ssm.portal.cache;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.portal.cache.entity.AnalysisTemplateCacheEntity;
import com.bi.queryer.ssm.portal.cache.entity.AnalysisTemplateCacheItem;
import com.bi.queryer.ssm.portal.cache.entity.AnalysisTemplateCacheLogEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 18:07 2024/12/26
 * @Description 分析模板缓存逻辑处理类
 **/
@Service
@Scope("prototype")
public class AnalysisTemplateCacheService {

    @Autowired
    private BaseDao dao = null;

    /**
     * 分析模板以便缓存模板数据缓存（通过doris sql缓存）
     * @param menuId
     * @return
     */
    public List<String> cache(String menuId){
        Map<String, Object> params = new HashMap<>();
        params.put("menuId", menuId);
        List<String> logInfos = new ArrayList<>();
        List<AnalysisTemplateCacheEntity> cacheEntities = (List<AnalysisTemplateCacheEntity>) dao.queryObjectList("ssm.analysis.template.cache.getAnalysisTemplate", params);
        if(BIUtil.isEmpty(cacheEntities)){
            logInfos.add("无分析模板需缓存");
            return logInfos;
        }

        List<AnalysisTemplateCacheItem> cacheItems = new ArrayList<>();
        // 按用户+日期粒度创建缓存item：因为不同的用户查询权限不同（如：查询记录数、数据行权限等）
        String uiBaseUrl = SC.v("ssm.ui.base.url", "https://datastudio-test.example.com");
        String users = SC.v("ssm.api.invoke.user.list", "ssm_simulator_01");
        String[] userNameList = users.split(",");
        for(String userName : userNameList){
            String token = UserTokenManager.create(new User(userName), null);
            for(AnalysisTemplateCacheEntity cacheEntity : cacheEntities){
                String dateGranularity = cacheEntity.getDateGranularity();
                if(BIUtil.isEmpty(dateGranularity)){
                    dateGranularity = DateGranularity.AUTO.getCode();
                }
                String dateGranularityList[] = dateGranularity.split(",");
                for(String dg : dateGranularityList){
                    AnalysisTemplateCacheItem item = new AnalysisTemplateCacheItem();
                    BeanUtil.copyProperties(cacheEntity, item, true);
                    String menuUrl = String.format("%s/ssm/?u_token=%s#/portal/template/%s/browse?dateGranularity=%s", uiBaseUrl, token, cacheEntity.getMenuId(), dg);
                    if(DateGranularity.AUTO == DateGranularity.get(dg)){
                        menuUrl = String.format("%s/ssm/?u_token=%s#/portal/template/%s/browse", uiBaseUrl, token, cacheEntity.getMenuId());
                    }
                    item.setMenuUrl(menuUrl);
                    item.setCacheBy(userName);
                    item.setAnalysisDateGranularity(dg);
                    cacheItems.add(item);
                }
            }
        }

        long t1 = System.currentTimeMillis();
        int successCount = 0;
        for(AnalysisTemplateCacheItem cacheItem : cacheItems){
            AnalysisTemplateCacheLogEntity log = this.cache(cacheItem);
            long consume = DateUtil.between(DateUtil.parseDateTime(log.getCacheBeginTime()), DateUtil.parseDateTime(log.getCacheEndTime()), DateUnit.SECOND, true);
            String info = String.format("%s:[%s]访问[%s/%s]，日期粒度=%s，耗时:%s秒",
                                        DateUtil.now(), cacheItem.getCacheBy(),
                                        cacheItem.getPortalName(), cacheItem.getAnalysisTplName(),
                                        cacheItem.getAnalysisDateGranularity(),
                                        consume);
            info = Enabled.isTrue(log.getIsCacheSuccess()) ? info + " 缓存成功" : info + " 缓存失败，原因：" + log.getCacheInfo();
            if(Enabled.isTrue(log.getIsCacheSuccess())){
                successCount++;
            }
            System.out.println(info);
            logInfos.add(info);

            // 暂停1.5秒
            BIUtil.sleep(1500);
        }
        long t2 = System.currentTimeMillis();

        String totalInfo = String.format("分析模板共%s个，成功%s个，失败%s个，共耗时%s秒", cacheItems.size(), successCount, cacheItems.size() - successCount, (t2-t1)/1000.0);
        logInfos.add(totalInfo);
        return logInfos;
    }

    protected AnalysisTemplateCacheLogEntity cache(AnalysisTemplateCacheItem cacheItem){
        AnalysisTemplateCacheLogEntity log = new AnalysisTemplateCacheLogEntity();
        try {
            log.setCacheBeginTime(DateUtil.now());
            String uiSimulatorUrl = SC.v("ssm.ui.simulator.url", "http://127.0.0.1:9018/api/ssm-portal/loadCache");
            // 请求头
            Map<String, String> headers = new HashMap<>();

            // 请求参数
            Map<String, Object> requestParameters = new HashMap<>();
            requestParameters.put("url", cacheItem.getMenuUrl());

            String response = HttpUtil.doPost(uiSimulatorUrl, requestParameters,"application/json", headers, 3 * 60 * 1000);
            JSONObject responseJson = JSONObject.parseObject(response);
            if("true".equalsIgnoreCase(responseJson.get("success") + "")){
                log.setIsCacheSuccess(Enabled.YES.getId());
            }else {
                log.setIsCacheSuccess(Enabled.NO.getId());
            }
            log.setCacheInfo(responseJson.get("message") + "");
        }catch (Exception e){
            log.setIsCacheSuccess(Enabled.NO.getId());
            log.setCacheInfo(e.getMessage());
        }finally {
            log.setPortalId(cacheItem.getPortalId());
            log.setPortalName(cacheItem.getPortalName());
            log.setMenuId(cacheItem.getMenuId());
            log.setMenuName(cacheItem.getMenuName());
            log.setAnalysisTplId(cacheItem.getAnalysisTplId());
            log.setAnalysisTplName(cacheItem.getAnalysisTplName());
            log.setAnalysisDateGranularity(cacheItem.getAnalysisDateGranularity());
            log.setMenuUrl(cacheItem.getMenuUrl());
            log.setCacheEndTime(DateUtil.now());
            log.setCacheBy(cacheItem.getCacheBy());
        }

        // 写日志表
        dao.insert("ssm.analysis.template.cache.addLog", log);
        return log;
    }
}
