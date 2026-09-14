package com.bi.queryer.ssm.query.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.template.enums.FavTemplateType;
import com.bi.queryer.ssm.query.template.enums.QueryTemplateType;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模版相关接口
 */
@Controller
@Scope("prototype")
@RequestMapping("ssd/template")
public class TemplateController {

    @Autowired
    protected TemplateConfigService configService = null;

    @Autowired
    protected TemplateShareService shareService = null;

    @Autowired
    protected TemplateLinkService linkService = null;

    /**
     * 通过id获取模板详情
     * @return
     */
    @RequestMapping("get")
    @ResponseBody
    public SSMResponseMessage<JSONObject> getTemplate(@RequestBody TemplateOperateReq req) {
        String templateId = req.getTplId();
        if (BIUtil.isEmpty(templateId)) {
            throw new SSDException("缺少参数:tplId");
        }
        SSDQueryTemplate tpl = configService.getTemplateById(templateId,req.getViewId());
        if (tpl == null) {
            throw new SSDException("查询模板不存在：" + templateId, "_custom_check_error_");
        }
        JSONObject data = BIUtil.toJSONObject(tpl);
        data.put("config", JSONObject.parseObject(tpl.getConfig()));
        return SSMResponseMessage.success("模版查询成功", data);
    }

    /**
     * 通过视图id获取模板详情
     * @return
     */
    @RequestMapping("getTemplateByViewId")
    @ResponseBody
    public SSMResponseMessage<JSONObject> getTemplateByViewId(String viewId){

        SSDQueryTemplate tpl = configService.getTemplateByViewId(viewId);
        if (tpl == null) {
            throw new SSDException("查询视图不存在：" + viewId, "_custom_check_error_");
        }
        JSONObject data = BIUtil.toJSONObject(tpl);
        data.put("config", JSONObject.parseObject(tpl.getConfig()));
        return SSMResponseMessage.success("模版查询成功", data);
    }

    /**
     * 通过视图id获取模板id
     * @return
     */
    @RequestMapping(value = "getTplIdByViewId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<String> getTplIdByViewId(String viewId){
        String tplId = configService.getTplIdByViewId(viewId);
        return SSMResponseMessage.success("通过视图id获取模板id成功", tplId);
    }

    /**
     * 通过模版id获取模版的基础信息
     * @param tplId
     * @return
     */
    @RequestMapping(value = "getBaseInfoByTplId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<TemplateRsp> getBaseInfoByTplId(String tplId){
        return SSMResponseMessage.success("查询成功",configService.getBaseInfoByTplId(tplId));
    }

    /**
     * 收藏模版
     */
    @RequestMapping("batchFav")
    @ResponseBody
    public SSMResponseMessage<String> batchFav(@RequestBody TemplateBatchOperateReq req) {
        if (CollectionUtil.isEmpty(req.getTplIdList())) {
            return SSMResponseMessage.operationFailed("未选择收藏模版！");
        }
        configService.batchFav(req);
        return SSMResponseMessage.success("批量收藏成功！");
    }

    /**
     * 修改收藏模版
     */
    @RequestMapping("updateFav")
    @ResponseBody
    public SSMResponseMessage<String> updateFav(@RequestBody TemplateBatchOperateReq req) {
        if (CollectionUtil.isEmpty(req.getTplIdList())) {
            return SSMResponseMessage.operationFailed("未选择修改的模版！");
        }
        configService.updateFav(req);
        return SSMResponseMessage.success("批量修改成功！");
    }

    /**
     * 取消收藏
     */
    @RequestMapping("cancelFavorite")
    @ResponseBody
    public SSMResponseMessage<String> cancelFavorite(@RequestBody TemplateBatchOperateReq req) {
        configService.cancelFavorite(req);
        return SSMResponseMessage.success("取消收藏成功！");
    }

