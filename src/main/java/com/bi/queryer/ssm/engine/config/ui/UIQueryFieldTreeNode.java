package com.bi.queryer.ssm.engine.config.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author contributor
 * @Date 17:27 2025/1/6
 * @Description 前端字段树节点
 **/
public class UIQueryFieldTreeNode {
    private String id;

    private String label;

    private String code;
    private String name;
    private String title;

    private String type;

    private String parentId;

    private String moduleCtgId;

    private Integer isCommonDate = 0;

    private List<UIQueryFieldTreeNode> sameCodeFieldList = new ArrayList<UIQueryFieldTreeNode>();

    private List<UIQueryFieldTreeNode> children = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getModuleCtgId() {
        return moduleCtgId;
    }

    public void setModuleCtgId(String moduleCtgId) {
        this.moduleCtgId = moduleCtgId;
    }

    public Integer getIsCommonDate() {
        return isCommonDate;
    }

    public void setIsCommonDate(Integer isCommonDate) {
        this.isCommonDate = isCommonDate;
    }

    public List<UIQueryFieldTreeNode> getSameCodeFieldList() {
        return sameCodeFieldList;
    }

    public void setSameCodeFieldList(List<UIQueryFieldTreeNode> sameCodeFieldList) {
        this.sameCodeFieldList = sameCodeFieldList;
    }

    public List<UIQueryFieldTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<UIQueryFieldTreeNode> children) {
        this.children = children;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
