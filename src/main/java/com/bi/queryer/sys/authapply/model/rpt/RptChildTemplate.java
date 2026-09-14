package com.bi.queryer.sys.authapply.model.rpt;

import com.bi.queryer.sys.authapply.model.BaseTemplate;

import java.util.ArrayList;
import java.util.List;

public class RptChildTemplate {

    //报表名称  权限时长和下载权限拼接到报表名
    private String rptId;

    //数据权限
    private String dataAuths;

    //报表文件夹
    private String rptPath;

    public List<BaseTemplate> buildRptChildTemplate() {
        List<BaseTemplate> list = new ArrayList<>();
        list.add(new BaseTemplate("rptId", getRptId()));
        list.add(new BaseTemplate("dataAuths", getDataAuths()));
        list.add(new BaseTemplate("rptPath", getRptPath()));
        return list;
    }

    public String getRptId() {
        return rptId;
    }

    public void setRptId(String rptId) {
        this.rptId = rptId;
    }

    public String getDataAuths() {
        return dataAuths;
    }

    public void setDataAuths(String dataAuths) {
        this.dataAuths = dataAuths;
    }

    public String getRptPath() {
        return rptPath;
    }

    public void setRptPath(String rptPath) {
        this.rptPath = rptPath;
    }
}
