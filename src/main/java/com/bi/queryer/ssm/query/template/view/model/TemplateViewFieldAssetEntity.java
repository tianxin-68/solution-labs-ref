package com.bi.queryer.ssm.query.template.view.model;

/**
 * 模板视图字段资产
 */
public class TemplateViewFieldAssetEntity {

    /**
     *
     */
    private String type;

    /**
     * 资产名称
     */
    private String name;

    /**
     * 资产标识
     */
    private String assetIdentifier;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetIdentifier() {
        return assetIdentifier;
    }

    public void setAssetIdentifier(String assetIdentifier) {
        this.assetIdentifier = assetIdentifier;
    }
}
