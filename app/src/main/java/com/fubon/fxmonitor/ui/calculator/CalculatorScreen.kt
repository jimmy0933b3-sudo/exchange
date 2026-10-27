package com.fubon.fxmonitor.ui.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.ConversionDirection
import com.fubon.fxmonitor.ui.theme.FxInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel, onBackClick: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val isTwdSource = state.direction == ConversionDirection.TWD_TO_FOREIGN
    var menuExpanded by remember { mutableStateOf(false) }
    val currency = state.selectedCurrency

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("換匯試算", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("快速算出換匯金額", style = MaterialTheme.typography.headlineMedium)
            Text(
                "依台北富邦銀行公開牌告匯率試算，金額僅供參考。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("選擇幣別", style = MaterialTheme.typography.titleMedium)
                    Box {
                        OutlinedButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(currency.flagEmoji, fontSize = 23.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("${currency.displayName} · ${currency.name}", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.heightIn(max = 350.dp)
                        ) {
                            CurrencyCode.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.flagEmoji}  ${option.displayName}  (${option.name})") },
                                    onClick = {
                                        viewModel.setCurrency(option)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text("匯率種類", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.selectedRateType == RateType.CASH,
                            onClick = { viewModel.setRateType(RateType.CASH) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("現鈔") }
                        SegmentedButton(
                            selected = state.selectedRateType == RateType.SPOT,
                            onClick = { viewModel.setRateType(RateType.SPOT) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.weight(1f)
                        ) { Text("即期") }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isTwdSource) "台幣 TWD" else "${currency.name} 外幣", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(
                            onClick = {
                                viewModel.setDirection(
                                    if (isTwdSource) ConversionDirection.FOREIGN_TO_TWD else ConversionDirection.TWD_TO_FOREIGN
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("交換方向")
                        }
                    }
                    OutlinedTextField(
                        value = state.inputAmountString,
                        onValueChange = viewModel::setInputAmount,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("輸入金額") },
                        suffix = { Text(if (isTwdSource) "TWD" else currency.name) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(15.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(FxInk, Color(0xFF165568))),
                    RoundedCornerShape(24.dp)
                ).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color(0xFFA3EBD9))
                    Spacer(Modifier.width(8.dp))
                    Text("預估可兌換", style = MaterialTheme.typography.titleMedium, color = Color(0xFFCCDFEA))
                }
                val result = state.conversionResult
                Text(
                    result?.outputAmount?.toPlainString() ?: "尚無試算結果",
                    style = if (result == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Text(
                    if (isTwdSource) currency.name else "TWD",
                    color = Color(0xFF8CE2D2),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                if (result != null) {
                    Text(
                        "匯率：${result.rateUsed} · ${if (result.rateType == RateType.SPOT) "即期" else "現鈔"}${if (result.bankSide == BankSide.SELL) "賣出" else "買入"}",
                        color = Color(0xFFE2EEF4),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("牌告：${result.officialUpdateTime}", color = Color(0xFFB9CDDB), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "請輸入有效金額。若銀行未提供此幣別的現鈔報價，可切換即期匯率。",
                        color = Color(0xFFCAD9E1), style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    state.conversionResult?.disclaimer ?: "實際成交價格、手續費及額度，請以銀行辦理當時條件為準。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
