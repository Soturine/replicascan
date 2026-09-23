package com.soturine.replicascan.feature.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.ui.component.EmptyStateCard
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState

@Composable
internal fun EditorMessage.text(): String = stringResource(
    when (this) {
        EditorMessage.PREVIEW_FAILED -> R.string.editor_message_preview_failed
        EditorMessage.SAVE_FAILED -> R.string.editor_message_save_failed
        EditorMessage.CLEANUP_PENDING -> R.string.editor_message_cleanup_pending
        EditorMessage.NAME_REQUIRED -> R.string.editor_message_name_required
        EditorMessage.NAME_TOO_LONG -> R.string.editor_message_name_too_long
    },
)

@Composable
internal fun EditorMessageEffect(
    message: EditorMessage?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val text = message?.text()
    LaunchedEffect(message) {
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            onShown()
        }
    }
}

@Composable
internal fun EditorBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.editor_back))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MissingDocument(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = {}, navigationIcon = { EditorBackButton(onBack) }) },
    ) { innerPadding ->
        EmptyStateCard(
            title = stringResource(R.string.editor_missing_title),
            message = stringResource(R.string.editor_missing_message),
            mascotState = ReplicaScanMascotState.Attention,
            modifier = Modifier.padding(innerPadding).padding(24.dp),
        )
    }
}
