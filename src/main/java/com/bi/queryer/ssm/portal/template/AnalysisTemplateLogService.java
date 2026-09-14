package com.bi.queryer.ssm.portal.template;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.portal.enums.AnalysisTplVisitType;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateVisitLogEntity;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVisitLogReq;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  16:34
 * @Description: 看板日志服务类
 */
public class AnalysisTemplateLogService {

    private static final Integer MAX_THREAD_NUM = 2;

    private static ExecutorService singlePool = Executors.newFixedThreadPool(MAX_THREAD_NUM);

    public static void addVisitLog(AnalysisTemplateVisitLogReq analysisTemplateVisitLogReq) {

        User user = UserManager.get();

        singlePool.execute(new Runnable() {
            @Override
            public void run() {
                String visitBeginTime = analysisTemplateVisitLogReq.getVisitBeginTime();
                String visitEndTime = analysisTemplateVisitLogReq.getVisitEndTime();

                //开始访问时间为空，默认当前时间
                if(StrUtil.isEmpty(visitBeginTime)){
                    visitBeginTime = DateUtil.now();
                }

                String visitType = analysisTemplateVisitLogReq.getVisitType();
                if (StrUtil.isBlank(visitType)) {
                    visitType = AnalysisTplVisitType.ANALYSIS_TEMPLATE.getCode();
                }
                AnalysisTemplateVisitLogEntity analysisTemplateVisitLogEntity = AnalysisTemplateVisitLogEntity.builder()
                        .analysisTplId(analysisTemplateVisitLogReq.getAnalysisTplId())
                        .portalId(analysisTemplateVisitLogReq.getPortalId())
                        .visitType(visitType)
                        .resourceVersion(analysisTemplateVisitLogReq.getResourceVersion())
                        .userName(user.getName())
                        .visitBeginTime(visitBeginTime)
                        .visitEndTime(visitEndTime)
                        .build();

                BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");
                dao.insert("ssm.analysis.template.visit.log.addVisitLog",analysisTemplateVisitLogEntity);
            }
        });


    }

}
