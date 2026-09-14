package com.bi.queryer.ssm.query.template.view;

import com.bi.queryer.ssm.query.template.model.TemplateMetricRsp;
import com.bi.queryer.ssm.query.template.model.TemplateViewRsp;
import com.bi.queryer.ssm.query.template.view.model.*;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 模板视图接口
 */
@Controller
@Scope("prototype")
@RequestMapping("ssm/template/view")
public class TemplateViewController {

    @Autowired
    private TemplateViewService templateViewService;

    /**
     * 根据模板ID获取模板视图
     * @param tplId 模版id
     * @return
     */
    @RequestMapping(value = "getByTplId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateViewEntity>> getByTplId(String tplId){
        return SSMResponseMessage.success("",templateViewService.getAllViewByTplId(tplId));
    }

    /**
     * 按视图 id 串批量获取视图基础信息（不含模板配置内容）。
     * 入参 {@link TemplateViewIdsQueryReq#getViewIds()} 中多个 id 以空格、英文逗号或英文分号分隔。
     */
    @RequestMapping(value = "listBasicByViewIds", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<TemplateViewEntity>> listBasicByViewIds(@RequestBody TemplateViewIdsQueryReq req) {
        String viewIds = req == null ? null : req.getViewIds();
        return SSMResponseMessage.success("查询成功", templateViewService.listBasicByViewIdsString(viewIds));
    }

    /**
     * 重命名模板视图
     * @param templateViewReq
     * @return
     */
    @RequestMapping(value = "rename", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage rename(@RequestBody TemplateViewReq templateViewReq) {
        templateViewService.rename(templateViewReq);
        return SSMResponseMessage.success("模板视图重命名成功");
    }

    /**
     * 设置模板视图为默认
     * @param templateViewReq
     * @return
     */
    @RequestMapping(value = "setDefault", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage setDefault(@RequestBody TemplateViewReq templateViewReq){
        templateViewService.setDefault(templateViewReq);
        return SSMResponseMessage.success("模板视图设为默认成功");
    }

    /**
     * 添加模板视图
     * @param req
     * @return
     */
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> add(@RequestBody TemplateViewSaveReq req){
        return SSMResponseMessage.success("添加成功",templateViewService.add(req));
    }

    /**
     * 另存为原模板视图
     * 通过 viewId 找到对应模板，再通过 ssd_query_template_share_rel 找到 root_tpl_id，
     * 在原始模板上新增一个个人视图
     * @param req 视图保存请求，viewId 为来源视图
     * @return 新视图ID
     */
    @RequestMapping(value = "saveAsToRootTpl", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<TemplateViewRsp> saveAsToRootTpl(@RequestBody TemplateViewSaveReq req){
        return SSMResponseMessage.success("另存为成功", templateViewService.saveAsToRootTpl(req));
    }

    /**
     * 修改模板视图
     * @param req
     * @return
     */
    @RequestMapping(value = "update", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<String> update(@RequestBody TemplateViewSaveReq req){
        return SSMResponseMessage.success("修改成功",templateViewService.update(req));
    }

    /**
     * 视图移动，传递被移动的菜单id和排在其后面第一位的菜单id
     * @param viewId
     * @param targetViewId
     * @param dragType : before  after 放在目标节点的位置
     * @return
     */
    @RequestMapping(value = "move", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage move(String viewId,String targetViewId,String dragType) {
        templateViewService.move(viewId, targetViewId, dragType);
        return SSMResponseMessage.success("模板视图移动成功");
    }

    /**
     * 删除模板视图
     * @param viewId
     * @return
     */
    @RequestMapping(value = "delete", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage delete(String viewId) {
        templateViewService.delete(viewId);
        return SSMResponseMessage.success("模板视图删除成功");
    }

    /**
     * 修改视图类型
     * @param viewId
     * @param viewType
     * @return
     */
    @RequestMapping(value = "changeViewType", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage changeViewType(String viewId,String viewType){
        templateViewService.changeViewType(viewId,viewType);
        return SSMResponseMessage.success("修改视图类型成功");
    }

    /**
     * 拷贝视图
     * @param viewId
     * @return
     */
    @RequestMapping(value = "copyViewById", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> copyViewById(String viewId){
        String newViewId = templateViewService.copyViewById(viewId);
        return SSMResponseMessage.success("拷贝视图成功",newViewId);
    }

    /**
     * 保存模板视图扩展配置
     * 用于历史模版初始化
     * @param req
     * @return
     */
    @RequestMapping(value = "saveTemplateCfgDtl", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage saveTemplateCfgDtl(@RequestBody TemplateCfgDtlSaveReq req){
        templateViewService.saveTemplateCfgDtl(req);
        return SSMResponseMessage.success("保存成功");
    }

    /**
     * 获取模板视图的指标列表
     * @return
     */
    @RequestMapping(value = "getTemplateMetricList", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<List<TemplateMetricRsp>> getTemplateMetricList(@RequestBody TemplateViewReq req) {
        return SSMResponseMessage.success("查询成功", templateViewService.getTemplateMetricList(req));
    }

    /**
     * 保存模版视图的etl作业信息
     * @param viewId
     * @return
     */
    @RequestMapping(value = "saveEtlJob", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage saveEtlJob(String viewId) {
        List<String> viewIdList = Arrays.asList(StringUtils.split(viewId, ","));
        List<TemplateViewEtlJobResp> respList = templateViewService.getEtlJob(viewId);
        templateViewService.saveEtlJob(viewIdList, respList);
        return SSMResponseMessage.success("保存模版视图的etl作业成功");
    }

    /**
     * 获取模版视图的etl作业信息
     * @return
     */
    @RequestMapping(value = "getEtlJobByViewId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateViewEtlJobResp>> getEtlJobByViewId(String viewId) {
        return SSMResponseMessage.success("获取模版视图的etl作业信息成功", templateViewService.getEtlJobByViewId(viewId));
    }

    /**
     * 获取模版名称和视图名称
     * @param viewId
     * @return
     */
    @RequestMapping(value = "getTemplateNameAndViewName", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<TemplateViewRsp> getTemplateNameAndViewName(String viewId){
        return SSMResponseMessage.success("获取模版名称和视图名称成功",templateViewService.getTemplateNameAndViewName(viewId));
    }

    /**
     * 获取视图数据更新时间
     * 目前支持准实时数据集，其他数据集返回空
     * @param viewId
     * @return
     */
    @RequestMapping(value = "getViewDataUpdateTimeByViewId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> getViewDataUpdateTimeByViewId(String viewId) {
        return SSMResponseMessage.success("获取视图数据更新时间成功",templateViewService.getViewDataUpdateTimeByViewId(viewId));
    }

    /**
     * 获取准实时数据更新时间（与 {@link #getViewDataUpdateTimeByViewId} 同一 Doris 批次口径）。
     * <ul>
     *   <li>tplType=query_template：tplId 为查询模板 id，若模板数据集为准实时则返回时间，否则空。</li>
     *   <li>tplType=analysis_template / tmp_analysis_template：tplId 为看板 id，若线上配置中任一关联查询模板使用准实时数据集则返回时间，否则空。</li>
     * </ul>
     */
    @RequestMapping(value = "getRtDataUpdateTime", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> getRtDataUpdateTime(@RequestParam("tplId") String tplId, @RequestParam("tplType") String tplType) {
        return SSMResponseMessage.success("获取准实时数据更新时间成功", templateViewService.getRtDataUpdateTime(tplId, tplType));
    }

    /**
     * 视图配置画像：基本信息、结果区字段结构（维度/指标及聚合释义）、默认参数。
     * 配置经 {@link com.bi.queryer.ssm.util.SSDUtil#normalizeConfig(String, java.util.Map, java.util.Map)} 归一化后解析。
     *
     * @param viewId 视图 id
     */
    @RequestMapping(value = "getConfigPortrait", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<TemplateViewConfigPortraitRsp> getConfigPortrait(String viewId) {
        return SSMResponseMessage.success("查询成功", templateViewService.getViewConfigPortrait(viewId));
    }

    /**
     * 视图配置画像：默认参数。
     * @param viewId 视图 id
     */
    @RequestMapping(value = "getConfigPortraitDefaults", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<TemplateViewConfigPortraitRsp> getConfigPortraitDefaults(String viewId) {
        return SSMResponseMessage.success("查询成功", templateViewService.getConfigPortraitDefaults(viewId));
    }

    /**
     * 按视图 id 获取准实时数据更新时间。
     * 仅当视图绑定的查询模板使用准实时数据集时返回时间，否则返回空字符串。
     *
     * @param viewId 视图 id
     */
    @RequestMapping(value = "getRuntimeDataUpdateTime", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> getRuntimeDataUpdateTime(@RequestParam String dataType) {
        return SSMResponseMessage.success("获取准实时数据更新时间成功", templateViewService.getRuntimeDataUpdateTime(dataType));
    }
}
