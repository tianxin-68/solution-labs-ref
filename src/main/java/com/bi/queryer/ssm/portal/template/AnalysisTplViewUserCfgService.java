package com.bi.queryer.ssm.portal.template;

import com.bi.queryer.ssm.portal.template.entity.AnalysisTplViewEntity;
import com.bi.queryer.ssm.portal.template.entity.AnalysisTplViewUserCfgEntity;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bi.queryer.util.BIUtil.isNotEmpty;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * 看板视图用户配置 ssm_analysis_tpl_view_user_cfg
 */
@Service
@Scope("prototype")
public class AnalysisTplViewUserCfgService {

    @Autowired
    private BaseDao dao;

    @Autowired
    private AnalysisTplViewService analysisTplViewService;

    /**
     * 新增用户视图配置（主键：模版 id + 视图 id + 创建人）
     */
    public void add(AnalysisTplViewUserCfgEntity entity) {
        checkArgument(entity != null, "传入参数为空");
        checkArgument(isNotEmpty(entity.getAnalysisTplId()), "模版 id 为空");
        checkArgument(isNotEmpty(entity.getViewId()), "视图 id 为空");
        checkArgument(isNotEmpty(entity.getCreatedBy()), "创建人为空");
        if (entity.getIsDefault() == null) {
            entity.setIsDefault(0);
        }
        dao.insert("ssm.analysis.tpl.view.user.cfg.insert", entity);
    }

    /**
     * 按主键删除
     */
    public void delete(String analysisTplId, String viewId, String createdBy) {
        checkArgument(isNotEmpty(analysisTplId), "模版 id 为空");
        checkArgument(isNotEmpty(viewId), "视图 id 为空");
        checkArgument(isNotEmpty(createdBy), "创建人为空");
        AnalysisTplViewUserCfgEntity param = AnalysisTplViewUserCfgEntity.builder()
                .analysisTplId(analysisTplId)
                .viewId(viewId)
                .createdBy(createdBy)
                .build();
        dao.delete("ssm.analysis.tpl.view.user.cfg.deleteByPk", param);
    }

    /**
     * 按主键查询单条
     */
    public AnalysisTplViewUserCfgEntity getByPk(String analysisTplId, String viewId, String createdBy) {
        checkArgument(isNotEmpty(analysisTplId), "模版 id 为空");
        checkArgument(isNotEmpty(viewId), "视图 id 为空");
        checkArgument(isNotEmpty(createdBy), "创建人为空");
        AnalysisTplViewUserCfgEntity param = AnalysisTplViewUserCfgEntity.builder()
                .analysisTplId(analysisTplId)
                .viewId(viewId)
                .createdBy(createdBy)
                .build();
        return dao.queryObject("ssm.analysis.tpl.view.user.cfg.getByPk", param, AnalysisTplViewUserCfgEntity.class);
    }

    /**
     * 条件查询；可传 analysisTplId、viewId、createdBy、isDefault（均为可选，按需组合）
     */
    public List<AnalysisTplViewUserCfgEntity> listByCondition(Map<String, Object> condition) {
        Map<String, Object> param = condition != null ? condition : new HashMap<>();
        return dao.queryObjectList("ssm.analysis.tpl.view.user.cfg.listByCondition", param,
                AnalysisTplViewUserCfgEntity.class);
    }

    /**
     * 查询当前用户在指定模版下的默认视图 id（is_default = 1）
     */
    public String queryUserDefaultViewId(String analysisTplId, String createdBy) {
        checkArgument(isNotEmpty(analysisTplId), "模版 id 为空");
        checkArgument(isNotEmpty(createdBy), "创建人为空");
        Map<String, String> param = new HashMap<>();
        param.put("analysisTplId", analysisTplId);
        param.put("createdBy", createdBy);
        return dao.queryObject("ssm.analysis.tpl.view.user.cfg.queryUserDefaultViewId", param, String.class);
    }

    /**
     * 将指定视图设为当前用户在对应看板下的默认视图：先按模版 id + 用户删除该表下全部记录，再插入一条默认配置（is_default = 1）
     */
    public void setAsDefaultView(String analysisTplId, String viewId) {
        checkArgument(isNotEmpty(analysisTplId), "模版 id 为空");
        checkArgument(isNotEmpty(viewId), "视图 id 为空");
        AnalysisTplViewEntity view = analysisTplViewService.getEntityById(viewId);
        checkArgument(view != null, "视图不存在");
        checkArgument(analysisTplId.equals(view.getAnalysisTplId()), "视图不属于该看板模版");

        String userName = UserManager.get().getName();
        checkArgument(isNotEmpty(userName), "无法获取当前用户");

        dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
            @Override
            public void execute() {
                Map<String, String> delParam = new HashMap<>();
                delParam.put("analysisTplId", analysisTplId);
                delParam.put("createdBy", userName);
                dao.delete("ssm.analysis.tpl.view.user.cfg.deleteByTplAndUser", delParam);
                AnalysisTplViewUserCfgEntity row = AnalysisTplViewUserCfgEntity.builder()
                        .analysisTplId(analysisTplId)
                        .viewId(viewId)
                        .createdBy(userName)
                        .isDefault(1)
                        .build();
                dao.insert("ssm.analysis.tpl.view.user.cfg.insert", row);
            }
        });
    }
}
