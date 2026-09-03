CREATE TABLE IF NOT EXISTS ai_weekly_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI周报ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    creator_id BIGINT NOT NULL COMMENT '生成人ID',
    title VARCHAR(160) NOT NULL COMMENT '周报标题',
    period_start DATE NOT NULL COMMENT '统计开始日期',
    period_end DATE NOT NULL COMMENT '统计结束日期',
    content MEDIUMTEXT NOT NULL COMMENT '周报正文',
    sources JSON DEFAULT NULL COMMENT '知识库引用快照',
    model VARCHAR(100) DEFAULT NULL COMMENT '生成模型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

    KEY idx_weekly_report_project (project_id, period_end, deleted),
    KEY idx_weekly_report_creator (creator_id, created_at),
    CONSTRAINT fk_weekly_report_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_weekly_report_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id),
    CONSTRAINT chk_weekly_report_period CHECK (period_end >= period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI项目周报表';
