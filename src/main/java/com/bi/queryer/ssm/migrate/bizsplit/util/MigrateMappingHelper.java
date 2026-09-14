package com.bi.queryer.ssm.migrate.bizsplit.util;

import com.bi.queryer.ssm.migrate.bizsplit.model.MetricMigrateMappingEntity;
import com.bi.queryer.ssm.portal.entity.Portal;
import com.bi.queryer.ssm.portal.entity.PortalMenu;
import com.bi.queryer.ssm.query.ctg.model.QueryTemplateCategory;
import com.bi.queryer.ssm.query.template.model.TemplateEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.exception.BIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 迁移映射表读写 + "同名复用"查找的公共小工具，供
 * {@link com.bi.queryer.ssm.migrate.bizsplit.service.MetricMigrateService} 与
 * {@link com.bi.queryer.ssm.migrate.bizsplit.processor.TplConfigMigrateProcessor} 共用，
 * 避免两处各自重复写一遍同样的 DAO 调用（执行计划第4.1、5、6节）。
 */
@Component
public class MigrateMappingHelper {

    @Autowired
    private BaseDao dao;

    /** 幂等判断：查某老对象在目标业务线下是否已经处理过（第6.1节第1层） */
    public MetricMigrateMappingEntity getMapping(String objectType, String oldId, String newBizLine) {
        Map<String, Object> params = new HashMap<>();
        params.put("objectType", objectType);
        params.put("oldId", oldId);
        params.put("newBizLine", newBizLine);
        return (MetricMigrateMappingEntity) dao.queryObject("ssm.migrate.bizsplit.getMapping", params);
    }

    /**
     * 记录一条新旧id映射（无论是新创建还是同名复用得到的，都要记录，见第4.1节末尾说明）。
     * @return 本次写入的映射记录（供调用方汇总到 {@code MetricMigrateExecuteRsp}），不含数据库生成的 id/createdTime
     */
    public MetricMigrateMappingEntity recordMapping(String objectType, String oldId, String oldName,
                                                       String newId, String newName, String oldBizLine, String newBizLine,
                                                       String portalId, boolean isReused, String createdBy) {
        // newId 和 oldId 相同意味着"复制"把源对象自己当成了已存在的同名副本直接复用了，不是一次真正的复制——
        // 常见于复制后名称没有变化（oldToken 在这个名字里没出现、nameSuffix 又是空）导致按名称查重时把
        // 源节点自己也匹配上了。这里做最后一道硬校验，不允许把这种自映射静默写进映射表。
        if (oldId != null && oldId.equals(newId)) {
            throw new BIException(String.format(
                    "[%s] 复制结果 newId 和 oldId 相同（%s），说明复制没有生成新对象，把源对象自己当成了同名副本复用了：%s",
                    objectType, oldId, oldName));
        }
        MetricMigrateMappingEntity entity = MetricMigrateMappingEntity.builder()
                .objectType(objectType)
                .oldId(oldId)
                .oldName(oldName)
                .newId(newId)
                .newName(newName)
                .oldBizLine(oldBizLine)
                .newBizLine(newBizLine)
                .portalId(portalId)
                .isReused(isReused ? 1 : 0)
                .createdBy(createdBy)
                .build();
        dao.insert("ssm.migrate.bizsplit.addMapping", entity);
        return entity;
    }

    /** 同名复用检查：门户目录节点（第4.1节，覆盖目录/内容/看板/共享空间菜单节点，统一落在 ssm_portal_menu） */
    public PortalMenu findExistingPortalMenu(String parentMenuId, String menuType, String menuName) {
        Map<String, Object> params = new HashMap<>();
        params.put("parentMenuId", parentMenuId);
        params.put("menuType", menuType);
        params.put("menuName", menuName);
        return (PortalMenu) dao.queryObject("ssm.migrate.bizsplit.getPortalMenuByParentAndName", params);
    }

    /** 同名复用检查：共享空间/查询模板目录树节点 */
    public QueryTemplateCategory findExistingCtg(String parentId, String name) {
        Map<String, Object> params = new HashMap<>();
        params.put("parentId", parentId);
        params.put("name", name);
        return (QueryTemplateCategory) dao.queryObject("ssm.migrate.bizsplit.getCtgByParentAndName", params);
    }

    /** 同名复用检查：门户本身（sourceRootName 命中门户名称，整个门户连同菜单树一起复制时使用） */
    public Portal findExistingPortal(String portalName) {
        return (Portal) dao.queryObject("ssm.migrate.bizsplit.getPortalByName", portalName);
    }

    /** 同名复用检查：查询模板 */
    public TemplateEntity findExistingTemplate(String ctgId, String tplName) {
        Map<String, Object> params = new HashMap<>();
        params.put("ctgId", ctgId);
        params.put("tplName", tplName);
        return (TemplateEntity) dao.queryObject("ssm.migrate.bizsplit.getTemplateByCtgAndName", params);
    }
}
