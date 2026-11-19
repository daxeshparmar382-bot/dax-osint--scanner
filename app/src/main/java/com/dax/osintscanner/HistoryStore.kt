package com.dax.osintscanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ScanRecord(val type: String, val query: String, val result: String, val time: String = java.time.Instant.now().toString())

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("dax_history", Context.MODE_PRIVATE)

    fun read(): List<ScanRecord> {
        val raw = prefs.getString("records", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                ScanRecord(
                    item.optString("type"),
                    item.optString("query"),
                    item.optString("result"),
                    item.optString("time")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(record: ScanRecord) {
        val records = (listOf(record) + read()).take(100)
        val array = JSONArray()
        records.forEach {
            array.put(JSONObject().put("type", it.type).put("query", it.query).put("result", it.result).put("time", it.time))
        }
        prefs.edit().putString("records", array.toString()).apply()
    }

    fun clear() = prefs.edit().remove("records").apply()
}
