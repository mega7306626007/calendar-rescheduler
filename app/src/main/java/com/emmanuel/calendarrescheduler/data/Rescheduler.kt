package com.emmanuel.calendarrescheduler.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.suspendCoroutine

const val MIN_SLOT_MIN = 15
const val DAY_START_HOUR = 8
const val MAX_PUSH_DAYS = 14
const val EMPTY_REASON = "Not specified"
const val PUSH_SEGMENT_MIN = 30
const val EXTEND_END_MIN = 23 * 60 + 55
const val EARLY_5_HOUR = 5
const val EARLY_4_HOUR = 4

data class MoveResult(
    val ok: Boolean,
    val reason: String? = null,
    val title: String? = null,
    val newStart: Long = 0,
    val newEnd: Long = 0,
    val trimmed: Boolean = false,
    val rollover: Boolean = false
)

data class MovePlan(val start: Long, val end: Long, val trimmed: Boolean, val rollover: Boolean)

data class QueueOption(val key: String, val title: String, val fitMin: Int, val trimmed: Boolean)

data class Segment(val start: Long, val end: Long, val part: Int, val total: Int, val min: Int)

data class PushAlternative(
    val kind: String,
    val label: String,
    val detail: String,
    val segments: List<Segment> = emptyList(),
    val target: Long = 0L
)

data class PushOutcome(
    val full: MovePlan? = null,
    val alternatives: List<PushAlternative> = emptyList()
)

object SlotEngine {
    fun freeRegions(from: Long, to: Long, busy: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
        val merged = mutableListOf<Pair<Long, Long>>()
        var cursor = from
        for (b in busy.sortedBy { it.first }) {
            if (b.first > cursor) merged.add(cursor to b.first)
            if (b.second > cursor) cursor = b.second
            if (cursor >= to) break
        }
        if (cursor < to) merged.add(cursor to to)
        val out = mutableListOf<Pair<Long, Long>>()
        for (r in merged) {
            val last = out.lastOrNull()
            if (last != null && (r.first - last.second) < 10 * 60000) {
                out[out.size - 1] = last.first to maxOf(last.second, r.second)
            } else {
                out.add(r)
            }
        }
        return out
    }

    val DAY_END_PREFIX = 22.75 // 10:45 PM

    fun dayEndLimit(dayMs: Long): Long {
        val base = dayMs + 86400000 - 1
        val cap = dayMs + (DAY_END_PREFIX * 3600000).toLong()
        return minOf(cap, base)
    }

    fun freeSlotsForDay(dayMs: Long, busy: List<Pair<Long, Long>>): List<Slot> {
        val from = dayMs + DAY_START_HOUR * 3600000L
        val to = dayEndLimit(dayMs)
        val step = 30 * 60000L
        val regions = freeRegions(from, to, busy)
        val out = mutableListOf<Slot>()
        var now = System.currentTimeMillis()
        for (r in regions) {
            var s = maxOf(r.first, now)
            val rem = s % step
            if (rem != 0L) s += step - rem
            while (s + step <= r.second) {
                out.add(Slot(s, s + step))
                s += step
                if (out.size >= 8) break
            }
            if (out.size >= 8) break
        }
        return out
    }
}

private val QUEUE = listOf(
    Triple("common-app-essays", "Common App essays", 120) to true,
    Triple("common-app-extras", "Common App extra activities", 60) to true,
    Triple("chatbot", "Chatbot dev", 60) to false,
    Triple("study-extra", "Study time - extra practice", 60) to false,
    Triple("transcriber", "Transcriber", 120) to false,
    Triple("poetry", "Poetry website / Poems typing", 150) to false,
    Triple("knowledge", "More knowledge", 45) to false
)

