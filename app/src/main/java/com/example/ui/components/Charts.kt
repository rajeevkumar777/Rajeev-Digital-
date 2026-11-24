package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SubtleGold
import com.example.ui.theme.WhatsAppGreen
import com.example.util.FormatUtils

data class BarDatum(
    val label: String,
    val value: Double
)

@Composable
fun RevenueBarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text(
            text = "No revenue recorded in this period yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 16.dp)
        )
        return
    }

    val maxVal = (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
        ) {
            val count = data.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.54f).coerceAtMost(42.dp.toPx())
            val chartHeight = size.height - 8.dp.toPx()

            // Horizontal guide lines
            for (step in 1..3) {
                val y = chartHeight * (1f - step / 3f)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            data.forEachIndexed { index, item ->
                val ratio = (item.value / maxVal).toFloat().coerceIn(0f, 1f)
                val barHeight = (chartHeight * ratio).coerceAtLeast(6.dp.toPx())
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = chartHeight - barHeight

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(SubtleGold, BrightPurple, ElectricPurple)
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = FormatUtils.formatCompactInr(item.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = SubtleGold,
                        maxLines = 1
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalBreakdownChart(
    items: List<Pair<String, Double>>,
    valueFormatter: (Double) -> String = { FormatUtils.formatInr(it) },
    barColors: List<Color> = listOf(BrightPurple, SubtleGold, WhatsAppGreen),
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text(
            text = "Not enough data to display breakdown.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val maxVal = (items.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEachIndexed { idx, (label, value) ->
            val color = barColors[idx % barColors.size]
            val fraction = (value / maxVal).toFloat().coerceIn(0.04f, 1f)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = valueFormatter(value),
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }
        }
    }
}
