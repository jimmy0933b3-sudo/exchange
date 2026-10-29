package com.fubon.fxmonitor.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onNavigateToWidgetSettings: () -> Unit = {},
    onNavigateToCloudBackup: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val updateText = if (state.lastSuccessTime > 0L)
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN).format(Date(state.lastSuccessTime))
    else "尚未成功更新"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("偏好設定", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text("打造你的匯率看板", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("調整更新頻率、通知方式與桌面小工具。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                SettingsPanel("Google 帳戶與雲端備份", Icons.Default.Cloud) {
                    Text("登入 Google 後，可將歷史匯率、收藏與提醒儲存到 Google Drive，換手機再合併還原。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = onNavigateToCloudBackup, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("登入與管理雲端備份")
                    }
                }
            }

            item {
                SettingsPanel("桌面小工具", Icons.Default.Widgets) {
                    Text("自訂 Widget 顯示幣別、字體及背景透明度。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = onNavigateToWidgetSettings, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("編輯桌面 Widget")
                    }
                }
            }

            item {
                SettingsPanel("資料更新", Icons.Default.Sync) {
                    Text("背景監控頻率", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("系統可能依省電模式調整實際執行時間。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15 to "15 分", 30 to "30 分", 60 to "1 小時").forEach { (minutes, label) ->
                            FilterChip(
                                selected = state.intervalMins == minutes,
                                onClick = { viewModel.setInterval(minutes) },
                                label = { Text(label) }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("最近更新", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(updateText, style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                SettingsPanel("通知與外觀", Icons.Default.Palette) {
                    SettingsToggle("提示音效", "匯率達標時播放通知聲", state.soundEnabled, viewModel::setSoundEnabled)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 7.dp))
                    SettingsToggle("震動提示", "提醒時同步震動", state.vibrationEnabled, viewModel::setVibrationEnabled)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 7.dp))
                    SettingsToggle("深色模式", "切換為深海藍夜間主題", state.darkMode, viewModel::setDarkMode)
                }
            }

            item {
                SettingsPanel("資料與維護", Icons.Default.Storage) {
                    Text("匯率資料取自台北富邦銀行公開網站及無障礙頁面。", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showClearDialog = true }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("清除歷史匯率與錯誤紀錄")
                    }
                    Text(
                        "不會刪除收藏幣別或到價提醒設定。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsPanel("診斷紀錄 · ${state.errorLogs.size}", Icons.Default.BugReport) {
                    if (state.errorLogs.isEmpty()) {
                        Text("目前沒有錯誤紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.errorLogs.take(5).forEachIndexed { index, log ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(log.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除歷史紀錄？") },
            text = { Text("歷史匯率及錯誤紀錄將被清空，無法復原。收藏與提醒不受影響。") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCache(); showClearDialog = false }) { Text("清除紀錄") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
