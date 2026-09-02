CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    description VARCHAR(2000) DEFAULT NULL COMMENT '项目描述',
    owner_id BIGINT NOT NULL COMMENT '项目所有者ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNING' COMMENT '项目状态',
    progress INT NOT NULL DEFAULT 0 COMMENT '项目进度：0-100',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

    KEY idx_project_owner_id (owner_id),
    KEY idx_project_status (status),
    CONSTRAINT fk_project_owner
        FOREIGN KEY (owner_id) REFERENCES sys_user (id),
    CONSTRAINT chk_project_progress CHECK (progress BETWEEN 0 AND 100)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='项目表';

CREATE TABLE IF NOT EXISTS project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目成员ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',

    UNIQUE KEY uk_project_user (project_id, user_id),
    KEY idx_project_member_user_id (user_id),
    CONSTRAINT fk_project_member_project
        FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_project_member_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='项目成员表';
