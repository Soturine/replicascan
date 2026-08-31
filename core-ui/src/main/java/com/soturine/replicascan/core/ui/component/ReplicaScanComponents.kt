package com.soturine.replicascan.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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

@Composable
fun ReplicaScanBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 4.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ReplicaScanSpacing.xl, vertical = ReplicaScanSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.md),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplicaScanSelectionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Card(
        modifier = modifier
            .semantics { this.selected = selected }
            .sizeIn(minHeight = 72.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                badge?.let { Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ReplicaScanSegmentedControl(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
