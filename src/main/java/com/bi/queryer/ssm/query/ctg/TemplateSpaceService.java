package com.bi.queryer.ssm.query.ctg;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.exception.SSDException;
import com.bi.queryer.ssm.query.ctg.enums.QueryTemplateCategoryType;
import com.bi.queryer.ssm.query.ctg.enums.TemplateSpaceRoleType;
import com.bi.queryer.ssm.query.ctg.model.*;
import com.bi.queryer.ssm.query.template.enums.QueryTemplateShareType;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.Guid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Scope("prototype")
public class TemplateSpaceService {

    @Autowired
    private BaseDao dao = null;

    @Autowired
    private QueryTemplateCategoryService ctgService;


    /**
     * 保存、编辑共享空间
     * @param req
     */
    public String saveSpace(TemplateSpaceAddReq req) {
        if (StrUtil.isEmpty(req.getId())) {
            return addSpace(req);
        } else {
            return editSpace(req);
        }
    }

    //保存
    private String addSpace(TemplateSpaceAddReq req) {
        QueryTemplateCategory spaceCtg = new QueryTemplateCategory();
        BeanUtil.copyProperties(req, spaceCtg);
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                spaceCtg.setType(QueryTemplateCategoryType.SPACE.getId());
                ctgService.addCategory(spaceCtg);
                List<TemplateSpaceEntity> spaceEntityList = buildSpaceEntityList(req, spaceCtg.getId());
                if (CollectionUtil.isNotEmpty(spaceEntityList)) {
                    dao.insert("ssm.template.ctg.batchAddTplSpace", spaceEntityList);
                }
            }
        });
        return spaceCtg.getId();
    }

    // 更改共享空间
    private String editSpace(TemplateSpaceAddReq req) {
        List<TemplateSpaceMember> authList = ListUtil.list(false);
        authList.addAll(req.getUserList());
        authList.addAll(req.getDeptList());
        if (authList.stream().filter(item -> TemplateSpaceRoleType.ADMIN.getCode().equals(item.getOwnerRole())).count() <= 0) {
            throw new SSDException("保存失败，需至少配置一个管理员权限的用户或部门！");
        }
        QueryTemplateCategory spaceCtg = new QueryTemplateCategory();
        BeanUtil.copyProperties(req, spaceCtg);
        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                spaceCtg.setUpdatedBy(UserManager.get().getName());
                dao.update("ssm.template.ctg.updateCategoryName", spaceCtg);
                // 先删除所有共享数据再插入
                dao.delete("ssm.template.ctg.deleteSpaceByCtgId", req.getId());
                List<TemplateSpaceEntity> spaceEntityList = buildSpaceEntityList(req, spaceCtg.getId());
                if (CollectionUtil.isNotEmpty(spaceEntityList)) {
                    dao.insert("ssm.template.ctg.batchAddTplSpace", spaceEntityList);
                }
            }
        });
        return spaceCtg.getId();
    }

    /**
     * 构建共享权限列表数据
     * @param req
     * @return
     */
    private List<TemplateSpaceEntity> buildSpaceEntityList(TemplateSpaceAddReq req, String ctgId) {
        List<TemplateSpaceEntity> spaceEntityList = ListUtil.list(false);
        User user = UserManager.get();
        for (TemplateSpaceMember templateSpaceMember : req.getUserList()) {
            TemplateSpaceEntity spaceEntity = new TemplateSpaceEntity();
            spaceEntity.setPkid(Guid.id());
            spaceEntity.setSpaceCtgId(ctgId);
            spaceEntity.setOwnerType(QueryTemplateShareType.USER.getId());
            spaceEntity.setOwnerId(templateSpaceMember.getUserName());
            spaceEntity.setOwnerDesc(templateSpaceMember.getUserRealName());
            spaceEntity.setOwnerRole(templateSpaceMember.getOwnerRole());
            spaceEntity.setCreatedBy(user.getName());
            spaceEntityList.add(spaceEntity);
        }
        for (TemplateSpaceMember templateSpaceMember : req.getDeptList()) {
            TemplateSpaceEntity spaceEntity = new TemplateSpaceEntity();
            spaceEntity.setPkid(Guid.id());
            spaceEntity.setSpaceCtgId(ctgId);
            spaceEntity.setOwnerType(QueryTemplateShareType.DEPT.getId());
            spaceEntity.setOwnerId(templateSpaceMember.getDeptId());
            spaceEntity.setOwnerDesc(templateSpaceMember.getDeptName());
            spaceEntity.setOwnerRole(templateSpaceMember.getOwnerRole());
            spaceEntity.setCreatedBy(user.getName());
            spaceEntityList.add(spaceEntity);
        }
        return spaceEntityList;
    }

    /**
     * 空间详情
     * @param ctgId
     * @return
     */
    public TemplateSpaceDetailRsp spaceDetail(String ctgId) {
        QueryTemplateCategory tplCtg = ctgService.getCategoryById(ctgId);
        if (tplCtg == null) {
            throw new SSDException("空间已被删除！");
        }
        List<TemplateSpaceEntity> spaceEntityList = (List<TemplateSpaceEntity>) dao.queryObjectList("ssm.template.ctg.querySpaceListByCtgId", ctgId);
        List<TemplateSpaceMember> userList = ListUtil.list(false);
        List<TemplateSpaceMember> deptList = ListUtil.list(false);
        int isAdmin = 0;
        User user = UserManager.get();
        for (TemplateSpaceEntity spaceEntity : spaceEntityList) {
            TemplateSpaceMember spaceMember = new TemplateSpaceMember();
            spaceMember.setOwnerRole(spaceEntity.getOwnerRole());
            if (QueryTemplateShareType.USER.getId().equals(spaceEntity.getOwnerType())) {
                spaceMember.setUserName(spaceEntity.getOwnerId());
                spaceMember.setUserRealName(spaceEntity.getOwnerDesc());
                userList.add(spaceMember);

                //owner_id = #{userName}
                if (TemplateSpaceRoleType.ADMIN.getCode().equals(spaceEntity.getOwnerRole()) &&
                        Objects.equals(spaceEntity.getOwnerId(), user.getName())
                ) {
                    isAdmin = 1;
                }
            } else {
                spaceMember.setDeptId(spaceEntity.getOwnerId());
                spaceMember.setDeptName(spaceEntity.getOwnerDesc());
                deptList.add(spaceMember);

                //and concat(#{deptId}, '/') like concat('%', s1.owner_id, '/%')
                if (TemplateSpaceRoleType.ADMIN.getCode().equals(spaceEntity.getOwnerRole()) &&
                        StringUtils.contains(user.getDeptId() + "/", spaceEntity.getOwnerId() + "/")
                ) {
                    isAdmin = 1;
                }
            }
        }

        List<String> publicCtgList = ctgService.getPublicCtgList(Collections.singletonList(ctgId));

        TemplateSpaceDetailRsp detailRsp = new TemplateSpaceDetailRsp();
        BeanUtil.copyProperties(tplCtg, detailRsp);
        detailRsp.setUserList(userList);
        detailRsp.setDeptList(deptList);
        detailRsp.setIsInPubicDomain(publicCtgList.contains(ctgId) ? 1 : 0);
        detailRsp.setIsAdmin(isAdmin);
        return detailRsp;
    }

    public void exitSpace(String ctgId) {
        // 查看退出空间的人所在部门是否在权限列表中，如果在，则无法退出共享空间
        User user = UserManager.get();
        List<TemplateSpaceEntity> spaceEntityList = (List<TemplateSpaceEntity>) dao.queryObjectList("ssm.template.ctg.querySpaceListByCtgId", ctgId);
        for (TemplateSpaceEntity spaceEntity : spaceEntityList) {
            if (QueryTemplateShareType.DEPT.equals(QueryTemplateShareType.get(spaceEntity.getOwnerType()))) {
                if (user.getDeptId().contains(spaceEntity.getOwnerId())) {
                    throw new SSDException("因您所在的部门拥有该共享空间权限，所以无法退出！");
                }
            }
        }
        if (spaceEntityList.stream().filter(item -> TemplateSpaceRoleType.ADMIN.getCode().equals(item.getOwnerRole())
                && !item.getOwnerId().equals(user.getName())).count() < 1) {
            throw new SSDException("当前共享空间只有最后一个管理员，无法退出。若该空间不再使用，可以解散空间！");
        }
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("ctgId", ctgId);
        queryMap.put("userName", user.getName());
        dao.queryObject("ssm.template.ctg.exitSpaceByUserName", queryMap);
    }
}
