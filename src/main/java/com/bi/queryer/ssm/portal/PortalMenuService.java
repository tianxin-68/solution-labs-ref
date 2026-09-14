package com.bi.queryer.ssm.portal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.portal.auth.service.PortalAuthService;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.entity.PortalMenuAccessLogEntity;
import com.bi.queryer.ssm.portal.enums.DragType;
import com.bi.queryer.ssm.portal.enums.FuncType;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.ssm.portal.enums.ResType;
import com.bi.queryer.ssm.portal.template.AnalysisTemplateService;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTemplateEntity;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplateOnlineStatus;
import com.bi.queryer.ssm.portal.template.enums.AnalysisTemplatePublishStatus;
import com.bi.queryer.ssm.portal.template.vo.AnalysisTemplateVO;
import com.bi.queryer.ssm.portal.vo.req.PortalMenuAccessLogAddReq;
import com.bi.queryer.ssm.portal.vo.req.PortalMenuContentReq;
import com.bi.queryer.ssm.portal.vo.req.PortalMenuUpdateReq;
import com.bi.queryer.ssm.portal.vo.req.PortalPublicDomainReq;
import com.bi.queryer.ssm.portal.vo.rsp.PortalMenuRsp;
import com.bi.queryer.ssm.portal.vo.rsp.PortalPublicDomainResp;
import com.bi.queryer.ssm.query.template.enums.FavTemplateType;
import com.bi.queryer.ssm.query.template.model.TemplateFavEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.SSMResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.Guid;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: contributor
 * @CreateTime: 2024-06-19  10:27
 * @Description: 门户菜单服务类
 */
