package com.fubon.fxmonitor.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class DriveBackupFile(val id: String, val name: String, val createdTime: String, val md5: String)

@Singleton
class DriveBackupClient @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val API = "https://www.googleapis.com/drive/v3/files"
        private const val PREFIX = "exchange-backup-"
    }
    private val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
    private val authorization = Identity.getAuthorizationClient(context)

    private fun request(accountEmail: String) = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(SCOPE)))
        .setAccount(Account(accountEmail, "com.google"))
        .build()

    suspend fun authorize(accountEmail: String): AuthorizationResult = withContext(Dispatchers.IO) {
        Tasks.await(authorization.authorize(request(accountEmail)))
    }

    /** Background jobs cannot launch Google UI. A missing grant is a user action, not a token to cache. */
    suspend fun tokenWithoutPrompt(accountEmail: String): String {
        val result = authorize(accountEmail)
        if (result.hasResolution()) throw IllegalStateException("需要開啟 App 重新授權 Google Drive")
        return result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google Drive 未提供存取權杖，請重新授權")
    }

    private fun authenticated(token: String, url: String): Request.Builder =
        Request.Builder().url(url).header("Authorization", "Bearer $token")

    private fun responseJson(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // Do not leak any bearer token or response credentials to logs.
                throw IOException("Google Drive HTTP ${response.code}: ${body.take(350)}")
            }
            return if (body.isBlank()) JSONObject() else JSONObject(body)
        }
    }

    suspend fun list(token: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val out = mutableListOf<DriveBackupFile>()
        var page: String? = null
        do {
            val url = API.toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("q", "name contains '$PREFIX' and trashed = false")
                .addQueryParameter("fields", "nextPageToken,files(id,name,createdTime,md5Checksum)")
                .addQueryParameter("pageSize", "100")
                .apply { page?.let { addQueryParameter("pageToken", it) } }
                .build()
            val json = responseJson(authenticated(token, url.toString()).get().build())
            val files = json.optJSONArray("files")
            if (files != null) for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name")
                if (name.startsWith(PREFIX) && name.endsWith(".json.gz")) {
                    out.add(DriveBackupFile(f.getString("id"), name, f.optString("createdTime"), f.optString("md5Checksum")))
                }
            }
            page = json.optString("nextPageToken").ifBlank { null }
        } while (page != null && out.size < 500)
        out.sortedByDescending { it.name }
    }

    suspend fun upload(token: String, filename: String, bytes: ByteArray): DriveBackupFile =
        withContext(Dispatchers.IO) {
            require(filename.startsWith(PREFIX) && filename.endsWith(".json.gz"))
            val metadata = JSONObject()
                .put("name", filename)
                .put("parents", org.json.JSONArray().put("appDataFolder"))
                .put("mimeType", "application/gzip")
            val initial = authenticated(token, "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id,name,createdTime,md5Checksum")
                .header("X-Upload-Content-Type", "application/gzip")
                .header("X-Upload-Content-Length", bytes.size.toString())
                .post(metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val location = client.newCall(initial).execute().use { response ->
                if (!response.isSuccessful) throw IOException("無法開始雲端備份：HTTP ${response.code}")
                response.header("Location") ?: throw IOException("Google Drive 未回傳上傳位置")
            }
            // Never follow a malicious upload redirect while sending an OAuth token.
            val uri = location.toHttpUrl()
            if (uri.scheme != "https" || (uri.host != "www.googleapis.com" && !uri.host.endsWith(".googleapis.com"))) {
                throw IOException("Google Drive 回傳不安全的上傳網址")
            }
            val completed = responseJson(
                authenticated(token, location)
                    .put(bytes.toRequestBody("application/gzip".toMediaType()))
                    .build()
            )
            val remoteMd5 = completed.optString("md5Checksum")
            val actual = md5(bytes)
            if (remoteMd5.isBlank() || !remoteMd5.equals(actual, ignoreCase = true)) {
                throw IOException("備份上傳後完整性驗證失敗，已停止清理舊版本")
            }
            DriveBackupFile(completed.getString("id"), filename, completed.optString("createdTime"), remoteMd5)
        }

    suspend fun download(token: String, file: DriveBackupFile): ByteArray = withContext(Dispatchers.IO) {
        require(file.id.matches(Regex("[A-Za-z0-9_-]+")))
        client.newCall(authenticated(token, "$API/${file.id}?alt=media").get().build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("下載備份失敗：HTTP ${response.code}")
            // Limit both network and decompression payload sizes.
            val bytes = response.body?.byteStream()?.use { it.readNBytes(20 * 1024 * 1024 + 1) } ?: ByteArray(0)
            if (bytes.isEmpty() || bytes.size > 20 * 1024 * 1024) throw IOException("備份檔過大或為空")
            if (file.md5.isNotEmpty() && !md5(bytes).equals(file.md5, ignoreCase = true)) {
                throw IOException("備份校驗失敗，已取消還原")
            }
            bytes
        }
    }

    suspend fun prune(token: String, keep: Int = 7) = withContext(Dispatchers.IO) {
        list(token).drop(keep).forEach { item ->
            require(item.id.matches(Regex("[A-Za-z0-9_-]+")))
            client.newCall(authenticated(token, "$API/${item.id}").delete().build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("清理舊備份失敗：HTTP ${response.code}")
            }
        }
    }

    private fun md5(data: ByteArray): String = MessageDigest.getInstance("MD5")
        .digest(data).joinToString("") { "%02x".format(it) }
}
