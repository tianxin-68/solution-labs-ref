-- =============================================================================
-- 字段发布（元数据发布）—— 字段 id 展开映射表
-- 用途：发布模块（SSMApiService#saveCtg）保存 ssm_field_ctg_rel（字段目录关联）时，
--      fieldId 命中本表 old_field_id 的，把这一条关联展开成挂在每个 new_field_id 上的多条
--      （old_field_id -> new_field_id 是一对多关系）；查不到映射的 fieldId 保持原样。
-- =============================================================================

CREATE TABLE `ssm_field_expansion_mapping` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `old_field_id` varchar(64) DEFAULT NULL,
  `new_field_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
