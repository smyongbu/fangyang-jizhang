package com.fangyang.jizhang.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val HOME = "home"
    const val EDIT = "edit"            // edit?id={id}，id=0 表示新增
    const val ARG_ID = "id"
    fun edit(id: Long = 0L) = "$EDIT?$ARG_ID=$id"
}

@Composable
fun AppNav(viewModel: TransactionViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onAdd = { navController.navigate(Routes.edit(0L)) },
                onEdit = { id -> navController.navigate(Routes.edit(id)) },
            )
        }
        composable(
            route = "${Routes.EDIT}?${Routes.ARG_ID}={${Routes.ARG_ID}}",
            arguments = listOf(
                navArgument(Routes.ARG_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(Routes.ARG_ID) ?: 0L
            AddEditScreen(
                viewModel = viewModel,
                transactionId = id,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
