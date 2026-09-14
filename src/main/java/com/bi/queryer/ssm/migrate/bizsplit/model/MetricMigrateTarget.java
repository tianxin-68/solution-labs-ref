package com.bi.queryer.ssm.migrate.bizsplit.model;

import com.bi.queryer.ssm.migrate.bizsplit.enums.MetricMigrateFilterExpandRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一个新业务线拆分目标：复制来源 + 命名规则（执行计划第4节）。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricMigrateTarget {

    /** 老业务线名称，如"改装超市"。只用于看板名称/模板名称/目录名称这三类"名称"字段的替换（见
     * {@link #applyBizLineNameRule}），跟 {@link #oldToken}/{@link #newToken} 是两回事——后者仍然
     * 用于名称以外的其它文本（描述、widget 标题/内容、筛选器取值等）的替换，不受这个字段影响。 */
    private String oldBizLine;

    /** 新业务线名称，如"保养油液" */
    private String newBizLine;

    /**
     * 复制来源所在的门户id。不同 target 的来源可以属于不同门户（比如"电子改装"和"超市改装"分属两个独立门户），
     * 同一次 execute 请求内允许混合多个门户的 target。
     */
    private String portalId;

    /**
     * 复制来源名称，先在该门户下精确匹配一个目录/看板节点名；查不到时再判断是否等于门户自身的名称——
     * 命中门户名称则整个门户（连同其菜单树）一起复制，见 {@code MetricMigrateService#copyWholePortal}。
     * 如"保养运营分析"（目录）或"电子改装运营分析"（可能是目录，也可能就是门户名本身）。
     */
    private String sourceRootName;

    /** 名称替换的老子串，如"保养"；自我复制场景下与 newToken 相同 */
    private String oldToken;

    /** 名称替换的新子串，如"保养油液"；自我复制场景下与 oldToken 相同（即不替换） */
    private String newToken;

    /** 命名冲突后缀，如"-V2"；无需加后缀则为空字符串。仅追加在复制根节点名称上，不影响子孙节点命名 */
    @Builder.Default
    private String nameSuffix = "";

    private MetricMigrateFilterExpandRule ruleType;

    /**
     * 按本目标的命名规则计算某个源节点复制后的新名称。
     * @param sourceName 源节点名称
     * @param isRoot     是否为本次复制的根节点（只有根节点才追加 nameSuffix，避免整棵子树的名称都带上版本后缀）
     */
    public String applyNameRule(String sourceName, boolean isRoot) {
        if (sourceName == null) {
            return null;
        }
        String replaced = (oldToken != null && newToken != null && !oldToken.equals(newToken))
                ? sourceName.replace(oldToken, newToken)
                : sourceName;
        if (isRoot && nameSuffix != null && !nameSuffix.isEmpty()) {
            return replaced + nameSuffix;
        }
        return replaced;
    }

    /**
     * 看板名称/模板名称/目录名称专用的命名规则：替换用 {@link #oldBizLine}/{@link #newBizLine}，
     * 不是 {@link #oldToken}/{@link #newToken}——名称以外的文本（描述、widget 内容等）仍然调用
     * {@link #applyNameRule}，两套替换字符串可以不一样。nameSuffix 的追加规则跟 {@link #applyNameRule}
     * 保持一致（只在根节点追加）。
     * @param sourceName 源名称
     * @param isRoot     是否为本次复制的根节点
     */
    public String applyBizLineNameRule(String sourceName, boolean isRoot) {
        if (sourceName == null) {
            return null;
        }
        String replaced = (oldBizLine != null && newBizLine != null && !oldBizLine.equals(newBizLine))
                ? sourceName.replace(oldBizLine, newBizLine)
                : sourceName;
        if (isRoot && nameSuffix != null && !nameSuffix.isEmpty()) {
            return replaced + nameSuffix;
        }
        return replaced;
    }
}
