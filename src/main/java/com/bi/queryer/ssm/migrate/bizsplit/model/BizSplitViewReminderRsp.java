package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务线拆分视图失效提醒查询响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BizSplitViewReminderRsp {

    /** 查询的（老）视图 id */
    private String viewId;

    /** 该视图是否命中业务线拆分迁移记录 */
    private Boolean affected = false;

    /** 该视图迁移前所属的老业务线 */
    private String oldBizLine;

    /** 拼接好的完整提醒文案；affected=false 时为 null */
    private String message;

    /** 结构化的拆分分支，已按当前用户部门收窄（未命中任何分支时回退为全部分支） */
    @Builder.Default
    private List<SplitGroup> splits = new ArrayList<>();

    /**
     * 单个拆分分支：老业务线拆分为某个新业务线后的新视图及指标明细。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitGroup {

        /** 拆分后的新业务线 */
        private String newBizLine;

        /** 拆分后的新视图 id */
        private String newViewId;


    }
}
