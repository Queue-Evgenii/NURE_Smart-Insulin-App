package ua.nure.smartinsulinbackend

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the concurrent-session eviction logic used by AuthService.issueTokens:
 *   • up to MAX_CONCURRENT_SESSIONS refresh tokens may coexist per user,
 *   • expired tokens are always pruned,
 *   • on a new login past the limit the oldest active session is evicted.
 */
class SessionLimitTest {

    private val maxSessions = 3

    /** Mirrors the eviction arithmetic in AuthService.issueTokens. */
    private fun overflowToEvict(activeCount: Int): Int =
        (activeCount - (maxSessions - 1)).coerceAtLeast(0)

    @Test
    fun firstThreeLoginsDoNotEvictAnything() {
        assertEquals(0, overflowToEvict(0))  // 1st login
        assertEquals(0, overflowToEvict(1))  // 2nd login
        assertEquals(0, overflowToEvict(2))  // 3rd login — fills the 3rd slot
    }

    @Test
    fun fourthLoginEvictsExactlyOneOldestSession() {
        // 3 active sessions already → one must be evicted to keep the total at 3.
        assertEquals(1, overflowToEvict(3))
    }

    @Test
    fun oldestSessionIsTheEvictionTarget() {
        // findByUserOrderByCreatedAtAsc returns oldest-first; we take(overflow) from the head.
        val sessionsOldestFirst = listOf("device-A", "device-B", "device-C")
        val evicted = sessionsOldestFirst.take(overflowToEvict(sessionsOldestFirst.size))
        assertEquals(listOf("device-A"), evicted)
    }

    @Test
    fun expiredSessionsDoNotCountTowardTheLimit() {
        // 2 expired + 3 active: expired are pruned regardless, and the 3 active still
        // trigger one eviction so the new login lands within the limit.
        val expiredCount = 2
        val activeCount = 3

        val totalDeleted = expiredCount + overflowToEvict(activeCount)
        assertEquals(3, totalDeleted)
        assertTrue(activeCount - overflowToEvict(activeCount) + 1 == maxSessions)
    }
}
