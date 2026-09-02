SET @add_vector_error = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'vector_error') = 0,
    'ALTER TABLE document ADD COLUMN vector_error TEXT DEFAULT NULL COMMENT ''向量化失败原因'' AFTER parsed_at',
    'SELECT 1'
);
PREPARE add_vector_error_statement FROM @add_vector_error;
EXECUTE add_vector_error_statement;
DEALLOCATE PREPARE add_vector_error_statement;

SET @add_vectorized_at = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'document' AND column_name = 'vectorized_at') = 0,
    'ALTER TABLE document ADD COLUMN vectorized_at DATETIME DEFAULT NULL COMMENT ''向量化完成时间'' AFTER vector_error',
    'SELECT 1'
);
PREPARE add_vectorized_at_statement FROM @add_vectorized_at;
EXECUTE add_vectorized_at_statement;
DEALLOCATE PREPARE add_vectorized_at_statement;
