package com.bi.queryer.ssm.query.ctg;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.portal.PortalMenuService;
import com.bi.queryer.ssm.portal.PortalService;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.portal.enums.PortalMenuType;
import com.bi.queryer.ssm.portal.vo.req.PortalPublicDomainReq;
import com.bi.queryer.ssm.portal.vo.rsp.PortalPublicDomainResp;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.enums.TemplateSpaceRoleType;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.ctg.model.TemplateSpaceEntity;
import com.bi.queryer.ssm.query.ctg.model.UserTemplateCategory;
import com.bi.queryer.ssm.query.template.TemplateBaseService;
import com.bi.queryer.ssm.query.template.TemplateShareService;
import com.bi.queryer.ssm.query.template.enums.QueryTemplateShareType;
import com.bi.queryer.ssm.query.template.model.*;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @Author contributor
 * @Date 22:39 2023-11-10
 * @Description 查询模板分类service
 **/
@Service
@Scope("prototype")
public class QueryTemplateCategoryService extends TemplateBaseService {
    @Autowired
    private BaseDao dao = null;

    @Autowired
    private TemplateShareService shareService = null;

    @Autowired
    @Lazy
    private PortalMenuService portalMenuService;

    @Autowired
    @Lazy
    private PortalService portalService;

    /**
     * 获取所有用户空间
     * @return
     */
    public List<QueryTemplateCategory> getAllSpaceCategories() {
        List<QueryTemplateCategory> categories = getCategoriesWithAuth();
        List<QueryTemplateCategory> result = categories.stream().filter(c -> BIConsts.TEMPLATE_CTG_SPACE_ID.equals(c.getParentId())).collect(Collectors.toList());

        // 公域的判断
        List<String> spaceCtgIds = result.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
        List<String> publicCtgIds = getPublicCtgList(spaceCtgIds);
        result.forEach(c -> c.setIsInPublicDomain(publicCtgIds.contains(c.getId()) ? Enabled.YES.getId() : Enabled.NO.getId()));
        return result;
    }

