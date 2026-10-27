package com.fubon.fxmonitor.ui.alert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.data.local.ExchangeAlertEntity
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.TriggerCondition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(viewModel: AlertViewModel, onBackClick: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("到價提醒", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    text = { Text("新增提醒") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { viewModel.openAddDialog() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text("關注每一次匯率變化", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(5.dp))
                Text(
                    "設定理想價位，符合條件時以通知提醒你。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("提醒條件 (${state.alerts.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("觸發紀錄 (${state.historyLogs.size})") }
                )
            }
            if (selectedTab == 0) {
                if (state.alerts.isEmpty()) {
                    AlertEmptyState(
                        "還沒有到價提醒",
                        "點選「新增提醒」，設定想關注的幣別與目標價。",
                        Icons.Default.NotificationsNone
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 15.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.alerts, key = { it.id }) { alert ->
                            AlertCard(
                                alert = alert,
                                onToggleEnabled = { viewModel.toggleAlertEnabled(alert, it) },
                                onDelete = { pendingDelete = alert.id }
                            )
                        }
                    }
                }
            } else {
                if (state.historyLogs.isEmpty()) {
                    AlertEmptyState("尚無觸發紀錄", "提醒達到條件後，紀錄會顯示在這裡。", Icons.Default.History)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.historyLogs, key = { it.id }) { log ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${log.currencyCode} · 到價通知", style = MaterialTheme.typography.titleMedium)
                                    Text("觸發匯率：${log.triggeredRate}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(log.conditionDescription, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN).format(Date(log.triggeredTime)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddAlertDialog(
            uiState = state,
            onDismiss = viewModel::closeAddDialog,
            onConfirm = viewModel::saveAlert,
            onCurrencyChange = viewModel::updateSelectedCurrency,
            onRateTypeChange = viewModel::updateSelectedRateType,
            onBankSideChange = viewModel::updateSelectedBankSide,
            onConditionChange = viewModel::updateSelectedCondition,
            onValueChange = viewModel::updateTargetValue
        )
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text("刪除這個提醒？") },
            text = { Text("提醒條件將被移除，這個動作無法復原。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAlert(id); pendingDelete = null }) { Text("刪除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun AlertEmptyState(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AlertCard(
    alert: ExchangeAlertEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val currency = CurrencyCode.fromString(alert.currencyCode) ?: CurrencyCode.USD
    val condition = when (TriggerCondition.fromString(alert.triggerCondition)) {
        TriggerCondition.GREATER_EQUAL -> "高於或等於 ${alert.targetValue}"
        TriggerCondition.LESS_EQUAL -> "低於或等於 ${alert.targetValue}"
        TriggerCondition.SINGLE_CHANGE_PCT -> "單次漲跌 ≥ ${alert.targetValue}%"
        TriggerCondition.DAILY_CHANGE_PCT -> "每日漲跌 ≥ ${alert.targetValue}%"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currency.flagEmoji, fontSize = 28.sp)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("${currency.displayName} · ${currency.name}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${if (alert.rateType == "SPOT") "即期" else "現鈔"} / ${if (alert.bankSide == "BUY") "銀行買入" else "銀行賣出"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = alert.isEnabled, onCheckedChange = onToggleEnabled)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            ) {
                Text(
                    condition,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (alert.isEnabled) "提醒已啟用" else "提醒已暫停",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("刪除")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertDialog(
    uiState: AlertUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCurrencyChange: (CurrencyCode) -> Unit,
    onRateTypeChange: (RateType) -> Unit,
    onBankSideChange: (BankSide) -> Unit,
    onConditionChange: (TriggerCondition) -> Unit,
    onValueChange: (String) -> Unit
) {
    var currencyMenu by remember { mutableStateOf(false) }
    val validValue = uiState.targetValueInput.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("新增到價提醒", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("選擇幣別", style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(onClick = { currencyMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${uiState.selectedCurrency.flagEmoji} ${uiState.selectedCurrency.displayName} (${uiState.selectedCurrency.name})")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = currencyMenu,
                        onDismissRequest = { currencyMenu = false },
                        modifier = Modifier.heightIn(max = 290.dp)
                    ) {
                        CurrencyCode.entries.forEach { code ->
                            DropdownMenuItem(
                                text = { Text("${code.flagEmoji}  ${code.displayName}  ${code.name}") },
                                onClick = { onCurrencyChange(code); currencyMenu = false }
                            )
                        }
                    }
                }
                Text("匯率類型", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.selectedRateType == RateType.SPOT,
                        onClick = { onRateTypeChange(RateType.SPOT) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.weight(1f)
                    ) { Text("即期") }
                    SegmentedButton(
                        selected = uiState.selectedRateType == RateType.CASH,
                        onClick = { onRateTypeChange(RateType.CASH) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.weight(1f)
                    ) { Text("現鈔") }
                }
                Text("銀行報價方向", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.selectedBankSide == BankSide.BUY,
                        onClick = { onBankSideChange(BankSide.BUY) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.weight(1f)
                    ) { Text("銀行買入") }
                    SegmentedButton(
                        selected = uiState.selectedBankSide == BankSide.SELL,
                        onClick = { onBankSideChange(BankSide.SELL) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.weight(1f)
                    ) { Text("銀行賣出") }
                }
                Text("觸發條件", style = MaterialTheme.typography.titleSmall)
                listOf(
                    TriggerCondition.GREATER_EQUAL to "大於或等於目標匯率",
                    TriggerCondition.LESS_EQUAL to "小於或等於目標匯率",
                    TriggerCondition.SINGLE_CHANGE_PCT to "單次漲跌幅 (%)",
                    TriggerCondition.DAILY_CHANGE_PCT to "每日漲跌幅 (%)"
                ).forEach { (condition, title) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.selectedCondition == condition,
                            onClick = { onConditionChange(condition) }
                        )
                        Text(title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = uiState.targetValueInput,
                    onValueChange = onValueChange,
                    label = { Text("目標匯率 / 百分比") },
                    isError = uiState.targetValueInput.isNotBlank() && !validValue,
                    supportingText = {
                        if (uiState.targetValueInput.isNotBlank() && !validValue) Text("請輸入大於 0 的數字")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = validValue) { Text("儲存提醒") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
