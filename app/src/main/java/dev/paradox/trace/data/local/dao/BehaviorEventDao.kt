package dev.paradox.trace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.paradox.trace.data.local.entity.BehaviorEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: BehaviorEventEntity): Long

    /** Returns the count of rows actually inserted; duplicates are ignored. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<BehaviorEventEntity>): List<Long>

    @Query("SELECT * FROM behavior_events ORDER BY occurred_at ASC")
    fun observeAll(): Flow<List<BehaviorEventEntity>>

    @Query("DELETE FROM behavior_events")
    suspend fun deleteAll()
}
