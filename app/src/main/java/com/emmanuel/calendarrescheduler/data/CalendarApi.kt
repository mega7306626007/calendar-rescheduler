package com.emmanuel.calendarrescheduler.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class InstanceInfo(val id: String, val originalStartMs: Long?, val status: String)

data class CreatedEvent(val id: String)

/**
 * Thin wrapper over the Apps Script backend. No OAuth available to the on-device
 * app, so every call is an HTTPS POST to the token-gated doPost web app.
 * All calls run on the IO dispatcher.
 */
class CalendarApi(context: Context) {

    private val appContext: Context = context.applicationContext

    private suspend fun rpc(fn: String, build: (JSONObject) -> Unit): Any? = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("fn", fn).put("token", API_TOKEN)
        build(payload)
        val conn = URL(APP_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 25000
            conn.readTimeout = 120000
            conn.doOutput = true
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8")
            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                ?: ""
            if (code !in 200..299) throw RuntimeException("Backend HTTP $code: ${body.take(200)}")
            val root = JSONObject(body)
            if (!root.optBoolean("ok", false)) {
                throw RuntimeException(root.optString("reason", "Backend error"))
            }
            root.opt("data")
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetchEvents(fromMs: Long, toMs: Long): List<CalEvent> = withContext(Dispatchers.IO) {
        val data = rpc("apiList") { it.put("timeMin", fromMs).put("timeMax", toMs) } as? JSONArray ?: JSONArray()
        val out = ArrayList<CalEvent>(data.length())
        for (i in 0 until data.length()) {
            out.add(toCalEvent(data.getJSONObject(i)))
        }
        out.sortedBy { it.start }
    }

    suspend fun getEvent(id: String): CalEvent? = withContext(Dispatchers.IO) {
        val data = rpc("apiGet") { it.put("id", id) }
        (data as? JSONObject)?.let { runCatching { toCalEvent(it) }.getOrNull() }
    }

    suspend fun freebusy(fromMs: Long, toMs: Long): List<Pair<Long, Long>> = withContext(Dispatchers.IO) {
        val data = rpc("apiFreebusy") { it.put("timeMin", fromMs).put("timeMax", toMs) } as? JSONArray ?: JSONArray()
        val out = ArrayList<Pair<Long, Long>>(data.length())
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            out.add(o.optLong("start") to o.optLong("end"))
        }
        out.sortedBy { it.first }
    }

    suspend fun instancesOf(masterId: String, aroundMs: Long): List<InstanceInfo> = withContext(Dispatchers.IO) {
        val data = rpc("apiInstances") {
            it.put("masterId", masterId)
                .put("timeMin", aroundMs - 2 * 3600000L)
                .put("timeMax", aroundMs + 2 * 3600000L)
        } as? JSONArray ?: JSONArray()
        val out = ArrayList<InstanceInfo>(data.length())
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            out.add(
                InstanceInfo(
                    id = o.optString("id"),
                    originalStartMs = if (o.isNull("originalStartMs")) null else o.optLong("originalStartMs"),
                    status = o.optString("status", "")
                )
            )
        }
        out
    }

    suspend fun findInstance(masterId: String, startMs: Long): InstanceInfo? {
        return instancesOf(masterId, startMs).firstOrNull {
            it.originalStartMs?.let { start -> Math.abs(start - startMs) < 120000 } == true
        }
    }

    suspend fun insertWithReminders(
        summary: String,
        startMs: Long,
        endMs: Long,
        colorId: String?,
        description: String?
    ): CreatedEvent = withContext(Dispatchers.IO) {
        val data = rpc("apiInsert") {
            it.put("summary", summary)
                .put("startMs", startMs)
                .put("endMs", endMs)
                .put("colorId", colorId ?: "")
                .put("desc", description ?: "")
        } as JSONObject
        CreatedEvent(data.optString("id"))
    }

    suspend fun cancelSingle(id: String, status: String = "cancelled") = withContext(Dispatchers.IO) {
        rpc("apiCancel") { it.put("id", id).put("status", status) }
    }

    suspend fun cancelInstance(masterId: String, startMs: Long): Boolean = withContext(Dispatchers.IO) {
        val data = rpc("apiCancelInstance") { it.put("masterId", masterId).put("startMs", startMs) } as? JSONObject
        data?.optBoolean("ok", false) == true
    }

    suspend fun deleteEventSync(id: String) = withContext(Dispatchers.IO) {
        try { rpc("apiDelete") { it.put("id", id) } } catch (e: Exception) { }
    }

    suspend fun patchDescription(id: String, newDesc: String) = withContext(Dispatchers.IO) {
        rpc("apiPatchDesc") { it.put("id", id).put("desc", newDesc) }
    }

    private fun toCalEvent(o: JSONObject): CalEvent {
        val title = o.optString("title", "(no title)")
        val desc = o.optString("desc", "")
        val start = o.optLong("start")
        val end = o.optLong("end")
        return CalEvent(
            id = o.optString("id"),
            title = title,
            start = start,
            end = end,
            colorId = o.optString("colorId", ""),
            recurring = o.optBoolean("recurring", false),
            protected = isProtected(title, desc, o.optString("colorId", "")),
            desc = desc.take(140),
            durationMin = Math.round((end - start) / 60000.0).toInt()
        )
    }

    private companion object {
        const val API_TOKEN = "sch3d-7Q2vLm9x-Kp4R.Android"
        const val APP_URL =
            "https://script.google.com/macros/s/AKfycbwpcc_eX6ITLIqZSbOvz1gP3PS6BzSTKDDPKBqGokBWmsHXtevabyprntLm1sz4Jx2AcA/exec"
    }
}