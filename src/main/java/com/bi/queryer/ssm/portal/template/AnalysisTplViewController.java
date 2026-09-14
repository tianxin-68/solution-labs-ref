package com.bi.queryer.ssm.portal.template;

import com.bi.queryer.ssm.portal.template.vo.AnalysisTplViewSaveVO;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTplViewSnapshotRsp;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTplViewVO;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 看板视图配置表 ssm_analysis_tpl_view 增删改查
 */
@RestController
@Scope("prototype")
@RequestMapping("ssm/analysisTpl/view")
public class AnalysisTplViewController {

    private static final Logger LOG = LoggerFactory.getLogger(AnalysisTplViewController.class);

    @Autowired
    private AnalysisTplViewService analysisTplViewService;

    @Autowired
    private AnalysisTplViewUserCfgService analysisTplViewUserCfgService;

    /**
     * 新增视图
     */
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public SSMResponseMessage<String> create(@RequestBody AnalysisTplViewSaveVO vo) {
        try {
            return SSMResponseMessage.success("",analysisTplViewService.create(vo));
        } catch (Exception e) {
            LOG.error("create analysis tpl view error, param: {}", vo, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 更新视图
     */
    @RequestMapping(value = "update", method = RequestMethod.POST)
    public ResponseMessage update(@RequestBody AnalysisTplViewSaveVO vo) {
        try {
            return ResponseMessage.success(analysisTplViewService.update(vo));
        } catch (Exception e) {
            LOG.error("update analysis tpl view error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 视图快照：新建临时看板（ssm_analysis_tpl_base）、写入线上配置（ssm_analysis_tpl_cfg_prod），并调用 create 生成视图
     *
     * @param vo viewId 必填；viewName 可选，作为新看板名称，默认「源视图名_快照」
     */
    @RequestMapping(value = "snapshot", method = RequestMethod.POST)
    public SSMResponseMessage<AnalysisTplViewSnapshotRsp> snapshot(@RequestBody AnalysisTplViewSaveVO vo) {
        try {
            return SSMResponseMessage.success("", analysisTplViewService.snapshotView(vo));
        } catch (Exception e) {
            LOG.error("snapshot analysis tpl view error, param: {}", vo, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 复制视图：从指定 view_id 生成新视图
     */
    @RequestMapping(value = "copy", method = RequestMethod.POST)
    public SSMResponseMessage<String> copy(@RequestBody AnalysisTplViewVO vo) {
        try {
            return SSMResponseMessage.success("", analysisTplViewService.copy(vo));
        } catch (Exception e) {
            LOG.error("copy analysis tpl view error, param: {}", vo, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 视图移动：被移动视图 id、目标视图 id、拖拽落点（before / after，含义同 TemplateViewController#move）
     */
    @RequestMapping(value = "move", method = RequestMethod.GET)
    public SSMResponseMessage move(String viewId, String targetViewId, String dragType) {
        try {
            analysisTplViewService.move(viewId, targetViewId, dragType);
            return SSMResponseMessage.success("看板视图移动成功");
        } catch (Exception e) {
            LOG.error("move analysis tpl view error, viewId: {}, targetViewId: {}, dragType: {}", viewId, targetViewId, dragType, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 重命名视图
     */
    @RequestMapping(value = "rename", method = RequestMethod.POST)
    public SSMResponseMessage rename(@RequestBody AnalysisTplViewVO vo) {
        try {
            analysisTplViewService.rename(vo);
            return SSMResponseMessage.success("视图重命名成功", "");
        } catch (Exception e) {
            LOG.error("rename analysis tpl view error, param: {}", vo, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 删除视图
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST)
    public ResponseMessage delete(@RequestBody AnalysisTplViewVO vo) {
        try {
            return ResponseMessage.success(analysisTplViewService.delete(vo));
        } catch (Exception e) {
            LOG.error("delete analysis tpl view error, param: {}", vo, e);
            return ResponseMessage.fail(e.getMessage());
        }
    }

    /**
     * 根据看板模板ID获取视图列表（对齐 ssm/template/view getByTplId）
     *
     * @param analysisTplId 看板模板id
     */
    @RequestMapping(value = "getByAnalysisTplId", method = RequestMethod.GET)
    public SSMResponseMessage<List<AnalysisTplViewVO>> getByAnalysisTplId(String analysisTplId) {
        return SSMResponseMessage.success("", analysisTplViewService.getByAnalysisTplId(analysisTplId));
    }

    /**
     * 将看板视图设为当前用户默认（写入 ssm_analysis_tpl_view_user_cfg，同一看板下仅一个默认视图）
     *
     * @param vo analysisTplId、viewId 必填
     */
    @RequestMapping(value = "setDefault", method = RequestMethod.POST)
    public SSMResponseMessage<String> setDefaultView(@RequestBody AnalysisTplViewVO vo) {
        try {
            analysisTplViewUserCfgService.setAsDefaultView(vo.getAnalysisTplId(), vo.getViewId());
            return SSMResponseMessage.success("设置默认视图成功", "");
        } catch (Exception e) {
            LOG.error("set default analysis tpl view error, param: {}", vo, e);
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

}
