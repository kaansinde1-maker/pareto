package com.pareto.timetracker.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pareto.timetracker.R
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag
import com.pareto.timetracker.ui.common.durationText
import com.pareto.timetracker.ui.common.percentText
import com.pareto.timetracker.ui.common.tagColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    ui: GoalUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goal = ui.goal
    val active = ui.isActive
    val progress = ui.progressMs
    val target = ui.targetMs

    // Durum sadece renkle belirtilir: Max aşıldı -> kırmızı, Min tamamlandı -> yeşil.
    val barColor = when {
        !active -> MaterialTheme.colorScheme.outline
        goal.goalType == GoalType.MAX && progress > target -> tagColor(Tag.CONSUMPTION)
        goal.goalType == GoalType.MIN && progress >= target -> tagColor(Tag.INVESTMENT)
        else -> MaterialTheme.colorScheme.primary
    }
    val dotColor = ui.category?.let { Color(CategoryPalette.color(it.tag, it.colorIndex)) }
        ?: tagColor(goal.tag ?: Tag.INVESTMENT)
    val title = ui.category?.name ?: stringResource((goal.tag ?: Tag.INVESTMENT).labelRes)
    val typeLabel = stringResource(
        if (goal.goalType == GoalType.MIN) R.string.goal_type_min else R.string.goal_type_max,
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (active) 1f else 0.5f),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        text = stringResource(R.string.goal_subtitle, typeLabel, durationText(target)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = percentText(progress, target),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            GoalBar(
                fraction = if (target > 0) (progress.toFloat() / target).coerceIn(0f, 1f) else 0f,
                color = barColor,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.goal_progress, durationText(progress), durationText(target)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!active) {
                    Text(
                        text = stringResource(R.string.goal_category_archived),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}
