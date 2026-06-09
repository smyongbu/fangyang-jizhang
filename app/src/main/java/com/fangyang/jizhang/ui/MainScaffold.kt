package com.fangyang.jizhang.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fangyang.jizhang.ui.query.QueryScreen
import com.fangyang.jizhang.ui.record.RecordFormScreen
import com.fangyang.jizhang.ui.scan.ScannerScreen
import com.fangyang.jizhang.ui.settings.SettingsScreen

private object Routes {
    const val SCAN = "scan"
    const val FORM = "form"
    const val QUERY = "query"
    const val SETTINGS = "settings"
}

@Composable
fun MainScaffold(viewModel: ProductViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val onRecordTab = route == Routes.SCAN || route == Routes.FORM

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = onRecordTab,
                    onClick = {
                        navController.navigate(Routes.SCAN) {
                            popUpTo(Routes.SCAN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text("记录") },
                )
                NavigationBarItem(
                    selected = route == Routes.QUERY,
                    onClick = {
                        navController.navigate(Routes.QUERY) {
                            popUpTo(Routes.SCAN)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("查询") },
                )
                NavigationBarItem(
                    selected = route == Routes.SETTINGS,
                    onClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(Routes.SCAN)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SCAN,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SCAN) {
                ScannerScreen(onBarcodeScanned = { code ->
                    viewModel.startForm(code)
                    navController.navigate(Routes.FORM)
                })
            }
            composable(Routes.FORM) {
                RecordFormScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    // 保存后回到来的页面（扫码继续扫，或查询页）
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.QUERY) {
                QueryScreen(
                    viewModel = viewModel,
                    onEditRecord = { record ->
                        viewModel.startEdit(record)
                        navController.navigate(Routes.FORM)
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel)
            }
        }
    }
}
