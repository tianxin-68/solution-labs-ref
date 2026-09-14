package com.bi.queryer.ssm.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ssm_field_expansion_mapping 单行：老字段 id -> 新字段 id，old_field_id 与 new_field_id 是一对多关系
 * （同一个 old_field_id 可能对应多行、多个 new_field_id）。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FieldExpansionMappingEntity {

    private String oldFieldId;

    private String newFieldId;
}
