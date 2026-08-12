package com.soturine.scanora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soturine.scanora.app.RootViewModel
import com.soturine.scanora.core.common.model.AppThemePreference
import com.soturine.scanora.core.ui.theme.ScanoraTheme
import com.soturine.scanora.navigation.ScanoraNavHost
import com.soturine.scanora.splash.SplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ScanoraApplication).container
        setContent {
            val rootViewModel: RootViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        RootViewModel(container.userPreferencesRepository) as T
                },
            )
            val rootState by rootViewModel.uiState.collectAsStateWithLifecycle()

            ScanoraTheme(
                darkTheme = when (rootState.themePreference) {
                    AppThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    AppThemePreference.LIGHT -> false
                    AppThemePreference.DARK -> true
                },
            ) {
                if (rootState.isReady) {
                    ScanoraNavHost(
                        container = container,
                        rootViewModel = rootViewModel,
                    )
                } else {
                    SplashScreen()
                }
            }
        }
    }
}
