package dev.paradox.trace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.paradox.trace.data.local.entity.ContentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(content: ContentItemEntity)

    @Query("SELECT * FROM content_items WHERE id = :id")
    suspend fun getById(id: String): ContentItemEntity?

    @Query("SELECT * FROM content_items WHERE platform_content_id = :platformContentId LIMIT 1")
    suspend fun getByPlatformContentId(platformContentId: String): ContentItemEntity?

    @Query("SELECT * FROM content_items ORDER BY last_seen_at DESC")
    fun observeAll(): Flow<List<ContentItemEntity>>

    @Query("DELETE FROM content_items")
    suspend fun deleteAll()
}
