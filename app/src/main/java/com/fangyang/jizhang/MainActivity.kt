package com.fangyang.jizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fangyang.jizhang.ui.AppNav
import com.fangyang.jizhang.ui.TransactionViewModel
import com.fangyang.jizhang.ui.theme.FangYangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FangYangTheme {
                val viewModel: TransactionViewModel =
                    viewModel(factory = TransactionViewModel.Factory)
                AppNav(viewModel)
            }
        }
    }
}
