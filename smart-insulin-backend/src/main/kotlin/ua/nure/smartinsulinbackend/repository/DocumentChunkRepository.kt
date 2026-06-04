package ua.nure.smartinsulinbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ua.nure.smartinsulinbackend.entity.DocumentChunk

interface DocumentChunkRepository : JpaRepository<DocumentChunk, Long> {

    fun countBySource(source: String): Long

    /**
     * Returns the [limit] most semantically similar chunks to [queryVector]
     * using cosine distance (pgvector <=> operator).
     *
     * [queryVector] must be a valid pgvector text literal, e.g. "[0.1, 0.2, ...]"
     */
    @Query(
        value = """
            SELECT id, source, chunk_index, content, embedding
            FROM document_chunks
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findTopSimilar(
        @Param("queryVector") queryVector: String,
        @Param("limit") limit: Int,
    ): List<DocumentChunk>
}
