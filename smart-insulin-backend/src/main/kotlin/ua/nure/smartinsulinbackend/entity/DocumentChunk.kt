package ua.nure.smartinsulinbackend.entity

import jakarta.persistence.*
import org.hibernate.annotations.ColumnTransformer

@Entity
@Table(name = "document_chunks")
class DocumentChunk(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** PDF filename this chunk was extracted from */
    @Column(nullable = false)
    val source: String,

    /** Sequential index of this chunk within the source document */
    @Column(name = "chunk_index", nullable = false)
    val chunkIndex: Int,

    /** Raw text content (~500 tokens) */
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    /**
     * Vector embedding from Google text-embedding-004 (768 dimensions).
     * Stored in pgvector format: "[0.1, 0.2, ...]"
     */
    @Column(nullable = false, columnDefinition = "vector(3072)")
    @ColumnTransformer(read = "embedding::text", write = "?::vector")
    val embedding: String,
)
