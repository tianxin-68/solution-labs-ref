package com.bi.queryer.ssm.migrate.bizsplit.service;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionCfgDtlShadowEntity;
import com.bi.queryer.ssm.migrate.bizsplit.model.MetricExpansionCfgShadowEntity;
import com.bi.queryer.ssm.query.template.model.TemplateCfgDtlEntity;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.Guid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 指标膨胀影子配置服务。
 *
 * 约定：
 * - 按正式 cfg_id / view_id 唯一；shadow_cfg_id 独立生成
 * - execute 写入影子；同一 view_id 重复执行则覆盖更新（复用 shadow_cfg_id）
 * - 复制视图：正式只拷正式；源有影子则为新 view/cfg 再插影子
 * - UT 保存：有影子则覆盖写影子；Product 始终正式库
 * - promote：影子写入新正式 cfg，视图 cfg_id 切到新 cfg，记录 old_cfg_id 供回滚
 * - rollback：视图 cfg_id 恢复为上线前 old_cfg_id
 */
@Service
@Scope("prototype")
public class MetricExpansionShadowService {

    @Autowired
    private BaseDao dao;

    /**
     * 是否 UT 环境（仅 RuntimeEnv.UT）。
     *
     * @return true 表示当前为 UT
     */
    public boolean isUtEnv() {
        return BIUtil.getRuntimeEnv() == RuntimeEnv.UT;
    }

    /**
     * 指定正式 cfg 是否已有影子。
     *
     * @param cfgId 正式配置 id
     * @return true 表示影子存在
     */
    public boolean existsShadow(String cfgId) {
        if (StrUtil.isEmpty(cfgId)) {
            return false;
        }
        Integer count = dao.queryCount("ssm.query.template.cfg.shadow.countByCfgId", cfgId);
        return count != null && count > 0;
    }

    /**
     * 指定视图是否已有影子。
     *
     * @param viewId 视图 id
     * @return true 表示影子存在
     */
    public boolean existsShadowByViewId(String viewId) {
        if (StrUtil.isEmpty(viewId)) {
            return false;
        }
        Integer count = dao.queryCount("ssm.query.template.cfg.shadow.countByViewId", viewId);
        return count != null && count > 0;
    }

