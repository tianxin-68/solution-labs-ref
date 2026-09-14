package com.bi.queryer.ssm.sensitive.model;

import com.bi.queryer.sys.enums.Enabled;

/**
 * @Author contributor
 * @Date 17:02 2025/9/28
 * @Description TODO
 **/
public class DownloadWhitelist {
    private String userName;
    private String categoryId;
    private String categoryFullName;
    private Integer isModuleCategory = Enabled.NO.getId();

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryFullName() {
        return categoryFullName;
    }

    public void setCategoryFullName(String categoryFullName) {
        this.categoryFullName = categoryFullName;
    }

    public Integer getIsModuleCategory() {
        return isModuleCategory;
    }

    public void setIsModuleCategory(Integer isModuleCategory) {
        this.isModuleCategory = isModuleCategory;
    }
}
