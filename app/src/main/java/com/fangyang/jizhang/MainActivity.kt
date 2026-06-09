package com.fangyang.jizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fangyang.jizhang.ui.MainScaffold
import com.fangyang.jizhang.ui.ProductViewModel
import com.fangyang.jizhang.ui.theme.FangYangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FangYangTheme {
                val viewModel: ProductViewModel =
                    viewModel(factory = ProductViewModel.Factory)
                MainScaffold(viewModel)
            }
        }
    }
}
