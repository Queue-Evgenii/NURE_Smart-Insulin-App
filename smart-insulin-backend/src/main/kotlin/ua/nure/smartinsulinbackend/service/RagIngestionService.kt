package ua.nure.smartinsulinbackend.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.nure.smartinsulinbackend.entity.DocumentChunk
import ua.nure.smartinsulinbackend.repository.DocumentChunkRepository

/**
 * Ingests PDF medical protocols from classpath:/rag/ into the pgvector store.
 * Runs automatically at application startup; skips files that are already ingested.
 *
 * Chunking strategy (per diploma section 2.3):
 *   - Chunk size : ~800 characters (~500 tokens)
 *   - Overlap    : ~150 characters (~100 tokens)
 * This preserves clinical context across chunk boundaries.
 */
@Service
class RagIngestionService(
    private val documentChunkRepository: DocumentChunkRepository,
    private val embeddingService: EmbeddingService,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(RagIngestionService::class.java)

    private val chunkSize    = 800
    private val chunkOverlap = 150
    private val batchSize    = 1    // one chunk at a time — EmbeddingService adds 5s delay between calls

    override fun run(args: ApplicationArguments) {
        val resolver = PathMatchingResourcePatternResolver()
        val resources = try {
            resolver.getResources("classpath:rag/*.pdf")
        } catch (e: Exception) {
            log.info("No PDF files found in classpath:rag/ — skipping RAG ingestion")
            return
        }

        if (resources.isEmpty()) {
            log.info("No PDF files found in classpath:rag/ — skipping RAG ingestion")
            return
        }

        for (resource in resources) {
            val filename = resource.filename ?: continue
            val alreadyIngested = documentChunkRepository.countBySource(filename) > 0
            if (alreadyIngested) {
                log.info("Skipping '{}' — already ingested", filename)
                continue
            }

            log.info("Ingesting '{}'…", filename)
            try {
                val text = extractText(resource.inputStream)
                val chunks = splitIntoChunks(text)
                log.info("  {} → {} chunks", filename, chunks.size)
                ingestChunks(filename, chunks)
                log.info("  '{}' ingested successfully", filename)
            } catch (e: Exception) {
                log.warn("  Failed to ingest '{}': {} — application will start without RAG for this file", filename, e.message)
            }
        }
    }

    @Transactional
    fun ingestChunks(source: String, chunks: List<String>) {
        chunks.chunked(batchSize).forEachIndexed { batchIdx, batch ->
            val embeddings = embeddingService.embedBatch(batch)
            val entities = batch.mapIndexed { i, text ->
                DocumentChunk(
                    source     = source,
                    chunkIndex = batchIdx * batchSize + i,
                    content    = text,
                    embedding  = embeddings[i],
                )
            }
            documentChunkRepository.saveAll(entities)
            log.debug("  Saved chunk {}/{}", batchIdx + 1, chunks.size)
        }
    }

    // ── PDF text extraction ───────────────────────────────────────────────────

    private fun extractText(input: java.io.InputStream): String =
        Loader.loadPDF(input.readBytes()).use { doc ->
            PDFTextStripper().getText(doc)
        }

    // ── Chunking ──────────────────────────────────────────────────────────────

    fun splitIntoChunks(text: String): List<String> {
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < normalized.length) {
            val end = minOf(start + chunkSize, normalized.length)
            chunks += normalized.substring(start, end).trim()
            start += chunkSize - chunkOverlap
        }
        return chunks.filter { it.length > 50 }  // drop tiny trailing fragments
    }
}