    /**
     * 按 view_id 查询影子配置。
     *
     * @param viewId 视图 id
     * @return 影子实体；不存在返回 null
     */
    public MetricExpansionCfgShadowEntity getShadowByViewId(String viewId) {
        if (StrUtil.isEmpty(viewId)) {
            return null;
        }
        return (MetricExpansionCfgShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getCfgByViewId", viewId);
    }

    /**
     * UT 环境下若存在影子则返回影子 tplConfig，否则返回 null。
     *
     * @param cfgId 正式配置 id
     * @return 影子 tplConfig；非 UT / 无影子时返回 null
     */
    public String getShadowTplConfigForUtRead(String cfgId) {
        if (!isUtEnv() || StrUtil.isEmpty(cfgId)) {
            return null;
        }
        MetricExpansionCfgShadowEntity shadow = (MetricExpansionCfgShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getCfgByCfgId", cfgId);
        if (shadow == null || StrUtil.isEmpty(shadow.getTplConfig())) {
            return null;
        }
        return shadow.getTplConfig();
    }

    /**
     * UT 环境下若存在影子 dtl 则转为 TemplateCfgDtlEntity 返回，否则返回 null。
     *
     * @param cfgId 正式配置 id
     * @return 影子明细；非 UT / 无影子时返回 null
     */
    public TemplateCfgDtlEntity getShadowDtlForUtRead(String cfgId) {
        if (!isUtEnv() || StrUtil.isEmpty(cfgId)) {
            return null;
        }
        MetricExpansionCfgDtlShadowEntity shadow = (MetricExpansionCfgDtlShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getDtlByCfgId", cfgId);
        if (shadow == null) {
            return null;
        }
        return toTemplateCfgDtl(shadow);
    }

    /**
     * 复制源视图影子到新视图（仅 UT；源无影子则跳过）。
     * 正式库复制与影子复制分离：正式只拷正式；影子仅在源有影子时为新 view/cfg 再插一条。
     *
     * @param oldCfgId 源正式 cfg id
     * @param newCfgId 新正式 cfg id
     * @param newViewId 新视图 id
     * @param newTplId  新模板 id
     */
    public void copyShadowIfPresent(String oldCfgId, String newCfgId, String newViewId, String newTplId) {
        // Product 等非 UT 不读写影子
        if (!isUtEnv() || StrUtil.isEmpty(oldCfgId) || StrUtil.isEmpty(newCfgId)) {
            return;
        }
        MetricExpansionCfgShadowEntity oldShadow = (MetricExpansionCfgShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getCfgByCfgId", oldCfgId);
        if (oldShadow == null || StrUtil.isEmpty(oldShadow.getTplConfig())) {
            return;
        }
        TemplateCfgDtlEntity dtl = null;
        MetricExpansionCfgDtlShadowEntity oldDtl = (MetricExpansionCfgDtlShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getDtlByCfgId", oldCfgId);
        if (oldDtl != null) {
            dtl = toTemplateCfgDtl(oldDtl);
            dtl.setCfgId(newCfgId);
        }
        insertShadow(newCfgId, newViewId, newTplId, oldShadow.getTplConfig(), dtl,
                oldShadow.getSourceBusinessline(), oldShadow.getTargetBusinessline());
    }

    /**
     * 查询影子表中全部 view_id（promote 未指定 viewIds 时使用）。
     *
     * @return view_id 列表
     */
    @SuppressWarnings("unchecked")
    public List<String> listAllShadowViewIds() {
        List<String> viewIds = (List<String>) dao.queryObjectList(
                "ssm.query.template.cfg.shadow.listAllViewIds", null, String.class);
        if (viewIds == null || viewIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(viewIds.size());
        for (String viewId : viewIds) {
            if (StrUtil.isNotEmpty(viewId)) {
                result.add(viewId);
            }
        }
        return result;
    }

    /**
     * 按正式 cfg_id 查询影子 dtl（上线 promote 使用，不限制 UT）。
     *
     * @param cfgId 影子表记录的正式 cfg id
     * @return 影子 dtl；不存在返回 null
     */
    public MetricExpansionCfgDtlShadowEntity getShadowDtlEntityByCfgId(String cfgId) {
        if (StrUtil.isEmpty(cfgId)) {
            return null;
        }
        return (MetricExpansionCfgDtlShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getDtlByCfgId", cfgId);
    }

    /**
     * UT 保存：若该正式 cfg 已有影子，则覆盖写影子并返回 true；否则返回 false。
     *
     * @param cfgId     正式配置 id
     * @param viewId    视图 id
     * @param tplId     模板 id
     * @param tplConfig 明文 tplConfig
     * @param dtl       明细
     * @return true 表示已写入影子
     */
    public boolean savePreferShadowOnUt(String cfgId, String viewId, String tplId,
                                        String tplConfig, TemplateCfgDtlEntity dtl) {
        if (!isUtEnv() || StrUtil.isEmpty(cfgId) || !existsShadow(cfgId)) {
            return false;
        }
        updateShadow(cfgId, viewId, tplId, tplConfig, dtl);
        return true;
    }

    /**
     * execute 写入影子：已有则覆盖更新（复用 shadow_cfg_id），否则首次插入。
     *
     * @param cfgId              正式配置 id
     * @param viewId             视图 id
     * @param tplId              模板 id
     * @param tplConfig          明文 tplConfig
     * @param dtl                明细，允许为空
     * @param sourceBusinessline 源业务线，可空
     * @param targetBusinessline 目标业务线，可空
     * @return 影子配置 id
     */
    public String saveShadowForExecute(String cfgId, String viewId, String tplId, String tplConfig,
                                       TemplateCfgDtlEntity dtl,
                                       String sourceBusinessline, String targetBusinessline) {
        if (existsShadow(cfgId)) {
            return replaceShadow(cfgId, viewId, tplId, tplConfig, dtl,
                    sourceBusinessline, targetBusinessline);
        }
        return insertShadow(cfgId, viewId, tplId, tplConfig, dtl,
                sourceBusinessline, targetBusinessline);
    }

    /**
     * 首次写入影子 cfg + dtl（仅 insert，不更新）。
     * 供 copyShadowIfPresent 等首次插入场景使用。
     *
     * @param cfgId              正式配置 id
     * @param viewId             视图 id
     * @param tplId              模板 id
     * @param tplConfig          明文 tplConfig
     * @param dtl                明细，允许为空
     * @param sourceBusinessline 源业务线，可空
     * @param targetBusinessline 目标业务线，可空
     * @return 影子配置 id
     */
    public String insertShadow(String cfgId, String viewId, String tplId, String tplConfig,
                               TemplateCfgDtlEntity dtl,
                               String sourceBusinessline, String targetBusinessline) {
        String operator = resolveOperator();
        String shadowCfgId = Guid.id();

        MetricExpansionCfgShadowEntity cfgShadow = new MetricExpansionCfgShadowEntity();
        cfgShadow.setShadowCfgId(shadowCfgId);
        cfgShadow.setCfgId(cfgId);
        cfgShadow.setViewId(viewId);
        cfgShadow.setTplId(tplId);
        cfgShadow.setTplConfig(tplConfig);
        cfgShadow.setSourceBusinessline(sourceBusinessline);
        cfgShadow.setTargetBusinessline(targetBusinessline);
        cfgShadow.setCreatedBy(operator);
        cfgShadow.setUpdatedBy(operator);
        dao.insert("ssm.query.template.cfg.shadow.insertCfg", cfgShadow);

        if (dtl != null) {
            MetricExpansionCfgDtlShadowEntity dtlShadow = new MetricExpansionCfgDtlShadowEntity();
            dtlShadow.setShadowCfgId(shadowCfgId);
            dtlShadow.setCfgId(cfgId);
            dtlShadow.setTplConfigFieldDimCodes(dtl.getTplConfigFieldDimCodes());
            dtlShadow.setTplConfigFieldMeasureCodes(dtl.getTplConfigFieldMeasureCodes());
            dtlShadow.setTplConfigFieldDimAsset(dtl.getTplConfigFieldDimAsset());
            dtlShadow.setTplConfigFieldMeasureAsset(dtl.getTplConfigFieldMeasureAsset());
            dtlShadow.setCreatedBy(operator);
            dtlShadow.setUpdatedBy(operator);
            dao.insert("ssm.query.template.cfg.shadow.insertDtl", dtlShadow);
        }
        return shadowCfgId;
    }

    /**
     * UT 保存覆盖写影子（复用既有 shadow_cfg_id，保留既有业务线元数据）。
     *
     * @param cfgId     正式配置 id
     * @param viewId    视图 id
     * @param tplId     模板 id
     * @param tplConfig 明文 tplConfig
     * @param dtl       明细，允许为空
     */
    public void updateShadow(String cfgId, String viewId, String tplId, String tplConfig,
                             TemplateCfgDtlEntity dtl) {
        MetricExpansionCfgShadowEntity existing = (MetricExpansionCfgShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getCfgByCfgId", cfgId);
        if (existing == null) {
            return;
        }
        replaceShadow(cfgId, viewId, tplId, tplConfig, dtl,
                existing.getSourceBusinessline(), existing.getTargetBusinessline());
    }

    /**
     * 覆盖写影子 cfg + dtl（复用既有 shadow_cfg_id）。
     *
     * @param cfgId              正式配置 id
     * @param viewId             视图 id
     * @param tplId              模板 id
     * @param tplConfig          明文 tplConfig
     * @param dtl                明细，允许为空
     * @param sourceBusinessline 源业务线，可空
     * @param targetBusinessline 目标业务线，可空
     * @return 复用的 shadow_cfg_id；影子不存在时返回 null
     */
    private String replaceShadow(String cfgId, String viewId, String tplId, String tplConfig,
                                 TemplateCfgDtlEntity dtl,
                                 String sourceBusinessline, String targetBusinessline) {
        MetricExpansionCfgShadowEntity existing = (MetricExpansionCfgShadowEntity) dao.queryObject(
                "ssm.query.template.cfg.shadow.getCfgByCfgId", cfgId);
        if (existing == null) {
            return null;
        }
        String operator = resolveOperator();
        String createdBy = StrUtil.isNotEmpty(existing.getCreatedBy()) ? existing.getCreatedBy() : operator;
        String shadowCfgId = existing.getShadowCfgId();

        dao.delete("ssm.query.template.cfg.shadow.deleteCfgByCfgId", cfgId);
        dao.delete("ssm.query.template.cfg.shadow.deleteDtlByCfgId", cfgId);

        MetricExpansionCfgShadowEntity cfgShadow = new MetricExpansionCfgShadowEntity();
        cfgShadow.setShadowCfgId(shadowCfgId);
        cfgShadow.setCfgId(cfgId);
        cfgShadow.setViewId(viewId);
        cfgShadow.setTplId(tplId);
        cfgShadow.setTplConfig(tplConfig);
        cfgShadow.setSourceBusinessline(sourceBusinessline);
        cfgShadow.setTargetBusinessline(targetBusinessline);
        cfgShadow.setCreatedBy(createdBy);
        cfgShadow.setUpdatedBy(operator);
        dao.insert("ssm.query.template.cfg.shadow.insertCfg", cfgShadow);

        if (dtl != null) {
            MetricExpansionCfgDtlShadowEntity dtlShadow = new MetricExpansionCfgDtlShadowEntity();
            dtlShadow.setShadowCfgId(shadowCfgId);
            dtlShadow.setCfgId(cfgId);
            dtlShadow.setTplConfigFieldDimCodes(dtl.getTplConfigFieldDimCodes());
            dtlShadow.setTplConfigFieldMeasureCodes(dtl.getTplConfigFieldMeasureCodes());
            dtlShadow.setTplConfigFieldDimAsset(dtl.getTplConfigFieldDimAsset());
            dtlShadow.setTplConfigFieldMeasureAsset(dtl.getTplConfigFieldMeasureAsset());
            dtlShadow.setCreatedBy(createdBy);
            dtlShadow.setUpdatedBy(operator);
            dao.insert("ssm.query.template.cfg.shadow.insertDtl", dtlShadow);
        }
        return shadowCfgId;
    }

    /**
     * 将影子 dtl 转为正式明细实体；对外仍返回正式 cfgId。
     *
     * @param shadow 影子 dtl
     * @return TemplateCfgDtlEntity
     */
    private TemplateCfgDtlEntity toTemplateCfgDtl(MetricExpansionCfgDtlShadowEntity shadow) {
        TemplateCfgDtlEntity dtl = new TemplateCfgDtlEntity();
        dtl.setCfgId(shadow.getCfgId());
        dtl.setTplConfigFieldDimCodes(shadow.getTplConfigFieldDimCodes());
        dtl.setTplConfigFieldMeasureCodes(shadow.getTplConfigFieldMeasureCodes());
        dtl.setTplConfigFieldDimAsset(shadow.getTplConfigFieldDimAsset());
        dtl.setTplConfigFieldMeasureAsset(shadow.getTplConfigFieldMeasureAsset());
        return dtl;
    }

    /**
     * @return 当前操作人用户名，未登录时返回 null
     */
    private String resolveOperator() {
        return UserManager.get() != null ? UserManager.get().getName() : null;
    }
}
