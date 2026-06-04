-- gemini-embedding-2 produces 3072-dimensional vectors
-- hnsw index does not support > 2000 dimensions, so we use sequential scan.
-- For ~300 chunks this is instant and no index is needed.
DROP INDEX IF EXISTS document_chunks_embedding_idx;
ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector(3072);
