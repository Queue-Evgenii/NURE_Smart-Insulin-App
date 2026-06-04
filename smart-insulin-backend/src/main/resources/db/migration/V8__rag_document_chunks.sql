-- Enable pgvector extension (requires pgvector/pgvector:pg15 image)
CREATE EXTENSION IF NOT EXISTS vector;

-- Stores text chunks from medical protocol PDFs with their vector embeddings
-- Embedding model: Google text-embedding-004 (768 dimensions)
CREATE TABLE IF NOT EXISTS document_chunks (
    id          BIGSERIAL PRIMARY KEY,
    source      VARCHAR(255) NOT NULL,   -- PDF filename
    chunk_index INT          NOT NULL,   -- sequential index within the source document
    content     TEXT         NOT NULL,   -- raw text of the chunk (~500 tokens)
    embedding   vector(768)  NOT NULL    -- dense vector from text-embedding-004
);

-- Index for fast approximate nearest-neighbour search (cosine distance)
CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);
