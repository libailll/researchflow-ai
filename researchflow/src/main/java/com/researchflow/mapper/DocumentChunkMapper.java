package com.researchflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.researchflow.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
    @Select("""
            SELECT id, document_id, project_id, page_number, chunk_index, content, char_count, created_at
            FROM document_chunk
            WHERE document_id = #{documentId}
            ORDER BY chunk_index
            LIMIT #{offset}, #{size}
            """)
    List<DocumentChunk> selectPageByDocumentId(@Param("documentId") Long documentId,
                                                @Param("offset") long offset,
                                                @Param("size") long size);

    @Select("SELECT COUNT(*) FROM document_chunk WHERE document_id = #{documentId}")
    long countByDocumentId(@Param("documentId") Long documentId);
}
