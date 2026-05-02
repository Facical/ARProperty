package com.arproperty.android.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaceholderCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onActionClick != null) {
                Button(onClick = onActionClick) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    PlaceholderCard(
        title = "로딩 중",
        body = message,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    PlaceholderCard(
        title = title,
        body = body,
        modifier = modifier,
    )
}

@Composable
fun ErrorState(title: String, body: String, modifier: Modifier = Modifier) {
    PlaceholderCard(
        title = title,
        body = body,
        modifier = modifier,
    )
}

@Composable
fun PermissionRequiredState(
    title: String,
    body: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderCard(
        title = title,
        body = body,
        actionLabel = actionLabel,
        onActionClick = onActionClick,
        modifier = modifier,
    )
}

@Composable
fun NotSupportedState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    PlaceholderCard(
        title = title,
        body = body,
        modifier = modifier,
    )
}
