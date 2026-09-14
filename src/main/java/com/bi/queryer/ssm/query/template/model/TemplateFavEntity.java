package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

@Data
public class TemplateFavEntity {
    private String favId;

    private String tplId;

    private Integer isTop;

    private Double topSortId;

    private String createdBy;

    private String createdTime;

    private String updatedBy;

    private String updatedTime;
    private String ctgId;
    private String favTplType;
    private Integer isFav;
}