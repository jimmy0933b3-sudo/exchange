package com.fubon.fxmonitor.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.ui.theme.FxInk
import com.fubon.fxmonitor.ui.theme.FxMint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCurrencyClick: (String) -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.refreshStatus) {
        val status = uiState.refreshStatus
        val message = when (status) {
            is RefreshStatus.Success -> status.message
            is RefreshStatus.NoChange -> status.message
            is RefreshStatus.Error -> status.message
            else -> ""
        }
        if (message.isNotEmpty()) {
            snackbar.showSnackbar(message)
            viewModel.clearRefreshStatus()
        }
    }

    val filtered = uiState.rates.filter {
        query.isBlank() || it.rate.currency.name.contains(query, ignoreCase = true) ||
            it.rate.currency.displayName.contains(query, ignoreCase = true)
    }
    val favorites = filtered.filter { it.isFavorite }
    val others = filtered.filterNot { it.isFavorite }
    val updated = if (uiState.lastUpdateTime > 0L) {
        SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(uiState.lastUpdateTime))
    } else "尚未取得資料"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("我的匯率", fontWeight = FontWeight.Bold)
                        Text(
                            "外幣行情一眼掌握",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }, enabled = !uiState.isRefreshing) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else Icon(Icons.Default.Refresh, contentDescription = "更新匯率")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(FxInk, Color(0xFF194E5B))),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (uiState.isStale) Color(0xFFFFC078) else FxMint, CircleShape)
                        )
                        Text(
                            if (uiState.isStale) "資料可能過期" else if (uiState.rates.isEmpty()) "等待匯率資料" else "匯率資料已就緒",
                            color = Color(0xFFB5EFE3),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("台北富邦銀行・公開牌告", color = Color(0xFFC6D6E1), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (uiState.rates.isEmpty()) "即時匯率看板" else "${uiState.rates.size} 種外幣匯率",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFB5CFDA), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("上次查詢：$updated", color = Color(0xFFD9E8EF), style = MaterialTheme.typography.bodySmall)
                    }
                    if (uiState.isStale) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "目前顯示上次成功取得的資料，請確認網路後重新整理。",
                            color = Color(0xFFFFD8A0),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeShortcut("換匯試算", Icons.Default.Calculate, onNavigateToCalculator, Modifier.weight(1f))
                    HomeShortcut("到價提醒", Icons.Default.NotificationsActive, onNavigateToAlerts, Modifier.weight(1f))
                    HomeShortcut("個人設定", Icons.Default.Tune, onNavigateToSettings, Modifier.weight(1f))
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("匯率類型", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "銀行買入 / 賣出",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = uiState.rateType == RateType.SPOT,
                            onClick = { viewModel.toggleRateType(RateType.SPOT) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("即期匯率") }
                        SegmentedButton(
                            selected = uiState.rateType == RateType.CASH,
                            onClick = { viewModel.toggleRateType(RateType.CASH) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("現鈔匯率") }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜尋幣別，例如 USD、日圓") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除搜尋")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (favorites.isNotEmpty()) {
                item { FxListHeading("我的收藏", "${favorites.size} 個幣別", Icons.Default.Star) }
                items(favorites, key = { "fav_${it.rate.currency.name}" }) { card ->
                    CurrencyCard(
                        cardState = card,
                        selectedType = uiState.rateType,
                        onCardClick = { onCurrencyClick(card.rate.currency.name) },
                        onFavoriteToggle = { viewModel.toggleFavorite(card.rate.currency, !card.isFavorite) }
                    )
                }
            }
            if (others.isNotEmpty()) {
                item { FxListHeading("其他幣別", "${others.size} 個幣別", Icons.Default.Language) }
                items(others, key = { "other_${it.rate.currency.name}" }) { card ->
                    CurrencyCard(
                        cardState = card,
                        selectedType = uiState.rateType,
                        onCardClick = { onCurrencyClick(card.rate.currency.name) },
                        onFavoriteToggle = { viewModel.toggleFavorite(card.rate.currency, !card.isFavorite) }
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                        Text(if (query.isBlank()) "目前還沒有匯率資料" else "沒有符合「$query」的幣別", style = MaterialTheme.typography.titleMedium)
                        Text("請重新整理或試試其他關鍵字", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { if (query.isNotEmpty()) query = "" else viewModel.refreshData() }) {
                            Text(if (query.isNotEmpty()) "清除搜尋" else "更新匯率")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun FxListHeading(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text(count, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CurrencyCard(
    cardState: CurrencyCardUiState,
    selectedType: RateType,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val rate = cardState.rate
    val buy = if (selectedType == RateType.SPOT) rate.spotBuy else rate.cashBuy
    val sell = if (selectedType == RateType.SPOT) rate.spotSell else rate.cashSell
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(45.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rate.currency.flagEmoji, fontSize = 25.sp)
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(rate.currency.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        rate.currency.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (cardState.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (cardState.isFavorite) "取消收藏" else "加入收藏",
                        tint = if (cardState.isFavorite) Color(0xFFE0A142) else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RateBox("銀行買入", buy?.toPlainString() ?: "未提供", modifier = Modifier.weight(1f))
                RateBox("銀行賣出", sell?.toPlainString() ?: "未提供", isHighlighted = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "牌告 ${rate.officialUpdateTime.ifBlank { "尚無資料" }}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ChevronRight, contentDescription = "查看詳情", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
fun RateBox(title: String, value: String, isHighlighted: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(
            if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            RoundedCornerShape(14.dp)
        ).padding(horizontal = 12.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = if (value.length > 9) 15.sp else if (value == "未提供") 16.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}
