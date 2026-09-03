CREATE TABLE IF NOT EXISTS ai_project_risk_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI项目风险报告ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    creator_id BIGINT NOT NULL COMMENT '生成人ID',
    title VARCHAR(180) NOT NULL COMMENT '风险报告标题',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级：LOW/MEDIUM/HIGH/CRITICAL',
    risk_score INT NOT NULL COMMENT '可解释风险评分：0-100',
    content MEDIUMTEXT NOT NULL COMMENT '结构化风险报告正文',
    analysis_snapshot JSON NOT NULL COMMENT '生成时风险指标及评分明细快照',
    sources JSON DEFAULT NULL COMMENT '知识库引用快照',
    model VARCHAR(100) DEFAULT NULL COMMENT '生成模型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    KEY idx_risk_report_project (project_id, deleted, created_at),
    KEY idx_risk_report_creator (creator_id),
    KEY idx_risk_report_level (project_id, risk_level, deleted),
    CONSTRAINT fk_risk_report_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_risk_report_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id),
    CONSTRAINT chk_risk_report_level CHECK (risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_risk_report_score CHECK (risk_score BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI项目风险分析报告表';
