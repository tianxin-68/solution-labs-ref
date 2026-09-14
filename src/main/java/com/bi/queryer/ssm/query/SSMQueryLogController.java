package com.bi.queryer.ssm.query;

import com.bi.queryer.ssm.query.log.SSDQueryLogEntity;
import com.bi.queryer.ssm.query.log.SSDQueryLogService;
import com.bi.queryer.ssm.query.log.SSDQueryLogUIEntity;
import com.bi.queryer.ssm.query.log.SSMQueryTplVisitEntity;
import com.bi.queryer.ssm.query.log.cfg.SSMQueryCfgLogService;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Auther: contributor
 * @Date: 2024/9/3 14:21
 * @Description:
 */

@RestController
@Scope("prototype")
@RequestMapping("ssm")
public class SSMQueryLogController {
    private final static Logger LOG = LoggerFactory.getLogger(SSMQueryLogController.class);
    @Autowired
    private SSMQueryCfgLogService queryCfgLogService;

    @Autowired
    private SSDQueryLogService queryLogService;

    @RequestMapping(value = "fe/log", method = {RequestMethod.POST})
    public ResponseMessage logQueryConfig(@RequestBody Map<String, Object> vo) {
        if (vo == null) {
            return ResponseMessage.fail("参数为空");
        }

        String sessionId = String.valueOf(vo.remove("sessionId"));
        if (BIUtil.isEmpty(sessionId)) {
            return ResponseMessage.fail("sessionId参数为空");
        }

        Object templateIdObj = vo.remove("templateId");

        if (BIUtil.isEmpty(vo)) {
            return ResponseMessage.fail("埋点内容为空");
        }
        String templateId = templateIdObj == null ? null : templateIdObj.toString();

        try {
            queryCfgLogService.logQueryCfgEvent(sessionId, templateId, vo);
            return ResponseMessage.success("success");
        } catch (Exception e) {
            LOG.error("埋点日志写入失败", e);
            return ResponseMessage.fail("埋点日志写入失败" + e.getMessage());
        }
    }



    @RequestMapping(value = "query/log", method = {RequestMethod.POST})
    public ResponseMessage logQuery(@RequestBody SSDQueryLogEntity entity) {
        if (entity == null) {
            return ResponseMessage.fail("参数为空");
        }

        String logId = entity.getId();
        if (BIUtil.isEmpty(logId)) {
            return ResponseMessage.fail("logId参数为空");
        }

        try {
            queryLogService.logExtend(entity);
            return ResponseMessage.success("success");
        } catch (Exception e) {
            LOG.error("UI埋点日志写入失败", e);
            return ResponseMessage.fail("UI埋点日志写入失败" + e.getMessage());
        }
    }

    /**
     * 保存UI查询埋点日志
     * @param entity
     * @return
     */
    @RequestMapping(value = "query/log/ui", method = {RequestMethod.POST})
    private ResponseMessage saveQueryLogUi(@RequestBody SSDQueryLogUIEntity entity){
        try {
            queryLogService.saveQueryLogUi(entity);
            return ResponseMessage.success("success");
        } catch (Exception e) {
            LOG.error("UI查询埋点日志写入失败", e);
            return ResponseMessage.fail("UI查询埋点日志写入失败" + e.getMessage());
        }
    }

    /**
     * 保存模板看板的访问日志
     * @param entity
     * @return
     */
    @RequestMapping(value = "tpl/visit/log", method = {RequestMethod.POST})
    private ResponseMessage logTpl(@RequestBody SSMQueryTplVisitEntity entity) {
        try {
            queryLogService.addTplVisitLog(entity);
            return ResponseMessage.success("success");
        } catch (Exception e) {
            LOG.error("保存模板看板的性能日志失败", e);
            return ResponseMessage.fail("保存模板看板的性能日志失败" + e.getMessage());
        }
    }


}
