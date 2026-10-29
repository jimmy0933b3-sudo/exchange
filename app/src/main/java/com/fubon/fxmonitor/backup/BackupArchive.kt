package com.fubon.fxmonitor.backup

import androidx.room.withTransaction
import com.fubon.fxmonitor.data.local.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

/**
 * Versioned logical snapshot, independent from Room primary keys and schema version.
 * Merge restore never deletes local data and remaps alert IDs for trigger history.
 */
class BackupArchive @Inject constructor(
    private val db: AppDatabase,
    private val rates: RateDao,
    private val alerts: AlertDao,
    private val prefs: UserPreferencesRepository
) {
    suspend fun export(): ByteArray {
        val root = db.withTransaction {
            JSONObject().apply {
                put("format", "exchange-cloud-backup")
                put("version", 1)
                put("createdAt", System.currentTimeMillis())
                put("latestRates", JSONArray().apply {
                    rates.snapshotLatest().forEach { r ->
                        put(JSONObject().apply {
                            put("code", r.currencyCode)
                            put("spotBuy", r.spotBuy); put("spotSell", r.spotSell)
                            put("cashBuy", r.cashBuy); put("cashSell", r.cashSell)
                            put("officialTime", r.officialUpdateTime)
                            put("fetched", r.localFetchTime)
                            put("source", r.source)
                            put("favorite", r.isFavorite)
                            put("order", r.displayOrder)
                        })
                    }
                })
                put("history", JSONArray().apply {
                    rates.snapshotHistory().forEach { r ->
                        put(JSONObject().apply {
                            put("code", r.currencyCode)
                            put("spotBuy", r.spotBuy); put("spotSell", r.spotSell)
                            put("cashBuy", r.cashBuy); put("cashSell", r.cashSell)
                            put("officialTime", r.officialUpdateTime)
                            put("timestamp", r.timestamp)
                            put("officialDaily", r.isOfficialDaily)
                            put("source", r.source)
                        })
                    }
                })
                put("alerts", JSONArray().apply {
                    alerts.snapshotAlerts().forEach { a ->
                        put(JSONObject().apply {
                            put("id", a.id); put("code", a.currencyCode)
                            put("rateType", a.rateType); put("bankSide", a.bankSide)
                            put("condition", a.triggerCondition); put("target", a.targetValue)
                            put("enabled", a.isEnabled); put("created", a.createdTime)
                        })
                    }
                })
                put("alertHistory", JSONArray().apply {
                    alerts.snapshotAlertHistory().forEach { h ->
                        put(JSONObject().apply {
                            put("alertId", h.alertId); put("code", h.currencyCode)
                            put("rateType", h.rateType); put("bankSide", h.bankSide)
                            put("rate", h.triggeredRate); put("target", h.targetValue)
                            put("condition", h.conditionDescription); put("time", h.triggeredTime)
                        })
                    }
                })
            }
        }
        root.put("preferences", JSONObject().apply {
            put("interval", prefs.backgroundIntervalMins.first())
            put("sound", prefs.soundEnabled.first())
            put("vibration", prefs.vibrationEnabled.first())
            put("dark", prefs.darkMode.first())
        })
        val raw = root.toString().toByteArray(Charsets.UTF_8)
        if (raw.size > 45 * 1024 * 1024) throw IOException("歷史資料超過單次備份上限")
        return ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { it.write(raw) }
            output.toByteArray()
        }
    }

    suspend fun mergeRestore(archive: ByteArray): Int {
        if (archive.size > 20 * 1024 * 1024) throw IOException("備份超過允許大小")
        val buffer = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(archive)).use { zip ->
            val block = ByteArray(8192)
            var size = 0
            while (true) {
                val n = zip.read(block)
                if (n < 0) break
                size += n
                if (size > 45 * 1024 * 1024) throw IOException("解壓縮資料過大")
                buffer.write(block, 0, n)
            }
        }
        val root = JSONObject(buffer.toString(Charsets.UTF_8.name()))
        require(root.optString("format") == "exchange-cloud-backup" && root.optInt("version") == 1) {
            "不支援的雲端備份版本"
        }
        for (name in listOf("latestRates", "history", "alerts", "alertHistory")) {
            if (!root.has(name) || root.optJSONArray(name) == null) throw IOException("備份內容缺少 $name")
        }

        // Parse and validate every record before beginning the transaction.
        val latest = root.getJSONArray("latestRates").objects().map { r ->
            LatestRateEntity(
                currencyCode = validCode(r.getString("code")),
                spotBuy = r.nullable("spotBuy"), spotSell = r.nullable("spotSell"),
                cashBuy = r.nullable("cashBuy"), cashSell = r.nullable("cashSell"),
                officialUpdateTime = r.getString("officialTime"),
                localFetchTime = r.getLong("fetched"),
                source = r.optString("source", "台北富邦銀行"),
                isFavorite = r.optBoolean("favorite"),
                displayOrder = r.optInt("order")
            )
        }
        val history = root.getJSONArray("history").objects().map { r ->
            RateHistoryEntity(
                currencyCode = validCode(r.getString("code")),
                spotBuy = r.nullable("spotBuy"), spotSell = r.nullable("spotSell"),
                cashBuy = r.nullable("cashBuy"), cashSell = r.nullable("cashSell"),
                officialUpdateTime = r.getString("officialTime"),
                timestamp = r.getLong("timestamp"),
                isOfficialDaily = r.optBoolean("officialDaily"),
                source = r.optString("source", "台北富邦銀行")
            )
        }
        val savedAlerts = root.getJSONArray("alerts").objects().map { r ->
            r.getLong("id") to ExchangeAlertEntity(
                currencyCode = validCode(r.getString("code")),
                rateType = r.getString("rateType"), bankSide = r.getString("bankSide"),
                triggerCondition = r.getString("condition"),
                targetValue = r.getDouble("target"),
                isEnabled = r.optBoolean("enabled", true),
                createdTime = r.getLong("created"),
                isTriggeredAndArmed = false
            )
        }
        val logs = root.getJSONArray("alertHistory").objects().map { r ->
            AlertHistoryEntity(
                alertId = r.getLong("alertId"),
                currencyCode = validCode(r.getString("code")),
                rateType = r.getString("rateType"), bankSide = r.getString("bankSide"),
                triggeredRate = r.getDouble("rate"), targetValue = r.getDouble("target"),
                conditionDescription = r.getString("condition"), triggeredTime = r.getLong("time")
            )
        }
        if ((latest.size + history.size + savedAlerts.size + logs.size) > 400_000) {
            throw IOException("備份筆數超過安全上限")
        }
        val settings = root.optJSONObject("preferences")
        val restored = db.withTransaction {
            val current = rates.snapshotLatest().associateBy { it.currencyCode }
            rates.insertLatestRates(latest.map { incoming ->
                val old = current[incoming.currencyCode]
                if (old == null) incoming
                else if (old.localFetchTime >= incoming.localFetchTime) old.copy(
                    isFavorite = old.isFavorite || incoming.isFavorite
                ) else incoming.copy(
                    isFavorite = old.isFavorite || incoming.isFavorite,
                    displayOrder = if (old.displayOrder != 0) old.displayOrder else incoming.displayOrder
                )
            })
            history.forEach { rates.insertHistory(it.copy(id = 0L)) }
            val currentAlerts = alerts.snapshotAlerts().toMutableList()
            val idMap = mutableMapOf<Long, Long>()
            savedAlerts.forEach { (oldId, incoming) ->
                val match = currentAlerts.firstOrNull {
                    it.currencyCode == incoming.currencyCode &&
                        it.rateType == incoming.rateType && it.bankSide == incoming.bankSide &&
                        it.triggerCondition == incoming.triggerCondition &&
                        it.targetValue == incoming.targetValue && it.createdTime == incoming.createdTime
                }
                val newId = match?.id ?: alerts.insertAlert(incoming.copy(id = 0L)).also {
                    currentAlerts.add(incoming.copy(id = it))
                }
                idMap[oldId] = newId
            }
            val existing = alerts.snapshotAlertHistory().map {
                listOf(it.alertId, it.currencyCode, it.triggeredTime, it.triggeredRate).joinToString("|")
            }.toHashSet()
            logs.forEach { entry ->
                val newId = idMap[entry.alertId] ?: return@forEach
                val key = listOf(newId, entry.currencyCode, entry.triggeredTime, entry.triggeredRate).joinToString("|")
                if (existing.add(key)) alerts.insertAlertHistory(entry.copy(id = 0L, alertId = newId))
            }
            latest.size + history.size + savedAlerts.size + logs.size
        }
        if (settings != null) {
            prefs.setBackgroundInterval(settings.optInt("interval", 15).coerceIn(15, 60))
            prefs.setSoundEnabled(settings.optBoolean("sound", true))
            prefs.setVibrationEnabled(settings.optBoolean("vibration", true))
            prefs.setDarkMode(settings.optBoolean("dark", false))
        }
        return restored
    }

    private fun validCode(code: String): String {
        require(code.matches(Regex("[A-Z]{3}"))) { "備份含無效幣別" }
        return code
    }
    private fun JSONObject.nullable(key: String) = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
}
