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
import com.fangyang.jizhang.ui.settings.BindingEditScreen
import com.fangyang.jizhang.ui.settings.SettingsScreen

private object Routes {
    const val RECORDS = "records"     // 记录（列表，默认首页）
    const val SCAN = "scan"           // 扫码（中间）
    const val FORM = "form"
    const val SETTINGS = "settings"
    const val SETTINGS_BINDINGS = "settings_bindings"
}

@Composable
fun MainScaffold(viewModel: ProductViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                // 第一个：记录（列表）
                NavigationBarItem(
                    selected = route == Routes.RECORDS,
                    onClick = {
                        navController.navigate(Routes.RECORDS) {
                            popUpTo(Routes.RECORDS) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("记录") },
                )
                // 中间：扫码
                NavigationBarItem(
                    selected = route == Routes.SCAN || route == Routes.FORM,
                    onClick = {
                        navController.navigate(Routes.SCAN) {
                            popUpTo(Routes.RECORDS)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text("扫码") },
                )
                // 第三个：设置
                NavigationBarItem(
                    selected = route == Routes.SETTINGS || route == Routes.SETTINGS_BINDINGS,
                    onClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(Routes.RECORDS)
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
            startDestination = Routes.RECORDS,   // 打开 app 进入记录页面
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.RECORDS) {
                QueryScreen(
                    viewModel = viewModel,
                    onEditRecord = { record ->
                        viewModel.startEdit(record)
                        navController.navigate(Routes.FORM)
                    },
                )
            }
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
                    // 保存后回到来的页面（扫码继续扫，或记录列表）
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenBindings = { navController.navigate(Routes.SETTINGS_BINDINGS) })
            }
            composable(Routes.SETTINGS_BINDINGS) {
                BindingEditScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
