package com.walletq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walletq.core.designsystem.WalletQTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

            WalletQTheme(
                themeMode = userPreferences.themeMode,
                dynamicColor = userPreferences.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WalletQApp(
                        userPreferences = userPreferences,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
