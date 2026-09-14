package com.bi.queryer.sys.authapply.model.ssm;

import com.bi.queryer.sys.authapply.model.BaseTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 行权限子表
 */
public class SsmRowPermissionChildTemplate {

    private String removedRowPermissions;
    private String hasRowPermissions;
    private String addedRowPermissions;
    private String rowDim;

    public List<BaseTemplate> buildChildTemplate() {
        List<BaseTemplate> list = new ArrayList<>();
        list.add(new BaseTemplate("removedRowPermissions", removedRowPermissions));
        list.add(new BaseTemplate("hasRowPermissions", hasRowPermissions));
        list.add(new BaseTemplate("addedRowPermissions", addedRowPermissions));
        list.add(new BaseTemplate("rowDim", rowDim));
        return list;
    }

    public String getRemovedRowPermissions() {
        return removedRowPermissions;
    }

    public void setRemovedRowPermissions(String removedRowPermissions) {
        this.removedRowPermissions = removedRowPermissions;
    }

    public String getHasRowPermissions() {
        return hasRowPermissions;
    }

    public void setHasRowPermissions(String hasRowPermissions) {
        this.hasRowPermissions = hasRowPermissions;
    }

    public String getAddedRowPermissions() {
        return addedRowPermissions;
    }

    public void setAddedRowPermissions(String addedRowPermissions) {
        this.addedRowPermissions = addedRowPermissions;
    }

    public String getRowDim() {
        return rowDim;
    }

    public void setRowDim(String rowDim) {
        this.rowDim = rowDim;
    }
}