object Rescheduler {
    suspend fun planPush(api: CalendarApi, ev: CalEvent): PushOutcome {
        val neededMin = maxOf(ev.durationMin, MIN_SLOT_MIN)
        val canTrim = ev.recurring
        val now = System.currentTimeMillis()
        val day = Dates.startOfDay(now)
        val firstFrom = maxOf(ev.end, now)
        val busy: List<Pair<Long, Long>> =
            try { api.freebusy(firstFrom, day + MAX_PUSH_DAYS * 86400000L) } catch (e: Exception) { emptyList() }

        val full = findSlot(firstFrom, busy, neededMin, canTrim, now, extendedEnd = false, earlyHour = null)
        if (full != null) return PushOutcome(full = full)

        val alternatives = mutableListOf<PushAlternative>()

        val segments = planSegments(firstFrom, busy, neededMin, now)
        if (segments.isNotEmpty()) {
            val totalMin = segments.sumOf { (it.end - it.start) / 60000 }.toInt()
            val detail = run {
                val firstEnd = segments.last().end
                val wide = firstEnd - segments.first().start >= 2 * 86400000L
                val span = if (wide) "${fmtDay(segments.first().start)} ${fmtTime(segments.first().start)} – ${fmtDay(lastEndOf(segments))} ${fmtTime(lastEndOf(segments))}"
                           else "${fmtDay(segments.first().start)} ${fmtTime(segments.first().start)} – ${fmtTime(firstEnd)}"
                "$totalMin min total in ${segments.size} slots · $span"
            }
            alternatives.add(
                PushAlternative(
                    kind = "split",
                    label = "Split into 30-min parts (${segments.size} parts)",
                    detail = detail,
                    segments = segments
                )
            )
        }

        findSlot(firstFrom, busy, neededMin, canTrim, now, extendedEnd = true, earlyHour = null)?.let { ext ->
            alternatives.add(
                PushAlternative(
                    kind = "extend",
                    label = "Temporarily extend the day until midnight",
                    detail = "Move it to ${fmtDay(ext.start)} ${fmtTime(ext.start)}",
                    target = ext.start
                )
            )
        }

        findSlot(firstFrom, busy, neededMin, canTrim, now, extendedEnd = false, earlyHour = EARLY_5_HOUR)?.let { e5 ->
            alternatives.add(
                PushAlternative(
                    kind = "early5",
                    label = "Wake up early — start at 5 AM",
                    detail = "Move it to ${fmtDay(e5.start)} ${fmtTime(e5.start)}",
                    target = e5.start
                )
            )
        }

        findSlot(firstFrom, busy, neededMin, canTrim, now, extendedEnd = false, earlyHour = EARLY_4_HOUR)?.let { e4 ->
            alternatives.add(
                PushAlternative(
                    kind = "early4",
                    label = "Wake up even earlier — start at 4 AM",
                    detail = "Move it to ${fmtDay(e4.start)} ${fmtTime(e4.start)}",
                    target = e4.start
                )
            )
        }

        return PushOutcome(full = null, alternatives = alternatives)
    }

    private fun lastEndOf(segments: List<Segment>): Long = segments.last().end

    private suspend fun findSlot(
        firstFrom: Long,
        busy: List<Pair<Long, Long>>,
        neededMin: Int,
        canTrim: Boolean,
        now: Long,
        extendedEnd: Boolean,
        earlyHour: Int?
    ): MovePlan? {
        val day = Dates.startOfDay(now)
        for (i in 0 until MAX_PUSH_DAYS) {
            val dayMs = day + i * 86400000L
            val startHour = earlyHour ?: DAY_START_HOUR
            val fromLimit = dayMs + startHour * 3600000L
            val from = if (i == 0) maxOf(firstFrom, fromLimit) else fromLimit
            val to = if (extendedEnd) dayMs + EXTEND_END_MIN * 60000L else SlotEngine.dayEndLimit(dayMs)
            if (to - from < MIN_SLOT_MIN * 60000L) continue
            var firstTrim: MovePlan? = null
            for (r in SlotEngine.freeRegions(from, to, busy)) {
                val s = maxOf(r.first, now + 30000L)
                if (s >= r.second) continue
                val lenMin = (r.second - s) / 60000
                if (lenMin >= neededMin) return MovePlan(s, s + neededMin * 60000L, trimmed = false, rollover = i > 0)
                if (canTrim && lenMin >= MIN_SLOT_MIN && firstTrim == null) firstTrim = MovePlan(s, r.second, trimmed = true, rollover = i > 0)
            }
            if (firstTrim != null) return firstTrim
        }
        return null
    }

