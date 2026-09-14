package com.bi.queryer.ssm.governance.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.governance.entity.GovernanceExempt;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 治理配置服务：豁免模块/目录、按 owner 灰度
 */
@Service
public class GovernanceConfigService {

    /** 灰度 owner 白名单（逗号分隔），为空=全量放开 */
    private static final String GRAY_OWNERS_KEY = "ssm.governance.gray.owners";

    /** owner 白名单：列出的 owner 不进入生命周期管理（逗号分隔） */
    private static final String WHITELIST_OWNERS_KEY = "ssm.governance.whitelist.owners";

    /** 无访问/无引用治理阈值（天）配置项 */
    private static final String NO_VISIT_DAYS_KEY = "ssm.governance.no.visit.days";
    /** 阈值默认值 */
    private static final int DEFAULT_NO_VISIT_DAYS = 180;

    @Autowired
    private BaseDao dao;

    /** 无访问/无引用治理阈值（天），从 SC 读取，未配置或非法时回退默认 90 */
    public int noVisitThreshold() {
        return Convert.toInt(SC.v(NO_VISIT_DAYS_KEY), DEFAULT_NO_VISIT_DAYS);
    }

    // ===================== owner 白名单（永久豁免治理） =====================

    /**
     * owner 白名单（逗号分隔）：名单内任一 owner 的模板，不进入生命周期管理。
     * 对应配置项 {@value WHITELIST_OWNERS_KEY}；未配置返回空集（即无豁免）。
     */
    public Set<String> whitelistOwners() {
        String v = SC.v(WHITELIST_OWNERS_KEY);
        if (StrUtil.isBlank(v)) {
            return Collections.emptySet();
        }
        Set<String> owners = new HashSet<>();
        for (String s : v.split(",")) {
            if (StrUtil.isNotBlank(s)) {
                owners.add(s.trim());
            }
        }
        return owners;
    }

    /**
     * ownersCsv 中任一 owner 在白名单内，则该模板豁免治理（返回 true）。
     * 白名单为空时始终返回 false（不豁免任何人）。
     */
    public boolean inOwnerWhitelist(String ownersCsv) {
        Set<String> whitelist = whitelistOwners();
        if (whitelist.isEmpty() || StrUtil.isBlank(ownersCsv)) {
            return false;
        }
        for (String o : ownersCsv.split(",")) {
            if (StrUtil.isNotBlank(o) && whitelist.contains(o.trim())) {
                return true;
            }
        }
        return false;
    }

    // ===================== 按 owner 灰度 =====================

    /** 灰度 owner 白名单；未配置返回空集（即全量放开） */
    public Set<String> grayOwners() {
        String v = SC.v(GRAY_OWNERS_KEY);
        if (StrUtil.isBlank(v)) {
            return Collections.emptySet();
        }
        Set<String> owners = new HashSet<>();
        for (String s : v.split(",")) {
            if (StrUtil.isNotBlank(s)) {
                owners.add(s.trim());
            }
        }
        return owners;
    }

    /**
     * 判断 owner 是否在本轮治理范围内。
     * 未配置灰度名单 = 全量放开；配置后仅名单内 owner 进入治理流程。
     */
    public boolean inGrayScope(String owner) {
        Set<String> owners = grayOwners();
        if (owners.isEmpty()) {
            return true;
        }
        return StrUtil.isNotBlank(owner) && owners.contains(owner.trim());
    }

    /**
     * 责任人可能是逗号分隔的多人：只要任一在灰度名单内即算命中。
     * 未配置灰度名单 = 全量放开。
     */
    public boolean inGrayScopeAny(String ownersCsv) {
        Set<String> owners = grayOwners();
        if (owners.isEmpty()) {
            return true;
        }
        if (StrUtil.isBlank(ownersCsv)) {
            return false;
        }
        for (String o : ownersCsv.split(",")) {
            if (StrUtil.isNotBlank(o) && owners.contains(o.trim())) {
                return true;
            }
        }
        return false;
    }

    // ===================== 豁免模块/目录 =====================

    @SuppressWarnings("unchecked")
    public PageInfo<GovernanceExempt> listExempt(String objectScope, int pageNum, int pageSize) {
        Map<String, Object> param = new HashMap<>();
        param.put("objectScope", objectScope);
        param.put("startNum", (pageNum - 1) * pageSize);
        param.put("pageSize", pageSize);
        List<GovernanceExempt> list = (List<GovernanceExempt>) dao.queryObjectList("ssm.governance.config.listExempt", param);
        Integer total = dao.queryCount("ssm.governance.config.listExemptCount", param);
        PageInfo<GovernanceExempt> page = new PageInfo<>(list);
        page.setTotal(total == null ? 0 : total);
        return page;
    }

    /** 扫描硬排除使用：生效中的豁免规则（scope 可空 = 全部） */
    @SuppressWarnings("unchecked")
    public List<GovernanceExempt> queryActiveExempt(String objectScope) {
        return (List<GovernanceExempt>) dao.queryObjectList("ssm.governance.config.queryActiveExempt", objectScope);
    }

    public void addExempt(GovernanceExempt exempt) {
        if (exempt.getIsActive() == null) {
            exempt.setIsActive(1);
        }
        if (StrUtil.isBlank(exempt.getCreatedBy())) {
            exempt.setCreatedBy(currentUser());
        }
        dao.insert("ssm.governance.config.insertExempt", exempt);
    }

    public void updateExemptActive(Long id, Integer isActive) {
        Map<String, Object> param = new HashMap<>();
        param.put("id", id);
        param.put("isActive", isActive);
        dao.update("ssm.governance.config.updateExemptActive", param);
    }

    /** 当前登录用户名，无登录上下文时回退 system */
    private String currentUser() {
        User user = UserManager.get();
        return user != null && StrUtil.isNotBlank(user.getName()) ? user.getName() : "system";
    }
}
