package dev.paradox.trace.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted app-session record; mirrors docs/DATA_MODEL.md `app_sessions`.
 * Times are UTC epoch milliseconds; `[started_at, ended_at)` is half-open.
 */
@Entity(
    tableName = "app_sessions",
    indices = [
        Index("started_at"),
        Index("content_id"),
    ],
)
data class AppSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "platform") val platform: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "completeness") val completeness: String,
    @ColumnInfo(name = "content_id") val contentId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
