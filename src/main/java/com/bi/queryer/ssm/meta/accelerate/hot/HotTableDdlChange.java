package com.bi.queryer.ssm.meta.accelerate.hot;

import com.bi.queryer.sys.enums.Enabled;
import lombok.Data;

/**
 * @Author: contributor
 * @CreateTime: 2024-09-09  18:54
 * @Description: 热表
 */
@Data
public class HotTableDdlChange {

    /**
     * 来源表名称
     */
    private String sourceTableName;

    /**
     * ddl变更次数
     */
    private Integer ddlChangeCnt = Enabled.NO.getId();

    /**
     * ddl最近一次变更时间
     */
    private String lastChangeTime = "";

    private Integer isActive = Enabled.YES.getId();

    private String createdBy;

    private String updatedBy;

}
