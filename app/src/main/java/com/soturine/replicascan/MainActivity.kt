package com.soturine.replicascan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soturine.replicascan.app.RootViewModel
import com.soturine.replicascan.core.common.model.AppThemePreference
import com.soturine.replicascan.core.ui.theme.ReplicaScanTheme
import com.soturine.replicascan.navigation.ReplicaScanNavHost
import com.soturine.replicascan.splash.SplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ReplicaScanApplication).container
        setContent {
            val rootViewModel: RootViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        RootViewModel(container.userPreferencesRepository) as T
                },
            )
            val rootState by rootViewModel.uiState.collectAsStateWithLifecycle()

            ReplicaScanTheme(
                darkTheme = when (rootState.themePreference) {
                    AppThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    AppThemePreference.LIGHT -> false
                    AppThemePreference.DARK -> true
                },
            ) {
                if (rootState.isReady) {
                    ReplicaScanNavHost(
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
