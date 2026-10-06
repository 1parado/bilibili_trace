package dev.paradox.trace.domain.repository

import dev.paradox.trace.domain.model.ContentSession
import kotlinx.coroutines.flow.Flow

/** Command for logging a session the user observed themselves. */
data class ManualSessionCommand(
    val platform: String,
    val title: String?,
    val platformContentId: String?,
    val creatorName: String?,
    val startedAtMs: Long,
    val endedAtMs: Long,
    /** Provenance of content metadata: null = user typed, "PLATFORM_API" = fetched. */
    val metadataSource: String? = null,
)

/** Read/write gateway for content sessions. Sources never certify truth. */
interface SessionRepository {

    fun observeAllSessions(): Flow<List<ContentSession>>

    /** Sessions overlapping the half-open window `[fromMs, toMs)`, ascending. */
    fun observeSessionsBetween(fromMs: Long, toMs: Long): Flow<List<ContentSession>>

    /**
     * Persists a user-logged session. Returns the stored session, or a
     * failure when the command violates time semantics.
     */
    suspend fun addManualSession(command: ManualSessionCommand): Result<ContentSession>

    suspend fun deleteSession(id: String)

    /** Deletes sessions, content metadata, events, and derived caches. */
    suspend fun deleteAllData()
}
