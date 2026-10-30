package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ProductRepository
import com.example.data.SettingsPreferences
import com.example.ui.ProductTrackerScreen
import com.example.ui.ProductTrackerViewModel
import com.example.ui.theme.LastDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ProductRepository(database.productDao())
        val settingsPreferences = SettingsPreferences(applicationContext)

        setContent {
            val trackerViewModel: ProductTrackerViewModel = viewModel(
                factory = ProductTrackerViewModel.provideFactory(repository, settingsPreferences)
            )
            val uiState by trackerViewModel.uiState.collectAsStateWithLifecycle()

            LastDropTheme(appTheme = uiState.userSettings.selectedTheme) {
                ProductTrackerScreen(viewModel = trackerViewModel)
            }
        }
    }
}
