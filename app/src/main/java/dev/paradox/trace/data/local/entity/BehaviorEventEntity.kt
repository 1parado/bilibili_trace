package dev.paradox.trace.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted raw behavior event; mirrors docs/DATA_MODEL.md `behavior_events`.
 * This is the append-only observation layer from which sessions derive.
 */
@Entity(
    tableName = "behavior_events",
    indices = [
        Index("occurred_at"),
        Index("session_id"),
        Index("content_id"),
        Index(value = ["dedupe_key"], unique = true),
    ],
)
data class BehaviorEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "platform") val platform: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "content_id") val contentId: String?,
    @ColumnInfo(name = "confidence") val confidence: Double?,
    @ColumnInfo(name = "evidence_ref") val evidenceRef: String?,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String?,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
