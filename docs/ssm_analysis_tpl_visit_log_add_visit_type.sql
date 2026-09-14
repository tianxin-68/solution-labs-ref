-- ssm_analysis_tpl_visit_log 增加访问类型等字段（区分看板 / 专题分析报告）
ALTER TABLE ssm_analysis_tpl_visit_log
    ADD COLUMN visit_type VARCHAR(64) NOT NULL DEFAULT 'analysis_template'
        COMMENT '访问资源类型：analysis_template=看板，dynamic_analysis_report=专题分析报告' AFTER portal_id,
    ADD COLUMN resource_version INT DEFAULT NULL
        COMMENT '资源版本号（专题分析报告多版本时使用，看板访问为空）' AFTER visit_type;

-- 存量数据保持为看板访问
UPDATE ssm_analysis_tpl_visit_log SET visit_type = 'analysis_template' WHERE visit_type IS NULL OR visit_type = '';