    public List<QueryTemplateCategory> getCategoriesWithAuth() {
        Map<String, Object> params = new HashMap<>();
        params.put("userName", "");
        List<QueryTemplateCategory> allCategories = dao.queryObjectList("ssm.template.ctg.queryAllCategories", params, QueryTemplateCategory.class);
        Map<String, QueryTemplateCategory> allCategoryMap = allCategories.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, c -> c, (c1, c2) -> c1));

        // 查询当前用户有最大权限角色的共享空间目录
        User user = UserManager.get();
        params.put("userName", user.getName());
        params.put("deptId", user.getDeptId());
        List<TemplateSpaceEntity> maxRoleSpaceCategories = dao.queryObjectList("ssm.template.ctg.queryMaxRoleSpaceCategories", params, TemplateSpaceEntity.class);
        Map<String, TemplateSpaceEntity> maxRoleSpaceCategoryMap = new HashMap<>();
        if (CollUtil.isNotEmpty(maxRoleSpaceCategories)) {
            maxRoleSpaceCategoryMap = maxRoleSpaceCategories.stream().collect(Collectors.toMap(TemplateSpaceEntity::getSpaceCtgId, v -> v));
        }

        // 构建tree获取其上下级
        for (QueryTemplateCategory ctg : allCategories) {
            TemplateSpaceEntity spaceAuth = maxRoleSpaceCategoryMap.get(ctg.getId());
            if (spaceAuth != null) {
                ctg.setIsAdmin(TemplateSpaceRoleType.ADMIN == TemplateSpaceRoleType.get(spaceAuth.getOwnerRole()) ? Enabled.YES.getId() : Enabled.NO.getId());
                ctg.setHasAuth(Enabled.YES.getId());
            }
            QueryTemplateCategory parent = allCategoryMap.get(ctg.getParentId());
            if (parent != null) {
                parent.getChildren().add(ctg);
            }
        }
        return allCategories;
    }

    // 获取所有有查看权限的共享空间，包含门户有权限的目录
    public List<QueryTemplateCategory> getAllViewableSpaceCategories() {
        List<QueryTemplateCategory> categories = getCategoriesWithAuth();
        List<QueryTemplateCategory> result = categories.stream()
                .filter(c -> BIConsts.TEMPLATE_CTG_SPACE_ID.equals(c.getParentId()))
                .collect(Collectors.toList());

        List<String> queryTemplateCtgIds = portalService.getStudioSpaceList();

        List<QueryTemplateCategory> res = new ArrayList<>();
        for (QueryTemplateCategory c : result) {
            c.setChildren(new ArrayList<>());
            if (Enabled.YES.getId().equals(c.getHasAuth())) {
                res.add(c);
            } else if (queryTemplateCtgIds.contains(c.getId())) {
                res.add(c);
            }
        }

        return res;
    }

    /**
     * 获取当前用户可编辑的共享空间树
     * @return
     */
    public List<QueryTemplateCategory> listUserEditableSpace() {
        List<QueryTemplateCategory> categories = getAllCategories();
        List<QueryTemplateCategory> spaceCategories = categories.stream().filter(c -> BIConsts.TEMPLATE_CTG_SPACE_ID.equals(c.getParentId())).collect(Collectors.toList());

        //只取一级目录
        List<QueryTemplateCategory> result = new ArrayList<>();
        for (QueryTemplateCategory ctg : spaceCategories) {

            //非管理员，无法编辑
            if (!Enabled.value(ctg.getIsAdmin())) {
                continue;
            }

            ctg.setChildren(new ArrayList<>());
            result.add(ctg);
        }

        return result;
    }

    /**
     * 获取当前用户下所有目录
     * @return
     */
    public List<QueryTemplateCategory> getAllCategories() {
        User user = UserManager.get();
        Map<String, Object> params = new HashMap<>();
        params.put("userName", user.getName());
        params.put("deptId", user.getDeptId());
        List<QueryTemplateCategory> allCategories = (List<QueryTemplateCategory>) dao.queryObjectList("ssm.template.ctg.queryAllCategories", params);
        Map<String, QueryTemplateCategory> allCategoryMap = allCategories.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, c->c, (c1,c2) ->c1));

        /*** 共享空间特殊处理 ***/
        /**
         * 上一步中查询owner是自己的所有目录 和 全部共享空间目录（含自己不是owner的）
         * 1、去掉无权限的共享目录
         * 2、若有共享目录的权限，则有其下级所有目录的权限
         */
        // 构建tree获取其上下级
        for (QueryTemplateCategory ctg : allCategories) {
            QueryTemplateCategory parent = allCategoryMap.get(ctg.getParentId());
            if (parent != null) {
                parent.getChildren().add(ctg);
            }
        }

        // 查询当前用户有最大权限角色的共享空间目录
        List<TemplateSpaceEntity> maxRoleSpaceCategories = (List<TemplateSpaceEntity>) dao.queryObjectList("ssm.template.ctg.queryMaxRoleSpaceCategories", params);

        Set<String> userSpaceCtgIdSet = new HashSet<>(); // 用户有权限的space id列表
        userSpaceCtgIdSet.add(QueryTemplateCategoryType.SPACE.getId()); // 添加根目录

        //查询共享空间下面的一级目录
        List<String> spaceCtgIdList = allCategories.stream()
                .filter(f->QueryTemplateCategoryType.SPACE.getId().equals(f.getParentId()))
                .map(QueryTemplateCategory::getId)
                .collect(Collectors.toList());

        //获取公域的共享空间目录id
        List<String> publicCtgIds = getPublicCtgList(spaceCtgIdList);

        // 记录有权限的共享空间目录id
        for(TemplateSpaceEntity space : maxRoleSpaceCategories){
            QueryTemplateCategory spaceCategory = allCategoryMap.get(space.getSpaceCtgId());
            if(spaceCategory == null) {
                continue;
            }
            // 设置其是否是管理员
            spaceCategory.setIsAdmin(TemplateSpaceRoleType.ADMIN == TemplateSpaceRoleType.get(space.getOwnerRole()) ? Enabled.YES.getId() : Enabled.NO.getId());
            userSpaceCtgIdSet.add(spaceCategory.getId());
            if(publicCtgIds.contains(spaceCategory.getId())){
                spaceCategory.setIsInPublicDomain(Enabled.YES.getId());
            }

            // 级列设置其下级目录
            List<QueryTemplateCategory>  allChildren = this.getAllChildren(spaceCategory.getChildren());
            for(QueryTemplateCategory child : allChildren){
                child.setIsAdmin(spaceCategory.getIsAdmin());
                child.setIsInPublicDomain(spaceCategory.getIsInPublicDomain());
                userSpaceCtgIdSet.add(child.getId());
            }
        }

        // 排查掉无权限的共享目录：是共享目录但不在用户有权限目录列表中
        Map<String, QueryTemplateCategory> categoryMap = allCategories.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, c->c, (c1,c2)->c1));
        List<QueryTemplateCategory> newCategories = new ArrayList<>();
        for(QueryTemplateCategory c : allCategories){
            QueryTemplateCategoryType ctype = QueryTemplateCategoryType.get(c.getType());
            if(ctype != QueryTemplateCategoryType.SPACE || userSpaceCtgIdSet.contains(c.getId())){
                newCategories.add(c);
                continue;
            }
            if(ctype == QueryTemplateCategoryType.SPACE && !userSpaceCtgIdSet.contains(c.getId())){
                // 需要排除的共享空间：从其父节点中删除掉
                if(categoryMap.containsKey(c.getParentId())) {
                    QueryTemplateCategory parent = categoryMap.get(c.getParentId());
                    parent.getChildren().remove(c);
                }
            }
        }
        return newCategories;
    }

    /**
     * 获取目录树（含权限）
     * @return 指定id下的目录树
     */
    public List<QueryTemplateCategory> getCategoryTree(String rootId) {
        List<QueryTemplateCategory> categories = this.getAllCategories();
        // 返回root节点
        String resultRootId = BIUtil.isEmpty(rootId) ? BIConsts.TEMPLATE_CTG_ROOT_ID : rootId;
        List<QueryTemplateCategory> roots = categories.stream().filter(c -> resultRootId.equals(c.getParentId())).collect(Collectors.toList());
        return roots;
    }

    /**
     * 通过id查询对应目录
     * @param ctgId
     * @return
     */
    public QueryTemplateCategory getCategoryById(String ctgId) {
        QueryTemplateCategory ctg = (QueryTemplateCategory) dao.queryObject("ssm.template.ctg.getCategoryById", ctgId);
        return ctg;
    }

    /**
     * 获取公共空间下的所有子目录, 去掉权限控制
     */
    public List<QueryTemplateCategory> getAllSpaceChildren(String parentId) {
        List<QueryTemplateCategory> categories = this.getCategoriesWithAuth();
        if (CollUtil.isEmpty(categories)) {
            return Collections.emptyList();
        }

        categories = categories.stream().filter(c -> Objects.equals(parentId, c.getId())).collect(Collectors.toList());
        List<QueryTemplateCategory> children = this.getAllChildren(categories);
        // 去掉自己
        children = children.stream().filter(c -> !c.getId().equals(parentId)).collect(Collectors.toList());
        return children;
    }

    /**
     * 级联获取子目录（含子孙目录，不含自己）
     * @param parentId 父目录id
     * @return
     */
    public List<QueryTemplateCategory> getAllChildren(String parentId) {
        List<QueryTemplateCategory> parentCategories = this.getCategoryTree(parentId);
        List<QueryTemplateCategory> children = this.getAllChildren(parentCategories);
        // 去掉自己
        children = children.stream().filter(c -> !c.getId().equals(parentId)).collect(Collectors.toList());
        return children;
    }

    public List<QueryTemplateCategory> getAllChildren(List<QueryTemplateCategory> parentCategories) {
        List<QueryTemplateCategory> allChildren = new ArrayList<>();
        this.recurseChildrenCascade(parentCategories, allChildren);
        return allChildren;
    }
    /**
     * 递归获取下级
     * @param parentCategories
     * @param resultChildrenCategories
     */
    private void recurseChildrenCascade(List<QueryTemplateCategory> parentCategories, List<QueryTemplateCategory> resultChildrenCategories) {
        if (BIUtil.isEmpty(parentCategories)) {
            return;
        }
        for (QueryTemplateCategory ctg : parentCategories) {
            resultChildrenCategories.add(ctg);
            recurseChildrenCascade(ctg.getChildren(), resultChildrenCategories);
        }
    }

    /**
     * 新增分类目录
     * @param ctg
     * @return
     */
    public String addCategory(QueryTemplateCategory ctg) {
        QueryTemplateCategory parent = this.getCategoryById(ctg.getParentId());
        if(parent == null){
            throw new SSDException(String.format("上级不存在，id:%s", ctg.getParentId()));
        }
        ctg.setId(Guid.id());
        ctg.setType(parent.getType());

        User user = UserManager.get();
        ctg.setCreatedBy(user.getName());
        if (ctg.getSortId() == null) {
            ctg.setSortId(9999D);
        }
        dao.insert("ssm.template.ctg.addCategory", ctg);

        // 若是共享空间，则需要将创建人设置为管理员
        if(QueryTemplateCategoryType.get(ctg.getType()) == QueryTemplateCategoryType.SPACE) {
            List<TemplateSpaceEntity> spaceEntityList = new ArrayList<>();
            TemplateSpaceEntity spaceEntity = new TemplateSpaceEntity();
            spaceEntity.setPkid(Guid.id());
            spaceEntity.setSpaceCtgId(ctg.getId());
            spaceEntity.setOwnerType(QueryTemplateShareType.USER.getId());
            spaceEntity.setOwnerId(ctg.getCreatedBy());
            spaceEntity.setOwnerDesc(user.getRealName());
            spaceEntity.setOwnerRole(TemplateSpaceRoleType.ADMIN.getCode());
            spaceEntity.setCreatedBy(ctg.getCreatedBy());
            spaceEntityList.add(spaceEntity);
            dao.insert("ssm.template.ctg.batchAddTplSpace", spaceEntityList);
        }

        return ctg.getId();
    }

    /**
     * 修改分类目录
     * @param ctg
     * @return
     */
    public String updateCategory(QueryTemplateCategory ctg) {
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                // 获取上级目录
                QueryTemplateCategory parentCategory = getCategoryById(ctg.getParentId());
                if (parentCategory == null) {
                    throw new SSDException(String.format("修改失败：上级分类目录不存在（上级id：%s）", ctg.getParentId()));
                }

                String ctgType = parentCategory.getType();

                if (QueryTemplateCategoryType.SHARE.getId().equals(ctg.getId())) {
                    String oldCtgId = ctg.getId();
                    // 如果修改的他人分享文件夹，则创建一个
                    String newCtgId = addCategory(ctg);

                    // 修改模板的分类
                    updateTemplateParent(oldCtgId, newCtgId, ctgType);

                    // 删除分类下的模板
                    List<QueryTemplateCategory> deleteCategories = getAllCategories();

                    List<String> childCtgIdList = new ArrayList<>();
                    deleteCategories.stream().filter(c -> c.getParentId().equals(oldCtgId))
                            .forEach(c -> childCtgIdList.add(c.getId()));

                    // 修改下层目录的父级
                    if (CollUtil.isNotEmpty(childCtgIdList)) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("ctgIdList", childCtgIdList);
                        params.put("userName", UserManager.get().getName());
                        params.put("parentId", newCtgId);
                        params.put("type", ctgType);
                        dao.delete("ssm.template.ctg.updateCategoryParent", params);
                    }
                } else {
                    // 修改当前目录本身
                    ctg.setUpdatedBy(UserManager.get().getName());
                    ctg.setType(ctgType);
                    dao.update("ssm.template.ctg.updateCategory", ctg);

                    // 级联修改目录的类型
                    List<QueryTemplateCategory> allChildren = getAllChildren(ctg.getId());
                    if (BIUtil.isEmpty(allChildren)) {
                        return;
                    }
                    List<String> ctgIdList = allChildren.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
                    // 添加当前目录
                    ctgIdList.add(ctg.getId());

                    Map<String, String> params = new HashMap<>();
                    params.put("type", ctgType);
                    params.put("ctgIdList", BIUtil.listToStr(ctgIdList, ",", "'"));
                    dao.update("ssm.template.ctg.updateCategoryType", params);
                }
            }
        });

        return ctg.getId();
    }

    /**
     * 删除目录（同时删除其下所有模板）
     * 20250919 修改, 删除该文件夹，并将该文件夹下所有内容移至父层目录
     * @param ctgId
     * @return 删除目录的上级目录id
     */
    public String deleteCategory(String ctgId) {
        QueryTemplateCategory ctg = this.getCategoryById(ctgId);
        if (ctg == null) {
            return ctgId;
        }

        if (QueryTemplateCategoryType.SPACE.getId().equals(ctg.getParentId())) {
            // 删除共享空间，保留以前逻辑
            return deleteSpaceCtg(ctg);
        }


        if (Objects.equals("-1", ctg.getParentId())) {
            ctg.setParentId(QueryTemplateCategoryType.MY.getId());
        }

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                // 删除分类下的模板
                List<QueryTemplateCategory> deleteCategories = getAllCategories();

                // 修改模板的分类
                updateTemplateParent(ctgId, ctg.getParentId(), ctg.getType());

                List<String> childCtgIdList = new ArrayList<>();
                deleteCategories.stream().filter(c -> c.getParentId().equals(ctgId))
                        .forEach(c -> childCtgIdList.add(c.getId()));

                // 修改下层目录的父级
                if (CollUtil.isNotEmpty(childCtgIdList)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("ctgIdList", childCtgIdList);
                    if (QueryTemplateCategoryType.SHARE.getId().equals(ctgId)) {
                        params.put("userName", UserManager.get().getName());
                    }
                    params.put("type", ctg.getType());
                    params.put("parentId", ctg.getParentId());
                    dao.delete("ssm.template.ctg.updateCategoryParent", params);
                }

                // 删除分类
                if (!QueryTemplateCategoryType.SHARE.getId().equals(ctgId)) {
                    Map<String, String> params = new HashMap<>();
                    params.put("ctgIdList", "'" + ctgId + "'");
                    dao.delete("ssm.template.ctg.deleteCategoryById", params);
                }
            }
        });

        return ctg.getParentId();
    }

    /**
     * 删除共享空间（同时删除其下所有模板）
     * @param ctg
     */
    public String deleteSpaceCtg(QueryTemplateCategory ctg) {

        List<String> menuTypes = ImmutableList.of(PortalMenuType.SPACE_CTG.getCode(), PortalMenuType.CONFIG_USED_SPACE_CTG.getCode());
        List<PortalMenu> portalMenuList = portalMenuService.getPortalMenuByRefIds(menuTypes, Collections.singletonList(ctg.getId()));
        checkState(CollUtil.isEmpty(portalMenuList), "该空间被门户[%s]关联，不能删除",
                portalMenuList.stream().map(PortalMenu::getPortalName).distinct().collect(Collectors.joining(",")));

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                // 删除分类下的模板
                List<QueryTemplateCategory> deleteCategories = getAllChildren(ctg.getId());

                // 添加当前分类
                deleteCategories.add(ctg);


                List<String> deleteIdList = new ArrayList<>();
                deleteCategories.forEach(c -> deleteIdList.add(c.getId()));

                // 删除模板
                deleteTemplateByCtg(deleteCategories);

                String ctgIdList = BIUtil.listToStr(deleteIdList, ",", "'");
                // 删除分类
                Map<String, String> params = new HashMap<>();
                params.put("ctgIdList", ctgIdList);
                dao.delete("ssm.template.ctg.deleteCategoryById", params);
            }
        });

        return ctg.getParentId();
    }


    /**
     * TODO 调整到模板service中
     * 通过目录删除所有模板
     * @param deleteCategories 目录列表
     */
    protected void deleteTemplateByCtg(List<QueryTemplateCategory> deleteCategories) {
        if (BIUtil.isEmpty(deleteCategories)) {
            return;
        }

        // 删除模板配置
        List<String> deleteCtgIdList = deleteCategories.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());

        Map<String, String> params = new HashMap<>();

        params.put("ctgIdList", BIUtil.listToStr(deleteCtgIdList, ",", "'"));
        List<String> cfgIdList = (List<String>) dao.queryObjectList("ssm.template.ctg.getTemplateConfigIdByCtg", params);

        if (BIUtil.isNotEmpty(cfgIdList)) {
            params.put("cfgIdList", BIUtil.listToStr(cfgIdList, ",", "'"));
            dao.delete("ssm.template.ctg.deleteTemplateConfigByCtg", params);
        }

        // 删除模板基础信息
        List<String> deleteIdList = deleteCategories.stream().map(QueryTemplateCategory::getId).collect(Collectors.toList());
        String ctgIdList = BIUtil.listToStr(deleteIdList, ",", "'");
        params.put("ctgIdList", ctgIdList);
        dao.delete("ssm.template.ctg.deleteTemplateBaseByCtg", params);
    }

    public QueryTemplateCategory getCategory(String ctgId) {
        return this.getCategoryById(ctgId);
    }

    // 更改我的收藏和模板的父级目录
    private void updateTemplateParent(String ctgId, String parentId, String ctgType) {
        Map<String, Object> params = new HashMap<>();
        if (Objects.equals(ctgId, QueryTemplateCategoryType.SHARE.getId())) {
            params.put("userName", UserManager.get().getName());
        } else if (Objects.equals(ctgId, QueryTemplateCategoryType.MY.getId())) {
            params.put("userName", UserManager.get().getName());
        }
        params.put("ctgIdList", Collections.singletonList(ctgId));
        List<TemplateRsp> templateRspList = dao.queryObjectList("ssm.template.ctg.batchQueryTemplateByCtg", params, TemplateRsp.class);
        if (Objects.equals(ctgType, QueryTemplateCategoryType.SPACE.getId())) {
            // 共享空间的目录
            List<String> templateIdList = templateRspList.stream()
                    .filter(v -> Objects.equals(v.getCtgType(), QueryTemplateCategoryType.SPACE.getId()))
                    .map(TemplateEntity::getTplId).collect(Collectors.toList());
            if (CollUtil.isEmpty(templateIdList)) {
                return;
            }

            params.put("tplIdList", templateIdList);
            params.put("ctgId", parentId);
            dao.update("ssm.template.ctg.updateTemplateCtg", params);
        } else {
            // 我的空间的目录
            List<String> templateIdList = templateRspList.stream()
                    .filter(v -> !Objects.equals(v.getCtgType(), QueryTemplateCategoryType.SPACE.getId()))
                    .map(TemplateEntity::getTplId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(templateIdList)) {
                params.put("tplIdList", templateIdList);
                params.put("ctgId", parentId);
                dao.update("ssm.template.ctg.updateTemplateCtg", params);
            }

            params.put("ctgId", parentId);
            params.put("userName", UserManager.get().getName());
            params.put("oldCtgId", ctgId);
            dao.update("ssm.template.fav.updateTplCtg", params);
        }
    }

    /**
     * 分享目录给用户：
     * - 分享的内容包括：当前目录、子孙目录、目录下的所有模板
     * - 分享的内容存储在"他人分享"中
     * 1、copy目录（含子孙）给到接受者（每个目录*每个接收者copy一份）
     * 2、copy模板基础信息给到接受者（每个模板*每个接收者copy一份）
     * 3、copy模板配置信息给到接收者（每个模板配置仅copy一份，通过cfg_id与基础信息关联）
     * @return
     */
    public void shareCtgToUsers(TemplateShareReq req) {
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                    @Override
                    public void execute() {
                        List<String> userNames = normalizeUserNames(req.getUserNames());
                        // copy目录
                        List<UserTemplateCategory> userCopyCategories = shareCategoryToUser(req.getCtgId(), userNames);
                        // copy模板基础数据+配置信息
                        List<String> sourceCtgIdList = userCopyCategories.stream().map(UserTemplateCategory::getSourceId).collect(Collectors.toList());
                        List<TemplateRsp> tplList = (List<TemplateRsp>) dao.queryObjectList("ssm.template.ctg.queryTemplateByCtgIds", sourceCtgIdList);
                        List<TemplateShareEntity> shareEntityList = ListUtil.list(false);
                        List<User> userList = getUserListByNames(userNames);
                        User user = UserManager.get();
                        for (TemplateRsp templateRsp : tplList) {
                            for (User sharedUser : userList) {
                                // 生成分享记录
                                TemplateShareEntity shareEntity = shareService.buildTmpUserShareEntity(user, templateRsp, sharedUser);
                                shareEntityList.add(shareEntity);
                            }
                        }
                        if(BIUtil.isNotEmpty(shareEntityList)) {
                            dao.insert("ssm.template.batchAddTplShare", shareEntityList);
                        }
                        shareService.shareTemplatesToUsers(tplList, userList, userCopyCategories);
                    }
                }
        );
    }

    /**
     * 分享目录给部门：
     * - 分享的内容包括：当前目录、子孙目录、目录下的所有模板
     * - 分享的内容存储在"他人分享"中
     * 1、copy目录（含子孙）给到接受者（每个目录*每个接收者copy一份）
     * 2、copy模板基础信息给到接受者（每个模板*每个接收者copy一份）
     * 3、copy模板配置信息给到接收者（每个模板配置仅copy一份，通过cfg_id与基础信息关联）
     * @return
     */
    public void shareCtgToDept(TemplateShareReq req) {
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                    @Override
                    public void execute() {
                        List<User> userList = getUserListByDept(req.getDeptId());
                        // 移除自己（不分享给自己）
                        /*userList = userList.stream().filter(item->!UserManager.get().getName().equals(item.getName())).collect(Collectors.toList());*/
                        List<String> userNames = userList.stream().map(User::getName).collect(Collectors.toList());
                        // copy目录
                        List<UserTemplateCategory> userCopyCategories = shareCategoryToUser(req.getCtgId(), userNames);
                        // copy模板基础数据+配置信息
                        List<String> sourceCtgIdList = userCopyCategories.stream().map(UserTemplateCategory::getSourceId).collect(Collectors.toList());
                        List<TemplateRsp> tplList = (List<TemplateRsp>) dao.queryObjectList("ssm.template.ctg.queryTemplateByCtgIds", sourceCtgIdList);
                        List<TemplateShareEntity> shareEntityList = ListUtil.list(false);
                        User user = UserManager.get();
                        for (TemplateRsp templateRsp : tplList) {
                            // 生成分享记录
                            TemplateShareEntity shareEntity = shareService.buildTmpDeptShareEntity(user, templateRsp, req.getDeptId());
                            shareEntityList.add(shareEntity);
                        }
                        if(BIUtil.isNotEmpty(shareEntityList)) {
                            dao.insert("ssm.template.batchAddTplShare", shareEntityList);
                        }
                        shareService.shareTemplatesToUsers(tplList, userList, userCopyCategories);
                    }
                }
        );
    }

    /**
     * 按用户分享目录（仅目录）
     * @param ctgId
     * @param userNames
     * @return 用户的源分类id信息（源+目标）
     */
    protected List<UserTemplateCategory> shareCategoryToUser(String ctgId, List<String> userNames) {
        List<UserTemplateCategory> userCategories = new ArrayList<>();

        if (BIUtil.isEmpty(userNames)) {
            return userCategories;
        }
        QueryTemplateCategory currentCategory = this.getCategoryById(ctgId);
        if (currentCategory == null) {
            return userCategories;
        }
        List<QueryTemplateCategory> sourceCategories = getAllChildren(currentCategory.getId());
        sourceCategories.add(currentCategory);

        List<QueryTemplateCategory> copyCategories = new ArrayList<>();
        for (QueryTemplateCategory ctg : sourceCategories) {
            for (String userName : userNames) {
                QueryTemplateCategory copy = ctg.clone();
                copy.setId(Guid.id());
                copy.setType(QueryTemplateCategoryType.SHARE.getId());
                copy.setCreatedBy(userName);
                copy.setParentId(QueryTemplateCategoryType.SHARE.getId());
                copyCategories.add(copy);
                userCategories.add(new UserTemplateCategory(userName, ctg.getId(), copy.getId()));
            }
        }

        if(BIUtil.isEmpty(copyCategories)) {
            return userCategories;
        }

        // 重新设置copy后的上下级关系：通过源和copy目标的关联关系找到copy后的上级目录
        Map<String, QueryTemplateCategory> copyCategoryMap = copyCategories.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, c->c, (c1,c2) ->c1));
        Map<String, QueryTemplateCategory> sourceCategoryMap = sourceCategories.stream().collect(Collectors.toMap(QueryTemplateCategory::getId, c->c, (c1,c2) ->c1));
        for(QueryTemplateCategory copy : copyCategories){
            Optional<UserTemplateCategory> sourceUserCategory = userCategories.stream()
                    .filter(c-> c.getTargetId().equals(copy.getId()))
                    .filter(c->c.getUserName().equals(copy.getCreatedBy())).findAny();
            if(!sourceUserCategory.isPresent()) {
                continue;
            }
            String sourceId = sourceUserCategory.get().getSourceId();// 源id
            QueryTemplateCategory sourceCategory = sourceCategoryMap.get(sourceId);
            if(sourceCategory == null){
                continue;
            }
            String sourceParentId = sourceCategory.getParentId();

            Optional<UserTemplateCategory> copyParentUserCategory = userCategories.stream()
                                        .filter(c-> c.getSourceId().equals(sourceParentId))
                                        .filter(c->c.getUserName().equals(copy.getCreatedBy())).findAny();
            if(!copyParentUserCategory.isPresent()) {
                continue;
            }
            String copyParentCategoryId = copyParentUserCategory.get().getTargetId();
            QueryTemplateCategory copyParentCategory = copyCategoryMap.get(copyParentCategoryId);
            if(copyParentCategory != null){
                copy.setParentId(copyParentCategory.getId());
            }
        }


        dao.insert("ssm.template.ctg.batchAddCategory", copyCategories);
        return userCategories;
    }

    // 查询公域共享空间
    public List<String> getPublicCtgList(List<String> ctgIds) {
        List<PortalPublicDomainResp> resps = getPublicDomainInfo(ctgIds, null);
        return resps.stream().filter(v -> Enabled.YES.getId().equals(v.getIsInPublicDomain()))
                .map(PortalPublicDomainResp::getCtgId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    public List<TemplateCtgPathEntity> getCtgPath(List<String> ctgIds) {
        if (CollUtil.isEmpty(ctgIds)) {
            return new ArrayList<>();
        }
        return dao.queryObjectList("ssm.template.ctg.getCtgPath", ctgIds, TemplateCtgPathEntity.class);
    }

    public List<PortalPublicDomainResp> getPublicDomainInfo(List<String> ctgIds, List<String> analysisTplIds) {
        // 查询目录树
        List<TemplateCtgPathEntity> ctgPathEntities = getCtgPath(ctgIds);

        // 目录和公共空间根目录的关系
        Map<String, List<PortalPublicDomainResp>> spaceCtgMap = new HashMap<>();
        for (TemplateCtgPathEntity entity : ctgPathEntities) {
            if (!StringUtils.startsWith(entity.getCtgIdPath(), QueryTemplateCategoryType.SPACE.getId())) {
                continue;
            }
            // 共享空间子目录和空间目录的关系
            String spaceCtgId = StringUtils.substring(entity.getCtgIdPath(), QueryTemplateCategoryType.SPACE.getId().length() + 1);
            spaceCtgId = StringUtils.substringBefore(spaceCtgId, "/");

            String spaceCtgName = StringUtils.substring(entity.getCtgNamePath(), QueryTemplateCategoryType.SPACE.getDesc().length() + 1);
            spaceCtgName = StringUtils.substringBefore(spaceCtgName, "/");

            PortalPublicDomainResp resp = new PortalPublicDomainResp();
            resp.setSpaceCtgId(spaceCtgId);
            resp.setSpaceCtgName(spaceCtgName);
            resp.setPortalMenuPath(entity.getCtgNamePath());
            resp.setCtgId(entity.getCtgId());
            resp.setIsInPublicDomain(Enabled.NO.getId());
            spaceCtgMap.computeIfAbsent(spaceCtgId, k -> new ArrayList<>()).add(resp);
        }

        // 查询公域相关信息
        PortalPublicDomainReq req = new PortalPublicDomainReq();
        req.setCtgIds(new ArrayList<>(spaceCtgMap.keySet()));
        req.setDashboardMenuIds(analysisTplIds);
        List<PortalPublicDomainResp> res = portalMenuService.listPublicDomainInfo(req);

        List<PortalPublicDomainResp> result = new ArrayList<>();
        for (PortalPublicDomainResp resp : res) {
            if (resp.getCtgId() == null) {
                // 看板的
                resp.setIsInPublicDomain(Enabled.YES.getId());
                result.add(resp);
            } else {
                // 查询模板
                List<PortalPublicDomainResp> leafCtgRespList = spaceCtgMap.remove(resp.getCtgId());
                if (leafCtgRespList == null) {
                    continue;
                }
                for (PortalPublicDomainResp leafCtgResp : leafCtgRespList) {
                    PortalPublicDomainResp leafResp = PortalPublicDomainResp.copy(resp);
                    leafResp.setSpaceCtgId(leafCtgResp.getSpaceCtgId());
                    leafResp.setSpaceCtgName(leafCtgResp.getSpaceCtgName());
                    leafResp.setCtgId(leafCtgResp.getCtgId());
                    leafResp.setIsInPublicDomain(Enabled.YES.getId());
                    result.add(leafResp);
                }
            }
        }

        for (Map.Entry<String, List<PortalPublicDomainResp>> entry : spaceCtgMap.entrySet()) {
            result.addAll(entry.getValue());
        }

        return result;
    }
}
