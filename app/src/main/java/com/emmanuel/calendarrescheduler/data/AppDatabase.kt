package com.emmanuel.calendarrescheduler.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

@Entity(tableName = "cached_events")
data class CachedEvent(
    @PrimaryKey val id: String,
    val title: String,
    val start: Long,
    val end: Long,
    val colorId: String,
    val recurring: Boolean,
    val protected_: Boolean,
    val desc: String,
    val durationMin: Int,
    val cachedAt: Long
)

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(events: List<CachedEvent>)

    @Query("SELECT * FROM cached_events WHERE start >= :from AND start < :to ORDER BY start ASC")
    suspend fun getBetween(from: Long, to: Long): List<CachedEvent>

    @Query("DELETE FROM cached_events WHERE cachedAt < :before")
    suspend fun deleteBefore(before: Long)

    @Query("SELECT COUNT(*) FROM cached_events")
    suspend fun count(): Int
}

@Entity(tableName = "undo_actions")
data class UndoAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val created: Long,
    val originalId: String?,
    val originalIsRecurring: Boolean,
    val createdId: String?,
    val title: String,
    val createdIds: String? = null
)

@Dao
interface UndoDao {
    @Insert
    suspend fun insert(action: UndoAction): Long

    @Query("SELECT * FROM undo_actions ORDER BY created DESC LIMIT 1")
    suspend fun last(): UndoAction?

    @Query("SELECT * FROM undo_actions ORDER BY created DESC LIMIT 30")
    suspend fun recent(): List<UndoAction>

    @Query("DELETE FROM undo_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM undo_actions")
    suspend fun clear()
}

@Database(entities = [CachedEvent::class, UndoAction::class, SeriesEntity::class, SeriesMember::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun undoDao(): UndoDao
    abstract fun seriesDao(): SeriesDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE undo_actions ADD COLUMN createdIds TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `series` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `created` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `series_members` (`seriesId` TEXT NOT NULL, `eventId` TEXT NOT NULL, `start` INTEGER NOT NULL, PRIMARY KEY(`seriesId`, `eventId`))")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "calres.db")
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build().also { INSTANCE = it }
                } catch (e: Exception) {
                    try { context.applicationContext.deleteDatabase("calres.db") } catch (_: Exception) {}
                    Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "calres.db")
                        .fallbackToDestructiveMigration()
                        .build().also { INSTANCE = it }
                }
            }
    }
}