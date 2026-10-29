package com.fubon.fxmonitor.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RateDao {
    @Query("SELECT * FROM latest_rates ORDER BY currencyCode ASC")
    suspend fun snapshotLatest(): List<LatestRateEntity>

    @Query("SELECT * FROM rate_history ORDER BY currencyCode, timestamp ASC")
    suspend fun snapshotHistory(): List<RateHistoryEntity>

    @Query("SELECT * FROM latest_rates ORDER BY displayOrder ASC, currencyCode ASC")
    fun getAllLatestRates(): Flow<List<LatestRateEntity>>

    @Query("SELECT * FROM latest_rates WHERE currencyCode = :code")
    suspend fun getLatestRate(code: String): LatestRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLatestRates(rates: List<LatestRateEntity>)

    @Query("UPDATE latest_rates SET isFavorite = :isFavorite WHERE currencyCode = :code")
    suspend fun updateFavorite(code: String, isFavorite: Boolean)

    @Query("UPDATE latest_rates SET displayOrder = :order WHERE currencyCode = :code")
    suspend fun updateDisplayOrder(code: String, order: Int)

    // Rate History
    @Query("SELECT * FROM rate_history WHERE currencyCode = :code ORDER BY timestamp DESC")
    fun getHistoryForCurrency(code: String): Flow<List<RateHistoryEntity>>

    @Query("SELECT * FROM rate_history WHERE currencyCode = :code AND officialUpdateTime = :officialTime LIMIT 1")
    suspend fun getHistoryByOfficialTime(code: String, officialTime: String): RateHistoryEntity?

    @Query("SELECT * FROM rate_history WHERE currencyCode = :code ORDER BY timestamp DESC LIMIT 2")
    suspend fun getLatestTwoHistory(code: String): List<RateHistoryEntity>

    @Query("SELECT * FROM rate_history WHERE currencyCode = :code AND SUBSTR(officialUpdateTime, 1, 10) < SUBSTR(:currentOfficialTime, 1, 10) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getPreviousDayHistory(code: String, currentOfficialTime: String): RateHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: RateHistoryEntity)

    @Query("DELETE FROM rate_history")
    suspend fun clearHistory()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM exchange_alerts ORDER BY createdTime ASC")
    suspend fun snapshotAlerts(): List<ExchangeAlertEntity>

    @Query("SELECT * FROM alert_history ORDER BY triggeredTime ASC")
    suspend fun snapshotAlertHistory(): List<AlertHistoryEntity>

    @Query("SELECT * FROM exchange_alerts ORDER BY createdTime DESC")
    fun getAllAlerts(): Flow<List<ExchangeAlertEntity>>

    @Query("SELECT * FROM exchange_alerts WHERE isEnabled = 1")
    suspend fun getActiveAlerts(): List<ExchangeAlertEntity>

    @Query("SELECT * FROM exchange_alerts WHERE id = :id")
    suspend fun getAlertById(id: Long): ExchangeAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: ExchangeAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: ExchangeAlertEntity)

    @Query("DELETE FROM exchange_alerts WHERE id = :id")
    suspend fun deleteAlert(id: Long)

    // Alert Trigger Log
    @Query("SELECT * FROM alert_history ORDER BY triggeredTime DESC")
    fun getAlertHistory(): Flow<List<AlertHistoryEntity>>

    @Insert
    suspend fun insertAlertHistory(log: AlertHistoryEntity)

    @Query("DELETE FROM alert_history")
    suspend fun clearAlertHistory()
}

@Dao
interface ErrorLogDao {
    @Query("SELECT * FROM error_logs ORDER BY timestamp DESC LIMIT 100")
    fun getErrorLogs(): Flow<List<ErrorLogEntity>>

    @Insert
    suspend fun insertErrorLog(log: ErrorLogEntity)

    @Query("DELETE FROM error_logs")
    suspend fun clearLogs()
}
