-- =============================================================================
-- 看板工作台目录迁移（业务线拆分）—— 迁移映射表
-- 关联方案：doc/02设计/看板工作台目录迁移-业务线拆分执行计划.md
-- 用途：记录老对象（门户/目录/看板/查询模板/查询模板视图/共享空间目录）到新业务线对应对象的 id 映射，
--      既是审计追溯依据，也是增量执行的幂等判断依据（同一 old_id 在同一 new_biz_line 下只处理一次）。
-- =============================================================================

CREATE TABLE IF NOT EXISTS ssm_migrate_bizsplit_mapping (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    object_type    VARCHAR(32)  NOT NULL COMMENT '对象类型：portal/portal_menu/analysis_tpl/query_tpl/query_tpl_view/space_ctg',
    old_id         VARCHAR(64)  NOT NULL COMMENT '老对象id',
    old_name       VARCHAR(255)          COMMENT '老对象名称',
    new_id         VARCHAR(64)  NOT NULL COMMENT '新对象id',
    new_name       VARCHAR(255)          COMMENT '新对象名称',
    old_biz_line   VARCHAR(64)           COMMENT '老业务线名称',
    new_biz_line   VARCHAR(64)  NOT NULL COMMENT '新业务线名称',
    portal_id      VARCHAR(64)           COMMENT '所属门户id',
    is_reused      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '该new_id是否为同名复用得到的已存在对象：1=是 0=本次新创建',
    created_by     VARCHAR(64)           COMMENT '创建人',
    created_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_object_old_bizline (object_type, old_id, new_biz_line) COMMENT '幂等判断依据：同一老对象在同一新业务线下只应有一条映射记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板工作台目录迁移-业务线拆分新旧id映射表';
