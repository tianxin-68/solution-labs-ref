-- 门户菜单访问日志表
CREATE TABLE IF NOT EXISTS `ssm_portal_menu_access_log` (
  `log_id`      BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_name`   VARCHAR(100) NOT NULL COMMENT '用户域账号',
  `menu_id`     VARCHAR(100) NOT NULL COMMENT '菜单ID',
  `menu_name`   VARCHAR(100)     NULL COMMENT '菜单名称',
  `portal_id`   VARCHAR(100)     NULL COMMENT '门户ID',
  `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_user` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户菜单访问日志表';
