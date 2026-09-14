package com.bi.queryer.ssm.migrate.bizsplit.model;

import lombok.Data;

/**
 * 业务线拆分迁移映射表实体，对应表 ssm_migrate_bizsplit_mapping。
 *
 * object_type 取值：portal/portal_menu/analysis_tpl/query_tpl/query_tpl_view/space_ctg。
 */
@Data
public class SsmMigrateBizsplitMappingEntity {

    /** 主键 */
    private Long id;
    /** 对象类型 */
    private String objectType;
    /** 老对象 id */
    private String oldId;
    /** 老对象名称 */
    private String oldName;
    /** 新对象 id */
    private String newId;
    /** 新对象名称 */
    private String newName;
    /** 老业务线名称 */
    private String oldBizLine;
    /** 新业务线名称 */
    private String newBizLine;
    /** 所属门户 id */
    private String portalId;
    /** 该 new_id 是否为同名复用得到的已存在对象：1=是 0=本次新创建 */
    private Boolean isReused;
    /** 创建人 */
    private String createdBy;
    /** 创建时间 */
    private String createdTime;
}
