package com.bi.queryer.ssm.portal.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.engine.accelerate.cache.QueryTemplateCacheManager;
import com.bi.queryer.ssm.enums.Env;
import com.bi.queryer.ssm.meta.MetaDataset;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.mgr.dataset.DatasetService;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetRsq;
import com.bi.queryer.ssm.portal.PortalMenuService;
import com.bi.queryer.ssm.portal.PortalService;
import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.*;
import com.bi.queryer.ssm.portal.template.ai.AnalysisTemplateAiScriptService;
import com.bi.queryer.ssm.portal.template.builder.AnalysisTemplateBuilder;
import com.bi.queryer.ssm.portal.template.entity.*;
import com.bi.queryer.ssm.portal.template.enums.*;
import com.bi.queryer.ssm.portal.template.vo.*;
import com.bi.queryer.ssm.portal.vo.rsp.PortalRsp;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.ssm.query.template.TemplateBaseService;
import com.bi.queryer.ssm.query.template.TemplateShareService;
import com.bi.queryer.ssm.query.template.enums.DataTypeEnum;
import com.bi.queryer.ssm.query.template.enums.TemplateViewType;
import com.bi.queryer.ssm.query.template.model.TemplateEntity;
import com.bi.queryer.ssm.query.template.model.TemplateViewMapping;
import com.bi.queryer.ssm.query.template.model.TemplateViewRsp;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;

import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isEmpty;
import static com.bi.queryer.util.BIUtil.isNotEmpty;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.*;

/**
 * @Auther: contributor
 * @Date: 2024/6/17 15:11
 * @Description: 看板的相关的服务处理类
 */

@Service
@Scope("prototype")
public class AnalysisTemplateService {

    //标准时间格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String QUERY_TPL_WIDGET_TYPE_CODE = "queryTemplate";

    private static final String AI_SUMMARY_WIDGET_TYPE_CODE = "aiInterpreter";

    @Autowired
    private BaseDao dao;

    @Autowired
    @Lazy
    private PortalMenuService portalMenuService;

    @Autowired
    private TemplateShareService queryTplShareService;

    @Autowired
    private PortalAuthService portalAuthService;

    @Autowired
    private SSDQueryService queryService;

    @Autowired
    private TemplateBaseService templateBaseService;

    @Autowired
    private AnalysisTemplateAiScriptService aiScriptService;

    @Autowired
    @Lazy
    private PortalService portalService;

    @Autowired
    @Lazy
    private DatasetService datasetService;

    @Autowired
    private AnalysisTplViewService analysisTplViewService;

    /**
     * 新增看板
     */
    public AnalysisTemplateVO create(AnalysisTemplateVO vo) {
        //参数校验
        checkCreateParams(vo);
        //权限校验
        checkAnalysisTplAuth(vo.getPortalId(), FuncType.EDIT);

        //插入数据
        User user = UserManager.get();
        String analysisTplId = Guid.id();
        vo.setAnalysisTplType(AnalysisTplType.NORMAL.getCode());
        addAnalysisTemplateBase(vo, analysisTplId, user.getName());

        //挂载到门户目录下
        PortalMenu portalMenu = PortalMenu.builder().portalId(vo.getPortalId())
                .menuName(vo.getAnalysisTplName())
                .menuDesc(vo.getAnalysisTplDesc())
                .menuType(PortalMenuType.ANALYSIS_TEMPLATE.getCode())
                .parentMenuId(vo.getParentMenuId())
                .contentRefId(analysisTplId).build();
        String menuId = portalMenuService.addMenu(portalMenu);
        vo.setMenuId(menuId);
        vo.setAnalysisTplId(analysisTplId);

        // 记录日志
        addOperationLog(analysisTplId, null, vo.getPortalId(), AnalysisTemplateOperationType.CREATE, "");
        return vo;
    }

