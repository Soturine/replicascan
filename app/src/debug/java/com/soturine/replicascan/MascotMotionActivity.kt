package com.soturine.replicascan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.soturine.replicascan.core.ui.component.ReplicaScanMascot
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.theme.ReplicaScanTheme

/** Debug-only native scene for real-time screenrecord without Compose test-clock control. */
class MascotMotionActivity : ComponentActivity() {
    private var currentState by mutableStateOf(ReplicaScanMascotState.Idle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentState = requestedState(intent)
        setContent {
            ReplicaScanTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ReplicaScanMascot(state = currentState, size = 240.dp)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentState = requestedState(intent)
    }

    private fun requestedState(intent: Intent): ReplicaScanMascotState =
        ReplicaScanMascotState.entries.firstOrNull { it.name == intent.getStringExtra("state") }
            ?: ReplicaScanMascotState.Idle
}
