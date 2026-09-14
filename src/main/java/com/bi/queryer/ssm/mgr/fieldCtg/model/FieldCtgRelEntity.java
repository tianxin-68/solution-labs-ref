package com.bi.queryer.ssm.mgr.fieldCtg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: contributor
 * @CreateTime: 2024-07-22  19:26
 * @Description: 字段目录关联实体
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FieldCtgRelEntity {

    /**
     * 字段id
     */
    private String fieldId;

    /**
     * 分类类型:front=前台，back=后台
     */
    private String ctgType;

    /**
     * 字段分类id
     */
    private String ctgId;

    /**
     * 排序id
     */
    private Double sortId;

    /**
     * 是否可用
     */
    private Integer isActive;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 修改人
     */
    private String updatedBy;

}
