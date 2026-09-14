package com.bi.queryer.ssm.query.template.model;

import lombok.Data;

@Data
public class MoveTopTplReq {

    /**
     * 模版ID
     */
    private String tplId;

    /**
     * 移动到target上面或下面 top/bottom
     */
    private String pos;
    //移动到哪个模板后面， ""表示置顶
    private String targetTplId;
}
