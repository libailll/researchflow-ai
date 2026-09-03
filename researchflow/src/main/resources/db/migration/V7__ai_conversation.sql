CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI会话ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '会话所属用户ID',
    title VARCHAR(120) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

    KEY idx_ai_conversation_owner (project_id, user_id, deleted, updated_at),
    CONSTRAINT fk_ai_conversation_project
        FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_ai_conversation_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI对话会话表';

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：USER/ASSISTANT',
    content MEDIUMTEXT NOT NULL COMMENT '消息正文',
    reasoning MEDIUMTEXT DEFAULT NULL COMMENT '模型思考过程',
    sources JSON DEFAULT NULL COMMENT 'RAG引用来源快照',
    model VARCHAR(100) DEFAULT NULL COMMENT '回答模型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_ai_message_conversation (conversation_id, created_at, id),
    CONSTRAINT fk_ai_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_ai_message_role CHECK (role IN ('USER', 'ASSISTANT'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI对话消息表';