    /**
     * 置顶模版
     */
    @RequestMapping("top")
    @ResponseBody
    public SSMResponseMessage<String> top(@RequestBody TemplateOperateReq req) {
        try {
            configService.top(req);
            return SSMResponseMessage.success("设置书签成功！");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 取消模版置顶
     */
    @RequestMapping("cancelTop")
    @ResponseBody
    public SSMResponseMessage<String> cancelTop(@RequestBody TemplateBatchOperateReq req) {
        configService.cancelTop(req);
        return SSMResponseMessage.success("取消书签成功！");
    }

    /**
     * 首页查询置顶列表模版
     */
    @RequestMapping("topList")
    @ResponseBody
    public SSMResponseMessage<List<TemplateFavRsp>> topList() {
        return SSMResponseMessage.success("查询书签列表成功！", configService.topList());
    }

    /**
     * 上移下移改变置顶顺序
     */
    @RequestMapping("moveTopTpl")
    @ResponseBody
    public SSMResponseMessage<Object> moveTopTpl(@RequestBody MoveTopTplReq req) {
        configService.moveTopTpl(req);
        return SSMResponseMessage.success("移动成功！");
    }

    /**
     * 查询模版列表
     */
    @RequestMapping("templateList")
    @ResponseBody
    public SSMResponseMessage<TemplatePageListRsp> templateList(@RequestBody TemplatePageListReq req) {
        return SSMResponseMessage.success("查询列表成功！", configService.templateList(req));
    }

    /**
     * 查询模板树
     * @return
     */
    @RequestMapping(value = "tree", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<QueryTemplateCategory>> templateTree(String datasetType) {
        List<QueryTemplateCategory> queryTemplateCategoryList = configService.templateTree(BIConsts.TEMPLATE_CTG_SPACE_ID, datasetType);
        if (CollUtil.isNotEmpty(queryTemplateCategoryList)) {
            // 20251027只出公域模板
            for (QueryTemplateCategory ctg : queryTemplateCategoryList) {
                List<QueryTemplateCategory> children = ctg.getChildren();
                if (CollUtil.isEmpty(children)) {
                    continue;
                }
                ctg.setChildren(children.stream()
                        .filter(v -> Enabled.YES.getId().equals(v.getIsInPublicDomain())).collect(Collectors.toList()));
            }
        }
        return SSMResponseMessage.success("查询成功！", queryTemplateCategoryList);
    }

    @RequestMapping(value = "listAllViewableTree", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<QueryTemplateCategory>> listAllViewableTree(String datasetType) {
        return SSMResponseMessage.success("查询成功！", configService.listAllViewableTree(datasetType));
    }

    /**
     * 批量删除模板
     * @return
     */
    @RequestMapping("batchDelete")
    @ResponseBody
    public SSMResponseMessage<String> batchDelete(@RequestBody TemplateBatchOperateReq req) {
        try {
            if (CollectionUtil.isEmpty(req.getTplIdList())) {
                return SSMResponseMessage.operationFailed("未选择删除模版！");
            }
            configService.batchDelete(req);
            return SSMResponseMessage.success("批量删除成功！");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 批量移动模板
     * @return
     */
    @RequestMapping("batchMove")
    @ResponseBody
    public SSMResponseMessage<String> batchMove(@RequestBody TemplateMoveCtgReq req) {
        try {
            if (CollectionUtil.isEmpty(req.getTplIdList())) {
                return SSMResponseMessage.operationFailed("未选择移动模版！");
            }
            configService.batchMove(req);
            return SSMResponseMessage.success("批量移动成功！");
        } catch (Exception e) {
            return SSMResponseMessage.operationFailed(e.getMessage());
        }
    }

    /**
     * 模板分享给用户（多个用户、多个模版）
     * @return
     */
    @RequestMapping("shareTemplatesToUsers")
    @ResponseBody
    public SSMResponseMessage<Object> shareTemplateToUser(@RequestBody TemplateShareReq req) {
        if (CollectionUtil.isEmpty(req.getTplIdList())) {
            return SSMResponseMessage.operationFailed("未选择分享模版！");
        }
        shareService.shareTemplatesToUsers(req);
        return SSMResponseMessage.success("分享成功！");
    }

    /**
     * 模板分享给部门（多个模版）
     * @return
     */
    @RequestMapping("shareTemplatesToDept")
    @ResponseBody
    public SSMResponseMessage<Object> shareTemplatesToDept(@RequestBody TemplateShareReq req) {
        if (CollectionUtil.isEmpty(req.getTplIdList())) {
            return SSMResponseMessage.operationFailed("未选择分享模版！");
        }
        shareService.shareTemplatesToDept(req);
        return SSMResponseMessage.success("分享成功！");
    }

    /**
     * 保存模板(弃用)。使用saveTemplateAndView
     * @return
     */
    @Deprecated
    @RequestMapping(path = {"saveTpl", "save"})
    @ResponseBody
    public SSMResponseMessage<String> saveTemplate(@RequestBody TemplateAddReq req) {
        TemplateViewRsp templateViewSaveRsp = configService.saveTemplate(req);
        return SSMResponseMessage.success("模版保存成功！", templateViewSaveRsp.getTplId());
    }

    /**
     * 保存模板和视图
     * @return
     */
    @RequestMapping(value = "saveTemplateAndView", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<TemplateViewRsp> saveTemplateAndView(@RequestBody TemplateAddReq req) {
        TemplateViewRsp templateViewSaveRsp = configService.saveTemplate(req);
        return SSMResponseMessage.success("模版和视图保存成功！", templateViewSaveRsp);
    }

    /**
     * 另存模板和视图
     */
    @RequestMapping(value = "saveAsTemplateAndView", method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<TemplateViewRsp> saveAsTemplateAndView(@RequestBody TemplateAddReq req) {
        TemplateViewRsp templateViewSaveRsp = configService.saveAsTemplateAndView(req);
        return SSMResponseMessage.success("另存模版和视图成功！", templateViewSaveRsp);
    }

    /**
     * 生成快照分享（弃用），shareTplViewSnapshot
     */
    @Deprecated
    @RequestMapping("shareTplSnapshot")
    @ResponseBody
    public SSMResponseMessage<String> shareTplSnapshot(@RequestBody TemplateAddReq req) {
        req.setTplType(QueryTemplateType.SNAPSHOT.getId());
        String tplId = shareService.shareTplSnapshot(req).getTplId();
        return SSMResponseMessage.success("模版快照分享成功！", tplId);
    }

    /**
     * 生成模版视图快照分享
     */
    @RequestMapping("shareTplViewSnapshot")
    @ResponseBody
    public SSMResponseMessage<TemplateViewRsp> shareTplViewSnapshot(@RequestBody TemplateAddReq req) {
        req.setTplType(QueryTemplateType.SNAPSHOT.getId());
        return SSMResponseMessage.success("模版视图快照分享成功！", shareService.shareTplSnapshot(req));
    }

    /**
     * 更新模板名称
     * @return
     */
    @RequestMapping("updateTemplateTitle")
    @ResponseBody
    public SSMResponseMessage<Object> updateTemplateTitle(@RequestBody TemplateAddReq req) {
        String tplId = req.getTplId();
        if (StrUtil.isEmpty(tplId)) {
            throw new SSDException("缺少参数:tplId");
        }
        configService.updateTemplateTitle(req);
        return SSMResponseMessage.success("模版名称保存成功！");
    }

    /**
     * 更新模板名称和描述
     * @return
     */
    @RequestMapping("updateTemplateTitleAndDesc")
    @ResponseBody
    public SSMResponseMessage<Object> updateTemplateTitleAndDesc(@RequestBody TemplateAddReq req) {
        String tplId = req.getTplId();
        if (StrUtil.isEmpty(tplId)) {
            throw new SSDException("缺少参数:tplId");
        }
        configService.updateTemplateTitleAndDesc(req);
        return SSMResponseMessage.success("模版名称和描述保存成功！");
    }

    /**
     * 查询批量用户的详细信息
     */
    @RequestMapping("queryUserListByNames")
    @ResponseBody
    public SSMResponseMessage<List<User>> queryUserListByNames(@RequestBody TemplateShareReq req) {
        List<User> userList = configService.getUserListByNames(req.getUserNames());
        return SSMResponseMessage.success("查询成功！", userList);
    }

    /**
     * 获取模板跳转下拉列表
     * @param req
     * @return
     */
    @RequestMapping("queryTemplateLinkList")
    @ResponseBody
    public SSMResponseMessage<List<TemplateLinkRsp>> queryTemplateLinkList(@RequestBody TemplateLinkReq req) {

        //兼容旧版本只传FieldCode
        if (CollUtil.isEmpty(req.getFieldCodeList())) {
            if (StrUtil.isNotEmpty(req.getFieldCode())) {
                List<String> fieldCodeList = new ArrayList<>();
                fieldCodeList.add(req.getFieldCode());
                req.setFieldCodeList(fieldCodeList);
            }
        }

        if (CollUtil.isEmpty(req.getFieldCodeList())) {
            throw new SSDException("缺少参数:fieldCodeList");
        }

        List<TemplateLinkRsp> result = linkService.queryTemplateLinkList(req);
        return SSMResponseMessage.success("查询成功！", result);
    }

    /**
     * 通过模板id获取模板跳转详情
     * @param req
     * @return
     */
    @RequestMapping("getTemplateLinkDetailByTemplateId")
    @ResponseBody
    public SSMResponseMessage<TemplateLinkDetailRsp> getTemplateLinkDetailByTemplateId(@RequestBody TemplateLinkReq req) {

        String tplId = req.getTplId();
        if (StrUtil.isEmpty(tplId)) {
            throw new SSDException("缺少参数:tplId");
        }

        TemplateLinkDetailRsp templateLinkDetail = linkService.getTemplateLinkDetailByTemplateId(req);
        return SSMResponseMessage.success("查询成功！", templateLinkDetail);
    }

    /**
     * 获取模板数据更新时间
     * @param req
     * @return
     */
    @RequestMapping("getTemplateDataUpdateTime")
    @ResponseBody
    public SSMResponseMessage<List<TemplateDataUpdateTimeRsp>> getTemplateDataUpdateTime(@RequestBody TemplateDataUpdateTimeReq req){
        return  SSMResponseMessage.success("查询成功！",configService.getTemplateDataUpdateTime(req));
    }

    /**
     * 通过分类id获取模版
     * 模版在子目录也会查询出来
     * @param ctgId
     * @return
     */
    @RequestMapping(value = "getTemplateByCtgId", method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateRsp>> getTemplateByCtgId(String ctgId) {
        List<TemplateRsp> templateList = configService.getTemplateByCtgId(ctgId);
        return SSMResponseMessage.success("查询成功！", templateList);
    }

    /**
     * 我的空间目录树
     * @return
     */
    @RequestMapping(path = {"myspace/tree"})
    @ResponseBody
    public SSMResponseMessage<List<TemplateCtgTreeRsp>> mySpaceTree() {
        return SSMResponseMessage.success("查询成功！", configService.getCategoryTplTree(getMyRootIds(), true, null));
    }

    /**
     * 我的空间目录树
     * @return
     */
    @RequestMapping(path = {"space/tree"}, method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateCtgTreeRsp>> ctgTree(String ctgId, String portalId) {
        List<TemplateCtgTreeRsp> tree;
        if (QueryTemplateCategoryType.MY.getId().equals(ctgId)) {
            tree = configService.getCategoryTplTree(getMyRootIds(), true, portalId);
        } else {
            tree = configService.getCategoryTplTree(Collections.singletonList(ctgId), false, portalId);
        }
        return SSMResponseMessage.success("查询成功！", tree);
    }

    /**
     * 我的空间目录树
     * @return
     */
    @RequestMapping(path = {"space/list"}, method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<List<TemplateCtgTreeRsp>> listSpaceQueryTemplate(String ctgId) {
        List<TemplateCtgTreeRsp> tree;
        String portalId = "all"; // 不鉴权
        if (QueryTemplateCategoryType.MY.getId().equals(ctgId)) {
            tree = configService.getCategoryTplTree(getMyRootIds(), true, null);
        } else {
            tree = configService.getCategoryTplTree(Collections.singletonList(ctgId), false, portalId);
        }

        // 过滤掉目录
        tree.removeIf(item -> !FavTemplateType.QUERY_TEMPLATE.getCode().equals(item.getType()));
        return SSMResponseMessage.success("查询成功！", tree);
    }


    /**
     * 获取模版配置的资产信息
     * @param tplId
     * @return
     */
    @RequestMapping(path = {"getTemplateCfgDtlByTplId"}, method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<SSDQueryTemplate> getTemplateCfgDtlByTplId(String tplId){
        return SSMResponseMessage.success("查询成功！", configService.getTemplateCfgDtlByTplId(tplId));
    }

    /**
     * 获取模版是否更新, 如果更新则返回模版信息
     * @return
     */
    @RequestMapping(path = {"getAssetIfTemplateUpdated"}, method = RequestMethod.POST)
    @ResponseBody
    public SSMResponseMessage<SSDQueryTemplate> getAssetIfTemplateUpdated(@RequestBody TemplateAssetUpdatedReq templateAssetUpdatedReq) {
        return SSMResponseMessage.success("查询成功！", configService.getAssetIfTemplateUpdated(templateAssetUpdatedReq));
    }

    /**
     * 获取模版负责人
     * @param tplId
     * @return
     */
    @RequestMapping(path = {"getTemplateOwnerByTplId"}, method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<TemplateOwnerRsp> getTemplateOwnerByTplId(String tplId) {
        return SSMResponseMessage.success("查询成功！", configService.getTemplateOwnerByTplId(tplId));
    }

    /**
     * 判断当前用户是否是快照模板原模板的 owner
     * @param tplId 快照模板ID
     * @return 1=是 0=否
     */
    @RequestMapping(path = {"isRootTplOwner"}, method = RequestMethod.GET)
    @ResponseBody
    public SSMResponseMessage<Integer> isRootTplOwner(String tplId) {
        return SSMResponseMessage.success("查询成功！", configService.isRootTplOwner(tplId));
    }

    private List<String> getMyRootIds() {
        List<String> rootIds = new ArrayList<>();
        rootIds.add(QueryTemplateCategoryType.MY.getId());
        rootIds.add(QueryTemplateCategoryType.FAV.getId());
        return rootIds;
    }

}
