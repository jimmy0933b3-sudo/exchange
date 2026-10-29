package com.fubon.fxmonitor

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.fubon.fxmonitor.data.local.UserPreferencesRepository
import com.fubon.fxmonitor.backup.CloudBackupManager
import com.fubon.fxmonitor.backup.DriveBackupClient
import com.fubon.fxmonitor.backup.CloudBackupScreen
import com.fubon.fxmonitor.ui.alert.AlertScreen
import com.fubon.fxmonitor.ui.alert.AlertViewModel
import com.fubon.fxmonitor.ui.calculator.CalculatorScreen
import com.fubon.fxmonitor.ui.calculator.CalculatorViewModel
import com.fubon.fxmonitor.ui.detail.DetailScreen
import com.fubon.fxmonitor.ui.detail.DetailViewModel
import com.fubon.fxmonitor.ui.home.HomeScreen
import com.fubon.fxmonitor.ui.home.HomeViewModel
import com.fubon.fxmonitor.ui.settings.SettingsScreen
import com.fubon.fxmonitor.ui.settings.SettingsViewModel
import com.fubon.fxmonitor.ui.settings.WidgetSettingsScreen
import com.fubon.fxmonitor.ui.settings.WidgetSettingsViewModel
import com.fubon.fxmonitor.ui.theme.FubonFXMonitorTheme
import com.fubon.fxmonitor.worker.ExchangeMonitorWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPrefs: UserPreferencesRepository

    @Inject lateinit var backupManager: CloudBackupManager
    @Inject lateinit var driveBackupClient: DriveBackupClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "需允許通知權限才能接收到價提醒！", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule periodic background worker
        lifecycleScope.launchWhenCreated {
            val interval = userPrefs.backgroundIntervalMins.first()
            ExchangeMonitorWorker.schedule(this@MainActivity, interval.toLong())
        }

        val targetCurrency = intent?.getStringExtra("target_currency")

        setContent {
            val darkMode by userPrefs.darkMode.collectAsState(initial = isSystemInDarkTheme())

            FubonFXMonitorTheme(darkTheme = darkMode) {
                AppNavigation(startTargetCurrency = targetCurrency, backupManager = backupManager, driveBackupClient = driveBackupClient)
            }
        }
    }
}

@Composable
fun AppNavigation(startTargetCurrency: String?, backupManager: CloudBackupManager, driveBackupClient: DriveBackupClient) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabs = listOf("home", "calculator", "alerts", "settings")
    val selected = tabs.indexOf(currentRoute)
    val start = if (startTargetCurrency != null) "detail/$startTargetCurrency" else "home"

    // Navigation appears on primary destinations; detail, create-alert, and
    // widget configuration are focused tasks with their own Back control.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selected >= 0) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    listOf(
                        Triple("home", "首頁", Icons.Default.Home),
                        Triple("calculator", "換匯", Icons.Default.Calculate),
                        Triple("alerts", "提醒", Icons.Default.NotificationsNone),
                        Triple("settings", "設定", Icons.Default.Tune)
                    ).forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { navPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(navPadding)) {
            NavHost(navController = navController, startDestination = start) {
                composable("home") {
                    val viewModel: HomeViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onCurrencyClick = { code -> navController.navigate("detail/$code") },
                        onNavigateToAlerts = { navController.navigate("alerts") },
                        onNavigateToCalculator = { navController.navigate("calculator") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable(
                    route = "detail/{currency}",
                    arguments = listOf(navArgument("currency") { type = NavType.StringType })
                ) {
                    val viewModel: DetailViewModel = hiltViewModel()
                    DetailScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onAddAlertClick = { code -> navController.navigate("alerts_add/$code") }
                    )
                }
                composable("alerts") {
                    val viewModel: AlertViewModel = hiltViewModel()
                    AlertScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
                }
                composable(
                    route = "alerts_add/{currency}",
                    arguments = listOf(navArgument("currency") { type = NavType.StringType })
                ) { backStack ->
                    val currencyCode = backStack.arguments?.getString("currency")
                    val viewModel: AlertViewModel = hiltViewModel()
                    LaunchedEffect(currencyCode) { viewModel.openAddDialog(currencyCode) }
                    AlertScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
                }
                composable("calculator") {
                    val viewModel: CalculatorViewModel = hiltViewModel()
                    CalculatorScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
                }
                composable("settings") {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToWidgetSettings = { navController.navigate("widget_settings") },
                        onNavigateToCloudBackup = { navController.navigate("cloud_backup") }
                    )
                }
                composable("cloud_backup") {
                    CloudBackupScreen(manager = backupManager, drive = driveBackupClient, onBackClick = { navController.popBackStack() })
                }
                composable("widget_settings") {
                    val viewModel: WidgetSettingsViewModel = hiltViewModel()
                    WidgetSettingsScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
