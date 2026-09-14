package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

/**
 * @Auther: contributor
 * @Date: 2025/9/18 10:37
 * @Description:
 */

@Data
public class TemplateFavRsp {
    private String favId;
    private String tplId;
    private String tplName;
    private Double topSortId = 9999D;
}
