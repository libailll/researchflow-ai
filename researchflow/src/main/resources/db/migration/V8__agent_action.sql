CREATE TABLE IF NOT EXISTS agent_action_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Agent操作审计ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '确认操作的用户ID',
    conversation_id BIGINT DEFAULT NULL COMMENT '来源AI会话ID',
    action_type VARCHAR(40) NOT NULL COMMENT '操作类型：CREATE_TASK/UPDATE_TASK',
    target_type VARCHAR(40) NOT NULL DEFAULT 'TASK' COMMENT '目标资源类型',
    target_id BIGINT DEFAULT NULL COMMENT '执行后目标资源ID',
    request_payload JSON NOT NULL COMMENT '用户确认时的操作参数快照',
    result_payload JSON DEFAULT NULL COMMENT '执行结果快照',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：SUCCESS/FAILED',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审计记录时间',
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '实际执行时间',

    KEY idx_agent_action_project (project_id, created_at),
    KEY idx_agent_action_user (user_id, created_at),
    KEY idx_agent_action_conversation (conversation_id),
    CONSTRAINT fk_agent_action_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_agent_action_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_agent_action_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id),
    CONSTRAINT chk_agent_action_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_agent_action_type CHECK (action_type IN ('CREATE_TASK', 'UPDATE_TASK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent写操作审计表';
