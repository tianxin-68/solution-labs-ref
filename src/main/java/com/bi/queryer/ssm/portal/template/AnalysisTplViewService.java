package com.bi.queryer.ssm.portal.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.meta.SSDQueryTemplate;
import com.bi.queryer.ssm.portal.PortalService;
import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.enums.AnalysisTplType;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplShareRelEntity;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplViewEntity;
import com.bi.queryer.ssm.portal.enums.DragType;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTplViewSaveVO;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTplViewSnapshotRsp;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTplViewVO;
import com.bi.queryer.ssm.portal.template.vo.WidgetNodeVO;
import com.bi.queryer.ssm.query.template.TemplateConfigService;
import com.bi.queryer.ssm.query.template.enums.TemplateViewType;
import com.bi.queryer.ssm.query.template.enums.ViewStatusType;
import com.bi.queryer.ssm.query.template.view.model.TemplateViewEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import com.bi.queryer.util.SpringContextUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bi.queryer.util.BIUtil.isNotEmpty;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * 看板视图配置 ssm_analysis_tpl_view
 */
@Service
@Scope("prototype")
public class AnalysisTplViewService {

    @Autowired
    private BaseDao dao;

    public String create(AnalysisTplViewSaveVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewName()), "视图名称为空");
        User user = UserManager.get();
        String userName = user.getName();
        String viewId = Guid.id();
        String analysisTplCfgId = Guid.id();


        Double nextSortId = dao.queryObject("ssm.analysis.tpl.view.nextSortIdByAnalysisTplId",
                vo.getAnalysisTplId(), Double.class);
        if (nextSortId == null) {
            nextSortId = 1.0;
        }

        Integer isActive = Enabled.YES.getId();
        AnalysisTplViewEntity entity = AnalysisTplViewEntity.builder()
                .viewId(viewId)
                .viewName(vo.getViewName())
                .viewType(vo.getViewType())
                .analysisTplId(vo.getAnalysisTplId())
                .analysisTplCfgId(analysisTplCfgId)
                .isActive(isActive)
                .sortId(nextSortId)
                .createdBy(userName)
                .updatedBy(userName)
                .build();

        //获取当前页面的看板配置ID,供复制py脚本使用
        String oldAnalysisTplCfgId = "";
        if(StrUtil.isNotEmpty(vo.getViewId())) {
            AnalysisTplViewEntity sourceView = getEntityById(vo.getViewId());
            if(sourceView != null){
                oldAnalysisTplCfgId = sourceView.getAnalysisTplCfgId();
            }
        }

        String finalOldAnalysisTplCfgId = oldAnalysisTplCfgId;
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("ssm.analysis.tpl.view.insert", entity);
                AnalysisTemplateService analysisTemplateService = (AnalysisTemplateService)SpringContextUtil.getBean("analysisTemplateService");
                //保存ssm_analysis_tpl_widget
                analysisTemplateService.batchSaveWidgetConfig(vo.getWidgetConfigs(), vo.getAnalysisTplId(), analysisTplCfgId,
                        vo.getQueryTplFillSourceCfgId(), finalOldAnalysisTplCfgId);
                analysisTemplateService.copyAnalysisTplWidgetQueryTplRel(vo.getAnalysisTplId(), analysisTplCfgId);

            }
        });

        return viewId;
    }

    /**
     * 仅写入 ssm_analysis_tpl_view，不保存组件、不复制 query_tpl 关联
     *
     * @param viewName 视图名称，为空时默认「公共视图」
     * @return 新生成的 view_id
     */
    public String insertViewRowOnly(String analysisTplId, String analysisTplCfgId, String viewName) {
        checkArgument(isNotEmpty(analysisTplId), "看板模板 id 为空");
        checkArgument(isNotEmpty(analysisTplCfgId), "看板配置 id 为空");
        User user = UserManager.get();
        String userName = user.getName();
        String newViewId = Guid.id();
        String resolvedName = isNotEmpty(viewName) ? viewName : "公共视图";

        Double nextSortId = dao.queryObject("ssm.analysis.tpl.view.nextSortIdByAnalysisTplId",
                analysisTplId, Double.class);
        if (nextSortId == null) {
            nextSortId = 1.0;
        }

        AnalysisTplViewEntity entity = AnalysisTplViewEntity.builder()
                .viewId(newViewId)
                .viewName(resolvedName)
                .viewType(TemplateViewType.PUBLIC.getCode())
                .analysisTplId(analysisTplId)
                .analysisTplCfgId(analysisTplCfgId)
                .isActive(Enabled.YES.getId())
                .sortId(nextSortId)
                .createdBy(userName)
                .updatedBy(userName)
                .build();

        dao.insert("ssm.analysis.tpl.view.insert", entity);
        return newViewId;
    }

    /**
     * 视图快照：新建临时看板（ssm_analysis_tpl_base）、写入线上配置（ssm_analysis_tpl_cfg_prod），并调用 {@link #create} 生成视图。
     *
     * @param vo viewId 源视图；viewName 可选，默认「源视图名_快照」
     * @return 新看板 id 与新视图 id
     */
    public AnalysisTplViewSnapshotRsp snapshotView(AnalysisTplViewSaveVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewId()), "源视图id为空");
        AnalysisTplViewEntity sourceView = getEntityById(vo.getViewId());
        checkArgument(sourceView != null, "源视图不存在");

        User user = UserManager.get();
        String userName = user.getName();
        AnalysisTemplateService analysisTemplateService =
                (AnalysisTemplateService) SpringContextUtil.getBean("analysisTemplateService");

        AnalysisTemplateEntity templateBase = analysisTemplateService.getTemplateBase(sourceView.getAnalysisTplId());

        String newAnalysisTplId = Guid.id();
        AnalysisTemplateVO baseVo = new AnalysisTemplateVO();
        String baseViewName = sourceView.getViewName();
        String snapName = baseViewName + "_快照";
        baseVo.setAnalysisTplName(snapName);
        baseVo.setAnalysisTplDesc(snapName);
        baseVo.setAnalysisTplType(AnalysisTplType.SNAPSHOT.getCode());
        baseVo.setSourceDataType(templateBase.getSourceDataType());
        baseVo.setHasAiSummary(templateBase.getHasAiSummary());
        analysisTemplateService.addAnalysisTemplateBase(baseVo, newAnalysisTplId, userName);

        AnalysisTplViewSaveVO saveVO = new AnalysisTplViewSaveVO();
        saveVO.setAnalysisTplId(newAnalysisTplId);
        saveVO.setViewName(baseViewName);
        saveVO.setViewType(TemplateViewType.PERSONAL.getCode());
        saveVO.setWidgetConfigs(vo.getWidgetConfigs());
        saveVO.setQueryTplFillSourceCfgId(sourceView.getAnalysisTplCfgId());
        saveVO.setViewId(vo.getViewId());

        String newViewId = create(saveVO);

        AnalysisTplViewEntity created = getEntityById(newViewId);
        checkArgument(created != null, "快照视图创建失败");
        analysisTemplateService.insertProdSnapshotAndMarkTplOnline(newAnalysisTplId, created.getAnalysisTplCfgId(), userName);

        // 记录看板快照的分享血缘（源看板 -> 新看板），与 TemplateShareService#shareTplSnapshot 末尾写入 ssd_query_template_share_rel 一致
        insertAnalysisTplShareRel(sourceView.getAnalysisTplId(), newAnalysisTplId, userName);

        return AnalysisTplViewSnapshotRsp.builder()
                .analysisTplId(newAnalysisTplId)
                .viewId(newViewId)
                .build();
    }

    /**
     * 写入 ssm_analysis_tpl_share_rel：源看板 id 为快照来源看板，目标看板 id 为本次生成的新看板；
     * root 通过「源看板作为 target」在分享关系表上反查已有 root，若无则根视为源看板本身。
     *
     * @param sourceAnalysisTplId 源看板（快照前的 analysis_tpl_id）
     * @param targetAnalysisTplId 新生成的快照看板 id
     * @param createdBy           操作人
     */
    private void insertAnalysisTplShareRel(String sourceAnalysisTplId, String targetAnalysisTplId, String createdBy) {
        if (!isNotEmpty(sourceAnalysisTplId) || !isNotEmpty(targetAnalysisTplId)) {
            return;
        }
        AnalysisTplShareRelEntity rel = new AnalysisTplShareRelEntity();
        rel.setSourceAnalysisTplId(sourceAnalysisTplId);
        rel.setTargetAnalysisTplId(targetAnalysisTplId);
        rel.setCreatedBy(createdBy);
        // 此时源看板在链上可能已是他人分享的「目标」，取其 root；否则根即源看板（对齐 shareTplSnapshot 中 queryRootTplIdByTargetTplId 逻辑）
        String rootAnalysisTplId = dao.queryObject(
                "ssm.analysis.tpl.share.rel.queryRootAnalysisTplIdByTargetAnalysisTplId",
                sourceAnalysisTplId,
                String.class);
        if (StrUtil.isEmpty(rootAnalysisTplId)) {
            rootAnalysisTplId = sourceAnalysisTplId;
        }
        rel.setRootAnalysisTplId(rootAnalysisTplId);
        dao.insert("ssm.analysis.tpl.share.rel.insert", rel);
    }

    /**
     * 从已有视图复制出一个新视图（新 view_id、新 analysis_tpl_cfg_id，组件与关联从源视图拷贝）
     *
     * @param vo viewId 源视图；viewName 可选，默认「源名称 + 副本」
     * @return 新视图 viewId
     */
    public String copy(AnalysisTplViewVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewId()), "源视图id为空");
        AnalysisTplViewEntity source = getEntityById(vo.getViewId());
        checkArgument(source != null, "源视图不存在");

        checkViewAuth(source, vo.getViewId(), "复制");

        User user = UserManager.get();
        String userName = user.getName();
        String newViewId = Guid.id();
        String newCfgId = Guid.id();
        String viewName = source.getViewName() + "_副本";

        Double nextSortId = dao.queryObject("ssm.analysis.tpl.view.nextSortIdByAnalysisTplId",
                source.getAnalysisTplId(), Double.class);
        if (nextSortId == null) {
            nextSortId = 1.0;
        }

        AnalysisTplViewEntity entity = AnalysisTplViewEntity.builder()
                .viewId(newViewId)
                .viewName(viewName)
                .viewType(source.getViewType())
                .analysisTplId(source.getAnalysisTplId())
                .analysisTplCfgId(newCfgId)
                .isActive(Enabled.YES.getId())
                .sortId(nextSortId)
                .createdBy(userName)
                .updatedBy(userName)
                .build();

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.insert("ssm.analysis.tpl.view.insert", entity);
                AnalysisTemplateService analysisTemplateService =
                        (AnalysisTemplateService) SpringContextUtil.getBean("analysisTemplateService");
                analysisTemplateService.copyWidgetConfigToNewCfg(
                        source.getAnalysisTplId(), source.getAnalysisTplCfgId(), newCfgId);
                analysisTemplateService.copyAnalysisTplWidgetQueryTplRelFromCfg(
                        source.getAnalysisTplId(), source.getAnalysisTplCfgId(), newCfgId);
            }
        });

        return newViewId;
    }

    public String update(AnalysisTplViewSaveVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewId()), "视图id为空");
        User user = UserManager.get();
        String userName =  user.getName();

        AnalysisTplViewEntity viewEntity = getEntityById(vo.getViewId());

        checkViewAuth(viewEntity, vo.getViewId(), "更新");

        AnalysisTplViewEntity entity = AnalysisTplViewEntity.builder()
                .viewId(vo.getViewId())
                .viewName(vo.getViewName())
                .viewType(vo.getViewType())
                .analysisTplId(viewEntity.getAnalysisTplId())
                .analysisTplCfgId(viewEntity.getAnalysisTplCfgId())
                .updatedBy(userName)
                .build();

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                dao.update("ssm.analysis.tpl.view.update", entity);
                AnalysisTemplateService analysisTemplateService = (AnalysisTemplateService)SpringContextUtil.getBean("analysisTemplateService");
                //保存ssm_analysis_tpl_widget
                analysisTemplateService.batchSaveWidgetConfig(vo.getWidgetConfigs(), viewEntity.getAnalysisTplId(), viewEntity.getAnalysisTplCfgId(),
                        vo.getQueryTplFillSourceCfgId(),"");
                //插入ssm_analysis_tpl_widget_query_tpl_rel
                analysisTemplateService.copyAnalysisTplWidgetQueryTplRel(viewEntity.getAnalysisTplId(), viewEntity.getAnalysisTplCfgId());
            }
        });


        return vo.getViewId();
    }

    /**
     * 重命名视图（仅更新名称）
     */
    public void rename(AnalysisTplViewVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewId()), "视图id为空");
        checkArgument(isNotEmpty(vo.getViewName()), "视图名称为空");
        User user = UserManager.get();
        AnalysisTplViewEntity viewEntity = getEntityById(vo.getViewId());
        checkArgument(viewEntity != null, "视图不存在");

        checkViewAuth(viewEntity, vo.getViewId(), "重命名");

        Map<String, String> param = new HashMap<>(4);
        param.put("viewId", vo.getViewId());
        param.put("viewName", vo.getViewName());
        param.put("updatedBy", user.getName());
        dao.update("ssm.analysis.tpl.view.rename", param);
    }

    /**
     * 视图排序移动：被移动视图与目标视图需属于同一 analysis_tpl_id，逻辑对齐 {@link com.bi.queryer.ssm.query.template.view.TemplateViewService#move}
     *
     * @param viewId       被移动的视图 id
     * @param targetViewId 目标视图 id（排在其前或后由 dragType 决定）
     * @param dragType     before：放在目标同 sort_id；after：放在目标 sort_id+1
     */
    public void move(String viewId, String targetViewId, String dragType) {
        checkArgument(isNotEmpty(viewId), "视图id为空");
        checkArgument(isNotEmpty(targetViewId), "目标视图id为空");
        checkArgument(isNotEmpty(dragType), "拖拽类型为空");

        AnalysisTplViewEntity templateView = getEntityById(viewId);

        checkViewAuth(templateView, viewId, "移动");

        AnalysisTplViewEntity targetTemplateView = getEntityById(targetViewId);
        checkArgument(templateView != null, "被移动的视图不存在");
        checkArgument(targetTemplateView != null, "目标视图不存在");
        checkArgument(templateView.getAnalysisTplId().equals(targetTemplateView.getAnalysisTplId()),
                "只能在同一看板模板下调整视图顺序");
        checkArgument(targetTemplateView.getSortId() != null, "目标视图排序为空");

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                DragType dgType = DragType.get(dragType);
                double targetSort = targetTemplateView.getSortId();
                if (DragType.AFTER == dgType) {
                    templateView.setSortId(targetSort + 1);
                } else {
                    templateView.setSortId(targetSort);
                }

                Map<String, Object> paramMap = new HashMap<>(8);
                paramMap.put("analysisTplId", templateView.getAnalysisTplId());
                paramMap.put("sortId", targetSort);
                paramMap.put("dragType", dgType.getCode());

                String userName = UserManager.get().getName();
                paramMap.put("userName", userName);

                templateView.setUpdatedBy(userName);
                dao.update("ssm.analysis.tpl.view.incrementViewSortId", paramMap);
                dao.update("ssm.analysis.tpl.view.updateViewSortId", templateView);
            }
        });
    }

    public boolean delete(AnalysisTplViewVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewId()), "视图id为空");

        AnalysisTplViewEntity templateView = getEntityById(vo.getViewId());
        checkViewAuth(templateView, vo.getViewId(), "删除");

        dao.delete("ssm.analysis.tpl.view.deleteById", vo.getViewId());
        return true;
    }

    /**
     * 按看板模板 id 一次性逻辑删除其下全部公共视图（与 {@link #delete} 相同：is_active=0）
     */
    public void deletePublicViewsByAnalysisTplId(String analysisTplId) {
        checkArgument(isNotEmpty(analysisTplId), "看板模板 id 为空");
        User user = UserManager.get();
        String userName = user != null ? user.getName() : "";
        Map<String, Object> param = new HashMap<>(4);
        param.put("analysisTplId", analysisTplId);
        param.put("viewType", TemplateViewType.PUBLIC.getCode());
        param.put("updatedBy", userName);
        dao.update("ssm.analysis.tpl.view.deletePublicByAnalysisTplId", param);
    }

    public AnalysisTplViewVO get(AnalysisTplViewVO vo) {
        checkArgument(vo != null, "传入参数为空");
        checkArgument(isNotEmpty(vo.getViewId()), "视图id为空");
        return toVo(getEntityById(vo.getViewId()));
    }

    /**
     * 检查视图权限
     * 公共视图 = 门户管理员有权限
     * 个人视图 = 视图owner有权限
     */
    public void checkViewAuth(AnalysisTplViewEntity viewEntity,String viewId, String operatorName) {

        if(viewEntity == null) {
            throw new SSDException("视图" + viewId + "不存在");
        }

        User user = UserManager.get();
        TemplateViewType viewType = TemplateViewType.get(viewEntity.getViewType());

        if (TemplateViewType.PUBLIC == viewType) {

            boolean isPortalAdmin = checkIsPortalAdmin(viewEntity.getAnalysisTplId());
            if (!isPortalAdmin) {
                throw new SSDException("您没有权限" + operatorName + "该公共视图！");
            }

        }

        if (TemplateViewType.PERSONAL == viewType) {
            //只有视图owner 才能操作
            if (viewEntity.getCreatedBy().equalsIgnoreCase(user.getName())) {
                return;
            }
            throw new SSDException("您没有权限" + operatorName + "该个人视图！");
        }
    }

    public Boolean checkIsPortalAdmin(String analysisTplId) {
        PortalService portalService = (PortalService) SpringContextUtil.getBean("portalService");
        Portal portal = portalService.getByContentRefId(analysisTplId);
        if (portal == null) {
            return false;
        }
        PortalAuthService portalAuthService = (PortalAuthService) SpringContextUtil.getBean("portalAuthService");
        return portalAuthService.isPortalAdmin(portal.getPortalId());
    }

    /**
     * 根据看板模板ID获取视图列表
     */
    public List<AnalysisTplViewVO> getByAnalysisTplId(String analysisTplId) {
        checkArgument(isNotEmpty(analysisTplId), "看板模板ID不能为空");

        User user = UserManager.get();
        List<AnalysisTplViewVO> result = new ArrayList<>();

        //判断是不是门户管理员
        boolean isPortalAdmin = checkIsPortalAdmin(analysisTplId);

        //查询公共视图
        List<AnalysisTplViewVO> publicViews = list(
                AnalysisTplViewVO.builder()
                        .analysisTplId(analysisTplId)
                        .viewType(TemplateViewType.PUBLIC.getCode())
                        .build()
        );

        result.addAll(publicViews);

        //查询私有视图
        List<AnalysisTplViewVO> privateViews = list(
                AnalysisTplViewVO.builder()
                        .analysisTplId(analysisTplId)
                        .viewType(TemplateViewType.PERSONAL.getCode())
                        .createdBy(user.getName())
                        .build()
        );

        if (!CollUtil.isEmpty(privateViews)) {
            result.addAll(privateViews);
        }

        Map<String, String> defaultParam = new HashMap<>(4);
        defaultParam.put("analysisTplId", analysisTplId);
        defaultParam.put("createdBy", user.getName());
        String defaultViewId = dao.queryObject("ssm.analysis.tpl.view.user.cfg.queryUserDefaultViewId", defaultParam,
                String.class);
        for (AnalysisTplViewVO vo : result) {
            vo.setIsDefault(defaultViewId != null && Objects.equals(defaultViewId, vo.getViewId())
                    ? Enabled.YES.getId()
                    : Enabled.NO.getId());

            TemplateViewType viewType = TemplateViewType.get(vo.getViewType());
            if (TemplateViewType.PUBLIC == viewType) {
                vo.setCanEdit(isPortalAdmin ? Enabled.YES.getId() : Enabled.NO.getId());
            } else {
                vo.setCanEdit(vo.getCreatedBy().equalsIgnoreCase(user.getName()) ? Enabled.YES.getId() : Enabled.NO.getId());
            }
        }

        return result;
    }

    public List<AnalysisTplViewVO> list(AnalysisTplViewVO query) {
        Map<String, Object> param = new HashMap<>(8);
        if (query != null) {
            if (isNotEmpty(query.getAnalysisTplId())) {
                param.put("analysisTplId", query.getAnalysisTplId());
            }
            if (isNotEmpty(query.getViewType())) {
                param.put("viewType", query.getViewType());
            }
            //按创建时间倒序
            if(isNotEmpty(query.getCreatedBy())){
                param.put("createdBy", query.getCreatedBy());
            }
        }
        List<AnalysisTplViewEntity> list = dao.queryObjectList("ssm.analysis.tpl.view.listByCondition", param, AnalysisTplViewEntity.class);
        if (CollectionUtils.isEmpty(list)) {
            return java.util.Collections.emptyList();
        }
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    public AnalysisTplViewEntity getEntityById(String viewId) {
        return dao.queryObject("ssm.analysis.tpl.view.getById", viewId, AnalysisTplViewEntity.class);
    }

    private AnalysisTplViewVO toVo(AnalysisTplViewEntity e) {
        if (e == null) {
            return null;
        }
        AnalysisTplViewVO.AnalysisTplViewVOBuilder b = AnalysisTplViewVO.builder()
                .viewId(e.getViewId())
                .viewName(e.getViewName())
                .viewType(e.getViewType())
                .analysisTplId(e.getAnalysisTplId())
                .analysisTplCfgId(e.getAnalysisTplCfgId())
                .isActive(e.getIsActive())
                .sortId(e.getSortId())
                .createdBy(e.getCreatedBy())
                .createdTime(e.getCreatedTime())
                .updatedBy(e.getUpdatedBy())
                .updatedTime(e.getUpdatedTime());
        return b.build();
    }
}
