package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

import java.util.List;

@Data
public class TemplateShareReq {

    /**
     * 分享的目录id
     */
    private String ctgId;

    /**
     * 模版ID集合
     */
    private List<String> tplIdList;

    /**
     * 分享用户（单个）
     */
   // private String userName;

    /**
     * 分享组织机构ID
     */
    private String deptId;

    /**
     * 批量分享给人员
     */
    private String userNames;

}
