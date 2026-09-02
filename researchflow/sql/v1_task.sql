CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description VARCHAR(5000) DEFAULT NULL COMMENT '任务描述',
    assignee_id BIGINT DEFAULT NULL COMMENT '负责人ID',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT '任务状态',
    progress INT NOT NULL DEFAULT 0 COMMENT '任务进度：0-100',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    due_date DATE DEFAULT NULL COMMENT '截止日期',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

    KEY idx_task_project_id (project_id),
    KEY idx_task_assignee_id (assignee_id),
    KEY idx_task_status (status),
    KEY idx_task_due_date (due_date),
    CONSTRAINT fk_task_project
        FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_task_assignee
        FOREIGN KEY (assignee_id) REFERENCES sys_user (id),
    CONSTRAINT fk_task_creator
        FOREIGN KEY (creator_id) REFERENCES sys_user (id),
    CONSTRAINT chk_task_progress CHECK (progress BETWEEN 0 AND 100)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='项目任务表';
