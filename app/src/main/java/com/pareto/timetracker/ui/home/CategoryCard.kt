package com.pareto.timetracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pareto.timetracker.R
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.ui.common.durationText
import com.pareto.timetracker.ui.common.formatClock
import kotlinx.coroutines.delay

/**
 * Kategori kartı: sol renk şeridi, ad, etiket, bugünkü süre.
 * Çalışan kart renkli çerçeve ve canlı sayaç ile ayrışır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    category: CategoryEntity,
    todayMs: Long,
    runningStartedAt: Long?,
    editMode: Boolean,
    onClick: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(CategoryPalette.color(category.tag, category.colorIndex))
    val running = runningStartedAt != null

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (running) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (running) BorderStroke(2.dp, accent) else null,
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(category.tag.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (runningStartedAt != null) {
                    LiveClock(startedAtMs = runningStartedAt)
                    Text(
                        text = stringResource(R.string.home_today, durationText(todayMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = durationText(todayMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    editMode -> Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.desc_edit_category),
                    )
                    running -> {
                        IconButton(onClick = onDiscard) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.desc_discard_timer),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.desc_stop_timer),
                        )
                    }
                    else -> Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.desc_start_timer),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveClock(startedAtMs: Long) {
    val now by produceState(System.currentTimeMillis(), startedAtMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    Text(
        text = formatClock(now - startedAtMs),
        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.SemiBold,
    )
}
