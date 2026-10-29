package com.fubon.fxmonitor.backup

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private tailrec fun Context.activity(): Activity? =
    when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupScreen(manager: CloudBackupManager, drive: DriveBackupClient, onBackClick: () -> Unit) {
    val state by manager.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var restoring by remember { mutableStateOf<DriveBackupFile?>(null) }
    fun notify(message: String) { scope.launch { snack.showSnackbar(message) } }

    val grant = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { permission ->
                runCatching { manager.acceptAuthorization(permission) }
                    .onSuccess { notify("雲端備份已啟用") }
                    .onFailure { notify(it.message ?: "Drive 授權失敗") }
            }.onFailure { notify(it.message ?: "Drive 授權失敗") }
        } else notify("已取消 Google Drive 授權")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("雲端備份", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("換手機，歷史資料也不遺失", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "使用自己的 Google Drive 私有應用程式空間，保存匯率歷史、收藏及到價提醒。未登入也能照常使用匯率功能。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                BackupPanel("Google 帳戶", Icons.Default.AccountCircle) {
                    if (!manager.configured()) {
                        Text("尚未設定 Google OAuth。請由開發者在 GitHub Actions 設定 GOOGLE_WEB_CLIENT_ID 後重新建置 APK。",
                            color = MaterialTheme.colorScheme.error)
                    } else if (state.email == null) {
                        Text("尚未登入", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val activity = context.activity() ?: error("無法取得登入畫面")
                                        val option = GetSignInWithGoogleOption.Builder(
                                            com.fubon.fxmonitor.BuildConfig.GOOGLE_WEB_CLIENT_ID
                                        ).build()
                                        val credential = CredentialManager.create(activity).getCredential(
                                            context = activity,
                                            request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                                        ).credential
                                        if (credential !is CustomCredential ||
                                            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            error("Google 帳戶格式不正確")
                                        }
                                        val google = GoogleIdTokenCredential.createFrom(credential.data)
                                        manager.signIn(google.id)
                                        notify("已連結 ${google.id}")
                                    } catch (e: Exception) {
                                        notify(e.localizedMessage ?: "Google 登入取消或失敗")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Default.Login, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("使用 Google 帳戶登入") }
                    } else {
                        Text(state.email.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                                manager.signOut()
                                runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
                                notify("已登出。雲端備份仍保留。")
                            }
                        }) { Text("登出") }
                    }
                }
            }
            if (state.email != null) {
                item {
                    BackupPanel("備份設定", Icons.Default.CloudSync) {
                        if (!state.authorized) {
                            Text("請先授權 Drive 應用程式資料，才能啟用定期備份。", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val result = drive.authorize(state.email!!)
                                        if (result.hasResolution()) {
                                            val pending = result.pendingIntent ?: error("無法開啟 Drive 授權")
                                            grant.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                                        } else {
                                            manager.acceptAuthorization(result)
                                            notify("Google Drive 已連結，每日自動備份已啟用")
                                        }
                                    } catch (e: Exception) { notify(e.message ?: "授權失敗") }
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("啟用 Google Drive 備份") }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("每天自動備份", style = MaterialTheme.typography.titleSmall)
                                    Text("依 Android 排程執行，不保證固定鐘點", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = state.enabled, onCheckedChange = manager::setAutoEnabled)
                            }
                            HorizontalDivider(Modifier.padding(vertical = 9.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("僅限 Wi-Fi 備份", modifier = Modifier.weight(1f))
                                Switch(checked = state.wifiOnly, onCheckedChange = manager::setWiFiOnly)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "最近成功備份：" + if (state.lastBackupTime == 0L) "尚無" else
                                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN).format(Date(state.lastBackupTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.lastError?.let { Text("上次錯誤：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (state.authorized) {
                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            onClick = {
                                scope.launch {
                                    try {
                                        manager.backupNow()
                                        notify("備份成功，已儲存到 Google Drive")
                                    } catch (e: Exception) {
                                        manager.reportError(e.message ?: "備份失敗")
                                        notify(e.message ?: "備份失敗")
                                    }
                                }
                            }
                        ) {
                            if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("立即備份")
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("雲端備份版本", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { manager.refreshFiles() }
                                        .onFailure { notify(it.message ?: "讀取備份列表失敗") }
                                }
                            }) { Text("重新整理") }
                        }
                    }
                    items(state.backups, key = { it.id }) { file ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name.removePrefix("exchange-backup-").removeSuffix(".json.gz"),
                                        style = MaterialTheme.typography.titleSmall)
                                    Text("私有雲端備份", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(enabled = !state.busy, onClick = { restoring = file }) { Text("還原") }
                            }
                        }
                    }
                    if (state.backups.isEmpty()) {
                        item {
                            Text("目前尚無備份。點選「立即備份」建立第一個版本。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text("備份保存在 Google Drive 隱藏的應用程式資料空間。還原採合併方式，不會清空手機現有紀錄；背景授權失效時需重新開啟此頁授權。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    restoring?.let { file ->
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text("合併還原雲端資料？") },
            text = { Text("將合併歷史匯率、收藏及提醒條件；同一筆資料不會重複新增。現有資料不會刪除。") },
            confirmButton = {
                Button(onClick = {
                    restoring = null
                    scope.launch {
                        try {
                            val count = manager.restore(file)
                            notify("還原完成，已處理 $count 筆紀錄")
                        } catch (e: Exception) { notify(e.message ?: "還原失敗") }
                    }
                }) { Text("確認還原") }
            },
            dismissButton = { TextButton(onClick = { restoring = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun BackupPanel(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
