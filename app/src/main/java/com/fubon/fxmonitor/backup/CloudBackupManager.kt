package com.fubon.fxmonitor.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.identity.AuthorizationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CloudBackupState(
    val email: String? = null,
    val enabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val lastBackupTime: Long = 0L,
    val lastError: String? = null,
    val busy: Boolean = false,
    val backups: List<DriveBackupFile> = emptyList()
)

@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val drive: DriveBackupClient,
    private val archive: BackupArchive
) {
    private val prefs = context.getSharedPreferences("exchange_cloud_backup", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<CloudBackupState> = _state

    private fun load() = CloudBackupState(
        email = prefs.getString("email", null),
        enabled = prefs.getBoolean("enabled", false),
        wifiOnly = prefs.getBoolean("wifiOnly", true),
        lastBackupTime = prefs.getLong("lastSuccess", 0L),
        lastError = prefs.getString("lastError", null)
    )

    fun configured() = com.fubon.fxmonitor.BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    fun signIn(email: String) {
        val previous = _state.value.email
        require(email.contains("@"))
        val same = previous == email
        prefs.edit().putString("email", email)
            .putBoolean("enabled", if (same) _state.value.enabled else false)
            .putLong("lastSuccess", if (same) _state.value.lastBackupTime else 0L)
            .remove("lastError").apply()
        _state.value = load()
        reschedule()
    }

    fun signOut() {
        prefs.edit().remove("email").putBoolean("enabled", false).remove("lastError").apply()
        _state.value = load()
        reschedule()
    }

    fun setWiFiOnly(onlyWiFi: Boolean) {
        prefs.edit().putBoolean("wifiOnly", onlyWiFi).apply()
        _state.value = _state.value.copy(wifiOnly = onlyWiFi)
        reschedule()
    }

    fun setAutoEnabled(enabled: Boolean) {
        if (enabled) check(!_state.value.email.isNullOrBlank()) { "請先登入 Google 帳號" }
        prefs.edit().putBoolean("enabled", enabled).apply()
        _state.value = _state.value.copy(enabled = enabled)
        reschedule()
    }

    fun acceptAuthorization(result: AuthorizationResult) {
        if (result.hasResolution() || result.accessToken.isNullOrBlank() ||
            !result.grantedScopes.contains(DriveBackupClient.SCOPE)) {
            throw IllegalStateException("尚未授權 Exchange 存取 Drive 應用程式資料")
        }
        setAutoEnabled(true)
    }

    fun reschedule() {
        val work = WorkManager.getInstance(context)
        if (_state.value.email == null || !_state.value.enabled) {
            work.cancelUniqueWork("exchange-cloud-daily-backup")
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (_state.value.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        work.enqueueUniquePeriodicWork(
            "exchange-cloud-daily-backup",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    suspend fun refreshFiles(): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val email = _state.value.email ?: error("請先登入 Google")
        val files = drive.list(drive.tokenWithoutPrompt(email))
        _state.value = _state.value.copy(backups = files)
        files
    }

    suspend fun backupNow(): DriveBackupFile = lock.withLock {
        _state.value = _state.value.copy(busy = true, lastError = null)
        try {
            withContext(Dispatchers.IO) {
                val email = _state.value.email ?: error("請先登入 Google")
                val token = drive.tokenWithoutPrompt(email)
                val bytes = archive.export()
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
                val uploaded = drive.upload(token, "exchange-backup-$timestamp.json.gz", bytes)
                prefs.edit().putLong("lastSuccess", System.currentTimeMillis()).remove("lastError").apply()
                _state.value = _state.value.copy(lastBackupTime = prefs.getLong("lastSuccess", 0L))
                uploaded
            }
        } finally {
            _state.value = _state.value.copy(busy = false)
        }
    }

    suspend fun restore(file: DriveBackupFile): Int = lock.withLock {
        _state.value = _state.value.copy(busy = true)
        try {
            withContext(Dispatchers.IO) {
                val email = _state.value.email ?: error("請先登入 Google")
                val token = drive.tokenWithoutPrompt(email)
                val found = drive.list(token).firstOrNull { it.id == file.id && it.name == file.name }
                    ?: error("找不到指定備份")
                archive.mergeRestore(drive.download(token, found))
            }
        } finally {
            _state.value = _state.value.copy(busy = false)
        }
    }

    fun reportError(message: String) {
        prefs.edit().putString("lastError", message.take(200)).apply()
        _state.value = _state.value.copy(lastError = message.take(200))
    }
}