    private void checkEditParams(AnalysisTemplateVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getAnalysisTplId()), "看板Id为空");
        checkArgument(isNotEmpty(vo.getPortalId()), "看板路径为空");
    }

    private void checkCreateParams(AnalysisTemplateVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getPortalId()), "看板路径为空");
        checkArgument(isNotEmpty(vo.getAnalysisTplName()), "看板名称为空");
    }

    /**
     * 查询是否可以编辑
     * @return
     */
    public AnalysisTemplateEditableVO checkEditable(AnalysisTemplateVO vo) {
        checkEditParams(vo);

        try {
            User user = UserManager.get();
            //编辑权限校验
            checkAnalysisTplAuth(vo.getPortalId(), FuncType.EDIT);

            //否是他人正在编辑
            checkEditable(vo.getAnalysisTplId(), user.getName());

            //获取编辑锁
            doWithEditLock(AnalysisTemplateLockOpType.ACQUIRE, vo.getAnalysisTplId(), user.getName());
        } catch (Exception e) {
            return new AnalysisTemplateEditableVO(0, e.getMessage());
        }

        return new AnalysisTemplateEditableVO(1, "");
    }

    /**
     * 查询模板是否有草稿
     *
     */
    public AnalysisTemplateDraftVO hasDraft(AnalysisTemplateVO vo) {
        checkEditParams(vo);

        //编辑权限校验
        checkAnalysisTplAuth(vo.getPortalId(), FuncType.EDIT);

        //是否有草稿
        AnalysisTplCfgLocalEntity localConfig = getLocalConfigs(vo.getAnalysisTplId());
        if (localConfig == null) {
            return new AnalysisTemplateDraftVO(0, "", UserManager.get().getName());
        }

        return new AnalysisTemplateDraftVO(1, localConfig.getUpdatedBy(), UserManager.get().getName());
    }

    private AnalysisTplCfgLocalLockEntity getEditLock(String analysisTplId) {
        checkArgument(isNotEmpty(analysisTplId), "看板Id为空");

        List<AnalysisTplCfgLocalLockEntity> res = dao.queryObjectList("ssm.analysisTemplate.getEditLock"
                , analysisTplId, AnalysisTplCfgLocalLockEntity.class);

        if (isNotEmpty(res)) {
            return res.get(0);
        } else {
            return null;
        }
    }

    private void checkEditable(String analysisTplId, String userName) {
        checkArgument(isNotEmpty(analysisTplId), "看板Id为空");
        checkArgument(isNotEmpty(userName), "用户名为空");

        AnalysisTplCfgLocalLockEntity entity = getEditLock(analysisTplId);
        if (entity == null) {
            //没人编辑
            return;
        } else if (Objects.equals(userName, entity.getUserName())) {
            //当前编辑人是自己
            return;
        } else if (entity.getCreatedTime() != null &&
                LocalDateTime.now().minusDays(2).isAfter(entity.getCreatedTime().toLocalDateTime())) {
            // 加锁时间超过2天
            return;
        }

        //有人编辑且当前编辑人不是自己
        throw new BIException(String.format("%s正在编辑该看板，同时只能有一个用户编辑该看板。", entity.getUserName()));
    }

    /**
     * 更新查询模板
     * @param vo
     * @return
     */
    public boolean update(AnalysisTemplateVO vo) {
        User user = UserManager.get();

        boolean isTmpAnalysisTpl = AnalysisTplType.TMP.getCode().equals(vo.getAnalysisTplType());
        if (!isTmpAnalysisTpl) {
            checkEditParams(vo);

            checkAnalysisTplAuth(vo.getPortalId(), FuncType.EDIT);

            // 编辑锁校验
            checkEditable(vo.getAnalysisTplId(), user.getName());
        }

        List<WidgetNodeVO> widgetNodeVOS = vo.getWidgetConfigs();
        checkArgument(isNotEmpty(widgetNodeVOS), "看板内容为空，请添加内容后保存");

        //写base表
        updateAnalysisTemplateBase(vo, user.getName(), AnalysisTemplateDraftStatus.UNPUBLISHED);

        String analysisTplCfgId;
        //写local表
        AnalysisTplCfgLocalEntity.AnalysisTplCfgLocalEntityBuilder localBuilder = AnalysisTplCfgLocalEntity.builder();
        localBuilder.analysisTplId(vo.getAnalysisTplId());

        AnalysisTplCfgLocalEntity localConfig = getLocalConfigs(vo.getAnalysisTplId());
        String oldAnalysisTplCfgId = null;
        if (localConfig == null) {
            // 没有草稿配置
            analysisTplCfgId = Guid.id();
            localBuilder.analysisTplCfgId(analysisTplCfgId);
            localBuilder.createdBy(user.getName());
            localBuilder.updatedBy(user.getName());

            // 找线上最大版本
            Long onlineMaxVersionNo = getOnlineMaxVersionNo(vo.getAnalysisTplId());
            if (onlineMaxVersionNo == null) {
                //没有线上版本
                localBuilder.localVersionNo(0);
            } else {
                //有线上版本, 版本号加1
                localBuilder.localVersionNo(onlineMaxVersionNo + 1);
                localBuilder.prodVersionNo(onlineMaxVersionNo);
            }
            //插入local表
            dao.insert("ssm.analysisTemplate.addLocalConfig", localBuilder.build());

        } else {
            // 有草稿配置, 直接更新草稿配置
            analysisTplCfgId = localConfig.getAnalysisTplCfgId();
            oldAnalysisTplCfgId = localConfig.getAnalysisTplCfgId();
            localConfig.setUpdatedBy(user.getName());
            checkState(isNotEmpty(analysisTplCfgId), "配置ID为空");
            dao.update("ssm.analysisTemplate.updateLocalConfig", localConfig);
        }

        //更新widget表
        List<AnalysisTplWidgetEntity> widgetEntities = new ArrayList<>(16);
        int index = 0;
        for (WidgetNodeVO widgetNodeVO : widgetNodeVOS) {
            widgetEntities.addAll(
                    buildWidgetEntities(widgetNodeVO, vo.getAnalysisTplId(), analysisTplCfgId
                            , vo.getAnalysisTplId(), user.getName(), index++)
            );
        }
        replaceWidgetConfigs(analysisTplCfgId, oldAnalysisTplCfgId, widgetEntities);

        if (!isTmpAnalysisTpl) {
            //释放锁
            doWithEditLock(AnalysisTemplateLockOpType.RELEASE, vo.getAnalysisTplId(), user.getName());
        }

        // 增加操作日志
        addOperationLog(vo.getAnalysisTplId(), analysisTplCfgId, vo.getPortalId(), AnalysisTemplateOperationType.UPDATE, "");

        return true;
    }

    /** genAI_feature/v3.15.0_start */
    public AnalysisTemplateVO createTmpTpl(AnalysisTemplateVO vo) {
        User user = UserManager.get();
        String analysisTplId = Guid.id();
        vo.setAnalysisTplType(AnalysisTplType.TMP.getCode());
        addAnalysisTemplateBase(vo, analysisTplId, user.getName());
        vo.setAnalysisTplId(analysisTplId);

        // 同步临时看板分类关系
        saveTmpAnalysisTplCtgRel(analysisTplId, vo.getParentMenuId(), user.getName());
        return vo;
    }

    /**
     * 删除临时看板（仅创建人可删除）
     */
    public boolean deleteTmpTpl(AnalysisTemplateVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getAnalysisTplId()), "看板Id为空");

        User user = UserManager.get();
        AnalysisTemplateEntity templateEntity = getTemplateBase(vo.getAnalysisTplId());
        checkState(templateEntity != null, "看板不存在");
        checkState(AnalysisTplType.TMP.getCode().equals(templateEntity.getAnalysisTplType()), "仅支持删除临时看板");
        checkState(StringUtils.equals(user.getName(), templateEntity.getAnalysisTplOwner()), "仅创建人可删除临时看板");

        templateEntity.setUpdatedBy(user.getName());
        dao.update("ssm.analysisTemplate.deleteAnalysisTemplate", templateEntity);
        dao.delete("ssd.tmpAnalysisTplCtgRel.deleteByAnalysisTplId", vo.getAnalysisTplId());
        return true;
    }
    /** genAI_feature/v3.15.0_end */

    public boolean updateName(AnalysisTemplateVO vo) {
        User user = UserManager.get();

        AnalysisTemplateEntity templateEntity = getTemplateBase(vo.getAnalysisTplId());
        checkState(templateEntity != null, "看板不存在");

        boolean isAnalysisTpl = AnalysisTplType.NORMAL.getCode().equals(templateEntity.getAnalysisTplType());
        if (isAnalysisTpl) {
            checkEditParams(vo);

            checkAnalysisTplAuth(vo.getPortalId(), FuncType.EDIT);

            PortalMenu portalMenu = PortalMenu.builder()
                    .contentRefId(vo.getAnalysisTplId())
                    .menuName(vo.getAnalysisTplName())
                    .updatedBy(user.getName())
                    .build();
            portalMenuService.updateMenuNameByContentRefId(portalMenu);
        }

        updateAnalysisTemplateBase(vo, user.getName(), null);
        return true;
    }

    public boolean publish(AnalysisTemplateVO vo) {
        return publish(vo,  true);
    }

    /**
     * 发布看板
     * @return
     */
    public boolean publish(AnalysisTemplateVO vo, boolean refresh) {
        User user = UserManager.get();

        String analysisTplId = vo.getAnalysisTplId();
        AnalysisTemplateEntity templateEntity = getTemplateBase(analysisTplId);
        checkState(templateEntity != null, "看板不存在");

        boolean isAnalysisTpl = AnalysisTplType.NORMAL.getCode().equals(templateEntity.getAnalysisTplType());
        if (isAnalysisTpl) {
            checkEditParams(vo);
            checkAnalysisTplAuth(vo.getPortalId(), FuncType.PUBLISH);
        }

        //获取local配置
        boolean isNoDraftPublish = false;
        AnalysisTplCfgLocalEntity localConfig = getLocalConfigs(analysisTplId);
        if (localConfig == null) {
            if (AnalysisTemplateOnlineStatus.OFFLINE.getCode().equals(templateEntity.getOnlineStatus())) {
                // 已下线的没有草稿, 直接上线
                publishAnalysisTemplateBase(templateEntity, vo, null, user.getName());
                return true;
            }

            if (AnalysisTemplateOnlineStatus.INIT.getCode().equals(templateEntity.getOnlineStatus())) {
                throw new BIException("空模板无法发布，请编辑并添加内容后发布");
            } else {
                isNoDraftPublish = true;
            }
        }

        String newAnalysisTplCfgId = Guid.id();
        String oldAnalysisTplCfgId = null;
        long versionNo;
        List<AnalysisTplWidgetEntity> widgetEntities;
        Map<String, String> queryTplIdMap = emptyMap();
        if (isNoDraftPublish) {
            // 没有草稿发布
            AnalysisTplCfgProdEntity prodEntity = getOnlineConfig(analysisTplId);
            checkState(prodEntity != null, "看板没有草稿，也没有线上版本，不能发布");
            widgetEntities = getWidgetConfigs(prodEntity.getAnalysisTplCfgId());
            queryTplIdMap = getAnalysisTplQueryTplRelMap(prodEntity.getAnalysisTplCfgId(), null);
            versionNo = prodEntity.getVersionNo() + 1;
        } else {
            oldAnalysisTplCfgId = localConfig.getAnalysisTplCfgId();
            versionNo = localConfig.getLocalVersionNo();
            widgetEntities = getWidgetConfigs(oldAnalysisTplCfgId);
            if (isEmpty(widgetEntities)) {
                throw new BIException("当前看板内容为空，不能发布");
            }
        }

        List<Pair<String, String>> queryTplMaps = new ArrayList<>(16);
        for (AnalysisTplWidgetEntity entity : widgetEntities) {
            if (QUERY_TPL_WIDGET_TYPE_CODE.equals(entity.getWidgetTypeCode()) &&
                    isNotEmpty(entity.getQueryTplId())) {
                String localQueryTplId = queryTplIdMap.getOrDefault(entity.getQueryTplId(), entity.getQueryTplId());
                //复制查询模板

                String viewId = null;
                JSONObject options = JSONObject.parseObject(entity.getWidgetOptions());
                if (options != null) {
                    viewId = options.getString("viewId");
                }
                TemplateViewRsp templateViewRsp = queryTplShareService.createAnalysisTemplateSnapshot(localQueryTplId, viewId);
                String newQueryTplId = templateViewRsp.getTplId();
                queryTplMaps.add(Pair.of(localQueryTplId, newQueryTplId));
                entity.setQueryTplId(newQueryTplId);
                entity.setQueryTplViewIdMapping(JSONObject.toJSONString(templateViewRsp.getViewIdMappingList()));
            }
            entity.setAnalysisTplCfgId(newAnalysisTplCfgId);
        }

        Boolean hasAiSummary = replaceWidgetConfigs(newAnalysisTplCfgId, oldAnalysisTplCfgId, widgetEntities);
        templateEntity.setHasAiSummary(hasAiSummary ? Enabled.YES.getId() : Enabled.NO.getId());

        //更新线上配置
        dao.update("ssm.analysisTemplate.updateOnlineConfigStatus", analysisTplId);
        AnalysisTplCfgProdEntity prodEntity = AnalysisTplCfgProdEntity.builder()
                .analysisTplCfgId(newAnalysisTplCfgId)
                .analysisTplId(analysisTplId)
                .versionNo(versionNo)
                .createdBy(user.getName())
                .updatedBy(user.getName())
                .isLatest(1).build();
        dao.update("ssm.analysisTemplate.addOnlineConfig", prodEntity);

        deleteLocalConfig(oldAnalysisTplCfgId);

        //保存查询模板和快照的关系
        addTemplateRelations(newAnalysisTplCfgId, analysisTplId, user.getName(), queryTplMaps);

        //更新base表
        publishAnalysisTemplateBase(templateEntity, vo, newAnalysisTplCfgId, user.getName());

        //添加公共视图
        addPublicAnalysisTplView(analysisTplId, newAnalysisTplCfgId);

        // 刷新分析模板里查询模板的缓存
        if (refresh) {
            QueryTemplateCacheManager.refreshAllServer();
        }
        return true;
    }

    private void deleteLocalConfig(String analysisTplCfgId) {
        if (isEmpty(analysisTplCfgId)) {
            return;
        }
        dao.delete("ssm.analysisTemplate.deleteLocalConfig", analysisTplCfgId);
    }

    private void publishAnalysisTemplateBase(AnalysisTemplateEntity templateEntity, AnalysisTemplateVO vo,
                                             String analysisTplCfgId, String userName) {
        templateEntity.setUpdatedBy(userName);
        templateEntity.setDraftStatus(AnalysisTemplateDraftStatus.PUBLISHED.getCode());
        templateEntity.setOnlineStatus(AnalysisTemplateOnlineStatus.ONLINE.getCode());
        dao.update("ssm.analysisTemplate.updateAnalysisTemplateBase", templateEntity);

        // 增加操作日志
        addOperationLog(vo.getAnalysisTplId(), analysisTplCfgId, vo.getPortalId(),
                AnalysisTemplateOperationType.PUBLISH, vo.getReason());
    }


    /**
     * 保存并发布看板
     * @param vo
     * @return
     */
    public boolean saveAndPublish(AnalysisTemplateVO vo) {
        update(vo);
        publish(vo);
        return true;
    }

    /**
     * 新增看板和查询模板的关系
     */
    private void addTemplateRelations(String newAnalysisTplCfgId, String analysisTplId, String userName,
                                      List<Pair<String, String>> queryTplMaps) {
        if (isEmpty(queryTplMaps)) {
            return;
        }

        List<AnalysisTplWidgetQueryTplRelEntity> entities = new ArrayList<>();
        for (Pair<String, String> e : queryTplMaps) {
            AnalysisTplWidgetQueryTplRelEntity entity = AnalysisTplWidgetQueryTplRelEntity.builder()
                    .analysisTplCfgId(newAnalysisTplCfgId)
                    .analysisTplId(analysisTplId)
                    .localQueryTplId(e.getLeft())
                    .prodQueryTplId(e.getRight())
                    .createdBy(userName)
                    .build();
            entities.add(entity);
        }
        dao.insert("ssm.analysisTemplate.batchAddAnalysisTplWidgetQueryTplRel", entities);
    }

    /**
     * 下线看板
     * @return
     */
    public boolean offline(AnalysisTemplateVO vo) {
        //权限校验
        checkEditParams(vo);
        checkAnalysisTplAuth(vo.getPortalId(), FuncType.OFFLINE);

        AnalysisTemplateEntity templateEntity = getTemplateBase(vo.getAnalysisTplId());
        checkState(templateEntity != null, "看板不存在");

        checkState(AnalysisTemplateOnlineStatus.ONLINE.getCode().equals(templateEntity.getOnlineStatus()), "看板未发布");

        templateEntity.setOnlineStatus(AnalysisTemplateOnlineStatus.OFFLINE.getCode());
        dao.update("ssm.analysisTemplate.updateAnalysisTemplateBase", templateEntity);

        // 增加操作日志
        addOperationLog(vo.getAnalysisTplId(), null, vo.getPortalId(), AnalysisTemplateOperationType.OFFLINE, vo.getReason());
        return true;
    }

    public boolean delete(AnalysisTemplateVO vo) {
        //权限校验
        checkEditParams(vo);
        checkAnalysisTplAuth(vo.getPortalId(), FuncType.DELETE);

        AnalysisTemplateEntity templateEntity = getTemplateBase(vo.getAnalysisTplId());
        checkState(templateEntity != null, "看板不存在");

        checkState(!AnalysisTemplateOnlineStatus.ONLINE.getCode().equals(templateEntity.getOnlineStatus()), "看板还在线使用，不能删除");

        templateEntity.setUpdatedBy(UserManager.get().getName());
        dao.update("ssm.analysisTemplate.deleteAnalysisTemplate", templateEntity);

        addOperationLog(vo.getAnalysisTplId(), null, vo.getPortalId(), AnalysisTemplateOperationType.DELETE, "");
        return true;
    }

    private void checkAnalysisTplAuth(String portalId, FuncType funcType) {
        checkArgument(isNotEmpty(portalId), "看板路径为空");

        // 校验门户存不存在
        Portal portalInfo = portalService.get(portalId);
        checkArgument(portalInfo != null, "业务门户不存在");
        checkArgument(Enabled.value(portalInfo.getIsActive()), "业务门户[%s]被删除", portalInfo.getPortalName());

        // 权限校验
        //boolean hasAuth = portalAuthService.check(portalId, ResType.PORTAL.getCode(), funcType.getCode());
        //checkState(hasAuth, String.format("您还没有业务门户[%s]的%s权限。", portalInfo.getPortalName(), funcType.getName()));
    }

    public void addAnalysisTemplateBase(AnalysisTemplateVO vo, String analysisTplId, String userName) {
        //插入数据
        AnalysisTemplateEntity entity = AnalysisTemplateEntity.builder()
                .analysisTplId(analysisTplId)
                .analysisTplName(vo.getAnalysisTplName())
                .analysisTplOwner(userName)
                .analysisTplDesc(vo.getAnalysisTplDesc())
                .sourceDataType(vo.getSourceDataType())
                .analysisTplType(vo.getAnalysisTplType())
                .onlineStatus(AnalysisTemplateOnlineStatus.INIT.getCode())
                .draftStatus(AnalysisTemplateDraftStatus.INIT.getCode())
                .createdBy(userName)
                .hasAiSummary(vo.getHasAiSummary())
                .build();
        dao.insert("ssm.analysisTemplate.create", entity);
    }

    /** genAI_feature/v3.15.0_start */
    private void saveTmpAnalysisTplCtgRel(String analysisTplId, String ctgId, String userName) {
        if (StrUtil.isBlank(analysisTplId)) {
            return;
        }

        dao.delete("ssd.tmpAnalysisTplCtgRel.deleteByAnalysisTplId", analysisTplId);
        if (StrUtil.isBlank(ctgId)) {
            return;
        }
        TmpAnalysisTplCtgRelEntity rel = new TmpAnalysisTplCtgRelEntity();
        rel.setCtgId(ctgId);
        rel.setAnalysisTplId(analysisTplId);
        rel.setCreatedBy(userName);
        rel.setUpdatedBy(userName);
        dao.insert("ssd.tmpAnalysisTplCtgRel.insert", rel);
    }
    /** genAI_feature/v3.15.0_end */

    public void updateAnalysisTemplateBase(AnalysisTemplateVO vo, String userName,
                                            AnalysisTemplateDraftStatus draftStatus) {
        AnalysisTemplateEntity.AnalysisTemplateEntityBuilder templateEntity = AnalysisTemplateEntity.builder()
                .analysisTplId(vo.getAnalysisTplId())
                .analysisTplName(vo.getAnalysisTplName())
                .analysisTplDesc(vo.getAnalysisTplDesc())
                .sourceDataType(vo.getSourceDataType())
                .draftStatus(draftStatus == null ? null : draftStatus.getCode())
                .updatedBy(userName);
        dao.update("ssm.analysisTemplate.updateAnalysisTemplateBase", templateEntity.build());
    }

    public List<AnalysisTemplateBaseVO> getAnalysisTemplateBaseInfo(List<String> analysisTplIds) {
        if (isEmpty(analysisTplIds)) {
            return emptyList();
        }

        List<AnalysisTemplateEntity> entities = getTemplateBaseList(analysisTplIds);
        List<AnalysisTemplateBaseVO> res = new ArrayList<>(entities.size());
        for (AnalysisTemplateEntity e : entities) {
            AnalysisTemplateBaseVO vo = new AnalysisTemplateBaseVO();
            vo.setAnalysisTplId(e.getAnalysisTplId());
            vo.setAnalysisTplName(e.getAnalysisTplName());
            vo.setAnalysisTplDesc(e.getAnalysisTplDesc());
            vo.setUpdatedTime(e.getUpdatedTime());
            vo.setUpdatedBy(e.getUpdatedBy());
            vo.setOnlineStatus(e.getOnlineStatus());
            vo.setPublishStatus(AnalysisTemplatePublishStatus.publishStatusCode(e.getOnlineStatus()));
            vo.setHasDraft(AnalysisTemplateDraftStatus.hasDraft(e.getDraftStatus()));
            res.add(vo);
        }
        return res;
    }

    public AnalysisTemplateVO getAnalysisTemplateConfig(String analysisTplId, String portalId,
                                                        AnalysisTemplateExecMode getMode, Integer useDraft,String viewId) {
        checkArgument(isNotEmpty(analysisTplId), "看板Id为空");

        AnalysisTemplateEntity templateEntity = getTemplateBase(analysisTplId);
        checkState(templateEntity != null, "看板不存在或已删除");

        boolean isAnalysisTpl = AnalysisTplType.NORMAL.getCode().equals(templateEntity.getAnalysisTplType());

        //是否是快照
        boolean isSnapshot = AnalysisTplType.SNAPSHOT == AnalysisTplType.get(templateEntity.getAnalysisTplType());

        if (isAnalysisTpl) {
            if(!isSnapshot){
                checkArgument(isNotEmpty(portalId), "看板路径为空");
            }
            if (AnalysisTemplateExecMode.PROD.equals(getMode) &&
                    AnalysisTemplateOnlineStatus.OFFLINE.getCode().equals(templateEntity.getOnlineStatus())) {
                throw new BIException(String.format("访问内容已下线，请联系[%s]", templateEntity.getAnalysisTplOwner()));
            }
            if(!isSnapshot){
                //鉴权
                checkAnalysisTplAuth(portalId, FuncType.VIEW);
            }

        } else {
            useDraft = 1;
        }

        AnalysisTemplateVO res;
        if (AnalysisTemplateExecMode.LOCAL.equals(getMode)) {
            //获取编辑态配置
            AnalysisTplCfgLocalEntity localEntity = getLocalConfigs(analysisTplId);
            if (localEntity != null) {
                if (Objects.equals(useDraft, 1)) {
                    //有草稿, 用户基于草稿编辑
                    List<AnalysisTplWidgetEntity> widgetEntities = getWidgetConfigs(localEntity.getAnalysisTplCfgId());
                    res = AnalysisTemplateBuilder.buildAnalysisTemplateVO(templateEntity, widgetEntities);
                    res.setDraftUpdatedTime(localEntity.getUpdatedTime().getTime());

                    //补充发布时间和发布人
                    AnalysisTplCfgProdEntity prodEntity = getOnlineConfig(templateEntity.getAnalysisTplId());
                    if (prodEntity != null) {
                        res.setPublishTime(DATE_FORMAT.format(prodEntity.getCreatedTime()));
                        res.setPublishUser(prodEntity.getCreatedBy());
                    }
                } else {
                    //有草稿, 用户不用草稿, 获取线上配置或默认配置
                    res = getOnlineAnalysisTemplateVO(templateEntity, getMode,viewId);
                }
            } else {
                //没有草稿
                res = getOnlineAnalysisTemplateVO(templateEntity, getMode,viewId);
            }

            //设置草稿的最后编辑人和编辑时间
            res.setDraftUpdatedBy(localEntity == null ? null : localEntity.getUpdatedBy());
        } else if (AnalysisTemplateExecMode.PROD.equals(getMode)) {
            //使用线上版本
            res = getOnlineAnalysisTemplateVO(templateEntity, getMode,viewId);
        } else {
            throw new BIException("未知的操作类型");
        }

        //添加在线状态
        res.setOnlineStatus(templateEntity.getOnlineStatus());

        res.setAnalysisTplType(templateEntity.getAnalysisTplType());

        //更新添加访问记录
        addVisitLog(analysisTplId, portalId, getMode);
        return res;
    }

    public AnalysisTemplateVO getByViewId(String viewId) {

        AnalysisTplViewEntity analysisTplViewEntity = analysisTplViewService.getEntityById(viewId);
        checkArgument(analysisTplViewEntity != null, "未找到" + viewId + "对应的视图");

        Portal portal = portalService.getByContentRefId(analysisTplViewEntity.getAnalysisTplId());
        String portalId = portal != null ? portal.getPortalId() : "";

        return getAnalysisTemplateConfig(analysisTplViewEntity.getAnalysisTplId(), portalId, AnalysisTemplateExecMode.PROD, 0, viewId);
    }

    public AnalysisTemplateQueryTplModifiedResp getModifiedQueryTpl(AnalysisTemplateQueryTplModifiedReq vo) {
        checkArgument(vo != null, "查询参数为空");

        List<String> queryTplIds = vo.getQueryTplIds();

        if (isEmpty(queryTplIds)) {
            return new AnalysisTemplateQueryTplModifiedResp();
        }

        Long date = vo.getDateTime();
        checkArgument(date != null, "轮询基准时间为空");
        Long currentTime = new Date().getTime();

        List<TemplateEntity> templateEntities = templateBaseService.batchQueryTemplateUpdateTime(queryTplIds);
        if (isEmpty(templateEntities)) {
            return new AnalysisTemplateQueryTplModifiedResp();
        }

        Map<String, DatasetRsq> datasetMap = datasetService.listAll().stream().collect(Collectors.toMap(DatasetRsq::getDatasetId, x -> x));

        Date baseDate = new Date(date);
        List<AnalysisTemplateQueryTplModifiedResp.QueryTplVO> modifiedQueryTplVOs = templateEntities.stream().filter(v -> {
            try {
                Date queryTplUpdatedTime = DATE_FORMAT.parse(v.getUpdatedTime());
                return queryTplUpdatedTime.compareTo(baseDate) > 0;
            } catch (Exception e) {
                return false;
            }
        }).map(v -> {
            String datasetType = null;
            DatasetRsq datasetRsq = datasetMap.get(v.getDatasetId());
            if (datasetRsq != null) {
                datasetType = datasetRsq.getDatasetType();
            }
            return new AnalysisTemplateQueryTplModifiedResp.QueryTplVO(v.getTplId(), datasetType);
        }).collect(Collectors.toList());

        return new AnalysisTemplateQueryTplModifiedResp(currentTime, modifiedQueryTplVOs);
    }

    public AnalysisTemplateVO getOnlineAnalysisTemplateVO(AnalysisTemplateEntity templateEntity,
                                                           AnalysisTemplateExecMode getMode,
                                                           String viewId) {
        AnalysisTemplateVO res ;
        AnalysisTplCfgProdEntity prodEntity = getOnlineConfig(templateEntity.getAnalysisTplId());
        String analysisTplId = templateEntity.getAnalysisTplId();

        String analysisTplCfgId = prodEntity != null ? prodEntity.getAnalysisTplCfgId() : null;
        String resolvedViewId = resolveViewIdForTemplateConfig(analysisTplId, viewId);

        String viewName = "";
        if (StrUtil.isNotEmpty(resolvedViewId)) {
            AnalysisTplViewEntity analysisTplViewEntity = analysisTplViewService.getEntityById(resolvedViewId);
            if (analysisTplViewEntity != null) {
                viewName = analysisTplViewEntity.getViewName();
                analysisTplCfgId = analysisTplViewEntity.getAnalysisTplCfgId();
            }
        }

        if (prodEntity != null) {
            //有线上配置, 拉取发布到线上配置
            List<AnalysisTplWidgetEntity> widgetEntities = getWidgetConfigs(analysisTplCfgId);
            Map<String, String> queryTplIdMap = getAnalysisTplQueryTplRelMap(analysisTplCfgId, null);
            res = AnalysisTemplateBuilder.buildAnalysisTemplateVO(templateEntity, widgetEntities, queryTplIdMap, getMode);
            res.setPublishTime(DATE_FORMAT.format(prodEntity.getCreatedTime()));
            res.setPublishUser(prodEntity.getCreatedBy());
            res.setDraftUpdatedTime(prodEntity.getCreatedTime().getTime());
            res.setViewId(resolvedViewId);
        } else {
            //没有线上配置, 返回默认配置
            res = AnalysisTemplateBuilder.buildAnalysisTemplateVOWithDefault(templateEntity, getDefaultWidgetConfigs());
            res.setDraftUpdatedTime(templateEntity.getUpdatedTime().getTime());
            res.setViewId(resolvedViewId);
        }

        res.setViewId(resolvedViewId);
        res.setViewName(viewName);
        return res;
    }

    /**
     * 解析本次应使用的视图 id：请求中的 viewId 有效则用之；否则用户默认视图；再否则第一个公共视图（按 sort_id）
     */
    private String resolveViewIdForTemplateConfig(String analysisTplId, String viewId) {
        if (isNotEmpty(viewId)) {
            AnalysisTplViewEntity v = analysisTplViewService.getEntityById(viewId);
            if (v != null && analysisTplId.equals(v.getAnalysisTplId())) {
                return viewId;
            }
        }
        User user = UserManager.get();
        if (user != null && isNotEmpty(user.getName())) {
            String defaultViewId = queryUserDefaultViewIdForTemplate(analysisTplId, user.getName());
            if (isNotEmpty(defaultViewId)) {
                AnalysisTplViewEntity dv = analysisTplViewService.getEntityById(defaultViewId);
                if (dv != null && analysisTplId.equals(dv.getAnalysisTplId())) {
                    return defaultViewId;
                }
            }
        }
        return firstPublicViewId(analysisTplId);
    }

    private String queryUserDefaultViewIdForTemplate(String analysisTplId, String userName) {
        Map<String, String> param = new HashMap<>(4);
        param.put("analysisTplId", analysisTplId);
        param.put("createdBy", userName);
        return dao.queryObject("ssm.analysis.tpl.view.user.cfg.queryUserDefaultViewId", param, String.class);
    }

    private String firstPublicViewId(String analysisTplId) {
        List<AnalysisTplViewVO> list = analysisTplViewService.list(
                AnalysisTplViewVO.builder()
                        .analysisTplId(analysisTplId)
                        .viewType(TemplateViewType.PUBLIC.getCode())
                        .build());
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0).getViewId();
    }

    /**
     * 新增公共看板视图：先删除该看板下已有公共视图，再只落库 ssm_analysis_tpl_view，视图绑定已有配置 {@code analysisTplCfgId}（组件与关联需已存在于该配置）
     *
     * @return 新视图 viewId
     */
    public String addPublicAnalysisTplView(String analysisTplId, String analysisTplCfgId) {
        analysisTplViewService.deletePublicViewsByAnalysisTplId(analysisTplId);
        return analysisTplViewService.insertViewRowOnly(analysisTplId, analysisTplCfgId, "默认视图");
    }

    public AnalysisTemplateAuthVO getDataAuth(String analysisTplId) {
        checkArgument(isNotEmpty(analysisTplId), "看板Id为空");
        AnalysisTplCfgProdEntity prodEntity = getOnlineConfig(analysisTplId);
        checkState(prodEntity != null, "看板不存在已发布版本");

        List<AnalysisTplWidgetQueryTplRelEntity> relEntities =
                getAnalysisTplQueryTplRelEntities(prodEntity.getAnalysisTplCfgId(), null);
        if (isEmpty(relEntities)) {
            return new AnalysisTemplateAuthVO(emptyList());
        } else {
            List<String> onlineQueryTplIds = relEntities.stream()
                    .map(AnalysisTplWidgetQueryTplRelEntity::getProdQueryTplId)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            return new AnalysisTemplateAuthVO(queryService.getUserUnAuthCtgIdListByTplIds(onlineQueryTplIds));
        }
    }

    public boolean doWithEditLock(AnalysisTemplateLockOpType opType, AnalysisTemplateVO vo) {
        checkEditParams(vo);
        checkAnalysisTplAuth(vo.getPortalId(), FuncType.EDIT);
        User user = UserManager.get();
        return doWithEditLock(opType, vo.getAnalysisTplId(), user.getName());
    }

    public boolean exitEdit(AnalysisTemplateEditExitVO exitVO) {
        checkArgument(exitVO != null, "传入参数为空");
        checkArgument(isNotEmpty(exitVO.getAnalysisTplId()), "看板Id为空");

        User user = UserManager.get();
        AnalysisTemplateEditExitType exitType = AnalysisTemplateEditExitType.codeOf(exitVO.getExitMode());
        if (AnalysisTemplateEditExitType.QUIT.equals(exitType)) {
            //放弃更改
            doWithEditLock(AnalysisTemplateLockOpType.RELEASE, exitVO.getAnalysisTplId(), user.getName());
        } else if (AnalysisTemplateEditExitType.SAVE.equals(exitType)) {
            update(exitVO);
        } else {
            throw new BIException("未知的操作类型:" + exitVO.getExitMode());
        }
        return true;
    }

    public List<AnalysisTplWidgetTypeVO> listWidgetTypes() {
        List<AnalysisTplWidgetTypeEntity> entities = dao.queryObjectList("ssm.analysisTemplate.listWidgetTypes",
                Collections.emptyMap(), AnalysisTplWidgetTypeEntity.class);
        if (isEmpty(entities)) {
            return emptyList();
        }
        entities.sort(Comparator.comparingDouble(AnalysisTplWidgetTypeEntity::getSortId));

        List<AnalysisTplWidgetTypeVO> res = new ArrayList<>();
        for (AnalysisTplWidgetTypeEntity e : entities) {
            AnalysisTplWidgetTypeVO vo = new AnalysisTplWidgetTypeVO();
            vo.setWidgetTypeName(e.getWidgetTypeName());
            vo.setWidgetTypeCode(e.getWidgetTypeCode());
            vo.setCreatedBy(e.getCreatedBy());
            vo.setUpdatedBy(e.getUpdatedBy());
            vo.setCreatedTime(e.getCreatedTime());
            vo.setUpdatedTime(e.getUpdatedTime());
            res.add(vo);
        }
        return res;
    }

    private boolean doWithEditLock(AnalysisTemplateLockOpType type, String analysisTplId, String userName) {
        checkArgument(isNotEmpty(analysisTplId), "看板Id为空");

        checkEditable(analysisTplId, userName);

        AnalysisTplCfgLocalLockEntity lockEntity = AnalysisTplCfgLocalLockEntity.builder()
                .analysisTplId(analysisTplId).userName(userName).build();
        if (AnalysisTemplateLockOpType.RELEASE.equals(type)) {
            dao.delete("ssm.analysisTemplate.releaseEditLock", lockEntity);
            addOperationLog(analysisTplId, null, null, AnalysisTemplateOperationType.RELEASE_LOCK, "");
        } else if (AnalysisTemplateLockOpType.ACQUIRE.equals(type)) {
            dao.update("ssm.analysisTemplate.acquireEditLock", lockEntity);
            addOperationLog(analysisTplId, null, null, AnalysisTemplateOperationType.ACQUIRE_LOCK, "");
        } else {
            throw new BIException("未知的操作类型");
        }
        return true;
    }

    private String getAnalysisTemplateDefaultOption(String key) {
        if (isEmpty(key)) {
            return null;
        }
        return dao.queryObject("ssm.analysisTemplate.getAnalysisTemplateDefaultOption", key, String.class);
    }

    private List<WidgetNodeVO> getDefaultWidgetConfigs() {
        Env env = SSDUtil.getEnv();
        String key = "widget_config";
        if (Env.UT == env) {
            key = "widget_config_ut";
        }
        String defaultConfigs = getAnalysisTemplateDefaultOption(key);
        if (defaultConfigs == null) {
            return emptyList();
        }
        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("storylineWidgetId", Guid.id());
        valuesMap.put("queryTplWidgetId1", Guid.id());
        valuesMap.put("queryTplWidgetId2", Guid.id());
        valuesMap.put("globalFilterGroupWidgetId", Guid.id());

        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return JSONArray.parseArray(sub.replace(defaultConfigs), WidgetNodeVO.class);
    }

    private AnalysisTplCfgLocalEntity getLocalConfigs(String analysisTplId) {
        List<AnalysisTplCfgLocalEntity> entities = dao.queryObjectList("ssm.analysisTemplate.getLocalConfig"
                , analysisTplId, AnalysisTplCfgLocalEntity.class);
        if (isNotEmpty(entities)) {
            return entities.get(0);
        }
        return null;
    }

    public AnalysisTplCfgProdEntity getOnlineConfig(String analysisTplId) {
        if (isEmpty(analysisTplId)) {
            return null;
        }

        List<AnalysisTplCfgProdEntity> entities = getOnlineConfigs(singletonList(analysisTplId));
        if (isNotEmpty(entities)) {
            return entities.get(0);
        }
        return null;
    }

    private List<AnalysisTplCfgProdEntity> getOnlineConfigs(List<String> analysisTplIds) {
        if (isEmpty(analysisTplIds)) {
            return emptyList();
        }

        List<AnalysisTplCfgProdEntity> entities = dao.queryObjectList("ssm.analysisTemplate.getOnlineConfigs"
                , analysisTplIds, AnalysisTplCfgProdEntity.class);
        if (isEmpty(entities)) {
            return emptyList();
        }
        return entities;
    }

    public AnalysisTemplateEntity getTemplateBase(String analysisTplId) {
        List<AnalysisTemplateEntity> entities = getTemplateBaseList(Collections.singletonList(analysisTplId));
        if (entities.size() > 0) {
            return entities.get(0);
        }
        return null;
    }

    public List<AnalysisTemplateEntity> getTemplateBaseList(List<String> analysisTplIds) {
        if (isEmpty(analysisTplIds)) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("analysisTplIds", analysisTplIds);
        List<AnalysisTemplateEntity> entities = dao.queryObjectList("ssm.analysisTemplate.getTemplateBaseList"
                , params, AnalysisTemplateEntity.class);
        if (isEmpty(entities)) {
            return Collections.emptyList();
        } else {
            return entities;
        }
    }

    public List<AnalysisTplWidgetEntity> getWidgetConfigs(String analysisTplCfgId) {
        checkArgument(isNotEmpty(analysisTplCfgId), "看板组件Id为空");
        List<AnalysisTplWidgetEntity> res = dao.queryObjectList("ssm.analysisTemplate.getWidgetConfigs"
                , analysisTplCfgId, AnalysisTplWidgetEntity.class);
        if (isEmpty(res)) {
            return emptyList();
        } else {

            try {
                for (AnalysisTplWidgetEntity entity : res) {
                    if (StrUtil.isNotEmpty(entity.getQueryTplViewIdMapping())) {
                        entity.setQueryTplViewIdMappingList(JSON.parseArray(entity.getQueryTplViewIdMapping(), TemplateViewMapping.class));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return res;
        }
    }

    private Long getOnlineMaxVersionNo(String analysisTplId) {
        return dao.queryObject("ssm.analysisTemplate.getOnlineMaxVersionNo"
                , analysisTplId, Long.class);
    }

    private List<AnalysisTplWidgetEntity> buildWidgetEntities(WidgetNodeVO widgetNodeVO, String analysisTplId,
                                                              String analysisTplCfgId, String parentWidgetId,
                                                              String userName, int index) {
        List<AnalysisTplWidgetEntity> widgetEntities = new ArrayList<>(16);
        List<WidgetNodeVO> children = widgetNodeVO.getChildren();
        if (isNotEmpty(children)) {
            int childIndex = 0;
            for (WidgetNodeVO child : children) {
                widgetEntities.addAll(buildWidgetEntities(child, analysisTplId, analysisTplCfgId,
                        widgetNodeVO.getWidgetId(), userName, childIndex++));
            }
        }
        AnalysisTplWidgetEntity widgetEntity = AnalysisTplWidgetEntity.builder()
                .widgetId(widgetNodeVO.getWidgetId())
                .analysisTplCfgId(analysisTplCfgId)
                .analysisTplId(analysisTplId)
                .widgetTypeCode(widgetNodeVO.getWidgetTypeCode())
                .parentWidgetId(parentWidgetId)
                .widgetTitle(widgetNodeVO.getWidgetTitle())
                .widgetDesc(widgetNodeVO.getWidgetDesc())
                .widgetSettings(widgetNodeVO.getWidgetSettings())
                .widgetOptions(widgetNodeVO.getWidgetOptions())
                .queryTplId(widgetNodeVO.getQueryTplId())
                .isActive(1)
                .sortId(index)
                .createdBy(userName)
                .updatedBy(userName)
                .build();
        widgetEntities.add(widgetEntity);
        return widgetEntities;
    }

    public List<AnalysisTplWidgetQueryTplRelVO> getAnalysisTplByQueryTplId(String localQueryTplId) {
        if (isEmpty(localQueryTplId)) {
            return Collections.emptyList();
        }

        List<AnalysisTplWidgetQueryTplRelEntity> entities = getAnalysisTplQueryTplRelEntities(null, localQueryTplId);
        if (isEmpty(entities)) {
            return Collections.emptyList();
        }

        List<String> analysisTplIds = entities.stream()
                .map(AnalysisTplWidgetQueryTplRelEntity::getAnalysisTplId)
                .collect(Collectors.toList());

        //查找在线的配置
        List<AnalysisTplCfgProdEntity> onlineConfigs = getOnlineConfigs(analysisTplIds);
        Set<String> onlineAnalysisTplCfgIds = onlineConfigs.stream().map(AnalysisTplCfgProdEntity::getAnalysisTplCfgId).collect(Collectors.toSet());

        analysisTplIds = entities.stream().filter(entity -> onlineAnalysisTplCfgIds.contains(entity.getAnalysisTplCfgId()))
                .map(AnalysisTplWidgetQueryTplRelEntity::getAnalysisTplId)
                .collect(Collectors.toList());

        List<PortalMenu> portalMenuList = portalMenuService.getPortalMenuInfoByAnalysisTplIds(analysisTplIds);
        Map<String, PortalMenu> portalMenuMap = emptyMap();
        if (isNotEmpty(portalMenuList)) {
            portalMenuMap = portalMenuList.stream().collect(Collectors.toMap(PortalMenu::getContentRefId, v -> v, (x, y) -> x));
        }

        List<AnalysisTemplateEntity> templateEntities = getTemplateBaseList(analysisTplIds);

        List<AnalysisTplWidgetQueryTplRelVO> res = new ArrayList<>(templateEntities.size());
        for (AnalysisTemplateEntity entity : templateEntities) {
            if (!AnalysisTemplateOnlineStatus.ONLINE.getCode().equals(entity.getOnlineStatus())) {
                continue;
            }
            PortalMenu portalMenu = portalMenuMap.get(entity.getAnalysisTplId());
            res.add(AnalysisTplWidgetQueryTplRelVO.of(entity, portalMenu));
        }

        return res;
    }

    // 获取查询模板应用的看板
    public List<AnalysisTplQueryTplRelEntity> getAnalysisTplByQueryTplIds(List<String> localQueryTplIds) {
        if (CollUtil.isEmpty(localQueryTplIds)) {
            return emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("localQueryTplIds", localQueryTplIds);
        List<AnalysisTplQueryTplRelEntity> relEntities = dao.queryObjectList("ssm.analysisTemplate.getAnalysisTplByQueryTplIds", params, AnalysisTplQueryTplRelEntity.class);

        if (CollUtil.isEmpty(relEntities)) {
            return emptyList();
        }

        return relEntities;
    }

    private List<AnalysisTplWidgetQueryTplRelEntity> getAnalysisTplQueryTplRelEntities(String analysisTplCfgId,
                                                                                       String localQueryTplId) {
        AnalysisTplWidgetQueryTplRelEntity analysisTplWidgetQueryTplRelEntity = AnalysisTplWidgetQueryTplRelEntity.builder()
                .analysisTplCfgId(analysisTplCfgId).localQueryTplId(localQueryTplId).build();
        List<AnalysisTplWidgetQueryTplRelEntity> res = dao.queryObjectList("ssm.analysisTemplate.queryAnalysisTplWidgetQueryTplRel",
                analysisTplWidgetQueryTplRelEntity, AnalysisTplWidgetQueryTplRelEntity.class);
        if (isEmpty(res)) {
            return emptyList();
        } else {
            return res;
        }
    }

    //获取线上版本和线下版本的映射
    public Map<String, String> getAnalysisTplQueryTplRelMap(String analysisTplCfgId,
                                                            String localQueryTplId) {
        List<AnalysisTplWidgetQueryTplRelEntity> queryTplRelEntities =
                getAnalysisTplQueryTplRelEntities(analysisTplCfgId, localQueryTplId);
        return queryTplRelEntities.stream()
                .collect(Collectors.toMap(AnalysisTplWidgetQueryTplRelEntity::getProdQueryTplId,
                        AnalysisTplWidgetQueryTplRelEntity::getLocalQueryTplId, (x, y) -> x)
                );

    }

    private Boolean replaceWidgetConfigs(String analysisTplCfgId, String oldAnalysisTplCfgId, List<AnalysisTplWidgetEntity> entities) {
        List<String> onUseWidgetIds = new ArrayList<>(16);
        AnalysisTplWidgetEntity aiWidget = null;
        for (AnalysisTplWidgetEntity entity : entities) {
            saveWidgetConfig(entity);
            onUseWidgetIds.add(entity.getWidgetId());
            if (AI_SUMMARY_WIDGET_TYPE_CODE.equals(entity.getWidgetTypeCode())) {
                aiWidget = entity;
            }
        }

        deleteWidgets(analysisTplCfgId, onUseWidgetIds);

        // 保存AI脚本
        return aiScriptService.saveAnalysisTplAiScript(analysisTplCfgId, oldAnalysisTplCfgId, aiWidget);
    }

    private void saveWidgetConfig(AnalysisTplWidgetEntity entity) {
        dao.update("ssm.analysisTemplate.saveWidgetConfig", entity);
    }

    /**
     * 批量保存组件
     * <p>写入前按 widget_id 覆盖 query_tpl_id、query_tpl_viewid_mapping：若 {@code queryTplFillSourceCfgId} 非空则从该配置 id
     * 对应组件行取值；否则从当前 {@code analysisTplId} 的线上版本（ssm_analysis_tpl_cfg_prod.is_latest=1）组件取值。
     * 无可用来源或同 id 组件不存在时保留 VO 构建结果。</p>
     */
    public void batchSaveWidgetConfig(List<WidgetNodeVO> widgetConfigs, String analysisTplId, String analysisTplCfgId,
                                      String queryTplFillSourceCfgId,String oldAnalysisTplCfgId) {

        //先删除
        dao.delete("ssm.analysisTemplate.deleteWidgetByAnalysisTplCfgId", analysisTplCfgId);

        if (CollUtil.isEmpty(widgetConfigs)) {
            return;
        }

        User user = UserManager.get();
        List<AnalysisTplWidgetEntity> widgetEntities = new ArrayList<>(16);
        int index = 0;
        for (WidgetNodeVO widgetNodeVO : widgetConfigs) {
            widgetEntities.addAll(
                    buildWidgetEntities(widgetNodeVO, analysisTplId, analysisTplCfgId
                            , analysisTplId, user.getName(), index++)
            );
        }

        fillQueryTplFieldsFromWidgetSource(widgetEntities, analysisTplId, queryTplFillSourceCfgId);

        dao.insert("ssm.analysisTemplate.batchSaveWidgetConfig", widgetEntities);

        //批量保存AI脚本
        aiScriptService.copyAnalysisTplAiScript(analysisTplId, oldAnalysisTplCfgId, analysisTplCfgId);
    }

    /**
     * 按 widget_id 从指定来源配置或线上版本组件填充 query_tpl_id、query_tpl_viewid_mapping
     *
     * @param queryTplFillSourceCfgId 非空时优先从该 analysis_tpl_cfg_id 加载组件；为空时从 analysisTplId 的线上配置加载
     */
    private void fillQueryTplFieldsFromWidgetSource(List<AnalysisTplWidgetEntity> widgetEntities, String analysisTplId,
                                                    String queryTplFillSourceCfgId) {
        if (CollUtil.isEmpty(widgetEntities)) {
            return;
        }
        String cfgIdToLoad = null;
        if (isNotEmpty(queryTplFillSourceCfgId)) {
            cfgIdToLoad = queryTplFillSourceCfgId;
        } else if (isNotEmpty(analysisTplId)) {
            AnalysisTplCfgProdEntity online = getOnlineConfig(analysisTplId);
            if (online != null && isNotEmpty(online.getAnalysisTplCfgId())) {
                cfgIdToLoad = online.getAnalysisTplCfgId();
            }
        }
        if (isEmpty(cfgIdToLoad)) {
            return;
        }
        List<AnalysisTplWidgetEntity> sourceWidgets = getWidgetConfigs(cfgIdToLoad);
        if (CollUtil.isEmpty(sourceWidgets)) {
            return;
        }
        Map<String, AnalysisTplWidgetEntity> byWidgetId = sourceWidgets.stream()
                .filter(w -> isNotEmpty(w.getWidgetId()))
                .collect(Collectors.toMap(AnalysisTplWidgetEntity::getWidgetId, w -> w, (a, b) -> a));
        for (AnalysisTplWidgetEntity entity : widgetEntities) {
            AnalysisTplWidgetEntity src = byWidgetId.get(entity.getWidgetId());
            if (src == null) {
                continue;
            }
            entity.setQueryTplId(src.getQueryTplId());
            entity.setQueryTplViewIdMapping(src.getQueryTplViewIdMapping());
        }
    }

    /**
     * 将源配置下的组件行复制到新配置（与 {@link #copyAnalysisTplWidgetQueryTplRelFromCfg} 相同方式：先清目标再 INSERT SELECT）
     */
    public void copyWidgetConfigToNewCfg(String analysisTplId, String sourceAnalysisTplCfgId, String targetAnalysisTplCfgId) {
        Map<String, String> params = new HashMap<>(8);
        params.put("analysisTplId", analysisTplId);
        params.put("oldAnalysisTplCfgId", sourceAnalysisTplCfgId);
        params.put("newAnalysisTplCfgId", targetAnalysisTplCfgId);
        params.put("userName", UserManager.get().getName());
        dao.insert("ssm.analysisTemplate.copyWidgetConfigToNewCfg", params);

        //批量保存AI脚本
        aiScriptService.copyAnalysisTplAiScript(analysisTplId, sourceAnalysisTplCfgId, targetAnalysisTplCfgId);
    }


    /**
     * 从指定源配置复制 ssm_analysis_tpl_widget_query_tpl_rel 到目标配置
     */
    public void copyAnalysisTplWidgetQueryTplRelFromCfg(String analysisTplId, String oldAnalysisTplCfgId, String newAnalysisTplCfgId) {
        dao.delete("ssm.analysisTemplate.deleteWidgetQueryTplRelByAnalysisTplCfgId", newAnalysisTplCfgId);
        Map<String, String> params = new HashMap<>(8);
        params.put("analysisTplId", analysisTplId);
        params.put("oldAnalysisTplCfgId", oldAnalysisTplCfgId);
        params.put("newAnalysisTplCfgId", newAnalysisTplCfgId);
        params.put("userName", UserManager.get().getName());
        dao.insert("ssm.analysisTemplate.copyAnalysisTplWidgetQueryTplRel", params);
    }

    /**
     * 从线上版本组件模版id的关联关系
     * @param analysisTplId
     * @param analysisTplCfgId
     */
    public void copyAnalysisTplWidgetQueryTplRel(String analysisTplId,String analysisTplCfgId){
        dao.delete("ssm.analysisTemplate.deleteWidgetQueryTplRelByAnalysisTplCfgId", analysisTplCfgId);

        AnalysisTplCfgProdEntity prodEntity = getOnlineConfig(analysisTplId);
        if (prodEntity != null) {
            Map<String, String> params = new HashMap<>();
            params.put("analysisTplId", analysisTplId);
            params.put("oldAnalysisTplCfgId", prodEntity.getAnalysisTplCfgId());
            params.put("newAnalysisTplCfgId", analysisTplCfgId);
            params.put("userName", UserManager.get().getName());
            dao.insert("ssm.analysisTemplate.copyAnalysisTplWidgetQueryTplRel", params);
        }
    }

    /**
     * 视图快照：写入 ssm_analysis_tpl_cfg_prod 首条线上版本，并将看板置为已发布、在线
     */
    public void insertProdSnapshotAndMarkTplOnline(String analysisTplId, String analysisTplCfgId, String userName) {
        dao.update("ssm.analysisTemplate.updateOnlineConfigStatus", analysisTplId);
        AnalysisTplCfgProdEntity prodEntity = AnalysisTplCfgProdEntity.builder()
                .analysisTplCfgId(analysisTplCfgId)
                .analysisTplId(analysisTplId)
                .versionNo(1L)
                .createdBy(userName)
                .updatedBy(userName)
                .isLatest(1)
                .build();
        dao.update("ssm.analysisTemplate.addOnlineConfig", prodEntity);

        AnalysisTemplateEntity templateEntity = getTemplateBase(analysisTplId);
        checkState(templateEntity != null, "看板不存在");
        templateEntity.setUpdatedBy(userName);
        templateEntity.setDraftStatus(AnalysisTemplateDraftStatus.PUBLISHED.getCode());
        templateEntity.setOnlineStatus(AnalysisTemplateOnlineStatus.ONLINE.getCode());
        dao.update("ssm.analysisTemplate.updateAnalysisTemplateBase", templateEntity);
    }

    /**
     * 删除组件， 反向删除
     *
     * @param analysisTplCfgId 配置ID
     * @param onUsedWidgetIds  这里是正在使用的组件ID，反向删除
     */
    private void deleteWidgets(String analysisTplCfgId, List<String> onUsedWidgetIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("analysisTplCfgId", analysisTplCfgId);
        params.put("onUsedWidgetIds", onUsedWidgetIds);
        dao.update("ssm.analysisTemplate.deleteWidgetConfig", params);
    }

    private void addVisitLog(String analysisTplId, String portalId, AnalysisTemplateExecMode getMode) {

        //|| StringUtils.isEmpty(portalId 存在临时快照视图，不归属到门户
        if (!AnalysisTemplateExecMode.PROD.equals(getMode)) {
            return;
        }
        AnalysisTemplateVisitLogReq visitLogReq = AnalysisTemplateVisitLogReq.builder()
                .analysisTplId(analysisTplId)
                .portalId(portalId)
                .visitType(AnalysisTplVisitType.ANALYSIS_TEMPLATE.getCode())
                .build();
        AnalysisTemplateLogService.addVisitLog(visitLogReq);
    }

    private void addOperationLog(String analysisTplId, String analysisTplCfgId, String portalId,
                                 AnalysisTemplateOperationType operationType, String operationReason) {
        AnalysisTplOperationLogEntity operationLog = AnalysisTplOperationLogEntity.builder()
                .analysisTplId(analysisTplId)
                .portalId(portalId)
                .analysisTplCfgId(analysisTplCfgId)
                .operationType(operationType.getCode())
                .operationReason(operationReason)
                .createdBy(UserManager.get().getName())
                .build();
        dao.update("ssm.analysis.template.operation.log.addOperationLog", operationLog);
    }

    /**
     * 查询看板列表
     * @return
     */
    public AnalysisTemplateListRsp list(AnalysisTemplateListQueryReq analysisTemplateListQueryReq){

        //查询看板id
        List<String> analysisTplIds = new ArrayList<>();

        PageHelper.startPage(analysisTemplateListQueryReq.getPageNum(), analysisTemplateListQueryReq.getPageSize());
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("userName",UserManager.get().getName());
        /* genAI_feature/v3.15.0_start */
        queryMap.put("analysisTplType", AnalysisTplType.NORMAL.getCode());
        /* genAI_feature/v3.15.0_end */

        TabType tabType = TabType.get(analysisTemplateListQueryReq.getTabType());
        switch (tabType){
            case VISIT:
                analysisTplIds =  getRecentVisitAnalysisTplIds(queryMap);
                break;
            case CREATE:
                analysisTplIds = getUserCreateAnalysisTplIds(queryMap);
                break;
        }

        //获取总行数
        PageInfo<String> analysisTplIdPageInfo = new PageInfo<>(analysisTplIds);

        if(CollUtil.isEmpty(analysisTplIds)){
            return new AnalysisTemplateListRsp();
        }

        //查询所有的门户、菜单，为构建路径使用
        Map<String,String> analysisTemplateMenuMap = new HashMap<>();
        Map<String,PortalMenu> portalMenuMap = new HashMap<>();
        Map<String,PortalRsp> portalMap = new HashMap<>();

        List<PortalRsp> portalList =  (List<PortalRsp>) dao.queryObjectList("ssm.portal.list", null);
        List<PortalMenu> portalMenuList = portalMenuService.getMenuList(null);
        List<AnalysisTemplateBaseVO> analysisTemplateVOList = getAnalysisTemplateBaseInfo(analysisTplIds);

        Map<String,AnalysisTemplateBaseVO> analysisTemplateMap = new HashMap<>();
        analysisTemplateVOList.stream().forEach(a->analysisTemplateMap.put(a.getAnalysisTplId(),a));


        portalList.stream().forEach(v->portalMap.put(v.getPortalId(),v));
        for(PortalMenu portalMenu : portalMenuList){
            analysisTemplateMenuMap.put(portalMenu.getContentRefId(),portalMenu.getMenuId());
            portalMenuMap.put(portalMenu.getMenuId(),portalMenu);
        }

        List<AnalysisTemplateListItemRsp> analysisTemplateListItemRspList = new ArrayList<>();

        //为保证排序，遍历看板id
        for( String analysisTplId : analysisTplIds){

            AnalysisTemplateBaseVO templateVO = analysisTemplateMap.get(analysisTplId);

            String menuId = analysisTemplateMenuMap.get(templateVO.getAnalysisTplId());
            String portalId = "";
            PortalMenu portalMenu = portalMenuMap.get(menuId);
            if(portalMenu != null){
                portalId = portalMenu.getPortalId();
            }
            String path = getAnalysisTemplatePath(portalMap,portalMenuMap,menuId);

            AnalysisTemplateListItemRsp templateListItemRsp = AnalysisTemplateListItemRsp.builder()
                    .analysisTplId(templateVO.getAnalysisTplId())
                    .analysisTplName(templateVO.getAnalysisTplName())
                    .publishStatus(templateVO.getPublishStatus())
                    .analysisTplDesc(templateVO.getAnalysisTplDesc())
                    .path(path)
                    .updateBy(templateVO.getUpdatedBy())
                    .updateTime(DATE_FORMAT.format(templateVO.getUpdatedTime()))
                    .menuId(menuId)
                    .portalId(portalId)
                    .build();

            analysisTemplateListItemRspList.add(templateListItemRsp);
        }

        //封装结果集
        AnalysisTemplateListRsp analysisTemplateListRsp = new AnalysisTemplateListRsp();
        analysisTemplateListRsp.setTotal(analysisTplIdPageInfo.getTotal());
        analysisTemplateListRsp.setList(analysisTemplateListItemRspList);
        return analysisTemplateListRsp;
    }

    /**
     * 构建看板路径
     * @param portalMap
     * @param portalMenuMap
     * @param menuId
     * @return
     */
    public String getAnalysisTemplatePath(Map<String,PortalRsp> portalMap,Map<String,PortalMenu> portalMenuMap,String menuId) {

        List<String> paths = new ArrayList<>();

        if(StrUtil.isEmpty(menuId)){
            return "";
        }

        PortalMenu portalMenu = portalMenuMap.get(menuId);
        PortalMenu parentMenu = portalMenuMap.get(portalMenu.getParentMenuId());

        int i = 50;
        while (parentMenu != null && i > 0) {
            paths.add(parentMenu.getMenuName());
            parentMenu = portalMenuMap.get(parentMenu.getParentMenuId());
            i--;
        }

        //添加门户
        PortalRsp portalRsp = portalMap.get(portalMenu.getPortalId());
        paths.add(portalRsp.getPortalName());

        paths = Lists.reverse(paths);
        String path = BIUtil.listToStr(paths, "-");

        return path;
    }

    /**
     * 获取最近查看的看板id
     * @return
     */
    public List<String> getRecentVisitAnalysisTplIds(Map<String,Object> queryMap) {
        List<String> analysisTplIds = (List<String>) dao.queryObjectList("ssm.analysis.template.visit.log.getRecentVisitAnalysisTplIds", queryMap);
        return analysisTplIds;
    }

    /**
     * 获取我创建的看板id
     * @param queryMap
     * @return
     */
    public List<String> getUserCreateAnalysisTplIds(Map<String,Object> queryMap){
        List<String> analysisTplIds = (List<String>) dao.queryObjectList("ssm.analysisTemplate.getUserCreateAnalysisTplIds", queryMap);
        return analysisTplIds;
    }

    /**
     * 通过门户id查询看板列表
     * 限定已上线的门户看板
     * @param portalId
     * @return
     */
    public List<AnalysisTemplateVO> getProdAnalysisTplByPortalId(String portalId) {
        List<AnalysisTemplateVO> result = (List<AnalysisTemplateVO>) dao.queryObjectList("ssm.analysisTemplate.getProdAnalysisTplByPortalId", portalId);
        return result;
    }

    /**
     * 通过看板id查询看板基础信息
     * 限定已上线的门户看板
     * @param analysisTplId
     * @return
     */
    public AnalysisTemplateVO getProdAnalysisTplByAnalysisTplId(String analysisTplId) {
        AnalysisTemplateVO analysisTemplateVO = (AnalysisTemplateVO) dao.queryObject("ssm.analysisTemplate.getProdAnalysisTplByAnalysisTplId", analysisTplId);

        //判断是否有全局筛选器
        AnalysisTemplateEntity templateEntity = getTemplateBase(analysisTplId);
        AnalysisTemplateVO res = getOnlineAnalysisTemplateVO(templateEntity, AnalysisTemplateExecMode.PROD, "");

        int globalFilterCount = 0;
        int globalDateFilterCount = 0;
        for (WidgetNodeVO widgetNodeVO : res.getWidgetConfigs()) {
            AnalysisTemplateWidgetType widgetType = AnalysisTemplateWidgetType.get(widgetNodeVO.getWidgetTypeCode());

            if (AnalysisTemplateWidgetType.GLOBALFILTERGROUP != widgetType) {
                continue;
            }

            globalFilterCount++;
            for (WidgetNodeVO child : widgetNodeVO.getChildren()) {
                if (child != null && "dateFilter".equals(child.getWidgetTypeCode())) {
                    globalDateFilterCount++;
                    break;
                }
            }
            break;
        }

        if (globalFilterCount > 0) {
            analysisTemplateVO.setHasGlobalFilter(Enabled.YES.getId());
        }

        if (globalDateFilterCount > 0) {
            analysisTemplateVO.setHasGlobalDateFilter(Enabled.YES.getId());
        }

        return analysisTemplateVO;
    }


    /**
     * 保存etl任务
     * @param analysisTplId
     */
    /**
     * 解析看板关联的所有视图对应 MetaTable，供 saveEtlJob / getEtlJobByAnalysisTplId 复用。
     */
    public List<AnalysisTemplateEtlJobResp> resolveMetaTables(String analysisTplId) {
        AnalysisTemplateEntity templateEntity = getTemplateBase(analysisTplId);
        if (templateEntity == null) {
            return emptyList();
        }
        AnalysisTemplateVO res = getOnlineAnalysisTemplateVO(templateEntity, AnalysisTemplateExecMode.PROD, "");

        List<String> tplIds = new ArrayList<>();
        List<String> viewIds = new ArrayList<>();
        for (WidgetNodeVO widgetNodeVO : res.getWidgetConfigs()) {
            AnalysisTemplateWidgetType widgetType = AnalysisTemplateWidgetType.get(widgetNodeVO.getWidgetTypeCode());
            if (AnalysisTemplateWidgetType.QUERYTEMPLATE != widgetType) {
                continue;
            }
            List<TemplateViewMapping> queryTplViewIdMappingList = widgetNodeVO.getQueryTplViewIdMappingList();
            if (CollUtil.isEmpty(queryTplViewIdMappingList)) {
                tplIds.add(widgetNodeVO.getQueryTplId());
            } else {
                JSONObject options = JSONObject.parseObject(widgetNodeVO.getWidgetOptions());
                String viewId = options.getString("viewId");
                Optional<TemplateViewMapping> optional = queryTplViewIdMappingList.stream()
                        .filter(mapping -> mapping.getOldViewId().equals(viewId)).findAny();
                if (optional.isPresent()) {
                    viewIds.add(optional.get().getNewViewId());
                } else {
                    tplIds.add(widgetNodeVO.getQueryTplId());
                }
            }
        }

        if (CollUtil.isNotEmpty(tplIds)) {
            viewIds.addAll((List<String>) dao.queryObjectList("ssm.template.view.getValidViewIdByTplIdList", tplIds));
        }
        if (CollUtil.isEmpty(viewIds)) {
            return emptyList();
        }

        List<TemplateViewEntity> viewEntityList = (List<TemplateViewEntity>) dao.queryObjectList("ssm.template.view.queryViewWithCfgByIds", viewIds);
        if (CollUtil.isEmpty(viewEntityList)) {
            return emptyList();
        }

        SSDQueryTemplate tpl = (SSDQueryTemplate) dao.queryObject("ssm.template.queryTemplateById",
                singletonMap("templateId", viewEntityList.get(0).getTplId()));
        if (tpl == null) {
            return emptyList();
        }

        List<String> configList = viewEntityList.stream().map(TemplateViewEntity::getTplConfig).collect(Collectors.toList());
        return SSDUtil.getEtlJobByConfig(configList, tpl.getDatasetId(), Enabled.YES.getId()).stream()
                .map(v -> toEtlJobResp(analysisTplId, v, tpl.getDatasetId())).collect(Collectors.toList());
    }

    private AnalysisTemplateEtlJobResp toEtlJobResp(String analysisTplId, MetaTable t, String datasetId) {
        AnalysisTemplateEtlJobResp resp = new AnalysisTemplateEtlJobResp();
        resp.setTplId(analysisTplId);
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

    public void saveEtlJob(String analysisTplId, List<AnalysisTemplateEtlJobResp> etlJobResps) {
        if (CollUtil.isEmpty(etlJobResps)) {
            return;
        }
        List<String> etljobs = etlJobResps.stream()
                .flatMap(t -> t.getEtlJobs().stream())
                .distinct()
                .collect(Collectors.toList());
        String username = UserManager.get().getName();
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.delete("ssm.analysisTemplate.deleteEtlJobByAnalysisTplId", analysisTplId);
                if (CollUtil.isNotEmpty(etljobs)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("analysisTplId", analysisTplId);
                    params.put("etlJobList", etljobs);
                    params.put("userName", username);
                    dao.insert("ssm.analysisTemplate.batchInsertEtlJob", params);
                }
            }
        });
    }

    /**
     * 获取看板关联的 ETL 作业及表信息。兼容未初始化数据：若 DB 无记录则先执行懒初始化。
     */
    public List<AnalysisTemplateEtlJobResp> getEtlJobByAnalysisTplId(String analysisTplId) {
        List<String> etljobs = (List<String>) dao.queryObjectList("ssm.analysisTemplate.getEtlJobByAnalysisTplId", analysisTplId);
        List<AnalysisTemplateEtlJobResp> etlJobResps = resolveMetaTables(analysisTplId);
        if (CollUtil.isEmpty(etljobs)) {
            saveEtlJob(analysisTplId, etlJobResps);
        }
        return etlJobResps;
    }

}
