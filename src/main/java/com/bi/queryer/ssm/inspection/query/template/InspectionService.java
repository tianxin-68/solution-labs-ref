package com.bi.queryer.ssm.inspection.query.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.QueryModeType;
import com.bi.queryer.ssm.enums.TaskExecStatus;
import com.bi.queryer.ssm.inspection.query.template.entity.InspectionCfgEntity;
import com.bi.queryer.ssm.inspection.query.template.entity.InspectionLogEntity;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.UserTokenManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.network.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class InspectionService {

    @Autowired
    private BaseDao dao;

    public List<String> execute(String viewIds, String queryMode, String inspectDsKey) {

        List<String> logInfos = new ArrayList<>();

        List<InspectionCfgEntity> inspectionCfgEntities = new ArrayList<>();

        if(StrUtil.isEmpty(queryMode)){
            queryMode = QueryModeType.COLUMNS.getCode();
        }

        //从巡检模版配置表查询
        inspectionCfgEntities = (List<InspectionCfgEntity>) dao.queryObjectList("ssm.inspection.queryInspectionCfgList", queryMode);

        if(CollUtil.isEmpty(inspectionCfgEntities)){
            logInfos.add("无巡检内容");
            return logInfos;
        }

        //过滤指定的巡检主体id
        if(StrUtil.isNotEmpty(viewIds)){
            inspectionCfgEntities = inspectionCfgEntities
                    .stream()
                    .filter(cfg->viewIds.contains(cfg.getViewId()))
                    .collect(Collectors.toList());
        }

        if(CollUtil.isEmpty(inspectionCfgEntities)){
            logInfos.add("无巡检内容");
            return logInfos;
        }

        long t1 = System.currentTimeMillis();
        int successCount = 0;

        //获取模版对应的视图
        List<String> viewIdList = inspectionCfgEntities.stream()
                .map(InspectionCfgEntity::getViewId)
                .collect(Collectors.toList());

        //获取视图信息
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("viewIdList",viewIdList);
        List<TemplateViewEntity> viewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.batchGetByViewId", paramMap);

        Map<String, TemplateViewEntity> viewMap = new HashMap<>();
        for(TemplateViewEntity view : viewList){
            viewMap.put(view.getViewId(),view);
        }

        int totalCount = 0;
        Long batchNo = System.currentTimeMillis();
        for(InspectionCfgEntity inspectionCfg : inspectionCfgEntities) {

            TemplateViewEntity inspectionView = viewMap.get(inspectionCfg.getViewId());
            if (inspectionView == null) {
                continue;
            }

            inspectionCfg.setViewId(inspectionView.getViewId());
            inspectionCfg.setViewName(inspectionView.getViewName());

            InspectionLogEntity logEntry = inspect(inspectionCfg, batchNo, inspectDsKey);

            long consume = DateUtil.between(DateUtil.parseDateTime(logEntry.getBeginTime()), DateUtil.parseDateTime(logEntry.getEndTime()), DateUnit.SECOND, true);
            String info = String.format("%s:[%s]访问[%s-%s]视图[%s-%s],负责人%s，耗时:%s秒",
                    DateUtil.now(), logEntry.getCreatedBy(),
                    logEntry.getInspectionSubjectName(),
                    logEntry.getInspectionSubjectId(),
                    inspectionView.getViewName(),
                    inspectionView.getViewId(),
                    logEntry.getInspectionSubjectOwner(),
                    consume);

            TaskExecStatus execStatus = TaskExecStatus.get(logEntry.getExecResult());
            info = TaskExecStatus.SUCCESS == execStatus ? info + " 巡检成功" : info + " 巡检失败，原因：" + logEntry.getRemark();
            if (TaskExecStatus.SUCCESS == execStatus) {
                successCount++;
            }
            logInfos.add(info);

            totalCount++;
            // 暂停1秒
            BIUtil.sleep(1000);

        }

        long t2 = System.currentTimeMillis();

        String totalInfo = String.format("巡检内容共%s个，成功%s个，失败%s个，共耗时%s秒", totalCount, successCount, totalCount - successCount, (t2-t1)/1000.0);
        logInfos.add(totalInfo);

        return logInfos;
    }

    protected InspectionLogEntity inspect(InspectionCfgEntity inspectionCfg, Long batchNo, String inspectDsKey) {
        InspectionLogEntity log = new InspectionLogEntity();

        String uiBaseUrl = SC.v("ssm.ui.base.url", "https://datastudio-test.example.com");
        String users = SC.v("ssm.api.invoke.user.list", "ssm_simulator_01");
        String[] userNameList = users.split(",");

        try {
            log.setBeginTime(DateUtil.now());
            String uiSimulatorUrl = SC.v("ssm.ui.simulator.url", "http://127.0.0.1:9018/api/ssm-portal/loadCache");
            // 请求头
            Map<String, String> headers = new HashMap<>();
            String token = UserTokenManager.create(new User(userNameList[0]), null);

            String url = String.format("%s/ssm?u_token=%s#/template/%s?viewId=%s&isAutoSearch=1&isInspect=1",
                    uiBaseUrl,
                    token,
                    inspectionCfg.getInspectionSubjectId(),
                    inspectionCfg.getViewId()
            );

            QueryModeType queryModeType = QueryModeType.get(inspectionCfg.getInspectionQueryMode());
            if(QueryModeType.COLUMNS == queryModeType){
                url += "&queryModeType=columns";
            }

            url += "&inspectDsKey=" + inspectDsKey;

            // 请求参数
            Map<String, Object> requestParameters = new HashMap<>();
            requestParameters.put("url", url);

            String response = HttpUtil.doPost(uiSimulatorUrl, requestParameters,"application/json", headers, 3 * 60 * 1000);
            JSONObject responseJson = JSONObject.parseObject(response);
            if("true".equalsIgnoreCase(responseJson.get("success") + "")){
                log.setExecResult(TaskExecStatus.SUCCESS.getCode());
            }else {
                log.setExecResult(TaskExecStatus.FAIL.getCode());
            }
            log.setRemark(responseJson.get("message") + "");
        }catch (Exception e){
            log.setExecResult(TaskExecStatus.FAIL.getCode());
            log.setRemark(e.getMessage());
        }finally {
            log.setInspectionSubjectId(inspectionCfg.getInspectionSubjectId());
            log.setInspectionSubjectName(inspectionCfg.getInspectionSubjectName());
            log.setInspectionSubjectType(inspectionCfg.getInspectionSubjectType());
            log.setInspectionSubjectOwner(inspectionCfg.getInspectionSubjectOwner());
            log.setViewId(inspectionCfg.getViewId());
            log.setViewName(inspectionCfg.getViewName());
            log.setEndTime(DateUtil.now());
            log.setCreatedBy(userNameList[0]);
            log.setBatchNo(batchNo);
            log.setDsKey(inspectDsKey);
        }

        // 写日志表
        dao.insert("ssm.inspection.log.add", log);
        return log;
    }

    /**
     * 添加当日查询的视图，纳入巡检
     */
    public void insertCurrentQueryView(String viewId) {

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        try {

            executorService.execute(() -> {
                if(StrUtil.isEmpty(viewId)){
                    return;
                }

                //判断在巡检中视图是否已经存在
                Map<String,String> param = new HashMap<>();
                param.put("viewId", viewId);
                Integer cnt = (Integer) dao.queryObject("ssm.inspection.queryCountByViewId", param);

                if(cnt > 0){
                    return;
                }

                TemplateViewEntity templateView = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);
                if(templateView == null){
                    return;
                }

                Map<String, String> queryMap = new HashMap<>();
                queryMap.put("templateId", templateView.getTplId());
                SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);
                if(tpl ==  null){
                    return;
                }

                //不存在则插入巡检表
                InspectionCfgEntity inspectionCfg = new InspectionCfgEntity();
                inspectionCfg.setInspectionSubjectId(tpl.getId());
                inspectionCfg.setInspectionSubjectName(tpl.getName());
                inspectionCfg.setInspectionSubjectType("query_template");
                inspectionCfg.setInspectionSubjectOwner(tpl.getTplOwner());
                inspectionCfg.setInspectionQueryMode("all");
                inspectionCfg.setViewId(viewId);
                inspectionCfg.setViewName(templateView.getViewName());
                dao.insert("ssm.inspection.insert", inspectionCfg);

            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

    }

}
