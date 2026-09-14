package com.bi.queryer.ssm.engine.acl;

import java.util.Locale;
import java.util.Objects;

/**
 * 行级数据权限 exists 过滤描述对象
 * 当字段编码命中系统配置 ssm.data.auth.filter.by.exists 时，
 * 不再把权限枚举值写入 QueryField.values，改为在查询 SQL 的 where 中追加 exists 半连接过滤
 *
 * @author contributor
 */
public class AclDataAuthExistsFilterField {

    /** SSM 元字段编码，例如门店 ID 字段 code */
    private String fieldCode;

    /** 权限维度编码 */
    private String dimCode;

    /** 字段的数据权限模式：no_cfg_all / no_cfg_none */
    private String dataAuthMode;

    public AclDataAuthExistsFilterField() {
    }

    public AclDataAuthExistsFilterField(String fieldCode, String dimCode, String dataAuthMode) {
        this.fieldCode = fieldCode;
        this.dimCode = dimCode;
        this.dataAuthMode = dataAuthMode;
    }

    /**
     * exists 过滤的唯一性由模型、字段、权限维度和授权状态共同决定。
     * 字段编码和维度编码来自配置/元数据，统一按大小写不敏感比较，避免重复 ACL 注入时生成重复 SQL。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AclDataAuthExistsFilterField)) {
            return false;
        }
        AclDataAuthExistsFilterField that = (AclDataAuthExistsFilterField) obj;
        return Objects.equals(normalizeKey(fieldCode), normalizeKey(that.fieldCode))
                && Objects.equals(normalizeKey(dimCode), normalizeKey(that.dimCode));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                normalizeKey(fieldCode),
                normalizeKey(dimCode)
        );
    }

    private String normalizeKey(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "DataAuthExistsFilter{" +
                ", fieldCode='" + fieldCode + '\'' +
                ", dimCode='" + dimCode + '\'' +
                ", dataAuthMode='" + dataAuthMode + '\'' +
                '}';
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getDimCode() {
        return dimCode;
    }

    public void setDimCode(String dimCode) {
        this.dimCode = dimCode;
    }

    public String getDataAuthMode() {
        return dataAuthMode;
    }

    public void setDataAuthMode(String dataAuthMode) {
        this.dataAuthMode = dataAuthMode;
    }
}
