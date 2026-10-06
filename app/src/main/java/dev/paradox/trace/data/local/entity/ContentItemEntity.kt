package dev.paradox.trace.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted content metadata; mirrors docs/DATA_MODEL.md `content_items`.
 * Titles stay on-device and are display indexes only, never logged.
 */
@Entity(tableName = "content_items")
data class ContentItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "platform") val platform: String,
    @ColumnInfo(name = "platform_content_id") val platformContentId: String?,
    @ColumnInfo(name = "canonical_url") val canonicalUrl: String?,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "creator_id") val creatorId: String?,
    @ColumnInfo(name = "creator_name") val creatorName: String?,
    @ColumnInfo(name = "user_topic") val userTopic: String?,
    @ColumnInfo(name = "metadata_source") val metadataSource: String?,
    @ColumnInfo(name = "metadata_confidence") val metadataConfidence: Double?,
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long?,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long?,
    @ColumnInfo(name = "user_verified") val userVerified: Boolean,
)
