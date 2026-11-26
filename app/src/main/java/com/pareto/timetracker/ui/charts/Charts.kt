package com.pareto.timetracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pareto.timetracker.ui.common.durationText
import com.pareto.timetracker.ui.common.percentText

/** Halka pasta grafik. Dilimler arasında ince boşluk vardır (aynı renkli komşular ayrışsın). */
@Composable
fun PieChart(
    slices: List<ChartSlice>,
    centerText: String,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }.toFloat()
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (total <= 0f) return@Canvas
            val strokeWidth = size.minDimension * 0.22f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val gap = if (slices.size > 1) 2f else 0f
            var start = -90f
            slices.forEach { slice ->
                val sweep = slice.value / total * 360f
                drawArc(
                    color = slice.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth),
                )
                start += sweep
            }
        }
        Text(
            text = centerText,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** Yatay çubuklar. Çubuk uzunluğu gösterilen kümenin toplamındaki gerçek paydır (%100 = tam genişlik). */
@Composable
fun BarChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        slices.forEach { slice ->
            val fraction = if (total > 0) (slice.value.toFloat() / total).coerceIn(0.01f, 1f) else 0f
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(slice.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${durationText(slice.value)} · ${percentText(slice.value, total)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(slice.color),
                    )
                }
            }
        }
    }
}
