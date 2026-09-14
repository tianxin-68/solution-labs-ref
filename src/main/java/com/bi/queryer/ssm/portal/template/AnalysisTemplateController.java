package com.bi.queryer.ssm.portal.template;

import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateExecMode;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateLockOpType;
import com.bi.queryer.ssm.portal.template.vo.*;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2024/6/17 15:16
 * @Description:
 */

@RestController
@Scope("prototype")
@RequestMapping("ssm/analysisTpl")
public class AnalysisTemplateController {
    private final static Logger LOG = LoggerFactory.getLogger(AnalysisTemplateController.class);

    @Autowired
    private AnalysisTemplateService service;

    /**
     * 创建看板
     * @param vo
     * @return
     */
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public ResponseMessage create(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.create(vo));
        } catch (Exception e) {
            LOG.error("create analysis template error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "check/editable", method = RequestMethod.POST)
    public ResponseMessage checkEditable(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.checkEditable(vo));
        } catch (Exception e) {
            LOG.error("check analysis template edit error, param: {}", vo.getAnalysisTplId(), e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "check/hasDraft", method = RequestMethod.POST)
    public ResponseMessage hasDraft(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.hasDraft(vo));
        } catch (Exception e) {
            LOG.error("check analysis template has draft error, vo: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "update", method = RequestMethod.POST)
    public ResponseMessage update(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.update(vo));
        } catch (Exception e) {
            LOG.error("update analysis template error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "updateName", method = RequestMethod.POST)
    public ResponseMessage updateName(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.updateName(vo));
        } catch (Exception e) {
            LOG.error("update analysis template name error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "publish", method = RequestMethod.POST)
    public ResponseMessage publish(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.publish(vo));
        } catch (Exception e) {
            LOG.error("publish analysis template error, analysisTplId: {}", vo.getAnalysisTplId(), e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 保存并发布看板
     * @param vo
     * @return
     */
    @RequestMapping(value = "saveAndPublish", method = RequestMethod.POST)
    public ResponseMessage saveAndPublish(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.saveAndPublish(vo));
        } catch (Exception e) {
            LOG.error("saveAndPublish analysis template error, analysisTplId: {}", vo.getAnalysisTplId(), e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "offline", method = RequestMethod.POST)
    public ResponseMessage offline(@RequestBody AnalysisTemplateVO vo) {
        try {
            return ResponseMessage.success(service.offline(vo));
        } catch (Exception e) {
            LOG.error("offline analysis template error, vo: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "get", method = RequestMethod.GET)
    public ResponseMessage get(@RequestParam(value = "analysisTplId") String analysisTplId,
                               @RequestParam(value = "portalId", required = false) String portalId,
                               @RequestParam(value = "mode", required = false) String mode,
                               @RequestParam(value = "useDraft", required = false) Integer useDraft,
                               @RequestParam(value = "viewId", required = false) String viewId) {
        try {
            AnalysisTemplateExecMode getMode = AnalysisTemplateExecMode.codeOf(mode);
            return ResponseMessage.success(service.getAnalysisTemplateConfig(analysisTplId, portalId, getMode, useDraft, viewId));
        } catch (Exception e) {
            LOG.error("get analysis template error, analysisTplId: {}, mode: {}, useDraft: {}"
                    , analysisTplId, mode, useDraft, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 通过viewId获取看板信息
     * @param viewId
     * @return
     */
    @RequestMapping(value = "getByViewId", method = RequestMethod.GET)
    public ResponseMessage getByViewId(String viewId) {
        return ResponseMessage.success(service.getByViewId(viewId));
    }


    @RequestMapping(value = "getModifiedQueryTpl", method = RequestMethod.POST)
    public ResponseMessage getModifiedQueryTpl(@RequestBody AnalysisTemplateQueryTplModifiedReq req) {
        try {
            return ResponseMessage.success(service.getModifiedQueryTpl(req));
        } catch (Exception e) {
            LOG.error("get modified query template error, vo: {}", req, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 获取看板的数据权限
     * @param analysisTplId
     * @return
     */
    @RequestMapping(value = "getDataAuth", method = RequestMethod.GET)
    public ResponseMessage getDataAuth(@RequestParam(value = "analysisTplId") String analysisTplId) {
        try {
            return ResponseMessage.success(service.getDataAuth(analysisTplId));
        } catch (Exception e) {
            LOG.error("get analysis template data auth error, analysisTplId: {}", analysisTplId, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "edit/lock/{type}", method = RequestMethod.POST)
    public ResponseMessage doWithEditLock(@PathVariable("type") String typeCode, @RequestBody AnalysisTemplateVO vo) {
        try {
            AnalysisTemplateLockOpType lockOpType = AnalysisTemplateLockOpType.codeOf(typeCode);
            return ResponseMessage.success(service.doWithEditLock(lockOpType, vo));
        } catch (Exception e) {
            LOG.error("deal with analysis template edit lock error, type: {}, vo: {}", typeCode, vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "edit/exit", method = RequestMethod.POST)
    public ResponseMessage exitEdit(@RequestBody AnalysisTemplateEditExitVO exitVO) {
        try {
            return ResponseMessage.success(service.exitEdit(exitVO));
        } catch (Exception e) {
            LOG.error("exit analysis template edit status error, vo: {}", exitVO, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "listAnalysisTplByQueryTplId", method = RequestMethod.GET)
    public ResponseMessage listAnalysisTplByQueryTplId(@RequestParam(value = "queryTplId") String queryTplId) {
        try {
            return ResponseMessage.success(service.getAnalysisTplByQueryTplId(queryTplId));
        } catch (Exception e) {
            LOG.error("list analysis template by query tpl id error, queryTplId: {}", queryTplId, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    @RequestMapping(value = "listWidgetTypes", method = RequestMethod.GET)
    public ResponseMessage listWidgetTypes() {
        try {
            return ResponseMessage.success(service.listWidgetTypes());
        } catch (Exception e) {
            LOG.error("list widget types error", e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 查询看板列表
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.POST)
    public SSMResponseMessage<AnalysisTemplateListRsp> list(@RequestBody AnalysisTemplateListQueryReq analysisTemplateListQueryReq){
        return SSMResponseMessage.success("查询成功",service.list(analysisTemplateListQueryReq));
    }

    /**
     * 通过门户id查询看板列表
     * 限定已上线的门户看板
     * @param portalId
     * @return
     */
    @RequestMapping(value = "getProdAnalysisTplByPortalId", method = RequestMethod.GET)
    public SSMResponseMessage<List<AnalysisTemplateVO>> getProdAnalysisTplByPortalId(String portalId){
        return SSMResponseMessage.success("查询成功",service.getProdAnalysisTplByPortalId(portalId));
    }

    /**
     * 保存分析看板的etl作业信息
     * @return
     */
    @RequestMapping(value = "saveEtlJob", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage saveEtlJob(String analysisTplId) {
        service.saveEtlJob(analysisTplId, service.resolveMetaTables(analysisTplId));
        return SSMResponseMessage.success("保存分析看板的etl作业成功");
    }

    /**
     * 获取分析看板的etl作业信息
     * @return
     */
    @RequestMapping(value = "getEtlJobByAnalysisTplId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<AnalysisTemplateEtlJobResp>> getEtlJobByAnalysisTplId(String analysisTplId){
        return SSMResponseMessage.success("获取分析看板的etl作业成功",service.getEtlJobByAnalysisTplId(analysisTplId));
    }

    /**
     * 通过分析看板id查询已上线的分析看板的信息
     * @return
     */
    @RequestMapping(value = "getProdAnalysisTplByAnalysisTplId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<AnalysisTemplateVO> getProdAnalysisTplByAnalysisTplId(String analysisTplId){
        return SSMResponseMessage.success("查询成功",service.getProdAnalysisTplByAnalysisTplId(analysisTplId));
    }

}