    private suspend fun planSegments(
        firstFrom: Long,
        busy: List<Pair<Long, Long>>,
        neededMin: Int,
        now: Long
    ): List<Segment> {
        if (neededMin > 12 * PUSH_SEGMENT_MIN) return emptyList()
        val chunkMs = PUSH_SEGMENT_MIN * 60000L
        val totalChunks = (neededMin + PUSH_SEGMENT_MIN - 1) / PUSH_SEGMENT_MIN
        val day = Dates.startOfDay(now)
        val slots = mutableListOf<Pair<Long, Long>>()
        outer@ for (i in 0 until MAX_PUSH_DAYS) {
            val dayMs = day + i * 86400000L
            val from = if (i == 0) maxOf(firstFrom, now) else dayMs + DAY_START_HOUR * 3600000L
            val to = SlotEngine.dayEndLimit(dayMs)
            if (to - from < chunkMs) continue
            for (r in SlotEngine.freeRegions(from, to, busy)) {
                var s = maxOf(r.first, now + 30000L)
                val rem = s % chunkMs
                if (rem != 0L) s += chunkMs - rem
                while (s + chunkMs <= r.second) {
                    if (slots.size >= totalChunks) break@outer
                    slots.add(s to s + chunkMs)
                    s += chunkMs
                }
                if (slots.size >= totalChunks) break@outer
            }
        }
        if (slots.size < totalChunks) return emptyList()
        var remainMs = neededMin * 60000L
        return slots.take(totalChunks).mapIndexed { idx, (s, _) ->
            val lenMs = minOf(chunkMs, remainMs)
            remainMs -= lenMs
            Segment(start = s, end = s + lenMs, part = idx + 1, total = totalChunks, min = (lenMs / 60000).toInt())
        }
    }

    suspend fun applySplit(
        api: CalendarApi,
        db: AppDatabase,
        ev: CalEvent,
        segments: List<Segment>,
        reason: String
    ): MoveResult {
        if (segments.isEmpty()) return MoveResult(false, "No free 30-min slots were found.")
        val cancelledOk = cancelOriginal(api, ev)
        if (!cancelledOk) return MoveResult(false, "Could not cancel the original event.")
        val createdIds = mutableListOf<String>()
        try {
            for (seg in segments) {
                val title = if (segments.size > 1) "${ev.title} · part ${seg.part}/${seg.total}" else ev.title
                val note = buildString {
                    if (ev.desc.isNotEmpty()) append(ev.desc).append("\n")
                    append("— Split into ${segments.size} parts because no single free gap was available.")
                    if (reason.isNotBlank()) append("\nReason: $reason")
                }
                val created = api.insertWithReminders(
                    summary = title,
                    startMs = seg.start,
                    endMs = seg.end,
                    colorId = ev.colorId.takeIf { it.isNotEmpty() },
                    description = note
                )
                createdIds.add(created.id)
            }
        } catch (e: Exception) {
            restoreOriginal(api, ev)
            return MoveResult(false, "Could not create all parts: ${e.message}")
        }
        if (createdIds.isEmpty()) return MoveResult(false, "No parts were created.")
        db.undoDao().insert(
            UndoAction(
                created = System.currentTimeMillis(),
                originalId = ev.id,
                originalIsRecurring = ev.recurring,
                createdId = createdIds.first(),
                createdIds = createdIds.joinToString(","),
                title = ev.title
            )
        )
        return MoveResult(
            ok = true,
            title = ev.title,
            newStart = segments.first().start,
            newEnd = segments.last().end,
            trimmed = true
        )
    }

