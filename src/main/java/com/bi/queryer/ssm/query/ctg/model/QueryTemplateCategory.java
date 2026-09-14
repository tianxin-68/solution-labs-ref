package com.bi.queryer.ssm.query.ctg.model;

import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author contributor
 * @Date 11:30 2023-11-11
 * @Description 查询模板分类目录
 **/
public class QueryTemplateCategory implements Comparable<QueryTemplateCategory>, Cloneable {
    private String id;
    private String name;
    private String type;
    private String description;
    private Double sortId = 9999D;
    private String parentId;
    private Integer isAdmin = 0;

    private Integer hasAuth = 0;
    private String createdBy;
    private String updatedBy;
    private String createdTime;
    private String updatedTime;

    private Integer isInPublicDomain = Enabled.NO.getId();

    private List<QueryTemplateCategory> children = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getSortId() {
        return sortId;
    }

    public void setSortId(Double sortId) {
        this.sortId = sortId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public List<QueryTemplateCategory> getChildren() {
        if (BIUtil.isNotEmpty(children)) {
            Collections.sort(children);
        }
        return children;
    }

    public void setChildren(List<QueryTemplateCategory> children) {
        this.children = children;
    }

    @Override
    public int compareTo(QueryTemplateCategory o) {
        if (o == null || this.sortId == null || o.sortId == null) {
            return -1;
        }
        return (int) (this.sortId - o.sortId);
    }

    public Integer getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Integer isAdmin) {
        this.isAdmin = isAdmin;
    }

    @Override
    public String toString() {
        return name + " " + type + " " + id;
    }

    public QueryTemplateCategory clone() {
        QueryTemplateCategory copy = JSONObject.toJavaObject(BIUtil.toJSONObject(this), QueryTemplateCategory.class);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            QueryTemplateCategory c = (QueryTemplateCategory) obj;
            if (c.id == null || this.id == null) {
                return false;
            }
            return this.id.equalsIgnoreCase(c.getId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : id.hashCode();
    }

    public Integer getHasAuth() {
        return hasAuth;
    }

    public void setHasAuth(Integer hasAuth) {
        this.hasAuth = hasAuth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIsInPublicDomain() {
        return isInPublicDomain;
    }

    public void setIsInPublicDomain(Integer isInPublicDomain) {
        this.isInPublicDomain = isInPublicDomain;
    }
}
