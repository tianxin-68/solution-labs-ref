package com.bi.queryer.sys.dim.dim.vo;

import java.util.List;

/**
 * 数据权限管控树实体
 * @author contributor
 */
public class DimItem {

    private String id;

    private String label;

    private String parentId;

    /**
     * 显示顺序
     */
    private Integer index;

    /**
     * 类型
     */
    private String type;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 维度编码
     */
    private String dimCode;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 维度名称
     */
    private String dimName;

    private List<com.bi.queryer.sys.dim.vo.DimItem> children;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<com.bi.queryer.sys.dim.vo.DimItem> getChildren() {
        return children;
    }

    public void setChildren(List<com.bi.queryer.sys.dim.vo.DimItem> children) {
        this.children = children;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getDimCode() {
        return dimCode;
    }

    public void setDimCode(String dimCode) {
        this.dimCode = dimCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getDimName() {
        return dimName;
    }

    public void setDimName(String dimName) {
        this.dimName = dimName;
    }
}
