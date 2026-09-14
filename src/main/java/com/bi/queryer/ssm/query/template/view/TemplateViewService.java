package com.bi.queryer.ssm.query.template.view;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.api.OlapApiService;
import com.bi.queryer.ssm.api.vo.req.QueryOlapDataReq;
import com.bi.queryer.ssm.api.vo.rsp.OlapDatasetColumnMetadata;
import com.bi.queryer.ssm.engine.accelerate.route.DataSourceRouter;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.enums.CategoryType;
import com.bi.queryer.ssm.enums.DateGranularity;
import com.bi.queryer.ssm.enums.FieldType;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.*;
import com.bi.queryer.ssm.migrate.bizsplit.service.MetricExpansionShadowService;
import com.bi.queryer.ssm.portal.enums.DragType;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.query.ctg.QueryTemplateCategoryService;
import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.query.template.TemplateLinkService;
import com.bi.queryer.ssm.query.template.asset.change.apply.TemplateAssetChangeApplyService;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyEntity;
import com.bi.queryer.ssm.query.template.asset.change.apply.model.TemplateAssetChangeApplyRsp;
import com.bi.queryer.ssm.query.template.change.log.TemplateChangeLogService;
import com.bi.queryer.ssm.query.template.change.log.model.TemplateChangeLogEntity;
import com.bi.queryer.ssm.query.template.enums.*;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.ssm.query.template.view.model.*;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import com.bi.queryer.ssm.query.template.view.event.TemplateViewUpdatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
public class TemplateViewService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private OlapApiService olapApiService;

    @Autowired
    private QueryTemplateCategoryService categoryService;

    @Autowired
    protected TemplateLinkService linkService = null;

    @Autowired
    protected TemplateChangeLogService templateChangeLogService = null;

    @Autowired
    protected TemplateViewCfgHistoryService templateViewCfgHistoryService = null;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /** 指标膨胀影子配置：UT 读/存优先走影子 */
    @Autowired
    private MetricExpansionShadowService metricExpansionShadowService;

    /**
     * 根据模板ID获取模板视图
     * 1 公共视图 -> 所有人可以看
     * 2 个人视图 -> 创建人可以看
     * @param tplId
     * @return
     */
    public List<TemplateViewEntity> getByTplId(String tplId) {

        if (StrUtil.isEmpty(tplId)) {
            throw new RuntimeException("模板ID不能为空");
        }

        //获取公共视图
        List<TemplateViewEntity> viewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getPublicViewByTplId", tplId);

        //获取个人视图
        Map<String,Object> params = new HashMap<>();
        params.put("tplId", tplId);
        params.put("userName", UserManager.get().getName());
        List<TemplateViewEntity> personalViewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getPersonalViewByTplId", params);

        if(CollUtil.isNotEmpty(personalViewList)){
            viewList.addAll(personalViewList);
        }

        return viewList;
    }

    /**
     * 根据模板ID获取模板视图（含公共视图 + 模板下所有个人视图，供下拉框展示）
     * @param tplId
     * @return
     */
    public List<TemplateViewEntity> getAllViewByTplId(String tplId) {

        if (StrUtil.isEmpty(tplId)) {
            throw new RuntimeException("模板ID不能为空");
        }

        //获取公共视图
        List<TemplateViewEntity> viewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getPublicViewByTplId", tplId);

        //获取模板下所有个人视图（不限制用户）
        List<TemplateViewEntity> personalViewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getAllPersonalViewByTplId", tplId);

        if (CollUtil.isNotEmpty(personalViewList)) {
            viewList.addAll(personalViewList);
        }

        // 收集第二轮并发所需的入参
        List<String> createdByList = CollUtil.isEmpty(personalViewList) ? Collections.emptyList()
                : personalViewList.stream()
                        .map(TemplateViewEntity::getCreatedBy)
                        .filter(StrUtil::isNotEmpty)
                        .distinct()
                        .collect(Collectors.toList());

        List<Long> applyIdList = viewList.stream()
                .map(TemplateViewEntity::getLastAssetChangeApplyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 第二轮并发：查用户真实姓名 + 查申请信息 + 查默认视图
        CompletableFuture<Map<String, String>> userRealNameFuture = CollUtil.isEmpty(createdByList)
                ? CompletableFuture.completedFuture(Collections.emptyMap())
                : CompletableFuture.supplyAsync(() -> {
                    List<User> userList = (List<User>) dao.queryObjectList("user.queryUserListByNames", createdByList);
                    return userList.stream().collect(Collectors.toMap(User::getName, User::getRealName, (a, b) -> a));
                });

        CompletableFuture<Map<Long, TemplateAssetChangeApplyEntity>> applyMapFuture = CollUtil.isEmpty(applyIdList)
                ? CompletableFuture.completedFuture(Collections.emptyMap())
                : CompletableFuture.supplyAsync(() -> {
                    List<TemplateAssetChangeApplyEntity> applyList = (List<TemplateAssetChangeApplyEntity>) dao.queryObjectList("ssm.template.asset.change.apply.getApplyByIdList", applyIdList);
                    return applyList.stream().collect(Collectors.toMap(TemplateAssetChangeApplyEntity::getApplyId, apply -> apply));
                });

        // getDefaultViewIdByTplId 依赖 UserManager ThreadLocal，必须在主线程调用
        CompletableFuture.allOf(userRealNameFuture, applyMapFuture).join();
        String defaultViewId = getDefaultViewIdByTplId(tplId);

        // 回填用户真实姓名
        Map<String, String> userRealNameMap = userRealNameFuture.join();
        for (TemplateViewEntity viewEntity : personalViewList) {
            viewEntity.setUserRealName(userRealNameMap.get(viewEntity.getCreatedBy()));
        }

        // 回填申请审批状态
        Map<Long, TemplateAssetChangeApplyEntity> applyMap = applyMapFuture.join();
        for (TemplateViewEntity viewEntity : viewList) {
            TemplateAssetChangeApplyEntity applyEntity = applyMap.get(viewEntity.getLastAssetChangeApplyId());
            if (applyEntity != null) {
                viewEntity.setApprovalStatus(applyEntity.getApprovalStatus());
            }
        }

        // 设置默认视图标记
        if (StrUtil.isEmpty(defaultViewId)) {
            return viewList;
        }
        for (TemplateViewEntity viewEntity : viewList) {
            if (defaultViewId.equals(viewEntity.getViewId())) {
                viewEntity.setIsDefault(Enabled.YES.getId());
            } else {
                viewEntity.setIsDefault(Enabled.NO.getId());
            }
        }

        return viewList;
    }

    /**
     * 根据模板ID集合批量获取模板视图
     * 不区分公共视图还是个人视图
     * @param tplIdList
     * @return
     */
    public List<TemplateViewEntity> getByTplIdList(List<String> tplIdList){

        if(CollUtil.isEmpty(tplIdList)){
            return new ArrayList<>();
        }

        //公共视图
        List<TemplateViewEntity> viewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getPublicViewByTplIdList", tplIdList);

        //个人视图
        Map<String, Object> params = new HashMap<>();
        params.put("tplIdList", tplIdList);
        params.put("userName", UserManager.get().getName());
        List<TemplateViewEntity> personalViewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getPersonalViewByTplIdList", params);
        if(CollUtil.isNotEmpty(personalViewList)){
            viewList.addAll(personalViewList);
        }

        return viewList;
    }

    /**
     * 公共视图 + 个人视图
     * @param tplIdList
     * @return
     */
    public List<TemplateViewEntity> getAllViewByTplIdList(List<String> tplIdList){
        if(CollUtil.isEmpty(tplIdList)){
            return new ArrayList<>();
        }

        List<TemplateViewEntity> viewList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.getByTplIdList", tplIdList);
        return viewList;
    }

    /**
     * 重命名模板视图
     * @param templateViewReq
     */
    public void rename(TemplateViewReq templateViewReq) {

        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", templateViewReq.getViewId());
        SSDQueryTemplate tpl = queryTemplateById(templateViewEntity.getTplId());
        checkViewAuth(tpl, templateViewEntity, "重命名");

        Map<String, String> params = new HashMap<>();
        params.put("viewId", templateViewReq.getViewId());
        params.put("viewName", templateViewReq.getViewName());
        dao.update("ssm.template.view.rename", params);
    }

    /**
     * 设置模板视图为默认
     * @param templateViewReq
     */
    public void setDefault(TemplateViewReq templateViewReq) {

        String userName = UserManager.get().getName();

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                TemplateViewEntity viewEntity = new TemplateViewEntity();
                viewEntity.setViewId(templateViewReq.getViewId());
                viewEntity.setTplId(templateViewReq.getTplId());
                viewEntity.setCreatedBy(userName);
                viewEntity.setIsDefault(Enabled.YES.getId());
                saveViewUserCfg(viewEntity);

            }
        });

    }

    /**
     * 新增模板视图
     * @param req
     * @return
     */
    public String add(TemplateViewSaveReq req) {

        //checkTemplateLink(req.getTemplateLinkList());

        String viewId = Guid.id();
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                TemplateViewEntity viewEntity = new TemplateViewEntity();
                viewEntity.setTplId(req.getTplId());

                viewEntity.setViewId(viewId);
                viewEntity.setViewName(req.getViewName());
                viewEntity.setViewType(req.getViewType());
                viewEntity.setCreatedBy(UserManager.get().getName());

                //前端没传值，默认是公共视图
                if (StrUtil.isEmpty(req.getViewType())) {
                    viewEntity.setViewType(TemplateViewType.PUBLIC.getCode());
                    req.setViewType(TemplateViewType.PUBLIC.getCode());
                }

                //2025-11-24迭代视图状态，
                //兼容逻辑：如果前端传递的视图状态为空，则通过is_active判断
                if (StrUtil.isEmpty(req.getViewStatus())) {
                    req.setViewStatus(Enabled.value(req.getIsViewValid()) ? ViewStatusType.ACTIVE.getCode() : ViewStatusType.TEMPORARY.getCode());
                }
                viewEntity.setViewStatus(req.getViewStatus());

                //校验新增权限
                //公共视图-仅模版owner可保存，
                SSDQueryTemplate tpl = queryTemplateById(viewEntity.getTplId());
                checkViewAuth(tpl, viewEntity, "新增");

                // 新增个人视图时，数据集必须与模版当前数据集一致
                if (TemplateViewType.PERSONAL == TemplateViewType.get(viewEntity.getViewType())
                        && StrUtil.isNotEmpty(req.getDatasetId())
                        && StrUtil.isNotEmpty(tpl.getDatasetId())
                        && !tpl.getDatasetId().equalsIgnoreCase(req.getDatasetId())) {
                    throw new SSDException("个人视图的数据集需与模版保持一致，不允许新建数据集不一致的个人视图！");
                }

                //tplConfigFieldDimAsset,tplConfigFieldMeasureAsset 资产信息是加密的，使用前需要解密
                req.setTplConfigFieldDimAsset(SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldDimAsset()));
                req.setTplConfigFieldMeasureAsset(SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldMeasureAsset()));

                //添加模版资产变更记录
                insertTemplateAssetChangeLog(req);

                viewEntity.setIsDefault(Enabled.NO.getId());
                Double sortId = (double) dao.queryObject("ssm.template.view.getMaxSortId", req.getTplId());
                //第一次视图设置为默认
                if (sortId == 0) {
                    viewEntity.setIsDefault(Enabled.YES.getId());
                }
                viewEntity.setSortId(sortId + 1);

                String cfgId = Guid.id();
                viewEntity.setCfgId(cfgId);
                viewEntity.setIsActive(req.getIsViewValid());

                //如果模板是失效的，设置资产失效时间
                if (ViewStatusType.TEMPORARY == ViewStatusType.getByCode(viewEntity.getViewStatus())) {
                    viewEntity.setAssetExpiresTime(DateUtil.now());
                }

                viewEntity.setDatasetId(StrUtil.isNotEmpty(req.getDatasetId()) ? req.getDatasetId() : tpl.getDatasetId());

                dao.insert("ssm.template.view.add", viewEntity);

                //保存视图配置信息
                String tplConfig = SSDUtil.decryptTplConfig(req.getTplConfig());
                TemplateCfgEntity cfgEntity = new TemplateCfgEntity(cfgId, tplConfig);
                dao.insert("ssm.template.saveTemplateCfg", cfgEntity);

                //保存模板字段编码信息
                TemplateCfgDtlEntity cfgDtlEntity = new TemplateCfgDtlEntity(cfgId, req.getTplConfigFieldDimCodes(), req.getTplConfigFieldMeasureCodes());
                cfgDtlEntity.setTplConfigFieldDimAsset(req.getTplConfigFieldDimAsset());
                cfgDtlEntity.setTplConfigFieldMeasureAsset(req.getTplConfigFieldMeasureAsset());
                cfgDtlEntity.setDatasetId(viewEntity.getDatasetId());
                if (StrUtil.isNotEmpty(req.getTplConfigFieldDimCodes()) || StrUtil.isNotEmpty(req.getTplConfigFieldMeasureCodes())) {
                    dao.insert("ssm.query.template.cfg.dtl.add", cfgDtlEntity);
                }

                //新增视图时，只有视图为默认视图，才需要保存默认配置
                if (Enabled.value(viewEntity.getIsDefault())) {
                    saveViewUserCfg(viewEntity);
                }

                //公共视图才处理
                if (TemplateViewType.PUBLIC == TemplateViewType.get(viewEntity.getViewType())) {
                    setViewDisabled(req.getTplId(), viewId, cfgDtlEntity);
                }

                //保存模版跳转信息
                linkService.saveTemplateLink(req.getTplId(), viewId, req.getTemplateLinkList());
                linkService.deleteInvalidTemplateLink(req.getTplId(), viewId, req.getInvalidFieldCodeList());

                //入参的数据集id与模板的数据集id不一致，更新模板的数据集id
                syncTemplateDatasetOnViewSave(viewEntity, req, tpl);

                //添加模版视图配置历史记录
                addViewCfgHistory(viewEntity, cfgEntity);
            }
        });

        return viewId;
    }

    /**
     * 另存为原模板视图
     * 通过 viewId 找到 tplId，再通过 ssd_query_template_share_rel 找到 root_tpl_id，
     * 在 root_tpl_id 对应的模板上新增一个个人视图
     * @param req 视图保存请求，viewId 为来源视图
     * @return 新视图ID
     */
    public TemplateViewRsp saveAsToRootTpl(TemplateViewSaveReq req) {

        if (StrUtil.isEmpty(req.getViewId())) {
            throw new RuntimeException("来源视图ID不能为空");
        }

        // 通过 viewId 找到 tplId
        TemplateViewEntity sourceView = getByViewId(req.getViewId());
        if (sourceView == null) {
            throw new RuntimeException("来源视图不存在，viewId：" + req.getViewId());
        }

        // 通过 tplId 查找 root_tpl_id
        String rootTplId = (String) dao.queryObject("ssm.template.queryRootTplIdByTargetTplId", sourceView.getTplId());
        if (StrUtil.isEmpty(rootTplId)) {
            throw new RuntimeException("未找到关联的原始模板，tplId：" + sourceView.getTplId());
        }

        // 在 root_tpl_id 对应的模板上新增个人视图
        req.setTplId(rootTplId);
        req.setViewId(null);
        //req.setViewType(TemplateViewType.PERSONAL.getCode());

        String viewId = add(req);
        TemplateViewRsp rsp = new TemplateViewRsp();
        rsp.setTplId(rootTplId);
        rsp.setViewId(viewId);
        return rsp;
    }

    /**
     * 添加模版视图配置历史记录
     */
    public void addViewCfgHistory(TemplateViewEntity viewEntity, TemplateCfgEntity cfgEntity) {

        TemplateCfgHistoryEntity templateCfgHistoryEntity = new TemplateCfgHistoryEntity();

        templateCfgHistoryEntity.setViewId(viewEntity.getViewId());
        templateCfgHistoryEntity.setTplId(viewEntity.getTplId());
        templateCfgHistoryEntity.setCfgId(cfgEntity.getCfgId());
        templateCfgHistoryEntity.setTplConfig(cfgEntity.getTplConfig());
        templateCfgHistoryEntity.setCreatedBy(UserManager.get().getName());

        templateViewCfgHistoryService.add(templateCfgHistoryEntity);
    }

    /**
     * 修改模板视图
     * @param req
     * @return
     */
    public String update(TemplateViewSaveReq req) {

        //checkTemplateLink(req.getTemplateLinkList());

        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", req.getViewId());
        if (templateViewEntity == null) {
            throw new SSDException("该视图不存在，无法保存！");
        }

        checkViewApplyStatus(templateViewEntity,"更新");

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                //前端没传值，默认是公共视图
                if (StrUtil.isEmpty(req.getViewType())) {
                    req.setViewType(TemplateViewType.PUBLIC.getCode());
                }

                //2025-11-24迭代视图状态，
                //兼容逻辑：如果前端传递的视图状态为空，则通过is_active判断
                if(StrUtil.isEmpty(req.getViewStatus())){
                    req.setViewStatus(Enabled.value(req.getIsViewValid())?ViewStatusType.ACTIVE.getCode():ViewStatusType.TEMPORARY.getCode());
                }

                boolean isChangeViewType = false;
                if (!templateViewEntity.getViewType().equalsIgnoreCase(req.getViewType())) {
                    isChangeViewType = true;

                    //转化为个人视图前，至少需要保留一个有效视图
                    if (TemplateViewType.PERSONAL == TemplateViewType.get(req.getViewType())) {
                        List<TemplateViewEntity> templateViewEntityList = getByTplId(templateViewEntity.getTplId());
                        checkActiveViewCount(templateViewEntityList, templateViewEntity.getViewId());
                    }
                }

                //视图从有效状态变为无效状态时，需要设置资产失效时间
                if(ViewStatusType.ACTIVE == ViewStatusType.getByCode(templateViewEntity.getViewStatus())
                    && ViewStatusType.TEMPORARY == ViewStatusType.getByCode(req.getViewStatus())){
                    templateViewEntity.setAssetExpiresTime(DateUtil.now());
                }

                templateViewEntity.setViewName(req.getViewName());
                templateViewEntity.setViewType(req.getViewType());
                templateViewEntity.setViewStatus(req.getViewStatus());
                templateViewEntity.setUpdatedBy(UserManager.get().getName());

                //校验新增权限
                //公共视图-仅模版owner可保存，
                SSDQueryTemplate tpl = queryTemplateById(templateViewEntity.getTplId());
                checkViewAuth(tpl, templateViewEntity, "更新");

                //tplConfigFieldDimAsset,tplConfigFieldMeasureAsset 资产信息是加密的，使用前需要解密
                req.setTplConfigFieldDimAsset(SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldDimAsset()));
                req.setTplConfigFieldMeasureAsset(SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldMeasureAsset()));
                //添加模版资产变更记录
                insertTemplateAssetChangeLog(req);

                //设置视图状态
                templateViewEntity.setIsActive(req.getIsViewValid());
                templateViewEntity.setViewStatus(req.getViewStatus());
                templateViewEntity.setDatasetId(StrUtil.isNotEmpty(req.getDatasetId()) ? req.getDatasetId() : templateViewEntity.getDatasetId());

                dao.update("ssm.template.view.update", templateViewEntity);

                //视图类型是否发生变更
                //变更后，排序放在最后
                if (isChangeViewType) {
                    Double maxSortId = (Double) dao.queryObject("ssm.template.view.queryMaxSortIdByTplIdAndViewType", templateViewEntity);
                    templateViewEntity.setSortId(maxSortId + 1);
                    dao.update("ssm.template.view.updateViewSortId", templateViewEntity);
                }

                //更新视图配置信息（UT 有影子则只写影子，正式库不动）
                String tplConfig = SSDUtil.decryptTplConfig(req.getTplConfig());
                TemplateCfgEntity cfgEntity = new TemplateCfgEntity(templateViewEntity.getCfgId(), tplConfig);
                TemplateCfgDtlEntity cfgDtlEntity = new TemplateCfgDtlEntity(templateViewEntity.getCfgId(), req.getTplConfigFieldDimCodes(), req.getTplConfigFieldMeasureCodes());
                cfgDtlEntity.setTplConfigFieldDimAsset(req.getTplConfigFieldDimAsset());
                cfgDtlEntity.setTplConfigFieldMeasureAsset(req.getTplConfigFieldMeasureAsset());
                cfgDtlEntity.setDatasetId(templateViewEntity.getDatasetId());

                boolean savedToShadow = metricExpansionShadowService.savePreferShadowOnUt(
                        templateViewEntity.getCfgId(),
                        templateViewEntity.getViewId(),
                        templateViewEntity.getTplId(),
                        tplConfig,
                        cfgDtlEntity);
                if (!savedToShadow) {
                    dao.update("ssm.template.updateTemplateCfg", cfgEntity);

                    //更新模板字段编码信息
                    if (StrUtil.isNotEmpty(req.getTplConfigFieldDimCodes()) || StrUtil.isNotEmpty(req.getTplConfigFieldMeasureCodes())) {
                        dao.delete("ssm.query.template.cfg.dtl.delete", templateViewEntity.getCfgId());
                        dao.insert("ssm.query.template.cfg.dtl.add", cfgDtlEntity);
                    }
                }

                //公共视图才处理
                if (TemplateViewType.PUBLIC == TemplateViewType.get(templateViewEntity.getViewType())) {
                    setViewDisabled(req.getTplId(), req.getViewId(), cfgDtlEntity);
                }

                //前端不传，默认保存跳转链接
                if (req.getIsSaveTemplateLink() == null) {
                    req.setIsSaveTemplateLink(Enabled.YES.getId());
                }

                //更新视图，根据前端传递的参数判断是否需要保存跳转信息
                if (Enabled.value(req.getIsSaveTemplateLink())) {
                    //保存模版跳转信息
                    linkService.saveTemplateLink(req.getTplId(), templateViewEntity.getViewId(), req.getTemplateLinkList());
                    linkService.deleteInvalidTemplateLink(req.getTplId(), templateViewEntity.getViewId(), req.getInvalidFieldCodeList());
                }

                //入参的数据集id与模板的数据集id不一致，更新模板的数据集id
                syncTemplateDatasetOnViewSave(templateViewEntity, req, tpl);

                //添加模版视图配置历史记录
                addViewCfgHistory(templateViewEntity, cfgEntity);
            }
        });

        eventPublisher.publishEvent(new TemplateViewUpdatedEvent(this, req.getViewId()));

        return req.getViewId();
    }

    /**
     * 校验视图审批状态
     */
    public void checkViewApplyStatus(TemplateViewEntity templateViewEntity,String operatorName) {
        //视图审批中状态不允许修改
        if (templateViewEntity.getLastAssetChangeApplyId() == null) {
            return;
        }

        TemplateAssetChangeApplyEntity templateAssetChangeApplyEntityDb = (TemplateAssetChangeApplyEntity) dao.queryObject("ssm.template.asset.change.apply.get", templateViewEntity.getLastAssetChangeApplyId());
        if (templateAssetChangeApplyEntityDb == null) {
            return;
        }

        ApprovalStatusType approvalStatusType = ApprovalStatusType.getByCode(templateAssetChangeApplyEntityDb.getApprovalStatus());
        if (ApprovalStatusType.APPLY == approvalStatusType) {
            throw new BIException(String.format("发布申请中的视图不可%s，若需要%s请先撤回发布申请（在视图切换下拉框中-更多按钮中）",
                    operatorName,
                    operatorName));
        }

    }

    /**
     * 保存模版字段资产信息变更的日志
     * @param req
     */
    public void insertTemplateAssetChangeLog(TemplateViewSaveReq req) {

        //不是公共视图，不会产生资产变更
        TemplateViewType templateViewType = TemplateViewType.get(req.getViewType());
        if(TemplateViewType.PUBLIC != templateViewType){
            return;
        }

        //查询数据库中的有效的资产信息
        List<TemplateCfgDtlEntity> viewCfgList = (List<TemplateCfgDtlEntity>) dao.queryObjectList(
                "ssm.query.template.cfg.dtl.queryByTplId", req.getTplId());
        if (CollUtil.isEmpty(viewCfgList)) {
            return;
        }
        if (StrUtil.isEmpty(req.getViewId())) {
            return;
        }
        TemplateCfgDtlEntity viewCfg = viewCfgList.stream()
                .filter(cfg -> ViewStatusType.ACTIVE == ViewStatusType.getByCode(cfg.getViewStatus())
                        && !StrUtil.equalsIgnoreCase(cfg.getViewId(), req.getViewId()))
                .findFirst()
                .orElse(null);
        if (viewCfg == null) {
            return;
        }

        boolean datasetChanged = !StrUtil.equalsIgnoreCase(viewCfg.getDatasetId(), req.getDatasetId());
        boolean assetChanged = false;
        if (!datasetChanged) {
            // 数据集未变，才继续比较维度/指标资产
            String dimAssets = req.getTplConfigFieldDimAsset();
            if (!compareFieldAsset(viewCfg.getTplConfigFieldDimAsset(), dimAssets)) {
                assetChanged = true;
            }
            String measureAssets = req.getTplConfigFieldMeasureAsset();
            if (!compareFieldAsset(viewCfg.getTplConfigFieldMeasureAsset(), measureAssets)) {
                assetChanged = true;
            }
        }

        //没有变更，不处理
        if (!datasetChanged && !assetChanged) {
            return;
        }

        TemplateChangeLogEntity templateChangeLogEntity = new TemplateChangeLogEntity();
        templateChangeLogEntity.setTplId(req.getTplId());
        templateChangeLogEntity.setCreatedBy(UserManager.get().getName());
        templateChangeLogEntity.setChangeType(TemplateChangeContentType.ASSET.getCode());
        templateChangeLogEntity.setChangeName(TemplateChangeContentType.ASSET.getName());
        String changeContentTypeName = datasetChanged
                ? TemplateChangeContentType.DATASET.getName()
                : TemplateChangeContentType.ASSET.getName();
        templateChangeLogEntity.setChangeContent("更新了" + changeContentTypeName);
        templateChangeLogService.add(templateChangeLogEntity);

    }


    /**
     * 校验模板跳转的可用性
     * @param templateLinkList
     */
    private void checkTemplateLink(List<TemplateLinkAddReq> templateLinkList) {

        if (CollUtil.isEmpty(templateLinkList)) {
            return;
        }

        List<String> viewIdList = new ArrayList<>();
        List<String> tplIdList = new ArrayList<>();
        for(TemplateLinkAddReq templateLinkAddReq : templateLinkList){

            String tplId = templateLinkAddReq.getTplId();
            String viewId = templateLinkAddReq.getViewId();
            //兼容历史模版，历史模版初始化时，视图id=模版id
            if(StrUtil.isEmpty(viewId)){
                viewId = tplId;
            }

            viewIdList.add(viewId);
            tplIdList.add(tplId);
        }

        if(CollUtil.isEmpty(tplIdList) || CollUtil.isEmpty(viewIdList)){
            return;
        }

        //查询模版基础信息
        List<TemplateRsp> templateRspList = (List<TemplateRsp>) dao.queryObjectList("ssm.template.queryTemplateByIds", tplIdList);
        Map<String,TemplateRsp> templateRspMap = new HashMap<>();
        for(TemplateRsp templateRsp : templateRspList){
            templateRspMap.put(templateRsp.getTplId(), templateRsp);
        }

        //查询视图信息
        List<TemplateViewEntity> viewList = (List<TemplateViewEntity>)dao.queryObjectList("ssm.template.view.queryViewWithCfgByIds", viewIdList);

        String msg = "";
        for (TemplateLinkAddReq templateLinkAddReq : templateLinkList) {
            List<TemplateLinkFieldAddReq> linkFieldList = templateLinkAddReq.getTemplateLinkFieldList();
            if (CollUtil.isEmpty(linkFieldList)) {
                continue;
            }
            String tplId = templateLinkAddReq.getTplId();
            TemplateRsp templateRsp = templateRspMap.get(tplId);
            if(templateRsp == null){
                continue;
            }

            Optional<TemplateViewEntity> optionalTemplateView = viewList.stream().filter(f->f.getViewId().equals(templateLinkAddReq.getViewId()) && f.getTplId().equals(tplId)).findAny();
            if(!optionalTemplateView.isPresent()){
                continue;
            }

            TemplateViewEntity viewEntity = optionalTemplateView.get();
            String tplConfig = viewEntity.getTplConfig();
            for (TemplateLinkFieldAddReq fieldAddReq : linkFieldList) {
                boolean isValid = linkService.checkTemplateCanLink(fieldAddReq.getFieldCode(), tplConfig);
                if (!isValid) {
                    msg += String.format("筛选条件【%s】跳转至模板【%s-%s】-视图【%s-%s】已失效，", fieldAddReq.getFieldTitle(), tplId, templateRsp.getTplName(),viewEntity.getViewId(),viewEntity.getViewName());
                }
            }
        }

        if (StrUtil.isNotEmpty(msg)) {
            msg += "请重新选择！";
            throw new SSDException(msg);
        }

    }


    /**
     * 拷贝模版视图
     *  快照视图->不复制
     *  模版另存为-》公共视图+个人视图
     *  看板快照-》公共视图
     */
    public List<TemplateViewMapping> copyView(String oldTplId, String newTplId,String excludeViewId,String tplType) {

        List<TemplateViewMapping> viewIdMappingList = new ArrayList<>();

        QueryTemplateType queryTemplateType = QueryTemplateType.get(tplType);

        //快照视图->不复制
        if(queryTemplateType == QueryTemplateType.SNAPSHOT || QueryTemplateType.ANALYSIS_TEMPLATE_SNAPSHOT == queryTemplateType) {
            return viewIdMappingList;
        }

        if(StrUtil.isEmpty(oldTplId)) {
            return viewIdMappingList;
        }

        //1. 查询原始模版下的视图
        List<TemplateViewEntity> viewList = getByTplId(oldTplId);
        if (CollUtil.isEmpty(viewList)) {
            return viewIdMappingList;
        }

        //看板快照只获取公共视图
//        if(QueryTemplateType.ANALYSIS_TEMPLATE_SNAPSHOT == queryTemplateType){
//            viewList = viewList.stream().filter(v->TemplateViewType.PUBLIC == TemplateViewType.get(v.getViewType())).collect(Collectors.toList());
//        }

        /**
         * 只有一个视图不处理
         */
        Long viewCount = viewList.stream().filter(viewEntity -> !viewEntity.getViewId().equals(excludeViewId)).count();
        if (viewCount == 0) {
            return viewIdMappingList;
        }

        String userName = UserManager.get().getName();
        Map<String, Object> paramMap = new HashMap<>();

        //查询用户的默认视图配置信息
        String oldDefaultViewId = getDefaultViewIdByTplId(oldTplId);
        String newDefaultViewId = "";

        //视图配置，采用insert into select 的方式批量插入，需要构造cfgId的映射关系
        List<String> cfgIdCaseWhenSqlList = new ArrayList<>();
        List<String> cfgIdList = new ArrayList<>();


        paramMap.put("tplIds", Arrays.asList(oldTplId));

        SSDQueryTemplate oldTpl = queryTemplateById(oldTplId);

        Map<String, String> viewIdMap = new HashMap<>();
        Map<String, String> cfgIdMap = new HashMap<>();
        List<TemplateViewEntity> newViewList = new ArrayList<>();
        for (TemplateViewEntity viewEntity : viewList) {
            if (viewEntity.getViewId().equals(excludeViewId)) {
                continue;
            }

            TemplateViewEntity newViewEntity = new TemplateViewEntity();
            String viewId = Guid.id();
            newViewEntity.setViewId(viewId);
            newViewEntity.setViewName(viewEntity.getViewName());
            newViewEntity.setViewType(viewEntity.getViewType());
            newViewEntity.setViewStatus(viewEntity.getViewStatus());
            newViewEntity.setTplId(newTplId);
            newViewEntity.setSortId(viewEntity.getSortId());
            newViewEntity.setAssetExpiresTime(viewEntity.getAssetExpiresTime());
            newViewEntity.setDatasetId(StrUtil.isNotEmpty(viewEntity.getDatasetId()) ? viewEntity.getDatasetId() : oldTpl.getDatasetId());

            viewIdMap.put(viewEntity.getViewId(), viewId);

            String cfgId = Guid.id();
            newViewEntity.setCfgId(cfgId);
            cfgIdMap.put(viewEntity.getCfgId(), cfgId);

            cfgIdList.add(viewEntity.getCfgId());
            cfgIdCaseWhenSqlList.add(String.format(" when cfg_id = '%s' then '%s' ", viewEntity.getCfgId(), cfgId));

            newViewEntity.setIsActive(viewEntity.getIsActive());
            newViewEntity.setCreatedBy(userName);

            newViewList.add(newViewEntity);

            //设置新的默认视图
            if (StrUtil.isNotEmpty(oldDefaultViewId) && viewEntity.getViewId().equals(oldDefaultViewId)) {
                newDefaultViewId = viewId;
            }

            TemplateViewMapping viewIdMapping = new TemplateViewMapping(viewEntity.getViewId(), viewId);
            viewIdMappingList.add(viewIdMapping);

        }

        // 模版的跳转配置数据同步处理
        List<TemplateLinkRsp> linkToOtherTemplateList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryLinkToOtherTemplateList", paramMap);
        TemplateLinkService templateLinkService = (TemplateLinkService) SpringContextUtil.getBean("templateLinkService");
        templateLinkService.buildTemplateLinkField(linkToOtherTemplateList);

        List<TemplateLinkRsp> linkList = new ArrayList<>();
        for (TemplateLinkRsp linkItem : linkToOtherTemplateList) {

            String viewId = viewIdMap.get(linkItem.getQueryViewId());
            if (StrUtil.isEmpty(viewId)) {
                continue;
            }

            linkItem.setQueryTplId(newTplId);
            linkItem.setQueryViewId(viewId);
            linkList.add(linkItem);
        }

        List<TemplateLinkEntity> templateLinkEntityList = new ArrayList<>();
        List<TemplateLinkEntity> tplLinkList = templateLinkService.buildLinkEntityList(linkList);
        templateLinkEntityList.addAll(tplLinkList);

        String finalNewDefaultViewId = newDefaultViewId;
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("viewList", newViewList);
                dao.insert("ssm.template.view.bacthAdd", paramMap);

                if (CollUtil.isNotEmpty(cfgIdList)) {
                    String cfgCaseWhenSql = String.format(" case %s else cfg_id end  ", BIUtil.listToStr(cfgIdCaseWhenSqlList, " "));
                    paramMap.put("cfgIdCaseWhenSql", cfgCaseWhenSql);
                    paramMap.put("cfgIdList", cfgIdList);

                    // 正式 cfg/dtl 只从正式表拷贝，不读影子
                    dao.insert("ssm.template.insertIntoTemplateCfg", paramMap);
                    dao.insert("ssm.query.template.cfg.dtl.insertIntoTemplateCfgDtl", paramMap);

                    // UT 且源视图有影子时，为新视图再插一条影子（新 view_id / cfg_id / shadow_cfg_id）
                    if (metricExpansionShadowService.isUtEnv()) {
                        for (TemplateViewEntity oldView : viewList) {
                            if (oldView == null || StrUtil.isEmpty(oldView.getViewId())
                                    || oldView.getViewId().equals(excludeViewId)) {
                                continue;
                            }
                            String newViewId = viewIdMap.get(oldView.getViewId());
                            String newCfgId = cfgIdMap.get(oldView.getCfgId());
                            if (StrUtil.isEmpty(newViewId) || StrUtil.isEmpty(newCfgId)) {
                                continue;
                            }
                            metricExpansionShadowService.copyShadowIfPresent(
                                    oldView.getCfgId(), newCfgId, newViewId, newTplId);
                        }
                    }
                }

                if (CollectionUtil.isNotEmpty(templateLinkEntityList)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("templateLinkEntityList", templateLinkEntityList);
                    dao.insert("ssm.template.link.batchInsertTemplateLink", map);
                    templateLinkService.saveTemplateLinkFiled(templateLinkEntityList);
                }

                //处理默认视图相关
                if (StrUtil.isNotEmpty(finalNewDefaultViewId)) {
                    TemplateViewEntity defaultViewEntity = new TemplateViewEntity();
                    defaultViewEntity.setViewId(finalNewDefaultViewId);
                    defaultViewEntity.setTplId(newTplId);
                    defaultViewEntity.setCreatedBy(userName);
                    defaultViewEntity.setIsDefault(Enabled.YES.getId());
                    saveViewUserCfg(defaultViewEntity);
                }

            }
        });

        return viewIdMappingList;
    }

    /**
     * 使新老视图排序一致
     * @param oldViewId
     * @param newViewId
     */
    public void syncViewSorting(String oldViewId,String newViewId) {
        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", oldViewId);

        if (templateViewEntity == null) {
            return;
        }

        TemplateViewEntity viewEntity = new TemplateViewEntity();
        viewEntity.setViewId(newViewId);
        viewEntity.setSortId(templateViewEntity.getSortId());
        viewEntity.setUpdatedBy(UserManager.get().getName());
        dao.update("ssm.template.view.updateViewSortId", viewEntity);
    }

    /**
     * 通过模板ID获取默认视图ID
     * @param tplId
     * @return
     */
    public String getDefaultViewIdByTplId(String tplId) {
        Map<String, Object> paramMap = new HashMap<>();
        //查询用户的默认视图配置信息
        paramMap.put("tplId", tplId);
        paramMap.put("userName", UserManager.get().getName());
        String defaultViewId = (String) dao.queryObject("ssm.template.view.user.cfg.queryUserDefaultView", paramMap);
        return defaultViewId;
    }

    /**
     * 通过模板ID获取第一个有效的视图ID
     */
    public String getFirstValidViewIdByTplId(String tplId) {
        String viewId = (String) dao.queryObject("ssm.template.view.getFirstValidViewIdByTplId", tplId);
        return viewId;
    }

    /**
     * 获取当前视图信息（含 tplConfig）。
     * UT 环境若该 cfg 存在指标膨胀影子，则返回影子 tplConfig，正式库不变。
     *
     * @param viewId 视图 id
     * @return 视图实体；不存在时返回 null
     */
    public TemplateViewEntity getByViewId(String viewId) {
        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);

        //获取模版配置
        if (templateViewEntity != null) {
            String tplConfig = (String) dao.queryObject("ssm.template.cfg.getById", templateViewEntity.getCfgId());
            // UT：有影子则用影子配置，供页面正式读路径看到膨胀结果
            String shadowTplConfig = metricExpansionShadowService.getShadowTplConfigForUtRead(
                    templateViewEntity.getCfgId());
            if (StrUtil.isNotEmpty(shadowTplConfig)) {
                tplConfig = shadowTplConfig;
            }
            templateViewEntity.setTplConfig(tplConfig);
        }

        return templateViewEntity;
    }

    /**
     * 获取当前视图的模版ID
     * @param viewId
     * @return
     */
    public String getTplIdByViewId(String viewId) {
        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);
        if (templateViewEntity == null) {
            throw new RuntimeException("viewId:" + viewId + " 不存在");
        }
        return templateViewEntity.getTplId();
    }

    /**
     * 获取模版配置扩展信息(取有效的任一个)。
     * UT 且传入 cfgId 存在影子 dtl 时，优先返回影子明细。
     *
     * @param tplId 模板 id
     * @param cfgId 当前视图 cfg id，可空
     * @return 配置明细
     */
    public TemplateCfgDtlEntity getCfgDtlByTplId(String tplId, String cfgId) {
        if (StrUtil.isNotEmpty(cfgId)) {
            TemplateCfgDtlEntity shadowDtl = metricExpansionShadowService.getShadowDtlForUtRead(cfgId);
            if (shadowDtl != null) {
                return shadowDtl;
            }
        }
        return (TemplateCfgDtlEntity) dao.queryObject("ssm.query.template.cfg.dtl.getByTplId", tplId);
    }

    /**
     * 获取模版配置扩展信息(取有效的任一个)
     * @param tplId
     * @return
     */
    public TemplateCfgDtlEntity getCfgDtlByTplId(String tplId) {
        return getCfgDtlByTplId(tplId, null);
    }

    /**
     * 根据模版配置编码信息
     * 资产不一致的视图 -> 失效
     * 资产一致的视图 -> 生效
     */
    public void setViewDisabled(String tplId, String viewId,TemplateCfgDtlEntity tplCfgDtlEntity) {

        //查询模版下视图的编码信息
        List<TemplateCfgDtlEntity> viewCfgList = (List<TemplateCfgDtlEntity>) dao.queryObjectList("ssm.query.template.cfg.dtl.queryByTplId", tplId);

        if (CollUtil.isEmpty(viewCfgList)) {
            return;
        }

        List<String> disabledViewIds = new ArrayList<>();
        List<String> enabledViewIds = new ArrayList<>();
        for (TemplateCfgDtlEntity viewCfg : viewCfgList) {

            if (viewId.equalsIgnoreCase(viewCfg.getViewId())) {
                continue;
            }

            // 数据集不一致，直接判定失效，不再比较资产
            if (!StrUtil.equalsIgnoreCase(viewCfg.getDatasetId(), tplCfgDtlEntity.getDatasetId())) {
                disabledViewIds.add(viewCfg.getViewId());
                continue;
            }

            //比较维度资产
            String dimAssets = tplCfgDtlEntity.getTplConfigFieldDimAsset();
            if (!compareFieldAsset(viewCfg.getTplConfigFieldDimAsset(), dimAssets)) {
                disabledViewIds.add(viewCfg.getViewId());
                continue;
            }

            //比较指标资产
            String measureAssets = tplCfgDtlEntity.getTplConfigFieldMeasureAsset();
            if (!compareFieldAsset(viewCfg.getTplConfigFieldMeasureAsset(), measureAssets)) {
                disabledViewIds.add(viewCfg.getViewId());
                continue;
            }

            enabledViewIds.add(viewCfg.getViewId());

        }

        String userName = UserManager.get().getName();

        //将模版设置为失效
        if (CollUtil.isNotEmpty(disabledViewIds)) {
            Map<String, Object> params = new HashMap<>();
            params.put("viewIds", disabledViewIds);
            params.put("userName", userName);
            dao.update("ssm.template.view.batchDisabled", params);
        }

        //将模版设置为有效
        if (CollUtil.isNotEmpty(enabledViewIds)) {
            Map<String, Object> params = new HashMap<>();
            params.put("viewIds", enabledViewIds);
            params.put("userName", userName);
            dao.update("ssm.template.view.batchEnabled", params);

            //查询待审批的模板视图
            TemplateAssetChangeApplyService templateAssetChangeApplyService = (TemplateAssetChangeApplyService)SpringContextUtil.getBean("templateAssetChangeApplyService");
            List<TemplateAssetChangeApplyRsp> templateAssetChangeApplyList = templateAssetChangeApplyService.listByTplId(tplId);

            //审批中的视图也需要校验：如果审批中视图和资产一致，此申请完结
            if(CollUtil.isNotEmpty(templateAssetChangeApplyList)) {
                for (TemplateAssetChangeApplyRsp templateAssetChangeApply : templateAssetChangeApplyList) {
                    if (!enabledViewIds.contains(templateAssetChangeApply.getViewId())) {
                        continue;
                    }

                    TemplateAssetChangeApplyEntity templateAssetChangeApplyEntity = new TemplateAssetChangeApplyEntity();
                    templateAssetChangeApplyEntity.setApplyId(templateAssetChangeApply.getApplyId());
                    templateAssetChangeApplyEntity.setViewId(templateAssetChangeApply.getViewId());
                    templateAssetChangeApplyEntity.setTplId(templateAssetChangeApply.getTplId());
                    templateAssetChangeApplyEntity.setApprovedBy(userName);
                    templateAssetChangeApplyEntity.setCreatedBy(templateAssetChangeApply.getApplyUser());

                    templateAssetChangeApplyService.sysAutoApproval(templateAssetChangeApplyEntity);
                }
            }

        }

    }

    /**
     * 比较字段资产
     * @return
     */
    public boolean compareFieldAsset(String asset1,String asset2) {

        List<String> assetIdentifier1 = buildAssetIdentifier(asset1);
        List<String> assetIdentifier2 = buildAssetIdentifier(asset2);

        boolean isEqual = BIUtil.isListEqualWithoutSort(assetIdentifier1, assetIdentifier2);
        return isEqual;
    }

    /**
     * 资产存储格式
     * [
     *     {
     *         "type": "common",
     *         "name": "省份",
     *         "assetIdentifier": "AAE"
     *     },
     *     {
     *         "type": "calc",
     *         "name": "计算维度名称",
     *         "assetIdentifier": "case when [AAE] = '上海市' then '1' else '0' end"
     *     }
     * ]
     */
    public List<String> buildAssetIdentifier(String assets) {
        List<String> assetIdentifier = new ArrayList<>();
        if (StrUtil.isNotEmpty(assets)) {
            List<TemplateViewFieldAssetEntity> assetList = JSON.parseArray(assets, TemplateViewFieldAssetEntity.class);
            if (CollUtil.isNotEmpty(assetList)) {
                for (TemplateViewFieldAssetEntity asset : assetList) {
                    if (StrUtil.isEmpty(asset.getAssetIdentifier())) {
                        continue;
                    }
                    assetIdentifier.add(asset.getAssetIdentifier());
                }
            }
        }
        return assetIdentifier;
    }

    /**
     * 保存视图用户配置信息
     * @param templateViewEntity
     */
    public void saveViewUserCfg(TemplateViewEntity templateViewEntity) {
        //先删除
        dao.delete("ssm.template.view.user.cfg.delete", templateViewEntity);
        dao.insert("ssm.template.view.user.cfg.add", templateViewEntity);
    }

    /**
     * 视图修改排序
     * @param viewId
     * @param targetViewId
     * @param dragType
     */
    public void move(String viewId,String targetViewId,String dragType){

        TemplateViewEntity templateView = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);

        SSDQueryTemplate tpl = queryTemplateById(templateView.getTplId());
        checkViewAuth(tpl,templateView,"移动");

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                TemplateViewEntity targetTemplateView = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", targetViewId);

                DragType dgType = DragType.get(dragType);
                if (DragType.AFTER == dgType) {
                    templateView.setSortId(targetTemplateView.getSortId() + 1);
                } else {
                    templateView.setSortId(targetTemplateView.getSortId());
                }

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("tplId", templateView.getTplId());
                paramMap.put("sortId", targetTemplateView.getSortId());
                paramMap.put("dragType",dgType.getCode());

                String userName = UserManager.get().getName();
                paramMap.put("userName",userName);

                //将同一层级，大于移动节点的排序+1
                templateView.setUpdatedBy(userName);
                dao.update("ssm.template.view.incrementViewSortId", paramMap);
                dao.update("ssm.template.view.updateViewSortId", templateView);
            }
        });
    }

    /**
     * 获取模版基础信息
     * @param templateId
     * @return
     */
    public SSDQueryTemplate queryTemplateById(String templateId) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("templateId", templateId);
        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);

        if (tpl == null) {
            throw new SSDException("视图所属的模版不存在，无法删除！");
        }

        return tpl;
    }

    /**
     * 删除视图
     * @param viewId
     */
    public void delete(String viewId){
        TemplateViewEntity templateView = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);

        if (templateView == null) {
            throw new SSDException("该视图不存在，无法删除！");
        }

        SSDQueryTemplate tpl = queryTemplateById(templateView.getTplId());

        checkViewAuth(tpl,templateView,"删除");
        checkViewApplyStatus(templateView,"删除");

        List<TemplateViewEntity> templateViewEntityList = getByTplId(templateView.getTplId());

        //个人视图不处理
        //公共视图至少保留一个有效视图
        TemplateViewType viewType = TemplateViewType.get(templateView.getViewType());
        if(TemplateViewType.PUBLIC == viewType ){
            checkActiveViewCount(templateViewEntityList, viewId);
        }

        //将模版默认视图 = 删除视图的配置修改。
        //默认视图设置为第一个有效视图
        String newDefaultViewId = "";
            for (TemplateViewEntity tv : templateViewEntityList) {
                if (tv.getViewId().equalsIgnoreCase(templateView.getViewId())) {
                    continue;
                }

                if (Enabled.value(tv.getIsActive())) {
                    newDefaultViewId = tv.getViewId();
                    break;
                }
            }


        String finalNewDefaultViewId = newDefaultViewId;
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                //删除视图
                dao.delete("ssm.template.view.delete", viewId);
                //删除视图用户配置
                dao.delete("ssm.template.cfg.delete", templateView.getCfgId());
                //删除视图配置扩展信息
                dao.delete("ssm.query.template.cfg.dtl.delete", templateView.getCfgId());

                Map<String, Object> params = new HashMap<>();
                //删除模版调转信息
                List<String> linkIdList = (List<String>) dao.queryObjectList("ssm.template.link.queryLinkidByViewId", viewId);
                if (CollUtil.isNotEmpty(linkIdList)) {
                    params.put("linkIdList", linkIdList);
                    dao.delete("ssm.template.link.batchDeleteByLinkId", params);
                    dao.delete("ssm.template.link.field.batchDeleteByLinkId", params);
                }

                //默认视图=删除视图时，变更为默认视图=第一个有效视图
                if(StrUtil.isNotEmpty(finalNewDefaultViewId)){

                    params.put("oldDefaultViewId", templateView.getViewId());
                    params.put("defaultViewId", finalNewDefaultViewId);
                    params.put("tplId",templateView.getTplId());

                    dao.update("ssm.template.view.user.cfg.updateDefaultViewId", params);

                }

                //删除公共视图，设置模版更新时间
                if(TemplateViewType.PUBLIC == viewType ){
                    updateTemplateUpdatedTime(templateView.getTplId());
                }

            }
        });

    }

    /**
     * 系统级物理删除视图（供内容治理清理 Job 调用），自带事务。
     * 与 {@link #delete(String)} 的清理逻辑一致，但<strong>跳过操作人权限校验</strong>，
     * 仅用于已下线（is_active=0）且保护期满的视图，由系统按治理策略发起。
     * @param viewId 视图ID
     */
    public void deleteBySystem(String viewId) {
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                deleteBySystemNoTx(viewId);
            }
        });
    }

    /**
     * 系统级物理删除视图的清理逻辑（<strong>不开启事务</strong>），供已处于事务上下文的调用方复用，
     * 以便与调用方自身的写操作合并到同一事务中。独立调用请使用 {@link #deleteBySystem(String)}。
     * @param viewId 视图ID
     */
    @SuppressWarnings("unchecked")
    public void deleteBySystemNoTx(String viewId) {
        TemplateViewEntity templateView = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);
        if (templateView == null) {
            // 视图已不存在，幂等返回
            return;
        }

        List<TemplateViewEntity> templateViewEntityList = getByTplId(templateView.getTplId());
        TemplateViewType viewType = TemplateViewType.get(templateView.getViewType());

        // 默认视图重新指向第一个有效视图
        String finalNewDefaultViewId = "";
        for (TemplateViewEntity tv : templateViewEntityList) {
            if (tv.getViewId().equalsIgnoreCase(templateView.getViewId())) {
                continue;
            }
            if (Enabled.value(tv.getIsActive())) {
                finalNewDefaultViewId = tv.getViewId();
                break;
            }
        }

        //删除视图
        dao.delete("ssm.template.view.delete", viewId);
        //删除视图用户配置
        dao.delete("ssm.template.cfg.delete", templateView.getCfgId());
        //删除视图配置扩展信息
        dao.delete("ssm.query.template.cfg.dtl.delete", templateView.getCfgId());

        Map<String, Object> params = new HashMap<>();
        //删除模版跳转信息
        List<String> linkIdList = (List<String>) dao.queryObjectList("ssm.template.link.queryLinkidByViewId", viewId);
        if (CollUtil.isNotEmpty(linkIdList)) {
            params.put("linkIdList", linkIdList);
            dao.delete("ssm.template.link.batchDeleteByLinkId", params);
            dao.delete("ssm.template.link.field.batchDeleteByLinkId", params);
        }

        //默认视图=删除视图时，变更为默认视图=第一个有效视图
        if (StrUtil.isNotEmpty(finalNewDefaultViewId)) {
            params.put("oldDefaultViewId", templateView.getViewId());
            params.put("defaultViewId", finalNewDefaultViewId);
            params.put("tplId", templateView.getTplId());
            dao.update("ssm.template.view.user.cfg.updateDefaultViewId", params);
        }

        //删除公共视图，设置模版更新时间
        if (TemplateViewType.PUBLIC == viewType) {
            updateTemplateUpdatedTime(templateView.getTplId());
        }
    }

    /**
     * 系统级重建/上线视图（供内容治理回滚调用），<strong>不开启事务</strong>，由调用方的事务统一提交。
     * 删除视图时仅删了视图主行、未删 cfg，故此处只需重建视图主行（cfg 仍在）；视图仍存在时仅确保上线。幂等。
     * @param entity 待重建的视图实体（来自治理任务字段）
     */
    public void restoreViewNoTx(TemplateViewEntity entity) {
        if (entity == null || StrUtil.isBlank(entity.getViewId())) {
            return;
        }
        entity.setIsActive(Enabled.YES.getId());
        TemplateViewEntity exist = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", entity.getViewId());
        if (exist != null) {
            // 视图仍在，仅确保重新上线
            dao.update("ssm.template.view.updateViewActive", entity);
            return;
        }
        // 仅重建视图主行（cfg 删除时已保留，无需重插）
        dao.insert("ssm.template.view.add", entity);
    }


    /**
     * 修改视图类型
     * 只有模版owner有权限
     * 排序放在最后
     * @param viewId
     * @param viewType
     */
    public void changeViewType(String viewId,String viewType) {

        User user = UserManager.get();
        TemplateViewEntity templateViewEntity = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);
        SSDQueryTemplate tpl = queryTemplateById(templateViewEntity.getTplId());

        boolean isOwner = (tpl.getTplOwner() + ",").contains(user.getName() + ",");
        if (!Enabled.value(tpl.getIsSpaceTpl())) {
            if (!isOwner) {
                throw new SSDException("您没有权限转换该视图类型！");
            }
        }else{
            TemplateConfigService configService = (TemplateConfigService) SpringContextUtil.getBean("templateConfigService");
            boolean hasAuth = configService.checkUserCtgAuth(tpl.getCtgId());
            if (!hasAuth && !isOwner) {
                throw new SSDException("您没有权限转换该视图类型！");
            }
        }

        //转化为个人视图前，至少需要保留一个有效视图
        if(TemplateViewType.PERSONAL == TemplateViewType.get(viewType)){
            List<TemplateViewEntity> templateViewEntityList = getByTplId(templateViewEntity.getTplId());
            checkActiveViewCount(templateViewEntityList, viewId);
        }

        checkViewApplyStatus(templateViewEntity,"转换视图类型");

        templateViewEntity.setViewType(viewType);

        //查询排序
        Double maxSortId = (Double) dao.queryObject("ssm.template.view.queryMaxSortIdByTplIdAndViewType", templateViewEntity);
        templateViewEntity.setSortId(maxSortId + 1);
        templateViewEntity.setUpdatedBy(user.getName());

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.update("ssm.template.view.updateViewType", templateViewEntity);
                dao.update("ssm.template.view.updateViewSortId", templateViewEntity);
            }

        });

    }

    /**
     * 复制视图
     * @param viewId
     * @return
     */
    public String copyViewById(String viewId) {
        TemplateViewEntity templateViewEntity = getByViewId(viewId);

        SSDQueryTemplate tpl = queryTemplateById(templateViewEntity.getTplId());
        checkViewAuth(tpl, templateViewEntity, "拷贝");

        return copyViewByViewEntity(templateViewEntity);
    }

    /**
     * 复制视图
     * @param templateViewEntity
     */
    public String copyViewByViewEntity( TemplateViewEntity templateViewEntity){

        String viewId = templateViewEntity.getViewId();

        User user = UserManager.get();
        String newViewId = Guid.id();
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                String newCfgId = Guid.id();

                //1.保存视图
                TemplateViewEntity newTemplateView = new TemplateViewEntity();
                newTemplateView.setViewId(newViewId);
                newTemplateView.setViewName(templateViewEntity.getViewName()+"_副本");
                newTemplateView.setViewType(templateViewEntity.getViewType());
                newTemplateView.setViewStatus(templateViewEntity.getViewStatus());
                newTemplateView.setTplId(templateViewEntity.getTplId());
                newTemplateView.setCfgId(newCfgId);
                newTemplateView.setSortId(templateViewEntity.getSortId()+1);
                newTemplateView.setIsActive(templateViewEntity.getIsActive());
                newTemplateView.setCreatedBy(user.getName());
                newTemplateView.setAssetExpiresTime(templateViewEntity.getAssetExpiresTime());
                String datasetId = templateViewEntity.getDatasetId();
                if (StrUtil.isEmpty(datasetId)) {
                    SSDQueryTemplate tpl = queryTemplateById(templateViewEntity.getTplId());
                    datasetId = tpl != null ? tpl.getDatasetId() : null;
                }
                newTemplateView.setDatasetId(datasetId);

                //修改排序，将被复制的视图后面的视图下移
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("tplId", templateViewEntity.getTplId());
                paramMap.put("userName", user.getName());
                paramMap.put("sortId", templateViewEntity.getSortId());
                paramMap.put("dragType",DragType.AFTER.getCode());
                dao.update("ssm.template.view.incrementViewSortId", paramMap);

                dao.insert("ssm.template.view.add", newTemplateView);

                //保存视图配置信息
                TemplateCfgEntity cfgEntity = new TemplateCfgEntity(newCfgId, templateViewEntity.getTplConfig());
                dao.insert("ssm.template.saveTemplateCfg", cfgEntity);

                //保存视图配置扩展信息
                TemplateCfgDtlEntity cfgDtlEntity = (TemplateCfgDtlEntity)dao.queryObject("ssm.query.template.cfg.dtl.queryByViewId",viewId);
                cfgDtlEntity.setCfgId(newCfgId);
                dao.insert("ssm.query.template.cfg.dtl.add", cfgDtlEntity);

                //保存跳转信息
                paramMap.put("viewId",viewId);
                paramMap.put("tplIds", Arrays.asList(templateViewEntity.getTplId()));
                List<TemplateLinkRsp> linkList = (List<TemplateLinkRsp>) dao.queryObjectList("ssm.template.link.queryLinkToOtherTemplateList", paramMap);
                TemplateLinkService templateLinkService = (TemplateLinkService)SpringContextUtil.getBean("templateLinkService");
                templateLinkService.buildTemplateLinkField(linkList);
                //构造跳转信息
                List<TemplateLinkAddReq> linkAddReqList = templateLinkService.buildTemplateLinkAddReq(linkList);
                linkService.saveTemplateLink(templateViewEntity.getTplId(),newViewId, linkAddReqList);
            }
        });

        return newViewId;
    }


    /**
     * 检查视图权限
     * 公共视图 = 模版owner有权限
     * 个人视图 = 视图owner有权限
     */
    public void checkViewAuth(SSDQueryTemplate tpl, TemplateViewEntity templateView,String operatorName) {

        User user = UserManager.get();
        TemplateViewType viewType = TemplateViewType.get(templateView.getViewType());

        // 特定创建人新建模板时，自动追加配置的 owner
        List<String> autoAppendOwners = resolveAutoAppendOwners(tpl.getCreatedBy());
        if (autoAppendOwners.contains(user.getName())) {
           return;
        }

        if (TemplateViewType.PUBLIC == viewType) {

            //2025-11-12修改权限校验逻辑
            //1. 私域的模板，判断owner
            //2. 共享空间模版，判断owner+共享空间的管理员

            if ((tpl.getTplOwner() + ",").contains(user.getName() + ",")) {
               return;
            }

            if(Enabled.value(tpl.getIsSpaceTpl())){
                TemplateConfigService configService = (TemplateConfigService) SpringContextUtil.getBean("templateConfigService");
                boolean hasAuth = configService.checkUserCtgAuth(tpl.getCtgId());
                if (hasAuth) {
                    return;
                }
            }

            throw new SSDException("您没有权限" + operatorName + "该公共视图！");


        }

        if (TemplateViewType.PERSONAL == viewType) {
            //只有视图owner 才能操作
            if (templateView.getCreatedBy().equalsIgnoreCase(user.getName())) {
               return;
            }

            throw new SSDException("您没有权限" + operatorName + "该个人视图！");
        }

    }

    /**
     * 检查模版下，有效视图数量，至少需要保留一个
     * 只校验公共视图
     * @param viewId
     */
    public void checkActiveViewCount(List<TemplateViewEntity> templateViewEntityList,String viewId){

        Integer activeCount = 0;
        for(TemplateViewEntity templateViewEntity : templateViewEntityList){

            if(templateViewEntity.getViewId().equalsIgnoreCase(viewId)){
                continue;
            }

            TemplateViewType viewType = TemplateViewType.get(templateViewEntity.getViewType());
            if(TemplateViewType.PERSONAL == viewType){
                continue;
            }

            if(Enabled.value(templateViewEntity.getIsActive())){
                activeCount++;
            }
        }

        if(activeCount == 0){
            throw new SSDException("模版下至少需要保留一个有效的公共视图！");
        }
    }

    /**
     * 保存模版配置扩展信息
     * @param req
     */
    public void saveTemplateCfgDtl(TemplateCfgDtlSaveReq req) {

        String viewId = req.getViewId();
        TemplateViewEntity templateView = (TemplateViewEntity) dao.queryObject("ssm.template.view.getByViewId", viewId);

        if (templateView == null) {
            return;
        }


        if (StrUtil.isEmpty(req.getTplConfigFieldDimAsset()) || StrUtil.isEmpty(req.getTplConfigFieldMeasureAsset())) {
            throw new SSDException("多维前端有更新，请刷新页面后重试！");
        }

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.query.template.cfg.dtl.delete", templateView.getCfgId());

                TemplateCfgDtlEntity templateCfgDtlEntity = new TemplateCfgDtlEntity(templateView.getCfgId(), req.getTplConfigFieldDimCodes(), req.getTplConfigFieldMeasureCodes());
                templateCfgDtlEntity.setTplConfigFieldDimAsset(SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldDimAsset()));
                templateCfgDtlEntity.setTplConfigFieldMeasureAsset(SSDUtil.decryptTplConfigFieldAsset(req.getTplConfigFieldMeasureAsset()));
                dao.insert("ssm.query.template.cfg.dtl.add", templateCfgDtlEntity);

                //更新视图状态
                if (req.getIsViewValid() != null) {
                    templateView.setIsActive(req.getIsViewValid());
                    templateView.setUpdatedBy(UserManager.get().getName());
                    dao.update("ssm.template.view.updateViewActive", templateView);
                }
            }
        });

    }

    /**
     * 更新模版更新时间,便于在门户工作台感知模版已变更
     */
    public void updateTemplateUpdatedTime(String tplId) {
        String username = UserManager.get().getName();
        Map<String,String> params = new HashMap<>();
        params.put("tplId",tplId);
        params.put("userName",username);
        dao.update("ssm.template.updateTemplateUpdatedTime",params);
    }

    /**
     * 有效公共视图变更数据集时联动更新模板表 datasetId，否则仅刷新模板更新时间
     *
     * @param viewEntity 当前保存的视图
     * @param req        视图保存请求
     * @param tpl        模板基础信息
     */
    private void syncTemplateDatasetOnViewSave(TemplateViewEntity viewEntity, TemplateViewSaveReq req, SSDQueryTemplate tpl) {
        boolean isValidPublicView = TemplateViewType.PUBLIC == TemplateViewType.get(viewEntity.getViewType())
                && ViewStatusType.ACTIVE == ViewStatusType.getByCode(viewEntity.getViewStatus());
        if (isValidPublicView && StrUtil.isNotEmpty(req.getDatasetId()) && !tpl.getDatasetId().equalsIgnoreCase(req.getDatasetId())) {
            updateTemplateDatasetId(req.getTplId(), req.getDatasetId());
        } else {
            updateTemplateUpdatedTime(req.getTplId());
        }
    }

    /**
     * 更新模版数据集ID
     * @param tplId
     * @param datasetId
     */
    public void updateTemplateDatasetId(String tplId,String datasetId) {
        String username = UserManager.get().getName();
        Map<String,String> params = new HashMap<>();
        params.put("tplId",tplId);
        params.put("datasetId",datasetId);
        params.put("userName",username);
        dao.update("ssm.template.updateTemplateDatasetId",params);
    }

    /**
     * 获取模版指标列表
     * @param req
     * @return
     */
    public List<TemplateMetricRsp> getTemplateMetricList(TemplateViewReq req) {

        TemplateViewEntity templateView = getByViewId(req.getViewId());

        if (templateView == null) {
            throw new SSDException("视图不存在！");
        }

        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("templateId", templateView.getTplId());

        //查询模版基础信息
        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);

        //获取有分析思路的指标
        List<String> measureCodesWithWfConfig = (List<String>)dao.queryObjectList("llm.metric.getMetricCodeWithWfConfig",null,DataSourceType.AGENT);

        List<TemplateMetricRsp> result = new ArrayList<>();
        SSDQueryTemplate queryTemplate = new SSDQueryTemplate();

        Map<String, MetaField> fieldCodeMap = new HashMap<>();
        Map<String, MetaField> fieldIdMap = new HashMap<>();
        QueryFieldService queryFieldService = (QueryFieldService) SpringContextUtil.getBean("queryFieldService");
        String datasetId = StrUtil.isNotEmpty(templateView.getDatasetId()) ? templateView.getDatasetId() : tpl.getDatasetId();
        List treeNodes = queryFieldService.buildFieldTree(CategoryType.Front, datasetId);
        SSDUtil.fetchFieldFromTreeNodes(treeNodes, fieldCodeMap,fieldIdMap);

        String config = SSDUtil.normalizeConfig(templateView.getTplConfig(), fieldCodeMap,fieldIdMap);
        queryTemplate.setConfig(config);

        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();

        List<QueryField> measures = queryConfigure.getResult().getMeasures();
        for (QueryField measure : measures) {

            if (measure.isAppend()) {
                continue;
            }

            //不显示不处理
            if (!Enabled.value(measure.getIsShow())) {
                continue;
            }

            //前端计算字段不处理，排除lod和跨模型指标
            FieldType fieldType = FieldType.get(measure.getFieldType());
            if (!measure.getCustomFieldConfigure().isEmpty() && !measure.isLodField() && FieldType.CROSS_MODEL_MEASURE != fieldType) {
                continue;
            }

            String fieldId = measure.getId();
            //lod从lod的配置中获取字段id
            if (measure.isLodField()) {
                fieldId = measure.getCustomFieldConfigure().getLodConfig().getMeasureId();
            }

            MetaField metaField = SSDMetaCacheManager.getField(fieldId);
            if (metaField == null) {
                continue;
            }

            //获取含有分析思路的指标
            if (!measureCodesWithWfConfig.contains(metaField.getKpiNo())) {
                continue;
            }

            TemplateMetricRsp templateMetricRsp = new TemplateMetricRsp();
            templateMetricRsp.setMetricCode(metaField.getKpiNo());
            templateMetricRsp.setMetricName(metaField.getKpiName());

            result.add(templateMetricRsp);
        }

        return result;
    }

    /**
     * 解析视图对应的所有事实表/维表 MetaTable，供 getEtlJob / saveEtlJob 复用，避免重复查询。
     */
    private List<TemplateViewEtlJobResp> resolveMetaTables(TemplateViewEntity viewEntity) {
        if (viewEntity == null || StrUtil.isBlank(viewEntity.getTplConfig())) {
            return Collections.emptyList();
        }
        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById",
                Collections.singletonMap("templateId", viewEntity.getTplId()));
        if (tpl == null) {
            return Collections.emptyList();
        }

        String datasetId = StrUtil.isNotEmpty(viewEntity.getDatasetId()) ? viewEntity.getDatasetId() : tpl.getDatasetId();

        return SSDUtil.getEtlJobByConfig(
                        Collections.singletonList(viewEntity.getTplConfig()),
                        datasetId,
                        Enabled.YES.getId()).stream()
                .map(t -> toEtlJobResp(viewEntity, t, datasetId))
                .collect(Collectors.toList());
    }

    /**
     * 将 MetaTable 转换为前端出参 VO。
     */
    private TemplateViewEtlJobResp toEtlJobResp(TemplateViewEntity viewEntity, MetaTable t, String datasetId) {
        TemplateViewEtlJobResp resp = new TemplateViewEtlJobResp();
        resp.setTplId(viewEntity.getTplId());
        resp.setViewId(viewEntity.getViewId());
        resp.setTableName(t.getFullName());
        resp.setTableDesc(t.getTableDesc());
        resp.setTableOwner(t.getTableOwner());
        resp.setTableType(Enabled.value(t.getIsFactTable()) ? "factTable" : "dimTable");
        if (DataTypeEnum.OFFLINE.getCode().equals(t.getDataProcessType())) {
            resp.setDataType("offline");
        } else {
            if (SSDUtil.isNearRealTimeDataset(datasetId)) {
                resp.setDataType("nearRealTime");
            } else {
                MetaDataset dataset = SSDMetaCacheManager.getDataset(datasetId);
                if (dataset != null && DataTypeEnum.REAL_TIME.getCode().equals(dataset.getDatasetType())) {
                    resp.setDataType("realTime");
                } else {
                    resp.setDataType("offline");
                }
            }
        }
        resp.setEtlJobs(t.getEtlJobs());
        return resp;
    }

    /**
     * 将视图 ID 列表解析为带 viewId/tplId 的 TemplateViewEtlJobResp 列表，供 saveEtlJob 使用。
     */
    public List<TemplateViewEtlJobResp> buildEtlJobRespForSave(List<String> viewIdList) {
        List<TemplateViewEtlJobResp> result = new ArrayList<>();
        for (String viewId : viewIdList) {
            TemplateViewEntity viewEntity = getByViewId(viewId.trim());
            if (viewEntity == null) {
                continue;
            }
            for (TemplateViewEtlJobResp resp : resolveMetaTables(viewEntity)) {
                resp.setViewId(viewEntity.getViewId());
                resp.setTplId(viewEntity.getTplId());
                result.add(resp);
            }
        }
        return result;
    }

    /**
     * 保存模版视图的etl作业信息。
     * 批量删除所有涉及视图的旧数据，再批量插入新数据，整体一个事务，无循环 DB 调用。
     */
    public void saveEtlJob(List<String> viewIds, List<TemplateViewEtlJobResp> respList) {
        if (CollUtil.isEmpty(viewIds)) {
            return;
        }
        String username = UserManager.get().getName();
        List<Map<String, String>> insertRows = new ArrayList<>();
        if (CollUtil.isNotEmpty(respList)) {
            for (TemplateViewEtlJobResp entry : respList) {
                String viewId = entry.getViewId();
                String tplId = entry.getTplId();
                for (String job : entry.getEtlJobs()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("viewId", viewId);
                    row.put("tplId", tplId);
                    row.put("etlJob", job);
                    row.put("userName", username);
                    insertRows.add(row);
                }
            }
        }
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.template.view.batchDeleteEtlJobByViewIds", viewIds);
                if (CollUtil.isNotEmpty(insertRows)) {
                    dao.insert("ssm.template.view.batchInsertEtlJobList", insertRows);
                }
            }
        });
    }

    /**
     * 查询视图依赖的所有事实表/维表及其 ETL 作业信息（多 viewId 跨视图按 MetaTable id 去重）。
     */
    public List<TemplateViewEtlJobResp> getEtlJob(String viewIds) {
        Set<TemplateViewEtlJobResp> tableSet = new LinkedHashSet<>();
        for (String viewId : StringUtils.split(viewIds, ",")) {
            TemplateViewEntity viewEntity = getByViewId(viewId.trim());
            tableSet.addAll(resolveMetaTables(viewEntity));
        }
        return tableSet.stream().collect(Collectors.toList());
    }

    /**
     * 获取模版视图的etl作业信息
     * @param viewIds
     * @return
     */
    public List<TemplateViewEtlJobResp> getEtlJobByViewId(String viewIds) {
        List<String> viewIdList = Arrays.asList(StringUtils.split(viewIds, ","));
        List<TemplateViewEtlJobResp> respList = buildEtlJobRespForSave(viewIdList);
        List<String> etlJobList = (List<String>) dao.queryObjectList("ssm.template.view.getEtlJobByViewId", viewIdList);
        // 兼容未初始化的数据：懒初始化后直接从 respList 提取，避免多一次 DB 查询
        if (CollUtil.isEmpty(etlJobList)) {
            saveEtlJob(viewIdList, respList);
        }

        return respList;
    }

    /**
     * 获取模版名称和视图名称
     * @param viewId
     * @return
     */
    public TemplateViewRsp getTemplateNameAndViewName(String viewId){
        TemplateViewEntity viewEntity = getByViewId(viewId);
        if(viewEntity == null){
            throw new RuntimeException("视图不存在！");
        }

        //查询模版
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("templateId", viewEntity.getTplId());
        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);

        if(tpl == null){
            throw new RuntimeException("模版不存在！");
        }

        TemplateViewRsp templateViewRsp = new TemplateViewRsp();
        templateViewRsp.setViewId(viewId);
        templateViewRsp.setViewName(viewEntity.getViewName());
        templateViewRsp.setTplId(tpl.getId());
        templateViewRsp.setTplName(tpl.getName());
        return templateViewRsp;
    }

    /**
     * 获取模版数据更新时间
     * 目前支持准实时数据集，其他数据集返回空
     * @param viewId
     * @return
     */
    public String getViewDataUpdateTimeByViewId(String viewId) {
        List<String> viewIdList = Arrays.asList(StringUtils.split(viewId, ","));
        Map<String, Object> params = new HashMap<>();
        params.put("viewIdList", viewIdList);
        List<TemplateViewEntity> dbList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.batchGetByViewId", params);
        if (CollUtil.isEmpty(dbList)) {
            return "";
        }
        DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
        for (TemplateViewEntity entity : dbList) {
            if (!SSDUtil.isNearRealTimeDataset(entity.getDatasetId())) {
                continue;
            }
            return (String) dao.queryObject("ssm.doris.information.schema.getNearRealtimeLastBatch", null, dataSourceType);
        }
        return "";
    }

    /**
     * 获取准实时数据更新时间（与 Doris 准时批次一致）。
     *
     */
    public String getRuntimeDataUpdateTime(String dataType) {
        DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
        return (String) dao.queryObject("ssm.doris.information.schema.getNearRealtimeLastBatch", null, dataSourceType);
    }

    public String getRtDataUpdateTime(String tplId, String tplType) {
        if (StrUtil.isEmpty(tplId) || StrUtil.isEmpty(tplType)) {
            return "";
        }
        String type = tplType.trim();
        if (FavTemplateType.QUERY_TEMPLATE.getCode().equals(type)) {
            return getNearRealtimeDataUpdateTimeForQueryTemplate(tplId.trim());
        }
        if (FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(type)
                || FavTemplateType.TMP_ANALYSIS_TEMPLATE.getCode().equals(type)) {
            return getNearRealtimeDataUpdateTimeForAnalysisTemplate(tplId.trim());
        }
        return "";
    }

    /**
     * 单个查询模板：绑定数据集为准实时时返回 Doris 准时最近批次时间，否则空。
     */
    private String getNearRealtimeDataUpdateTimeForQueryTemplate(String queryTplId) {
        if (StrUtil.isEmpty(queryTplId)) {
            return "";
        }
        TemplateAddReq templateAddReq = new TemplateAddReq();
        templateAddReq.setTplId(queryTplId);
        TemplateEntity templateEntity = (TemplateEntity) dao.queryObject("ssm.template.queryTemplateEntityById", templateAddReq);
        if (templateEntity == null || StrUtil.isEmpty(templateEntity.getDatasetId())) {
            return "";
        }
        if (!SSDUtil.isNearRealTimeDataset(templateEntity.getDatasetId())) {
            return "";
        }
        DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
        return (String) dao.queryObject("ssm.doris.information.schema.getNearRealtimeLastBatch", null, dataSourceType);
    }

    /**
     * 看板：取当前线上版本关联的全部查询模板，任一使用准实时数据集则返回 Doris 准时最近批次时间；否则空。
     */
    private String getNearRealtimeDataUpdateTimeForAnalysisTemplate(String analysisTplId) {
        Map<String, Object> analysisQueryParams = new HashMap<>(2);
        analysisQueryParams.put("analysisTplIds", Collections.singletonList(analysisTplId));
        List<AnalysisTemplateEntity> templateRows = dao.queryObjectList(
                "ssm.analysisTemplate.getTemplateBaseList", analysisQueryParams, AnalysisTemplateEntity.class);
        if (CollUtil.isEmpty(templateRows)) {
            return "";
        }
        DataSourceType dataSourceType = DataSourceRouter.getFinalDorisDataSource(UserManager.get(), DataSourceType.Doris_Slave01);
        for (AnalysisTemplateEntity templateRow : templateRows) {
            if (DataTypeEnum.OFFLINE.getCode().equalsIgnoreCase(templateRow.getSourceDataType())) {
                continue;
            }
            return (String) dao.queryObject("ssm.doris.information.schema.getNearRealtimeLastBatch", null, dataSourceType);
        }
        return "";
    }

    /**
     * 解析视图 id 串：按空格、英文逗号、英文分号拆分，去空白、去重且保持首次出现顺序。
     */
    private List<String> parseViewIdList(String viewIds) {
        if (StrUtil.isBlank(viewIds)) {
            return Collections.emptyList();
        }
        String[] tokens = viewIds.trim().split("[\\s,;]+");
        List<String> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String t : tokens) {
            String id = StrUtil.trim(t);
            if (StrUtil.isEmpty(id) || seen.contains(id)) {
                continue;
            }
            seen.add(id);
            ordered.add(id);
        }
        return ordered;
    }

    /**
     * 按视图 id 列表批量查询视图基础信息（不含 tpl_config 等大字段），返回顺序与入参 id 顺序一致；不存在的 id 跳过。
     */
    public List<TemplateViewEntity> listBasicByViewIdList(List<String> viewIdList) {
        if (CollUtil.isEmpty(viewIdList)) {
            return new ArrayList<>();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("viewIdList", viewIdList);
        List<TemplateViewEntity> dbList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.batchGetByViewId", params);
        if (CollUtil.isEmpty(dbList)) {
            return new ArrayList<>();
        }
        Map<String, TemplateViewEntity> map = dbList.stream()
                .collect(Collectors.toMap(TemplateViewEntity::getViewId, v -> v, (a, b) -> a));
        List<TemplateViewEntity> ordered = new ArrayList<>();
        for (String id : viewIdList) {
            TemplateViewEntity entity = map.get(id);
            if (entity != null) {
                ordered.add(entity);
            }
        }
        List<String> ctgIds = ordered.stream()
                .map(TemplateViewEntity::getCtgId)
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(ctgIds)) {
            List<TemplateCtgPathEntity> ctgPathEntities = categoryService.getCtgPath(ctgIds);
            Map<String, String> ctgIdToNamePath = ctgPathEntities.stream()
                    .collect(Collectors.toMap(TemplateCtgPathEntity::getCtgId, TemplateCtgPathEntity::getCtgNamePath, (a, b) -> a));
            for (TemplateViewEntity entity : ordered) {
                entity.setCtgPath(ctgIdToNamePath.get(entity.getCtgId()));
            }
        }
        return ordered;
    }

    /**
     * 按视图 id 串（空格、逗号、分号分隔）批量查询视图基础信息。
     */
    public List<TemplateViewEntity> listBasicByViewIdsString(String viewIds) {
        return listBasicByViewIdList(parseViewIdList(viewIds));
    }

    /**
     * 视图配置画像：基于 {@link SSDUtil#normalizeConfig(String, Map, Map)} 归一化后的配置，
     * 汇总基本信息、结果区字段结构及默认查询参数（日期、维度、过滤、TopN、汇总方式等）。
     */
    public TemplateViewConfigPortraitRsp getViewConfigPortrait(String viewId) {
        if (StrUtil.isEmpty(viewId)) {
            throw new SSDException("视图ID不能为空");
        }
        TemplateViewEntity templateView = getByViewId(viewId);
        if (templateView == null) {
            throw new SSDException("视图不存在！");
        }

        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("templateId", templateView.getTplId());
        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById", queryMap);
        if (tpl == null) {
            throw new SSDException("视图所属模板不存在！");
        }

        Map<String, MetaField> fieldCodeMap = new HashMap<>();
        Map<String, MetaField> fieldIdMap = new HashMap<>();
        QueryFieldService queryFieldService = (QueryFieldService) SpringContextUtil.getBean("queryFieldService");
        String datasetId = StrUtil.isNotEmpty(templateView.getDatasetId()) ? templateView.getDatasetId() : tpl.getDatasetId();
        List<?> treeNodes = queryFieldService.buildFieldTree(CategoryType.Front, datasetId);
        SSDUtil.fetchFieldFromTreeNodes(treeNodes, fieldCodeMap, fieldIdMap);
        String normalizedConfig = SSDUtil.normalizeConfig(templateView.getTplConfig(), fieldCodeMap, fieldIdMap);

        SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
        queryTemplate.setConfig(normalizedConfig);
        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();

        TemplateViewConfigPortraitRsp rsp = new TemplateViewConfigPortraitRsp();
        rsp.setBasic(buildPortraitBasic(templateView, tpl));
        rsp.setMetadata(buildPortraitMetadata(queryConfigure));
        return rsp;
    }

    public TemplateViewConfigPortraitRsp getConfigPortraitDefaults(String viewId) {
        if (StrUtil.isEmpty(viewId)) {
            throw new SSDException("视图ID不能为空");
        }
        TemplateViewEntity templateView = getByViewId(viewId);
        if (templateView == null) {
            throw new SSDException("视图不存在！");
        }

        QueryOlapDataReq queryOlapDataReq = new QueryOlapDataReq();
        queryOlapDataReq.setUserName(UserManager.get().getName());
        String queryConfig = olapApiService.getQueryConfig(queryOlapDataReq, templateView);
        JSONObject queryObject = JSONObject.parseObject(queryConfig);
        String config = queryObject.getString("config");

        SSDQueryTemplate queryTemplate = new SSDQueryTemplate();
        queryTemplate.setConfig(config);
        QueryConfigure queryConfigure = new QueryConfigure(queryTemplate);
        queryConfigure.load();

        TemplateViewConfigPortraitRsp rsp = new TemplateViewConfigPortraitRsp();
        rsp.setDefaults(buildPortraitDefaults(queryConfigure));
        return rsp;
    }

    private TemplateViewConfigPortraitBasic buildPortraitBasic(TemplateViewEntity templateView, SSDQueryTemplate tpl) {
        TemplateViewConfigPortraitBasic basic = new TemplateViewConfigPortraitBasic();
        basic.setViewId(templateView.getViewId());
        basic.setViewName(templateView.getViewName());
        basic.setTplId(tpl.getId());
        basic.setTplName(tpl.getName());
        return basic;
    }

    private List<TemplateViewConfigPortraitMetadataRow> buildPortraitMetadata(QueryConfigure queryConfigure) {
        List<Pair<QueryField, OlapDatasetColumnMetadata>> pairs = olapApiService.buildMetadataOrderedByResultFieldPairs(queryConfigure);
        List<TemplateViewConfigPortraitMetadataRow> list = new ArrayList<>(pairs.size());
        for (Pair<QueryField, OlapDatasetColumnMetadata> pair : pairs) {
            OlapDatasetColumnMetadata om = pair.getRight();
            QueryField field = pair.getLeft();

            if (Enabled.value(field.getIsAnalysis())) {
                continue;
            }

            TemplateViewConfigPortraitMetadataRow row = new TemplateViewConfigPortraitMetadataRow();
            row.setColumnName(om.getColumnName());
            row.setColumnType(om.getColumnType());
            row.setColumnDesc(om.getColumnDesc());
            row.setAggregationType(om.getAggregationType());

            //如果是指标且聚合方式为空，则设置为默认。
            if ("指标".equalsIgnoreCase(om.getColumnType()) && StrUtil.isEmpty(om.getAggregationType())) {
                row.setAggregationType("默认");
            }

            list.add(row);
        }
        return list;
    }


    private static String resolveFieldDisplayName(QueryField field) {
        if (StrUtil.isNotEmpty(field.getTitle())) {
            return field.getTitle();
        }
        if (StrUtil.isNotEmpty(field.getName())) {
            return field.getName();
        }
        return StrUtil.emptyToDefault(field.getCode(), field.getId());
    }


    private TemplateViewConfigPortraitDefaults buildPortraitDefaults(QueryConfigure queryConfigure) {
        TemplateViewConfigPortraitDefaults def = new TemplateViewConfigPortraitDefaults();
        String dg = queryConfigure.getSettings().getDateGranularity();
        DateGranularity granularity = DateGranularity.get(dg);
        String dateGranularityDesc = "按" + granularity.getDesc()  ;

        List<String> dimTitles = new ArrayList<>();
        for (QueryField f : queryConfigure.getResult().getRowDimensions()) {
            dimTitles.add(resolveFieldDisplayName(f));
        }
        for (QueryField f : queryConfigure.getResult().getColDimensions()) {
            dimTitles.add(resolveFieldDisplayName(f));
        }
        def.setQueryDimensionsText(String.join("、", dimTitles));

        String dateRange = "";
        List<String> filterParts = new ArrayList<>();
        for (QueryField filterField : queryConfigure.getFilter().getFields()) {
            if (BIConsts.DATE_CODE.equalsIgnoreCase(filterField.getCode())) {
                dateRange = formatFilterValuesRange(filterField.getValues());
                continue;
            }
            String part = formatFilterFieldSummary(filterField);
            if (StrUtil.isNotEmpty(part)) {
                filterParts.add(part);
            }
        }

        def.setFilterConditionText(String.join("；", filterParts));

        if (StrUtil.isNotEmpty(dateRange)) {
            def.setDateRangeSummary(dateRange + " · " + dateGranularityDesc);
        } else {
            def.setDateRangeSummary(dateGranularityDesc);
        }

        Integer limit = queryConfigure.getSettings().getQueryRowLimit();
        if (limit == null || limit < 0) {
            def.setTopN("");
        } else {
            def.setTopN(String.valueOf(limit));
        }

        def.setAggregationMode("default");
        return def;
    }

    private static String formatFilterValuesRange(List<FieldValue> values) {
        if (CollUtil.isEmpty(values)) {
            return "";
        }
        List<String> titles = new ArrayList<>();
        for (FieldValue v : values) {
            String t = StrUtil.isNotEmpty(v.getTitle()) ? v.getTitle() : v.getId();
            if (StrUtil.isNotEmpty(t)) {
                titles.add(t);
            }
        }
        if (titles.size() >= 2) {
            return titles.get(0) + " ~ " + titles.get(titles.size() - 1);
        }
        return String.join("、", titles);
    }

    private static String formatFilterFieldSummary(QueryField filterField) {
        if (CollUtil.isEmpty(filterField.getValues())) {
            return "";
        }
        String label = resolveFieldDisplayName(filterField);
        List<String> vals = filterField.getValues().stream()
                .map(v -> StrUtil.isNotEmpty(v.getTitle()) ? v.getTitle() : v.getId())
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(vals)) {
            return "";
        }
        return label + "=" + String.join(",", vals);
    }

    /**
     * 解析「新建模板自动追加 owner」配置。
     * 配置格式：创建人:追加owner（多个逗号隔开），多组用 ; 分隔，
     * 如 chenmin:xxx,yyy;zhangsan:zzz
     * @return 该创建人需要追加的 owner 列表，无配置返回空列表
     */
    public List<String> resolveAutoAppendOwners(String createdBy) {
        String config = SC.v("ssm.tpl.owner.auto.append", "chenmin:xxx");
        if (StrUtil.isEmpty(config) || StrUtil.isEmpty(createdBy)) {
            return Collections.emptyList();
        }
        for (String pair : config.split(";")) {
            String[] parts = pair.split(":");
            if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(createdBy.trim())) {
                return Arrays.stream(parts[1].split(","))
                        .map(String::trim)
                        .filter(StrUtil::isNotEmpty)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

}
