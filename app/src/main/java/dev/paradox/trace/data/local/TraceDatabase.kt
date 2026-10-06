package dev.paradox.trace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.paradox.trace.data.local.dao.AppSessionDao
import dev.paradox.trace.data.local.dao.BehaviorEventDao
import dev.paradox.trace.data.local.dao.ContentItemDao
import dev.paradox.trace.data.local.entity.AppSessionEntity
import dev.paradox.trace.data.local.entity.BehaviorEventEntity
import dev.paradox.trace.data.local.entity.ContentItemEntity

/**
 * Local-first store. Version 1 matches docs/DATA_MODEL.md; every schema
 * change requires a Room migration and migration tests per AGENTS.md.
 */
@Database(
    entities = [
        AppSessionEntity::class,
        ContentItemEntity::class,
        BehaviorEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TraceDatabase : RoomDatabase() {
    abstract fun appSessionDao(): AppSessionDao
    abstract fun contentItemDao(): ContentItemDao
    abstract fun behaviorEventDao(): BehaviorEventDao

    companion object {
        const val NAME: String = "trace.db"
    }
}
