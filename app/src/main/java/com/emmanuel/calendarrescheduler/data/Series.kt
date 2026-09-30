package com.emmanuel.calendarrescheduler.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Calendar
import java.util.UUID

@Entity(tableName = "series")
data class SeriesEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val created: Long
)

@Entity(tableName = "series_members", primaryKeys = ["seriesId", "eventId"])
data class SeriesMember(
    val seriesId: String,
    val eventId: String,
    val start: Long
)

@Dao
interface SeriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(s: SeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(m: List<SeriesMember>)

    @Query("SELECT * FROM series_members WHERE eventId = :eventId LIMIT 1")
    suspend fun memberOf(eventId: String): SeriesMember?

    @Query("SELECT * FROM series_members WHERE seriesId = :seriesId AND start >= :fromMs ORDER BY start ASC")
    suspend fun following(seriesId: String, fromMs: Long): List<SeriesMember>

    @Query("SELECT * FROM series_members WHERE seriesId = :seriesId ORDER BY start ASC")
    suspend fun membersOf(seriesId: String): List<SeriesMember>

    @Query("SELECT * FROM series")
    suspend fun allSeries(): List<SeriesEntity>

    @Query("DELETE FROM series_members WHERE seriesId = :seriesId AND start >= :fromMs")
    suspend fun deleteFollowing(seriesId: String, fromMs: Long)

    @Query("SELECT COUNT(*) FROM series_members WHERE seriesId = :seriesId")
    suspend fun count(seriesId: String): Int

    @Query("DELETE FROM series WHERE id = :seriesId")
    suspend fun deleteSeries(seriesId: String)
}

enum class RepeatRule { ONCE, DAILY, WEEKDAYS, WEEKLY, MONTHLY }

fun repeatLabel(rule: RepeatRule): String = when (rule) {
    RepeatRule.ONCE -> "Once"
    RepeatRule.DAILY -> "Daily"
    RepeatRule.WEEKDAYS -> "Weekdays"
    RepeatRule.WEEKLY -> "Weekly"
    RepeatRule.MONTHLY -> "Monthly"
}

/** Start-times for each occurrence of a rule, beginning at [dayMs] [hour]:[minute]. */
fun repeatStarts(rule: RepeatRule, dayMs: Long, hour: Int, minute: Int, count: Int): List<Long> {
    val first = Dates.timeOn(dayMs, hour, minute)
    if (rule == RepeatRule.ONCE || count <= 1) return listOf(first)
    val n = count.coerceIn(1, 24)
    return when (rule) {
        RepeatRule.DAILY -> (0 until n).map { first + it * 86400000L }
        RepeatRule.WEEKLY -> (0 until n).map { first + it * 7 * 86400000L }
        RepeatRule.WEEKDAYS -> {
            val out = mutableListOf<Long>()
            var d = dayMs
            var guard = 0
            while (out.size < n && guard < 60) {
                val dow = Calendar.getInstance().apply { timeInMillis = d }.get(Calendar.DAY_OF_WEEK)
                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                    out.add(Dates.timeOn(d, hour, minute))
                }
                d += 86400000L
                guard++
            }
            out
        }
        RepeatRule.MONTHLY -> {
            (0 until n).map { i ->
                Calendar.getInstance().apply {
                    timeInMillis = first
                    add(Calendar.MONTH, i)
                }.timeInMillis
            }
        }
        RepeatRule.ONCE -> listOf(first)
    }
}

fun newSeriesId(): String = UUID.randomUUID().toString().take(8)
