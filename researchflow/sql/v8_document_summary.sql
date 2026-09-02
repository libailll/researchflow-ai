CREATE TABLE IF NOT EXISTS ai_document_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI文档总结ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    creator_id BIGINT NOT NULL COMMENT '生成人ID',
    title VARCHAR(180) NOT NULL COMMENT '总结标题',
    content MEDIUMTEXT NOT NULL COMMENT '结构化总结正文',
    sources JSON DEFAULT NULL COMMENT '引用文档片段快照',
    model VARCHAR(100) DEFAULT NULL COMMENT '生成模型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

    KEY idx_document_summary_document (document_id, deleted, created_at),
    KEY idx_document_summary_project (project_id, created_at),
    KEY idx_document_summary_creator (creator_id, created_at),
    CONSTRAINT fk_document_summary_document FOREIGN KEY (document_id) REFERENCES document (id),
    CONSTRAINT fk_document_summary_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_document_summary_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI单篇文档总结表';
