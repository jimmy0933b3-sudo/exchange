package com.fubon.fxmonitor.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.fubon.fxmonitor.ui.theme.FxInk
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.data.local.RateHistoryEntity
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.RateType
import java.math.BigDecimal
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onAddAlertClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isHistoryListExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${uiState.currencyCode.displayName} · ${uiState.currencyCode.name}", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onAddAlertClick(uiState.currencyCode.name) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "設定提醒")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clear hierarchy: the selected quote is the focal point.
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(FxInk, Color(0xFF155B65))),
                        RoundedCornerShape(24.dp)
                    ).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.currencyCode.flagEmoji, fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${uiState.currencyCode.displayName} · ${uiState.currencyCode.name}",
                            style = MaterialTheme.typography.titleMedium, color = Color.White
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "目前報價 · ${if (uiState.selectedRateType == RateType.SPOT) "即期" else "現鈔"} / ${if (uiState.selectedBankSide == BankSide.BUY) "銀行買入" else "銀行賣出"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFB6DAD8)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uiState.currentPrice,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("台北富邦銀行公開匯率", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC2D5DF))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(21.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("匯率類型", style = MaterialTheme.typography.titleSmall)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.selectedRateType == RateType.SPOT,
                                onClick = { viewModel.setRateType(RateType.SPOT) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.weight(1f)
                            ) { Text("即期") }
                            SegmentedButton(
                                selected = uiState.selectedRateType == RateType.CASH,
                                onClick = { viewModel.setRateType(RateType.CASH) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.weight(1f)
                            ) { Text("現鈔") }
                        }
                        Text("報價方向", style = MaterialTheme.typography.titleSmall)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.selectedBankSide == BankSide.BUY,
                                onClick = { viewModel.setBankSide(BankSide.BUY) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.weight(1f)
                            ) { Text("銀行買入") }
                            SegmentedButton(
                                selected = uiState.selectedBankSide == BankSide.SELL,
                                onClick = { viewModel.setBankSide(BankSide.SELL) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.weight(1f)
                            ) { Text("銀行賣出") }
                        }
                    }
                }
            }

            // Time range selector
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "歷史匯率趨勢",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TimeRange.entries) { range ->
                            FilterChip(
                                selected = uiState.selectedTimeRange == range,
                                onClick = { viewModel.setTimeRange(range) },
                                label = { Text(range.label, fontWeight = if (uiState.selectedTimeRange == range) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }
            }

            // Trend Chart Canvas Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        RateTrendChart(
                            history = uiState.chartHistory,
                            rateType = uiState.selectedRateType,
                            bankSide = uiState.selectedBankSide,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Statistics Grid (區間統計表)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(21.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "區間匯率統計分析",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(title = "區間最高", value = uiState.highPrice, modifier = Modifier.weight(1f))
                            StatCard(title = "區間最低", value = uiState.lowPrice, modifier = Modifier.weight(1f))
                            StatCard(title = "區間平均", value = uiState.avgPrice, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPositive = uiState.changePct.startsWith("+")
                            val isNegative = uiState.changePct.startsWith("-")
                            val changeColor = when {
                                isPositive -> Color(0xFFD32F2F) // Red for rise in TW standard
                                isNegative -> Color(0xFF388E3C) // Green for drop
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("區間變動金額", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.changeAmount,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = changeColor
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("區間漲跌幅", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.changePct,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = changeColor
                                    )
                                }
                            }
                        }

                        if (uiState.comparisonDateRange.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.comparisonDateRange,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Data Coverage / Notice Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "提示",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.dataCoverageNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Historical Price Detail List Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHistoryListExpanded = !isHistoryListExpanded },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "歷史價格明細 (共 ${uiState.history.size} 筆)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isHistoryListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isHistoryListExpanded) "收起" else "展開"
                        )
                    }
                }
            }

            // Historical Price Detail Items
            if (isHistoryListExpanded) {
                if (uiState.history.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "選取時間區間內無歷史資料",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("日期與時間", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                                    Text(
                                        if (uiState.selectedRateType == RateType.SPOT) "即期買入" else "現鈔買入",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (uiState.selectedRateType == RateType.SPOT) "即期賣出" else "現鈔賣出",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Table rows
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    uiState.history.forEach { item ->
                                        val buyStr = if (uiState.selectedRateType == RateType.SPOT) {
                                            item.spotBuy ?: "N/A"
                                        } else {
                                            item.cashBuy ?: "N/A"
                                        }
                                        val sellStr = if (uiState.selectedRateType == RateType.SPOT) {
                                            item.spotSell ?: "N/A"
                                        } else {
                                            item.cashSell ?: "N/A"
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp, horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1.8f)) {
                                                Text(
                                                    text = item.officialUpdateTime,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = item.source,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = buyStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (uiState.selectedBankSide == BankSide.BUY) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.selectedBankSide == BankSide.BUY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = sellStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (uiState.selectedBankSide == BankSide.SELL) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.selectedBankSide == BankSide.SELL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RateTrendChart(
    history: List<RateHistoryEntity>, // Oldest to newest
    rateType: RateType,
    bankSide: BankSide,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 10.sp, color = Color.Gray)

    // Extract valid data points
    val validPoints = remember(history, rateType, bankSide) {
        history.mapNotNull { hist ->
            val str = when (rateType) {
                RateType.SPOT -> if (bankSide == BankSide.BUY) hist.spotBuy else hist.spotSell
                RateType.CASH -> if (bankSide == BankSide.BUY) hist.cashBuy else hist.cashSell
            }
            val value = str?.toFloatOrNull()
            if (value != null) hist to value else null
        }
    }

    var selectedIndex by remember(validPoints) { mutableStateOf<Int?>(null) }

    if (validPoints.size < 2) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (validPoints.isEmpty()) "選取時間與匯率類型無報價資料" else "有效資料過少（僅 1 筆），無法繪製趨勢圖",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "請切換時間範圍或選擇其他匯率種類",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        return
    }

    val minVal = validPoints.minOf { it.second }
    val maxVal = validPoints.maxOf { it.second }
    val range = if (maxVal == minVal) 1f else maxVal - minVal
    val minDisplay = minVal - range * 0.05f
    val maxDisplay = maxVal + range * 0.05f
    val displayRange = maxDisplay - minDisplay

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(validPoints) {
                    detectTapGestures { offset ->
                        val idx = ((offset.x / size.width) * (validPoints.size - 1)).roundToInt()
                            .coerceIn(0, validPoints.size - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(validPoints) {
                    detectDragGestures { change, _ ->
                        val idx = ((change.position.x / size.width) * (validPoints.size - 1)).roundToInt()
                            .coerceIn(0, validPoints.size - 1)
                        selectedIndex = idx
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 24.dp.toPx()
            val availableHeight = height - bottomPadding

            // Draw Y-axis background grid lines (3 lines)
            val gridCount = 3
            for (i in 0..gridCount) {
                val y = availableHeight * (i.toFloat() / gridCount)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val valAtY = maxDisplay - (i.toFloat() / gridCount) * displayRange
                val valStr = String.format("%.4f", valAtY)
                val textLayout = textMeasurer.measure(valStr, textStyle)
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(width - textLayout.size.width - 4.dp.toPx(), y - textLayout.size.height / 2)
                )
            }

            // Draw X-axis date labels (First and Last)
            val firstDate = validPoints.first().first.officialUpdateTime.take(10)
            val lastDate = validPoints.last().first.officialUpdateTime.take(10)

            val firstLayout = textMeasurer.measure(firstDate, textStyle)
            val lastLayout = textMeasurer.measure(lastDate, textStyle)

            drawText(
                textLayoutResult = firstLayout,
                topLeft = Offset(0f, availableHeight + 4.dp.toPx())
            )
            drawText(
                textLayoutResult = lastLayout,
                topLeft = Offset(width - lastLayout.size.width, availableHeight + 4.dp.toPx())
            )

            // Draw trend line
            val path = Path()
            val pointsOnCanvas = validPoints.mapIndexed { index, pair ->
                val x = (index.toFloat() / (validPoints.size - 1)) * width
                val y = availableHeight - ((pair.second - minDisplay) / displayRange) * availableHeight
                Offset(x, y)
            }

            pointsOnCanvas.forEachIndexed { index, offset ->
                if (index == 0) {
                    path.moveTo(offset.x, offset.y)
                } else {
                    path.lineTo(offset.x, offset.y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Draw data point dots
            pointsOnCanvas.forEachIndexed { index, offset ->
                drawCircle(
                    color = primaryColor,
                    radius = 3.dp.toPx(),
                    center = offset
                )
            }

            // Draw interactive highlight selection
            selectedIndex?.let { idx ->
                if (idx in pointsOnCanvas.indices) {
                    val selOffset = pointsOnCanvas[idx]

                    // Draw vertical guide line
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = Offset(selOffset.x, 0f),
                        end = Offset(selOffset.x, availableHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Draw larger highlight circle
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = selOffset
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = selOffset,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        // Overlay tooltip when point selected
        selectedIndex?.let { idx ->
            if (idx in validPoints.indices) {
                val point = validPoints[idx]
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = point.first.officialUpdateTime,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${point.second}",
                            color = MaterialTheme.colorScheme.primaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
