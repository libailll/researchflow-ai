SET @add_parse_error = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'parse_error') = 0,
    'ALTER TABLE document ADD COLUMN parse_error TEXT DEFAULT NULL COMMENT ''解析失败原因'' AFTER vector_status',
    'SELECT 1'
);
PREPARE add_parse_error_statement FROM @add_parse_error;
EXECUTE add_parse_error_statement;
DEALLOCATE PREPARE add_parse_error_statement;

SET @add_parsed_at = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'parsed_at') = 0,
    'ALTER TABLE document ADD COLUMN parsed_at DATETIME DEFAULT NULL COMMENT ''解析完成时间'' AFTER parse_error',
    'SELECT 1'
);
PREPARE add_parsed_at_statement FROM @add_parsed_at;
EXECUTE add_parsed_at_statement;
DEALLOCATE PREPARE add_parsed_at_statement;

CREATE TABLE IF NOT EXISTS document_chunk (
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
