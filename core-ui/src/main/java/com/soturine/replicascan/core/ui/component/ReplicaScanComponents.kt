package com.soturine.replicascan.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.ui.theme.ReplicaScanSizes
import com.soturine.replicascan.core.ui.theme.ReplicaScanSpacing

@Composable
fun ReplicaScanContent(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (expanded) ReplicaScanSizes.expandedContentMaxWidth else ReplicaScanSizes.compactContentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.lg),
            content = content,
        )
    }
}

@Composable
fun ReplicaScanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ReplicaScanSizes.primaryActionHeight),
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        icon?.invoke()
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ReplicaScanSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val buttonModifier = modifier
        .heightIn(min = 52.dp)
        .sizeIn(minWidth = ReplicaScanSizes.minimumTouchTarget)
    if (outlined) {
        OutlinedButton(modifier = buttonModifier, enabled = enabled, onClick = onClick) {
            icon?.invoke()
            Text(text = text)
        }
    } else {
        FilledTonalButton(modifier = buttonModifier, enabled = enabled, onClick = onClick) {
            icon?.invoke()
            Text(text = text)
        }
    }
}
