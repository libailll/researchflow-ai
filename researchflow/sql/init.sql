DROP TABLE IF EXISTS document_chunk;
DROP TABLE IF EXISTS document;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                          username VARCHAR(50) NOT NULL COMMENT '用户名',
                          password VARCHAR(255) NOT NULL COMMENT '密码',
                          nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                          email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                          avatar VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                          deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

                          UNIQUE KEY uk_username (username)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
    COMMENT='系统用户表';

CREATE TABLE project (
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
                         CONSTRAINT chk_project_progress
                             CHECK (progress BETWEEN 0 AND 100)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
    COMMENT='项目表';

CREATE TABLE project_member (
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

CREATE TABLE task (
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
                      CONSTRAINT chk_task_progress
                          CHECK (progress BETWEEN 0 AND 100)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
    COMMENT='项目任务表';

CREATE TABLE document (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
                         project_id BIGINT NOT NULL COMMENT '项目ID',
                         uploader_id BIGINT NOT NULL COMMENT '上传用户ID',
                         file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
                         original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
                         file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
                         file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
                         storage_path VARCHAR(1000) NOT NULL COMMENT '相对存储路径',
                         parse_status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '解析状态',
                         vector_status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '向量化状态',
                         parse_error TEXT DEFAULT NULL COMMENT '解析失败原因',
                         parsed_at DATETIME DEFAULT NULL COMMENT '解析完成时间',
                         vector_error TEXT DEFAULT NULL COMMENT '向量化失败原因',
                         vectorized_at DATETIME DEFAULT NULL COMMENT '向量化完成时间',
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

                         KEY idx_document_project_id (project_id),
                         KEY idx_document_uploader_id (uploader_id),
                         KEY idx_document_parse_status (parse_status),
                         KEY idx_document_vector_status (vector_status),
                         CONSTRAINT fk_document_project
                             FOREIGN KEY (project_id) REFERENCES project (id),
                         CONSTRAINT fk_document_uploader
                             FOREIGN KEY (uploader_id) REFERENCES sys_user (id),
                         CONSTRAINT chk_document_file_size CHECK (file_size > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='项目文档表';

CREATE TABLE document_chunk (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文本片段ID',
                                document_id BIGINT NOT NULL COMMENT '文档ID',
                                project_id BIGINT NOT NULL COMMENT '项目ID',
                                page_number INT DEFAULT NULL COMMENT '原文页码，从1开始',
                                chunk_index INT NOT NULL COMMENT '文档内片段序号，从0开始',
                                content LONGTEXT NOT NULL COMMENT '文本内容',
                                char_count INT NOT NULL COMMENT '字符数量',
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                UNIQUE KEY uk_document_chunk_index (document_id, chunk_index),
                                KEY idx_document_chunk_project_id (project_id),
                                CONSTRAINT fk_document_chunk_document
                                    FOREIGN KEY (document_id) REFERENCES document (id),
                                CONSTRAINT fk_document_chunk_project
                                    FOREIGN KEY (project_id) REFERENCES project (id),
                                CONSTRAINT chk_document_chunk_index CHECK (chunk_index >= 0),
                                CONSTRAINT chk_document_chunk_char_count CHECK (char_count > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='文档解析文本片段表';

CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI会话ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '会话所属用户ID',
    title VARCHAR(120) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    KEY idx_ai_conversation_owner (project_id, user_id, deleted, updated_at),
    CONSTRAINT fk_ai_conversation_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：USER/ASSISTANT',
    content MEDIUMTEXT NOT NULL COMMENT '消息正文',
    reasoning MEDIUMTEXT DEFAULT NULL COMMENT '模型思考过程',
    sources JSON DEFAULT NULL COMMENT 'RAG引用来源快照',
    model VARCHAR(100) DEFAULT NULL COMMENT '回答模型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_ai_message_conversation (conversation_id, created_at, id),
    CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_message_role CHECK (role IN ('USER', 'ASSISTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';
