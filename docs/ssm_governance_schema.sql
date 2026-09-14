-- =============================================================================
-- 多维内容治理 —— MySQL 业务表
-- 模块：查询模板视图生命周期管理 + 指标/维度治理
-- 数据来源：上游 Hive 结果表 ssm_governance_view_label / ssm_governance_field_label 导入 MySQL，
--          全量覆盖、仅保留最新单日快照（无 dt 分区），扫描 Job 读取全表进行打标。
-- 说明：业务操作表（任务/状态机、审计日志、豁免模块/目录）由 SSM 服务维护。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 上游同步结果表（只读，由数仓每日全量同步，扫描 Job 数据源）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ssm_governance_view_label (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '自增主键（仅用于扫描分页游标）',
    view_id           VARCHAR(64)   NOT NULL COMMENT '视图ID',
    cfg_id            VARCHAR(64)            COMMENT '视图配置ID',
    view_name         VARCHAR(255)           COMMENT '视图名称',
    view_type         VARCHAR(32)            COMMENT '视图类型：personal/shared/snapshot 等',
    tpl_type          VARCHAR(64)            COMMENT '模板类型（含 snapshot 即为快照模板）',
    tpl_id            VARCHAR(64)            COMMENT '模板ID',
    ctg_id            VARCHAR(64)            COMMENT '模板目录ID',
    tpl_name          VARCHAR(255)           COMMENT '模板名称',
    tpl_owner         VARCHAR(512)           COMMENT '模板责任人（逗号分隔）',
    view_owner        VARCHAR(255)           COMMENT '视图责任人：公共视图=tpl_owner，个人视图=创建人',
    tpl_created_time  VARCHAR(32)            COMMENT '模板创建时间',
    is_public_domain  INT  DEFAULT 0         COMMENT '是否公域模板：1=是 0=否',
    is_vip_owner      INT  DEFAULT 0         COMMENT '是否VIP用户模板：1=是 0=否',
    is_portal         INT  DEFAULT 0         COMMENT '是否挂载门户看板：1=是 0=否',
    is_owner_all_left INT  DEFAULT 0         COMMENT '责任人是否全部离职：1=无责任人 0=至少一人在职',
    days_no_visit     INT  DEFAULT 365       COMMENT '距今未访问天数（无记录视为365，上限365）',
    last_visit_time   VARCHAR(32)            COMMENT '最近一次访问时间',
    PRIMARY KEY (id),
    KEY idx_view_id (view_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维内容治理-视图打标结果表（同步自数仓，全量覆盖单日快照）';

CREATE TABLE IF NOT EXISTS ssm_governance_field_label (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '自增主键（仅用于扫描分页游标）',
    ctg_path         VARCHAR(512)           COMMENT '目录（前台目录路径）',
    white_paper_code VARCHAR(128)  NOT NULL COMMENT '白皮书编码（最小治理单元）',
    wp_name          VARCHAR(255)           COMMENT '白皮书名称',
    wp_owner         VARCHAR(255)           COMMENT '白皮书责任人',
    wp_type          VARCHAR(255)           COMMENT '白皮书类型metric/dim',
    wp_level         VARCHAR(32)            COMMENT '白皮书等级',
    last_visit_time  VARCHAR(32)            COMMENT '最近一次被查询引用日期',
    days_no_visit    INT  DEFAULT 365       COMMENT '距今未引用天数（近365天无引用视为365，上限365）',
    PRIMARY KEY (id),
    KEY idx_field_code (white_paper_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维内容治理-指标/维度打标结果表（同步自数仓，全量覆盖单日快照）';


-- -----------------------------------------------------------------------------
-- 1. 视图治理任务（状态机：pending/noticed/offline/purged/exempted/rolled_back）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ssm_governance_view_task (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    view_id             VARCHAR(64)  NOT NULL COMMENT '视图ID',
    cfg_id              VARCHAR(64)           COMMENT '视图配置ID',
    view_name           VARCHAR(255)          COMMENT '视图名称',
    view_type           VARCHAR(32)           COMMENT '视图类型：personal/shared/snapshot',
    tpl_type            VARCHAR(64)           COMMENT '模板类型（含 snapshot 即为快照模板）',
    tpl_id              VARCHAR(64)           COMMENT '模板ID',
    ctg_id              VARCHAR(64)           COMMENT '模板目录ID',
    tpl_name            VARCHAR(255)          COMMENT '模板名称',
    view_path           VARCHAR(512)          COMMENT '目录路径（模块/目录）',
    view_owner          VARCHAR(255)          COMMENT '视图责任人',
    is_public_domain    TINYINT  DEFAULT 0    COMMENT '是否公域模板：1=是 0=否',
    is_portal           TINYINT  DEFAULT 0    COMMENT '是否挂载门户看板：1=是 0=否',
    is_owner_all_left   TINYINT  DEFAULT 0    COMMENT '责任人是否全部离职：1=是 0=否',
    days_no_visit       INT      DEFAULT 365  COMMENT '距今未访问天数',
    last_visit_time     VARCHAR(32)           COMMENT '最近一次访问时间',
    policy_type         VARCHAR(32)           COMMENT '命中策略：no_visit/owner_left_no_visit',
    policy_name         VARCHAR(128)          COMMENT '命中策略名称',
    hit_reason          VARCHAR(512)          COMMENT '命中原因',
    status              VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态',
    plan_offline_date   DATE                  COMMENT '计划下线日期（noticed 时=通知日+宽限期）',
    notice_time         DATETIME              COMMENT '通知时间',
    offline_time        DATETIME              COMMENT '下线时间（软删除）',
    protect_expire_date DATE                  COMMENT '保护期到期日（offline+保护期）',
    exempt_time         DATETIME              COMMENT '豁免时间',
    exempt_expire_date  DATE                  COMMENT '豁免到期日（重入 pending）',
    rollback_time       DATETIME              COMMENT '回滚时间',
    batch_no            VARCHAR(16)           COMMENT '扫描批次（数据日期 dt）',
    is_active           TINYINT  DEFAULT 1    COMMENT '是否有效：1=在线 0=已下线/软删除',
    created_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time        DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_view_id (view_id),
    KEY idx_status (status),
    KEY idx_owner (view_owner),
    KEY idx_plan_offline (status, plan_offline_date),
    KEY idx_protect (status, protect_expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维内容治理-视图治理任务';


-- -----------------------------------------------------------------------------
-- 2. 指标/维度治理任务（状态机：pending/noticed/exempted，SSM 不执行下线）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ssm_governance_field_task (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    white_paper_code   VARCHAR(128) NOT NULL COMMENT '白皮书编码（最小治理单元）',
    object_type        VARCHAR(20)  DEFAULT 'field' COMMENT '对象类型：field/dim',
    ctg_path           VARCHAR(512)          COMMENT '目录（前台目录路径）',
    wp_name            VARCHAR(255)          COMMENT '白皮书名称',
    wp_owner           VARCHAR(255)          COMMENT '白皮书责任人',
    wp_level           VARCHAR(32)           COMMENT '白皮书等级',
    days_no_visit      INT      DEFAULT 365  COMMENT '距今未引用天数',
    last_visit_time    VARCHAR(32)           COMMENT '最近一次被引用日期',
    policy_type        VARCHAR(32)           COMMENT '命中策略：field_no_visit',
    policy_name        VARCHAR(128)          COMMENT '命中策略名称',
    hit_reason         VARCHAR(512)          COMMENT '命中原因',
    status             VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态',
    notice_time        DATETIME              COMMENT '首次通知时间',
    last_notice_time   DATETIME              COMMENT '最近一次通知时间（用于重复推送）',
    notice_count       INT      DEFAULT 0    COMMENT '累计通知次数',
    exempt_time        DATETIME              COMMENT '豁免时间',
    exempt_expire_date DATE                  COMMENT '豁免到期日（重入 pending）',
    batch_no           VARCHAR(16)           COMMENT '扫描批次（数据日期 dt）',
    created_time       DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time       DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wp_code (white_paper_code),
    KEY idx_status (status),
    KEY idx_owner (wp_owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维内容治理-指标/维度治理任务';


-- -----------------------------------------------------------------------------
-- 3. 治理审计日志（视图 + 指标共用）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ssm_governance_audit_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    object_type  VARCHAR(20)          COMMENT '对象类型：view/field/dim',
    object_id    VARCHAR(128)         COMMENT '对象ID（view_id 或 white_paper_code）',
    object_name  VARCHAR(255)         COMMENT '对象名称',
    action       VARCHAR(64)          COMMENT '操作：发送通知/自动下线/立即下线/申请延迟30天/回滚恢复/物理删除/重新评估',
    from_status  VARCHAR(20)          COMMENT '前状态',
    to_status    VARCHAR(20)          COMMENT '后状态',
    operator     VARCHAR(64)          COMMENT '操作人：system 或 用户名',
    remark       VARCHAR(512)         COMMENT '备注',
    created_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_object (object_type, object_id),
    KEY idx_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维内容治理-审计日志';


-- -----------------------------------------------------------------------------
-- 4. 豁免模块/目录（硬排除，扫描入口拦截）
--    ctg_id 为空 = 整个模块豁免；非空 = 仅豁免该目录
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ssm_governance_exempt (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    object_scope VARCHAR(20)  DEFAULT 'field' COMMENT '豁免范围：view/field',
    module_name  VARCHAR(128)         COMMENT '模块名称',
    ctg_id       VARCHAR(64)          COMMENT '目录ID（空=整个模块）',
    ctg_name     VARCHAR(128)         COMMENT '目录名称',
    reason       VARCHAR(512)         COMMENT '豁免原因',
    is_active    TINYINT  DEFAULT 1   COMMENT '是否生效：1=生效 0=停用',
    created_by   VARCHAR(64)          COMMENT '创建人',
    created_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_module (module_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维内容治理-豁免模块/目录';
