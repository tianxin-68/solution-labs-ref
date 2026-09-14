-- 文件夹/看板 权限继承配置表
-- 仅新增此一张表；人员/组织复用 ssm_portal_role_user / ssm_portal_role_dept
-- 父级关系不落库：继承按 ssm_portal_menu 的目录树结构推导
CREATE TABLE IF NOT EXISTS `ssm_portal_resource_auth_inherit` (
  `id`              bigint(20)   NOT NULL AUTO_INCREMENT,
  `portal_id`       varchar(64)      NULL COMMENT '所属门户ID，用于按门户批量查询',
  `res_id`          varchar(64)  NOT NULL COMMENT '资源ID（文件夹ID / 看板ID）',
  `res_type`        varchar(32)  NOT NULL COMMENT '资源类型 portal_menu / analysis_template',
  `inherit_enabled` tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否开启继承 1=开启 0=关闭',
  `created_by`      varchar(64)      NULL,
  `created_time`    datetime         NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`      varchar(64)      NULL,
  `updated_time`    datetime         NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_res` (`res_id`, `res_type`),
  KEY `idx_portal_id` (`portal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件夹/看板权限继承配置';

-- ============ 已建表的环境执行以下增量脚本 ============

-- 1. 加 portal_id（未执行过时）
ALTER TABLE `ssm_portal_resource_auth_inherit`
  ADD COLUMN `portal_id` varchar(64) NULL COMMENT '所属门户ID，用于按门户批量查询' AFTER `id`,
  ADD KEY `idx_portal_id` (`portal_id`);

-- 2. portal_id 存量回填：文件夹（res_id = menu_id）
UPDATE ssm_portal_resource_auth_inherit i
INNER JOIN ssm_portal_menu m ON m.menu_id = i.res_id
SET i.portal_id = m.portal_id
WHERE i.res_type = 'portal_menu' AND i.portal_id IS NULL;

-- 3. portal_id 存量回填：看板（res_id = content_ref_id）
UPDATE ssm_portal_resource_auth_inherit i
INNER JOIN ssm_portal_menu m ON m.content_ref_id = i.res_id AND m.is_active = 1
SET i.portal_id = m.portal_id
WHERE i.res_type = 'analysis_template' AND i.portal_id IS NULL;

-- 4. 删除冗余的父级字段（父级关系由 ssm_portal_menu 目录树推导）
ALTER TABLE `ssm_portal_resource_auth_inherit`
  DROP COLUMN `parent_res_id`,
  DROP COLUMN `parent_res_type`;
