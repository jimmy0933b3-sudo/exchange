package com.fubon.fxmonitor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fubon.fxmonitor.data.local.WidgetDisplayMode
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: WidgetSettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Widget 設定已儲存並更新桌面看板！")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("桌面 Widget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Widget Selection Section
            item {
                Text("自訂桌面匯率看板", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text("選擇 Widget 並調整預覽與顯示方式", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("選擇編輯的 Widget", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("選擇 Widget Instance:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.activeWidgetIds.forEachIndexed { index, id ->
                                FilterChip(
                                    selected = uiState.selectedWidgetId == id,
                                    onClick = { viewModel.selectWidget(id) },
                                    label = { Text("Widget #${index + 1}") }
                                )
                            }
                        }
                    }
                }
            }

            // Widget Size Guidance Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Widget 尺寸規格說明",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 1x1 迷你看板：顯示單一主幣別完整買賣匯率（建議首選 USD 或 JPY）。\n" +
                                   "• 3x1 中型看板：橫向 3 格，可同時清晰對比 2 個幣別（如 USD / JPY）。\n" +
                                   "• 大型看板：完整顯示多個自選幣別之即時牌告。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Display Mode Section
            item {
                Text("顯示方式設定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("選擇幣別顯示方式:", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.displayMode == WidgetDisplayMode.FLAG_AND_CODE,
                                onClick = { viewModel.setDisplayMode(WidgetDisplayMode.FLAG_AND_CODE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("國旗 + 幣別")
                            }
                            SegmentedButton(
                                selected = uiState.displayMode == WidgetDisplayMode.FLAG_ONLY,
                                onClick = { viewModel.setDisplayMode(WidgetDisplayMode.FLAG_ONLY) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("僅顯示國旗")
                            }
                        }
                    }
                }
            }

            // Font Size Adjustment Section
            item {
                Text("字體大小調整", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("字體大小:", fontWeight = FontWeight.Medium)
                            Text("${uiState.fontSizeScale}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.fontSizeScale.toFloat(),
                            onValueChange = { viewModel.setFontSizeScale(it.toInt()) },
                            valueRange = 80f..120f,
                            steps = 3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("小 (80%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("標準 (100%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("大 (120%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Rate Type Toggle
            item {
                Text("匯率類型設定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("模式選擇:", fontWeight = FontWeight.Medium)
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = uiState.rateType == RateType.SPOT,
                                onClick = { viewModel.setRateType(RateType.SPOT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("即期匯率")
                            }
                            SegmentedButton(
                                selected = uiState.rateType == RateType.CASH,
                                onClick = { viewModel.setRateType(RateType.CASH) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("現鈔匯率")
                            }
                        }
                    }
                }
            }

            // Background Transparency Section
            item {
                Text("背景透明度設定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("背景透明度:", fontWeight = FontWeight.Medium)
                            Text("${uiState.bgTransparency}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.bgTransparency.toFloat(),
                            onValueChange = { viewModel.setBgTransparency(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 100
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0% (不透明)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("50% (半透明)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("100% (完全透明)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Selected Currencies & Reordering
            item {
                Text("已選擇顯示幣別及排序 (最多 8 個幣別)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            itemsIndexed(uiState.selectedCurrencies) { index, code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${code.flagEmoji} ${code.displayName} (${code.name})", fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.moveCurrencyUp(index) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
                            }
                            IconButton(
                                onClick = { viewModel.moveCurrencyDown(index) },
                                enabled = index < uiState.selectedCurrencies.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
                            }
                            IconButton(
                                onClick = { viewModel.toggleCurrency(code) },
                                enabled = uiState.selectedCurrencies.size > 1
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "移除", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Available Currencies Selection
            item {
                Text("新增 / 移除 幣別", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyCode.entries.forEach { code ->
                                val isSelected = uiState.selectedCurrencies.contains(code)
                                val canSelect = isSelected || uiState.selectedCurrencies.size < 8
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { if (canSelect) viewModel.toggleCurrency(code) },
                                    enabled = canSelect,
                                    label = { Text("${code.flagEmoji} ${code.name}") }
                                )
                            }
                        }
                    }
                }
            }

            // Live Board Preview
            item {
                Text("Widget 預覽效果", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                if (uiState.selectedWidgetId == "no_widget") {
                    Text("請先新增 Widget 至桌面", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    WidgetPreviewCard(
                        rateType = uiState.rateType,
                        displayMode = uiState.displayMode,
                        fontSizeScale = uiState.fontSizeScale,
                        selectedCurrencies = uiState.selectedCurrencies,
                        bgTransparency = uiState.bgTransparency,
                        rates = uiState.availableRates,
                        widgetType = uiState.widgetTypes[uiState.selectedWidgetId] ?: "Small"
                    )
                }
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveConfig() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("儲存設定並更新 Widget", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewCard(
    rateType: RateType,
    displayMode: WidgetDisplayMode,
    fontSizeScale: Int,
    selectedCurrencies: List<CurrencyCode>,
    bgTransparency: Int,
    rates: List<ExchangeRate>,
    widgetType: String
) {
    val isSmall = widgetType == "Small"
    val isMedium = widgetType == "Medium"

    val maxCurrencies = if (isSmall) 2 else if (isMedium) 2 else 4
    val displayCurrencies = selectedCurrencies.take(maxCurrencies).ifEmpty { listOf(CurrencyCode.USD) }

    val previewWidth = if (isSmall) 80.dp else if (isMedium) 240.dp else 280.dp

    val alphaFloat = (100 - bgTransparency.coerceIn(0, 100)) / 100f
    val baseBgColor = Color(0xFF17212F)
    val cardBgColor = baseBgColor.copy(alpha = alphaFloat)

    val scale = fontSizeScale.coerceIn(80, 120) / 100f

    // Wallpaper simulation background box to show transparency effect
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF334155), shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(previewWidth)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isSmall && displayCurrencies.size == 1) {
                    val code = displayCurrencies.first()
                    val rate = rates.find { it.currency == code }
                    val buyVal = if (rateType == RateType.SPOT) rate?.spotBuy else rate?.cashBuy
                    val sellVal = if (rateType == RateType.SPOT) rate?.spotSell else rate?.cashSell

                    val buyStr = if (rate == null) "尚無" else (buyVal?.toPlainString() ?: "未提供")
                    val sellStr = if (rate == null) "尚無" else (sellVal?.toPlainString() ?: "未提供")

                    val timeStr = rate?.officialUpdateTime?.let { extractTimeFromOfficial(it) } ?: "--:--"

                    val titleText = if (displayMode == WidgetDisplayMode.FLAG_ONLY) code.flagEmoji else "${code.flagEmoji} ${code.name}"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = titleText,
                            color = Color(0xFFF8FAFC),
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeStr,
                            color = Color(0xFF94A3B8),
                            fontSize = (9 * scale).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "買 ",
                            color = Color(0xFF94A3B8),
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = buyStr,
                            color = Color(0xFFE2E8F0),
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "賣 ",
                            color = Color(0xFF94A3B8),
                            fontSize = (10 * scale).sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = sellStr,
                            color = Color(0xFF38BDF8),
                            fontSize = (12 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    displayCurrencies.forEachIndexed { index, code ->
                        val rate = rates.find { it.currency == code }
                        val buyVal = if (rateType == RateType.SPOT) rate?.spotBuy else rate?.cashBuy
                        val sellVal = if (rateType == RateType.SPOT) rate?.spotSell else rate?.cashSell

                        val buyStr = if (rate == null) "尚無" else (buyVal?.toPlainString() ?: "未提供")
                        val sellStr = if (rate == null) "尚無" else (sellVal?.toPlainString() ?: "未提供")

                        val timeStr = rate?.officialUpdateTime?.let { extractTimeFromOfficial(it) } ?: "--:--"

                        val currencyText = if (displayMode == WidgetDisplayMode.FLAG_ONLY) code.flagEmoji else "${code.flagEmoji} ${code.name}"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currencyText,
                                color = Color(0xFFF8FAFC),
                                fontSize = (11 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(if (isSmall) 1.5f else 2.2f)
                            )
                            Text(
                                text = buyStr,
                                color = Color(0xFFE2E8F0),
                                fontSize = (11 * scale).sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(if (isSmall) 1.5f else 2.2f)
                            )
                            Text(
                                text = sellStr,
                                color = Color(0xFF38BDF8),
                                fontSize = (11 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(if (isSmall) 1.5f else 2.2f)
                            )
                            if (!isSmall) {
                                Text(
                                    text = if (index == 0) timeStr else "",
                                    color = Color(0xFF94A3B8),
                                    fontSize = (9 * scale).sp,
                                    modifier = Modifier.weight(1.8f)
                                )
                            }
                        }
                        if (index < displayCurrencies.size - 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun extractTimeFromOfficial(officialTimeStr: String?): String {
    if (officialTimeStr.isNullOrBlank()) {
        return SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())
    }
    val match = Regex("""\d{2}:\d{2}""").find(officialTimeStr)
    return match?.value ?: SimpleDateFormat("HH:mm", Locale.TAIWAN).format(Date())
}