@Service
public class PortalMenuService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private AnalysisTemplateService analysisTemplateService;

    @Autowired
    private PortalAuthService portalAuthService;

    /**
     * 获取菜单列表
     * @param portalId
     * @return
     */
    public List<PortalMenu> getMenuList(String portalId) {

        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("portalId",portalId);

        List<PortalMenu> menuList = (List<PortalMenu>) dao.queryObjectList("ssm.portal.menu.getMenuList", queryMap);
        return menuList;
    }

    /**
     * 新增菜单
     * @param portalMenu
     * @return
     */
    public String addMenu(PortalMenu portalMenu){

        User user = UserManager.get();
        String menuId = Guid.id();

        portalMenu.setMenuId(menuId);
        portalMenu.setCreatedBy(user.getName());

        //父级为空，设置上级为门户id
        if(StrUtil.isEmpty(portalMenu.getParentMenuId())){
            portalMenu.setParentMenuId(portalMenu.getPortalId());
        }

        Double sortId = portalMenu.getSortId();
        if(sortId == null){
            sortId = getMaxSortId(portalMenu.getParentMenuId());
            sortId++;
        }

        portalMenu.setSortId(sortId);

        dao.insert("ssm.portal.menu.add",portalMenu);

        return menuId;
    }

    public List<PortalMenu> getMenuListByPortalId(String portalId, PortalMenuType menuType) {
        Map<String, String> param = new HashMap<>();
        param.put("portalId", portalId);
        param.put("menuType", menuType.getCode());
        List<PortalMenu> childMenuList = (List<PortalMenu>) dao.queryObjectList("ssm.portal.menu.getByPortalId", param);
        if (CollUtil.isEmpty(childMenuList)) {
            return Collections.emptyList();
        }

        return childMenuList;
    }

    /**
     * 批量更新菜单
     *
     */
    public void batchUpdateMenu(String portalId, PortalMenuType menuType, List<PortalMenuContentReq> menuList) {
        Map<String, String> param = new HashMap<>();
        param.put("portalId", portalId);
        param.put("menuType", menuType.getCode());
        List<PortalMenu> childMenuList = dao.queryObjectList("ssm.portal.menu.getByPortalId", param, PortalMenu.class);
        List<String> oldMenuIdList = new ArrayList<>();
        if (CollUtil.isNotEmpty(childMenuList)) {
            oldMenuIdList = childMenuList.stream().map(PortalMenu::getContentRefId).collect(Collectors.toList());
        }

        List<String> newMenuIdList = new ArrayList<>();
        if (CollUtil.isNotEmpty(menuList)) {
            newMenuIdList = menuList.stream().map(PortalMenuContentReq::getContentId).collect(Collectors.toList());
        }

        User user = UserManager.get();
        Collection<String> addMenuIdList = CollectionUtils.subtract(newMenuIdList, oldMenuIdList);
        Collection<String> deleteMenuIdList = CollectionUtils.subtract(oldMenuIdList, newMenuIdList);

        if (CollUtil.isNotEmpty(addMenuIdList)) {
            double sortId = getMinSortId(portalId) - addMenuIdList.size();
            List<PortalMenu> portalMenuList = new ArrayList<>(addMenuIdList.size());
            for (PortalMenuContentReq menuReq : menuList) {
                if (addMenuIdList.contains(menuReq.getContentId())) {
                    PortalMenu portalMenu = new PortalMenu();
                    portalMenu.setMenuId(Guid.id());
                    portalMenu.setPortalId(portalId);
                    portalMenu.setMenuType(menuType.getCode());
                    portalMenu.setParentMenuId(portalId);
                    portalMenu.setCreatedBy(user.getName());
                    portalMenu.setMenuName(menuReq.getContentName());
                    portalMenu.setContentRefId(menuReq.getContentId());
                    portalMenu.setSortId(sortId++);
                    portalMenuList.add(portalMenu);
                }
            }
            dao.insert("ssm.portal.menu.batchAdd", portalMenuList);
        }

        if (CollUtil.isNotEmpty(deleteMenuIdList)) {
            List<String> deleteMenuList = new ArrayList<>(deleteMenuIdList.size());
            for (PortalMenu menu : childMenuList) {
                if (deleteMenuIdList.contains(menu.getContentRefId())) {
                    deleteMenuList.add(menu.getMenuId());
                }
            }

            if (CollUtil.isNotEmpty(deleteMenuList)) {
                Map<String, Object> deleteParam = new HashMap<>();
                deleteParam.put("menuIds", deleteMenuList);
                deleteParam.put("updatedBy", user.getName());
                dao.delete("ssm.portal.menu.deleteByIds", deleteParam);
            }
        }
    }

    /**
     * 更新菜单
     * @return
     */
    public SSMResponseMessage<String> update(PortalMenuUpdateReq portalMenuUpdateReq) {

        PortalMenu portalMenu = getMenuById(portalMenuUpdateReq.getMenuId());

        checkAuth(portalMenuUpdateReq.getPortalId(), ResType.PORTAL.getCode(), FuncType.EDIT.getCode());

        String portalId;
        PortalMenu parentMenu = (PortalMenu) dao.queryObject("ssm.portal.menu.get", portalMenuUpdateReq.getParentMenuId());
        if (parentMenu == null) {
            portalId = portalMenuUpdateReq.getParentMenuId();
        } else {
            portalId = parentMenu.getPortalId();
        }

        // 修改子级的目录
        List<PortalMenu> childPortalMenuList = dao.queryObjectList("ssm.portal.menu.getByParentId", portalMenuUpdateReq.getMenuId(), PortalMenu.class);

        String userName = UserManager.get().getName();
        portalMenu.setMenuName(portalMenuUpdateReq.getMenuName());
        portalMenu.setPortalId(portalId);
        portalMenu.setMenuDesc(portalMenuUpdateReq.getMenuDesc());
        portalMenu.setParentMenuId(portalMenuUpdateReq.getParentMenuId());
        portalMenu.setUpdatedBy(userName);

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                    @Override
                    public void execute() {

                        //更新菜单
                        dao.update("ssm.portal.menu.update", portalMenu);

                        if (CollUtil.isNotEmpty(childPortalMenuList)) {
                            // 更新子级菜单
                            for (PortalMenu childPortalMenu : childPortalMenuList) {
                                childPortalMenu.setPortalId(portalId);
                                childPortalMenu.setUpdatedBy(userName);
                                dao.update("ssm.portal.menu.update", childPortalMenu);
                            }
                        }

                        //更新看板
                        if (PortalMenuType.ANALYSIS_TEMPLATE == PortalMenuType.get(portalMenu.getMenuType())) {

                            AnalysisTemplateVO analysisTemplateVO = new AnalysisTemplateVO();
                            analysisTemplateVO.setAnalysisTplId(portalMenu.getContentRefId());
                            analysisTemplateVO.setAnalysisTplName(portalMenu.getMenuName());
                            analysisTemplateVO.setAnalysisTplDesc(portalMenu.getMenuDesc());

                            analysisTemplateService.updateAnalysisTemplateBase(analysisTemplateVO, userName, null);
                        }
                    }
                }
        );

        return SSMResponseMessage.success("更新成功", portalMenuUpdateReq.getMenuId());
    }

    public boolean updateMenuNameByContentRefId(PortalMenu portalMenu) {
        dao.update("ssm.portal.menu.updateMenuNameByContentRefId", portalMenu);
        return true;
    }

    /**
     * 获取菜单id
     * @param menuId
     * @return
     */
    public PortalMenuRsp get(String menuId) {

        PortalMenu portalMenu = getMenuById(menuId);

        //查询门户
        Portal portal = getPortalById(portalMenu.getPortalId());

        PortalMenuRsp portalMenuRsp = PortalMenuRsp
                .builder()
                .menuId(portalMenu.getMenuId())
                .menuType(portalMenu.getMenuType())
                .contentRefId(portalMenu.getContentRefId())
                .parentMenuId(portalMenu.getParentMenuId())
                .portalId(portalMenu.getPortalId())
                .menuName(portalMenu.getMenuName())
                .menuUrl(portalMenu.getMenuUrl())
                .menuDesc(portalMenu.getMenuDesc())
                .menuPath(String.format("%s-%s", BIConsts.PORTAL_MENU_ROOT_NAME, portal.getPortalName()))
                .build();

        TemplateFavEntity favEntity = getMenuFavInfo(portalMenu);
        if (favEntity != null) {
            portalMenuRsp.setIsFav(favEntity.getIsFav());
            portalMenuRsp.setFavCtgId(favEntity.getCtgId());
        }

        return portalMenuRsp;
    }

    private TemplateFavEntity getMenuFavInfo(PortalMenu portalMenu) {
        if (PortalMenuType.ANALYSIS_TEMPLATE.getCode().equals(portalMenu.getMenuType())) {
            User user = UserManager.get();
            Map<String, Object> queryParams = MapUtil.newHashMap();
            queryParams.put("userName", user.getName());
            queryParams.put("tplIdList", Collections.singletonList(portalMenu.getMenuId()));
            List<TemplateFavEntity> existEntities = dao.queryObjectList("ssm.template.fav.selectByTplIdAndUserName", queryParams, TemplateFavEntity.class);
            if (CollUtil.isEmpty(existEntities)) {
                return null;
            }
            return existEntities.stream().filter(v -> FavTemplateType.ANALYSIS_TEMPLATE.getCode().equals(v.getFavTplType()))
                    .findFirst().orElse(null);
        }
        return null;
    }

    /**
     * 通过门户id获取门户
     * @param portalId
     * @return
     */
    public Portal getPortalById(String portalId) {
        Portal portal = (Portal) dao.queryObject("ssm.portal.get", portalId);
        if (portal == null) {
            throw new BIException("访问的门户不存在！");
        }

        if (!Enabled.value(portal.getIsActive())) {
            throw new BIException(String.format("访问的门户%s已下线！", portal.getPortalName()));
        }
        return portal;
    }


    /**
     * 删除菜单id
     * @param menuId
     * @return
     */
    public SSMResponseMessage<String> delete(String menuId) {

        PortalMenu portalMenu = getMenuById(menuId);

        checkAuth(portalMenu.getPortalId(),ResType.PORTAL.getCode(),FuncType.DELETE.getCode());

        //判断是否有子菜单
        List<PortalMenu> childPortalMenuList = dao.queryObjectList("ssm.portal.menu.getByParentId", menuId, PortalMenu.class);
        if(CollUtil.isNotEmpty(childPortalMenuList)){
            throw new BIException("该菜单下存在子菜单，无法删除！");
        }


        PortalMenuType portalMenuType = PortalMenuType.get(portalMenu.getMenuType());
        //如果是看板，判断是否下线
        if(PortalMenuType.ANALYSIS_TEMPLATE == portalMenuType) {
            List<String> analysisTemplateIds = new ArrayList<>();
            analysisTemplateIds.add(portalMenu.getContentRefId());
            List<AnalysisTemplateEntity> analysisTemplateEntities = analysisTemplateService.getTemplateBaseList(analysisTemplateIds);
            if (CollUtil.isNotEmpty(analysisTemplateEntities)) {

                AnalysisTemplateEntity analysisTemplateEntity = analysisTemplateEntities.get(0);
                if (AnalysisTemplateOnlineStatus.ONLINE == AnalysisTemplateOnlineStatus.codeOf(analysisTemplateEntity.getOnlineStatus())) {
                    throw new BIException("看板未下线，无法删除！");
                }

                if (AnalysisTemplatePublishStatus.PUBLISHING == AnalysisTemplatePublishStatus.codeOf(analysisTemplateEntity.getDraftStatus())) {
                    throw new BIException("看板发布中，无法删除！");
                }
            }
        }

        //校验删除权限
        if(!portalAuthService.check(portalMenu.getPortalId(), ResType.PORTAL.getCode(), FuncType.DELETE.getCode())) {
            throw new BIException("您没有删除该看板的权限！");
        }

        portalMenu.setUpdatedBy(UserManager.get().getName());

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                if (PortalMenuType.DYNAMIC_ANALYSIS_REPORT == portalMenuType) {
                    Map<String, Object> rp = new HashMap<>(4);
                    rp.put("reportId", portalMenu.getContentRefId());
                    rp.put("updatedBy", UserManager.get().getName());
                    dao.update("ssm.portal.dynamicReport.softDeleteByReportId", rp);
                }
                dao.delete("ssm.portal.menu.delete", portalMenu);
                if (PortalMenuType.ANALYSIS_TEMPLATE == portalMenuType) {
                    //删除看板
                    AnalysisTemplateVO analysisTemplateVO = new AnalysisTemplateVO();
                    analysisTemplateVO.setAnalysisTplId(portalMenu.getContentRefId());
                    analysisTemplateVO.setPortalId(portalMenu.getPortalId());
                    analysisTemplateService.delete(analysisTemplateVO);
                }
            }
        });

        return SSMResponseMessage.success("删除成功", menuId);
    }

    /**
     * 菜单移动
     * @param menuId
     * @param targetMenuId
     * @return
     */
    public SSMResponseMessage move(String menuId,String targetMenuId,String dragType) {

        PortalMenu portalMenu = getMenuById(menuId);

        checkAuth(portalMenu.getPortalId(), ResType.PORTAL.getCode(), FuncType.EDIT.getCode());

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {

                PortalMenu targetPortalMenu = getMenuById(targetMenuId);

                DragType dgType = DragType.get(dragType);
                if (DragType.AFTER == dgType) {
                    portalMenu.setSortId(targetPortalMenu.getSortId() + 1);
                } else {
                    portalMenu.setSortId(targetPortalMenu.getSortId());
                }

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("parentMenuId", targetPortalMenu.getParentMenuId());
                paramMap.put("sortId", targetPortalMenu.getSortId());
                paramMap.put("dragType",dgType.getCode());

                //将同一层级，大于移动节点的排序+1
                dao.update("ssm.portal.menu.incrementMenuSortId", paramMap);

                dao.update("ssm.portal.menu.updateMenuSortId", portalMenu);
            }
        });

        return SSMResponseMessage.success("菜单移动成功");
    }

    /**
     * 获取菜单信息
     * @param menuId
     * @return
     */
    public PortalMenu getMenuById(String menuId) {
        PortalMenu portalMenu = (PortalMenu) dao.queryObject("ssm.portal.menu.get", menuId);

        if (portalMenu == null) {
            throw new BIException("访问菜单不存在！");
        }

        if (!Enabled.value(portalMenu.getIsActive())) {
            throw new BIException(String.format("访问菜单已下线，请联系[%s]",portalMenu.getCreatedBy()));
        }

        return portalMenu;
    }

    /**
     * 获取同一层级下最大排序id
     * @param parentMenuId
     * @return
     */
    public Double getMaxSortId(String parentMenuId) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("parentMenuId", parentMenuId);
        Double sortId = (Double) dao.queryObject("ssm.portal.menu.queryMaxSortId", paramMap);
        if (sortId == null) {
            sortId = 0.0;
        }
        return sortId;
    }

    /**
     * 获取同一层级下最小排序id
     * @param parentMenuId
     * @return
     */
    public Double getMinSortId(String parentMenuId) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("parentMenuId", parentMenuId);
        Double sortId = (Double) dao.queryObject("ssm.portal.menu.queryMinSortId", paramMap);
        if (sortId == null) {
            sortId = 0.0;
        }
        return sortId;
    }

    /**
     * 权限校验
     * @param resId
     * @param resType
     * @param funcCode
     */
    public void checkAuth(String resId,String resType,String funcCode){

        if(!portalAuthService.check(resId,resType,funcCode)){

            Portal portal = (Portal) dao.queryObject("ssm.portal.get", resId);
            String resName = portal.getPortalName();

            FuncType funcType = FuncType.get(funcCode);
            throw new BIException(String.format("您没有%s的%s的功能权限",resName,funcType.getName()));
        }

    }

    /**
     * 通过看板id查询菜单信息
     * @param analysisTplIds
     * @return
     */
    public List<PortalMenu> getPortalMenuInfoByAnalysisTplIds(List<String> analysisTplIds ) {

        List<PortalMenu> portalMenuList = new ArrayList<>();

        if (CollUtil.isEmpty(analysisTplIds)) {
            return portalMenuList;
        }

        portalMenuList =  (List<PortalMenu>)dao.queryObjectList("ssm.portal.menu.getPortalMenuInfoByAnalysisTplIds",analysisTplIds);

        return portalMenuList;
    }

    /**
     * 通过菜单应用内容id查询菜单信息
     * @param refIds
     * @return
     */
    public List<PortalMenu> getPortalMenuByRefIds(List<String> menuTypes, List<String> refIds) {
        if (CollUtil.isEmpty(refIds)) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("menuTypes", menuTypes);
        params.put("refIds", refIds);
        List<PortalMenu> portalMenuList = dao.queryObjectList("ssm.portal.menu.getPortalMenuPath", params, PortalMenu.class);
        if (CollUtil.isEmpty(refIds)) {
            return Collections.emptyList();
        }
        return portalMenuList;
    }

    /**
     * 通过菜单id查询菜单信息
     * @param menuIds
     * @return
     */
    public List<PortalMenu> getPortalMenuByMenuIds(String menuType, List<String> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("menuTypes", Collections.singletonList(menuType));
        params.put("menuIds", menuIds);
        List<PortalMenu> portalMenuList = dao.queryObjectList("ssm.portal.menu.getPortalMenuPath", params, PortalMenu.class);
        if (CollUtil.isEmpty(portalMenuList)) {
            return Collections.emptyList();
        }
        return portalMenuList;
    }

    // 公域内容判断: 门户中的门户共享空间、门户看板、门户中AI报告为公域
    public List<PortalPublicDomainResp> listPublicDomainInfo(PortalPublicDomainReq req) {
        if (req == null) {
            return Collections.emptyList();
        }

        List<PortalPublicDomainResp> result = new ArrayList<>();
        List<String> ctgIds = req.getCtgIds();
        if (CollUtil.isNotEmpty(ctgIds)) {
            // 挂到门户的公共空间
            List<String> menuTypes = ImmutableList.of(PortalMenuType.SPACE_CTG.getCode(), PortalMenuType.CONFIG_USED_SPACE_CTG.getCode());
            List<PortalMenu> portalMenuList = getPortalMenuByRefIds(menuTypes, ctgIds);
            for (PortalMenu portalMenu : portalMenuList) {
                PortalPublicDomainResp resp = new PortalPublicDomainResp();
                resp.setPortalId(portalMenu.getPortalId());
                resp.setPortalName(portalMenu.getPortalName());
                resp.setPortalMenuId(portalMenu.getMenuId());
                resp.setPortalMenuPath(portalMenu.getMenuUrl());
                resp.setPortalMenuName(portalMenu.getMenuName());
                resp.setCtgId(portalMenu.getContentRefId());
                resp.setPortalMenuDesc(portalMenu.getMenuDesc());
                result.add(resp);
            }
        }

        List<String> dashboardMenuIds = req.getDashboardMenuIds();
        if (CollUtil.isNotEmpty(dashboardMenuIds)) {
            // 挂到门户的看板
            List<PortalMenu> portalMenuList = getPortalMenuByMenuIds(PortalMenuType.ANALYSIS_TEMPLATE.getCode(), dashboardMenuIds);

            // 查看板的信息
            List<String> dashboardIds = portalMenuList.stream().map(PortalMenu::getContentRefId).collect(Collectors.toList());
            List<AnalysisTemplateEntity> analysisTplList = analysisTemplateService.getTemplateBaseList(dashboardIds);
            Map<String, AnalysisTemplateEntity> analysisTplMap = analysisTplList.stream().collect(Collectors.toMap(AnalysisTemplateEntity::getAnalysisTplId, v -> v));

            for (PortalMenu portalMenu : portalMenuList) {
                PortalPublicDomainResp resp = new PortalPublicDomainResp();
                resp.setPortalId(portalMenu.getPortalId());
                resp.setPortalName(portalMenu.getPortalName());
                resp.setPortalMenuId(portalMenu.getMenuId());
                resp.setPortalMenuPath(portalMenu.getMenuUrl());
                resp.setPortalMenuName(portalMenu.getMenuName());
                resp.setAnalysisTplMenuId(portalMenu.getMenuId());
                resp.setPortalMenuDesc(portalMenu.getMenuDesc());
                AnalysisTemplateEntity analysisTpl = analysisTplMap.get(portalMenu.getContentRefId());
                if (analysisTpl != null) {
                    resp.setAnalysisTplOwners(analysisTpl.getAnalysisTplOwner());
                }
                result.add(resp);
            }
        }

        //List<String> queryTplIds = req.getQueryTplIds();
        //if (CollUtil.isNotEmpty(queryTplIds)) {
        //    // 配置到门户看板的查询模板
        //    List<AnalysisTplQueryTplRelEntity> relEntities = analysisTemplateService.getAnalysisTplByQueryTplIds(queryTplIds);
        //    List<String> analysisTplIds = relEntities.stream().map(AnalysisTplQueryTplRelEntity::getAnalysisTplId).collect(Collectors.toList());
        //    List<PortalMenu> portalMenuList = portalMenuService.getPortalMenuByRefIds(PortalMenuType.ANALYSIS_TEMPLATE.getCode(), analysisTplIds);
        //    Map<String, PortalMenu> portalMenuMap = portalMenuList.stream().collect(Collectors.toMap(PortalMenu::getContentRefId, v -> v));
        //    for (AnalysisTplQueryTplRelEntity entity : relEntities) {
        //        PortalMenu portalMenu = portalMenuMap.get(entity.getAnalysisTplId());
        //        if (portalMenu == null) {
        //            continue;
        //        }
        //        PortalPublicDomainResp resp = new PortalPublicDomainResp();
        //        resp.setPortalId(portalMenu.getPortalId());
        //        resp.setPortalName(portalMenu.getPortalName());
        //        resp.setPortalMenuId(portalMenu.getMenuId());
        //        resp.setPortalMenuPath(portalMenu.getMenuUrl());
        //        resp.setQueryTplId(entity.getLocalQueryTplId());
        //        result.add(resp);
        //    }
        //}

        return result;
    }

    /**
     * 新增菜单访问日志
     * @param req
     */
    public Boolean addAccessLog(PortalMenuAccessLogAddReq req) {

        if (StringUtils.isBlank(req.getPortalId()) || StringUtils.isBlank(req.getMenuId()) || StringUtils.isBlank(req.getMenuName())) {
            throw new BIException("参数为空，req=" + req + " ");
        }

        PortalMenuAccessLogEntity entity = PortalMenuAccessLogEntity.builder()
                .userName(UserManager.get().getName())
                .menuId(req.getMenuId())
                .menuName(req.getMenuName())
                .portalId(req.getPortalId())
                .build();

        dao.insert("ssm.portal.menu.access.log.add", entity);
        return true;
    }

}
