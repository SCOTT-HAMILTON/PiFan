package com.example.pifan

import android.os.Build
import android.graphics.Paint
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import android.graphics.Typeface
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.staakk.cchart.Chart
import io.github.staakk.cchart.axis.axisDrawer
import io.github.staakk.cchart.axis.horizontalAxisRenderer
import io.github.staakk.cchart.axis.verticalAxisRenderer
import io.github.staakk.cchart.data.Data
import io.github.staakk.cchart.data.Series
import io.github.staakk.cchart.data.Viewport
import io.github.staakk.cchart.data.pointOf
import io.github.staakk.cchart.label.IntLabelsProvider
import io.github.staakk.cchart.label.horizontalLabelRenderer
import io.github.staakk.cchart.label.verticalLabelRenderer
import io.github.staakk.cchart.renderer.lineDrawer
import io.github.staakk.cchart.renderer.lineRenderer
import io.github.staakk.cchart.util.TextAlignment
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun TempLineChart(dayData: List<TempDataPoint>) {
    println("dayData: ${dayData.size}")
    val (points, minX) = dayData.map(TempDataPoint::toTempPoint).let { rawPoints ->
        rawPoints.minByOrNull { it.x }?.let { minXPoint ->
            println("MinXPoint: $minXPoint")
            val result: List<Data.Point> = rawPoints.map { it.copy(x = it.x - minXPoint.x) }
            result to minXPoint.x
        }
    } ?: (listOf<Data.Point>() to 0f)

    val minY = points.minByOrNull { it.y }?.y ?: 0f
    val maxY = points.maxByOrNull { it.y }?.y ?: 100f
    val maxX = points.maxByOrNull { it.x }?.x ?: 1000f
    println("maxX: $maxX")

    val horizontalLabelRenderer = with(LocalDensity.current) {
        val paint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 12.sp.toPx()
            isAntiAlias = true
            color = MaterialTheme.colors.onSurface.toArgb()
        }
        horizontalLabelRenderer(
            paint,
            1f,
            io.github.staakk.cchart.util.Alignment.BottomCenter,
            TextAlignment.Left,
            Offset(0f, 12f),
            labelsProvider =
            { _, _ ->
                points.map {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val date =
                            LocalDateTime.ofEpochSecond((it.x + minX).toLong(), 0,
                                ZoneOffset.UTC)
                        val dateStr = DateTimeFormatter.ofPattern("HH").format(date) + "h"
                        dateStr to it.x
                    } else {
                        "${it.x}" to it.x
                    }
                }
            }
        )
    }
    val verticalLabelRenderer = with(LocalDensity.current) {
        val paint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 12.sp.toPx()
            isAntiAlias = true
            color = MaterialTheme.colors.onSurface.toArgb()
        }
        verticalLabelRenderer(
            paint,
            0f,
            io.github.staakk.cchart.util.Alignment.CenterLeft,
            TextAlignment.Left,
            Offset(-12f, 0f),
            IntLabelsProvider
        )
    }

    val onSurface = MaterialTheme.colors.onSurface
    val primary = MaterialTheme.colors.primary

    Chart(
        modifier = Modifier
            .padding(start = 32.dp, bottom = 32.dp)
            .aspectRatio(1f, false),
        viewport = Viewport(0f, maxX, minY-abs(minY)*0.01, maxY+abs(maxY*0.01))
    ) {
        series(
            Series(points),
            renderer = lineRenderer(lineDrawer = lineDrawer(brush = SolidColor(primary)))
        )

        horizontalAxis(horizontalAxisRenderer(
            axisDrawer = axisDrawer(brush = SolidColor(onSurface))))
        horizontalAxisLabels(horizontalLabelRenderer)

        verticalAxis(verticalAxisRenderer(
            axisDrawer = axisDrawer(brush = SolidColor(onSurface))))
        verticalAxisLabels(verticalLabelRenderer)
    }
}

fun TempDataPoint.toTempPoint(): Data.Point {
    return pointOf(dateToEpoch(date).toFloat(), temp)
}