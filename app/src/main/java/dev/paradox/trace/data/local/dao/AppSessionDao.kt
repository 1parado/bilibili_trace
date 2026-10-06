package dev.paradox.trace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.paradox.trace.data.local.entity.AppSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: AppSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<AppSessionEntity>)

    @Query("SELECT * FROM app_sessions ORDER BY started_at DESC")
    fun observeAll(): Flow<List<AppSessionEntity>>

    /**
     * Sessions overlapping the half-open window `[fromMs, toMs)`:
     * a session qualifies when it started before the window end and ended
     * at or after the window start.
     */
    @Query(
        "SELECT * FROM app_sessions " +
            "WHERE started_at < :toMs AND COALESCE(ended_at, started_at) >= :fromMs " +
            "ORDER BY started_at ASC",
    )
    fun observeOverlapping(fromMs: Long, toMs: Long): Flow<List<AppSessionEntity>>

    @Query("SELECT * FROM app_sessions WHERE id = :id")
    suspend fun getById(id: String): AppSessionEntity?

    @Query("DELETE FROM app_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM app_sessions")
    suspend fun deleteAll()
}
