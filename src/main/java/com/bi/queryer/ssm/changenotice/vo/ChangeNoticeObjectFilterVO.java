package com.bi.queryer.ssm.changenotice.vo;

import com.bi.queryer.ssm.changenotice.entity.ChangeNoticeObjectFilter;
import lombok.Data;

/**
 * 变更通知筛选出参（使用约束场景下的必须同查筛选）。
 * 字段取值与表 mgp_change_notice_object_filter 保持一致，不做 range 拆解。
 */
@Data
public class ChangeNoticeObjectFilterVO {

    /** 筛选对象编码 */
    private String objectId;
    /** 筛选对象名称 */
    private String objectName;
    /** 筛选对象类型：METRIC/DIMENSION */
    private String objectType;
    /** 筛选值，逗号分隔 */
    private String filterValues;
    /** 过滤方式：include/exclude */
    private String filterType;
    /** 筛选方式：detail/agg */
    private String filterValueMode;

    /**
     * 实体转 VO
     * @param entity 筛选实体
     * @return VO
     */
    public static ChangeNoticeObjectFilterVO from(ChangeNoticeObjectFilter entity) {
        if (entity == null) {
            return null;
        }
        ChangeNoticeObjectFilterVO vo = new ChangeNoticeObjectFilterVO();
        vo.setObjectId(entity.getObjectId());
        vo.setObjectName(entity.getObjectName());
        vo.setObjectType(entity.getObjectType());
        vo.setFilterValues(entity.getFilterValues());
        vo.setFilterType(entity.getFilterType());
        vo.setFilterValueMode(entity.getFilterValueMode());
        return vo;
    }
}
