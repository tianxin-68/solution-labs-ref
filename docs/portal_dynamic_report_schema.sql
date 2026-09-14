-- 专题分析报告（静态 HTML/ZIP）：挂载在门户菜单树下，支持多版本（同一 report_id 下 version 自增）
CREATE TABLE IF NOT EXISTS ssm_portal_dynamic_report (
    report_id       VARCHAR(64)  NOT NULL COMMENT '业务主键，与菜单 content_ref_id 一致',
    version         INT          NOT NULL DEFAULT 1 COMMENT '版本号，同一 report_id 从 1 起自增',
    menu_id         VARCHAR(64)  NOT NULL COMMENT '门户菜单 id',
    portal_id       VARCHAR(64)  NOT NULL COMMENT '门户 id',
    parent_menu_id  VARCHAR(64)  NOT NULL COMMENT '上传时选择的父目录菜单 id',
    report_name     VARCHAR(256) NOT NULL COMMENT '报告名称（展示）',
    oss_url         VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '扩展：预留 OSS 等字段',
    resource_url    VARCHAR(2048) NOT NULL COMMENT '部署/文件服务访问 URL',
    file_ext        VARCHAR(16)  NOT NULL COMMENT 'html 或 zip',
    report_type     VARCHAR(32)  NOT NULL DEFAULT 'html' COMMENT '报告类型，上传时传入（html / streamlit 等）',
    index_path      VARCHAR(512) DEFAULT NULL COMMENT '首页 HTML 相对路径',
    published_by    VARCHAR(128) NOT NULL COMMENT '发布人',
    published_time  DATETIME     NOT NULL COMMENT '发布时间',
    is_active       TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效 0删除',
    created_by      VARCHAR(128) DEFAULT NULL,
    created_time    DATETIME     DEFAULT NULL,
    updated_by      VARCHAR(128) DEFAULT NULL,
    updated_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (report_id, version),
    KEY idx_portal_active (portal_id, is_active),
    KEY idx_report_active_version (report_id, is_active, version)
) COMMENT='门户专题分析报告（动态站点）元数据-多版本';

-- 存量单版本表迁移（已存在 report_id 主键时执行）：
-- ALTER TABLE ssm_portal_dynamic_report ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '版本号' AFTER report_id;
-- ALTER TABLE ssm_portal_dynamic_report DROP PRIMARY KEY, ADD PRIMARY KEY (report_id, version);

-- 已建表环境新增 report_type：
ALTER TABLE ssm_portal_dynamic_report
  ADD COLUMN report_type VARCHAR(32) NOT NULL DEFAULT 'html' COMMENT '报告类型，上传时传入（html / streamlit 等）' AFTER file_ext;
