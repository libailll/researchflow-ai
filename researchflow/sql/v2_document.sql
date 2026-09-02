CREATE TABLE IF NOT EXISTS document (
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
