-- 将 ssm_portal_dynamic_report 从单版本改为多版本（report_id + version 联合主键）
-- 执行前请备份数据；存量数据 version 均为 1

ALTER TABLE ssm_portal_dynamic_report
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '版本号，同一 report_id 从 1 起自增' AFTER report_id;

ALTER TABLE ssm_portal_dynamic_report
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (report_id, version);

ALTER TABLE ssm_portal_dynamic_report
    ADD KEY idx_report_active_version (report_id, is_active, version);