    suspend fun placeAt(
        api: CalendarApi,
        db: AppDatabase,
        ev: CalEvent,
        startMs: Long,
        endMs: Long,
        reason: String,
        rollover: Boolean
    ): MoveResult {
        if (endMs - startMs < MIN_SLOT_MIN * 60000L) return MoveResult(false, "Not enough room at that time.")
        if (startMs <= System.currentTimeMillis() + 120000) return MoveResult(false, "That time has already passed.")
        val busy = try { api.freebusy(startMs, endMs) } catch (e: Exception) { emptyList() }
        for (b in busy) {
            if (b.first < endMs && b.second > startMs) {
                return MoveResult(false, "That slot just got taken — refresh and pick again.")
            }
        }
        return finishMove(api, db, ev, startMs, endMs, reason, rollover)
    }

    suspend fun moveTo(api: CalendarApi, db: AppDatabase, ev: CalEvent, slotMs: Long, reason: String): MoveResult {
        var endMs = slotMs + maxOf(ev.durationMin, MIN_SLOT_MIN) * 60000L
        val limit = SlotEngine.dayEndLimit(Dates.startOfDay(slotMs))
        if (endMs > limit) endMs = limit
        if ((endMs - slotMs) < MIN_SLOT_MIN * 60000L) return MoveResult(false, "Not enough room at that time.")
        if (slotMs <= System.currentTimeMillis() + 120000) return MoveResult(false, "That time has already passed.")
        val busy = try { api.freebusy(slotMs, endMs) } catch (e: Exception) { emptyList() }
        for (b in busy) {
            if (b.first < endMs && b.second > slotMs) {
                return MoveResult(false, "That slot just got taken — refresh and pick again.")
            }
        }
        return finishMove(api, db, ev, slotMs, endMs, reason, rollover = false)
    }

