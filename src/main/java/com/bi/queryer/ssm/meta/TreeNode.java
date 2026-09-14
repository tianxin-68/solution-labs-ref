package com.bi.queryer.ssm.meta;

import com.alibaba.fastjson.JSONObject;

/**
 * @author contributor
 */
public class TreeNode {

    private String id;
    private String label;
    private Integer isLeaf;

    private String parentId;
    private String parentName;

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Integer getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Integer isLeaf) {
        this.isLeaf = isLeaf;
    }

    public JSONObject toTreeNode(){
        JSONObject node = new JSONObject();
        node.put("id", id );
        node.put("label", label);
        node.put("isLeaf", 0);
        return node;
    }
}
