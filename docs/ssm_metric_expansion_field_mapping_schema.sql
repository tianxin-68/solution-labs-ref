-- =============================================================================
-- 看板/查询模板复制（业务线拆分）—— 指标字段映射表
-- 关联方案：doc/02设计/看板工作台目录迁移-业务线拆分执行计划.md
-- 用途：看板/查询模板复制到新业务线时，配置里用到的字段 code/id/展示名按
--      (tpl_id, view_id, source_businessline, target_businessline) 查出该行的
--      old_field_code→new_field_code / old_field_id→new_field_id / new_field_title 做替换；
--      查不到映射的 code/id（维度、与业务线无关的通用指标）保持不变。
-- 迁移工具本身不查 source_metric_code/target_metric_code 这两列（旧 ssm_metric_expansion_mapping
-- 表遗留下来的字段名），也不查 SSDMetaCacheManager 反查元数据——全部改写只信本表。
-- old_field_title/new_field_title 用于回填 widgetOptions/tplConfig 里字段的展示名
-- （name/title/displayTitle），因为新老指标的名称不一定只是业务线子串不同。
-- =============================================================================

CREATE TABLE `ssm_metric_expansion_field_mapping` (
  `pkid` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shadow_cfg_id` varchar(64) NOT NULL COMMENT '关联影子配置 id',
  `view_id` varchar(64) NOT NULL COMMENT '视图 id',
  `tpl_id` varchar(64) NOT NULL COMMENT '模板 id',
  `cfg_id` varchar(64) NOT NULL COMMENT '执行时正式 cfg id',
  `source_businessline` varchar(64) NOT NULL COMMENT '源业务线',
  `target_businessline` varchar(64) DEFAULT NULL COMMENT '本条新指标对应的目标业务线（计算变体/多 target 时必填）',
  `old_field_id` varchar(64) NOT NULL COMMENT '老指标 field id（tplConfig 中原 id）',
  `old_field_code` varchar(128) DEFAULT NULL COMMENT '老指标 code',
  `old_field_title` varchar(255) DEFAULT NULL COMMENT '老指标展示名',
  `new_field_id` varchar(64) NOT NULL COMMENT '新指标 field id',
  `new_field_code` varchar(128) DEFAULT NULL COMMENT '新指标 code',
  `new_field_title` varchar(255) DEFAULT NULL COMMENT '新指标展示名',
  `source_metric_code` varchar(128) DEFAULT NULL COMMENT '映射表 source_metric_code（普通指标，本方案不使用）',
  `target_metric_code` varchar(128) DEFAULT NULL COMMENT '映射表 target_metric_code（普通指标，本方案不使用）',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`pkid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标膨胀/替换字段 id 映射明细';