    private suspend fun finishMove(
        api: CalendarApi, db: AppDatabase, ev: CalEvent, startMs: Long, endMs: Long, reason: String, rollover: Boolean
    ): MoveResult {
        val requestedLen = ev.durationMs
        val trimmed = requestedLen > (endMs - startMs) + 30000
        val cancelledOk = cancelOriginal(api, ev)
        if (!cancelledOk) return MoveResult(false, "Could not cancel the original event.")

        val created = try {
            api.insertWithReminders(
                summary = ev.title,
                startMs = startMs,
                endMs = endMs,
                colorId = ev.colorId.takeIf { it.isNotEmpty() },
                description = ev.desc.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            restoreOriginal(api, ev)
            return MoveResult(false, "Cancelled original but could not create the replacement: ${e.message}")
        }

        try { patchReason(api, ev, created.id, startMs, reason) } catch (e: Exception) { }

        db.undoDao().insert(
            UndoAction(created = System.currentTimeMillis(), originalId = ev.id, originalIsRecurring = ev.recurring, createdId = created.id, title = ev.title)
        )

        return MoveResult(
            ok = true,
            title = ev.title,
            newStart = startMs,
            newEnd = endMs,
            trimmed = trimmed,
            rollover = rollover
        )
    }

    private suspend fun cancelOriginal(api: CalendarApi, ev: CalEvent): Boolean {
        return try {
            if (ev.recurring) {
                val master = ev.id.substringBeforeLast('_').ifEmpty { ev.id }
                api.cancelInstance(master, ev.start)
            } else {
                api.cancelSingle(ev.id)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun restoreOriginal(api: CalendarApi, ev: CalEvent) {
        try {
            if (ev.recurring) {
                val master = ev.id.substringBeforeLast('_').ifEmpty { ev.id }
                api.findInstance(master, ev.start)?.let { api.cancelSingle(it.id, "confirmed") }
            } else {
                api.cancelSingle(ev.id, "confirmed")
            }
        } catch (e: Exception) { }
    }

    private suspend fun patchReason(api: CalendarApi, ev: CalEvent, createdId: String, newStartMs: Long, reason: String) {
        val tz = java.util.TimeZone.getDefault()
        val fmt = java.text.SimpleDateFormat("EEE d MMM, h:mm a", java.util.Locale.getDefault()).apply { timeZone = tz }
        val lines = mutableListOf<String>()
        if (ev.desc.isNotEmpty()) lines.add(ev.desc)
        lines.add("— Moved from ${fmt.format(java.util.Date(ev.start))} to ${fmt.format(java.util.Date(newStartMs))}.")
        lines.add("Reason: ${reason.ifBlank { EMPTY_REASON }}")
        api.patchDescription(createdId, lines.joinToString("\n"))
    }

    suspend fun markDone(api: CalendarApi, evId: String): Boolean {
        return try {
            val ev = api.getEvent(evId)?.takeIf { it.desc.isNotBlank() }
            val tz = java.text.SimpleDateFormat("EEE d MMM yyyy", java.util.Locale.getDefault())
            val note = "✅ Completed on " + tz.format(java.util.Date())
            val cur = api.getEvent(evId) ?: return false
            if (cur.desc.contains("✅ Completed")) return true
            api.patchDescription(evId, (cur.desc.takeIf { it.isNotEmpty() }?.plus("\n") ?: "") + note)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun undoLast(api: CalendarApi, db: AppDatabase): Boolean {
        db.undoDao().last()?.let { return undo(api, db, it) }
        return false
    }

    suspend fun undo(api: CalendarApi, db: AppDatabase, action: UndoAction): Boolean {
        try {
            action.originalId?.let { oid ->
                try {
                    val ev = api.getEvent(oid) ?: throw IllegalStateException("gone")
                    if (!ev.protected) api.cancelSingle(oid, "confirmed")
                } catch (e: Exception) {
                    // original was a recurring instance that no longer shows — nothing to restore
                }
            }
            if (!action.createdIds.isNullOrBlank()) {
                action.createdIds.split(",").filter { it.isNotBlank() }.forEach { api.deleteEventSync(it) }
            } else {
                action.createdId?.let { api.deleteEventSync(it) }
            }
            db.undoDao().delete(action.id)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun fmtDay(ms: Long): String {
        return when (Dates.daysBetween(ms, System.currentTimeMillis())) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(ms))
        }
    }

    private fun fmtTime(ms: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

    fun overfillOptions(termActive: Boolean, windowMin: Int): List<QueueOption> {
        val out = mutableListOf<QueueOption>()
        for ((it, termGated) in QUEUE) {
            if (termGated && !termActive) continue
            val fit = minOf(it.third, windowMin)
            if (fit >= MIN_SLOT_MIN) out.add(QueueOption(it.first, it.second, fit, fit < it.third))
            if (out.size >= 5) break
        }
        return out
    }

    suspend fun doOverfill(api: CalendarApi, db: AppDatabase, ev: CalEvent, key: String): MoveResult {
        val item = QUEUE.firstOrNull { it.first.first == key } ?: return MoveResult(false, "Unknown queue item.")
        val winStart = ev.start
        val winEnd = ev.end
        val winMin = (winEnd - winStart) / 60000
        val fit = minOf(item.first.third, winMin.toInt())
        if (fit < MIN_SLOT_MIN) return MoveResult(false, "Freed window too short for this item.")
        val cancelledOk = cancelOriginal(api, ev)
        if (!cancelledOk) return MoveResult(false, "Could not cancel the class.")
        val created = try {
            api.insertWithReminders(item.first.second, winStart, winStart + fit * 60000L, null, null)
        } catch (e: Exception) {
            restoreOriginal(api, ev)
            return MoveResult(false, "Cancelled class but could not create item: ${e.message}")
        }
        db.undoDao().insert(
            UndoAction(created = System.currentTimeMillis(), originalId = ev.id, originalIsRecurring = ev.recurring, createdId = created.id, title = ev.title)
        )
        return MoveResult(ok = true, title = item.first.second, newStart = winStart, newEnd = winStart + fit * 60000L, trimmed = fit < item.first.third)
    }

    suspend fun isTermActive(api: CalendarApi): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val later = now + 14 * 86400000L
            val all = api.fetchEvents(now, later)
            all.any { it.title.contains("Common App") }
        } catch (e: Exception) {
            false
        }
    }
}