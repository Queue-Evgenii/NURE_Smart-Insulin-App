package ua.nure.smartinsulinbackend.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

/**
 * Calls Google Gemini embedding API (gemini-embedding-2, 3072-dimensional vectors).
 * Respects free-tier rate limit: ~15 requests/min → 5-second delay between calls.
 */
@Service
class EmbeddingService(
    @Value("\${gemini.api.key:}") private val apiKey: String,
    @Value("\${gemini.embedding.model:gemini-embedding-2}") private val embeddingModel: String,
) {
    private val log = LoggerFactory.getLogger(EmbeddingService::class.java)
    private val restClient = RestClient.create()

    /** Delay between consecutive embedding calls (ms). 5s → ≤12 req/min, safe within 15 RPM. */
    private val interCallDelayMs = 5_000L
    private val maxRetries = 3

    /** Embed a single text. Returns a pgvector literal "[v1,v2,...]" */
    fun embed(text: String): String {
        check(apiKey.isNotBlank()) { "gemini.api.key is not configured" }
        return toVectorLiteral(callWithRetry(text))
    }

    /**
     * Embed multiple texts sequentially with inter-call delay.
     * Returns one pgvector literal per input text.
     */
    fun embedBatch(texts: List<String>): List<String> {
        check(apiKey.isNotBlank()) { "gemini.api.key is not configured" }
        return texts.mapIndexed { i, text ->
            if (i > 0) Thread.sleep(interCallDelayMs)
            toVectorLiteral(callWithRetry(text))
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun callWithRetry(text: String): FloatArray {
        repeat(maxRetries) { attempt ->
            try {
                return callEmbedSingle(text)
            } catch (e: HttpClientErrorException.TooManyRequests) {
                val waitMs = interCallDelayMs * (attempt + 2)
                log.warn("429 rate limit on attempt ${attempt + 1}, waiting ${waitMs}ms…")
                Thread.sleep(waitMs)
            }
        }
        // last attempt — let exception propagate
        return callEmbedSingle(text)
    }

    @Suppress("UNCHECKED_CAST")
    private fun callEmbedSingle(text: String): FloatArray {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                "$embeddingModel:embedContent?key=$apiKey"

        val body = mapOf(
            "model"   to "models/$embeddingModel",
            "content" to mapOf("parts" to listOf(mapOf("text" to text))),
        )

        val raw = restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map::class.java) as Map<*, *>

        val values = ((raw["embedding"] as? Map<*, *>)?.get("values") as? List<*>)
            ?: error("Unexpected embedding response: $raw")

        return values.map { (it as Number).toFloat() }.toFloatArray()
    }

    private fun toVectorLiteral(values: FloatArray): String =
        values.joinToString(",", prefix = "[", postfix = "]")
}
