package com.example.pifan

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import com.example.pifan.ui.theme.PiFanTheme
import io.github.staakk.cchart.Chart
import io.github.staakk.cchart.axis.axisDrawer
import io.github.staakk.cchart.axis.horizontalAxisRenderer
import io.github.staakk.cchart.axis.verticalAxisRenderer
import io.github.staakk.cchart.data.*
import io.github.staakk.cchart.label.*
import io.github.staakk.cchart.renderer.*
import io.github.staakk.cchart.util.TextAlignment
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    companion object {
        private val PREF_DAYS_JSON_DATA_KEY = "pref_days_json_data"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = Color.Black,
                modifier = Modifier.fillMaxSize()) {
            }
        }
        requestDaysJsonData(this) { succeed, response ->
            val jsonStr = if (succeed) {
                getPreferences(Context.MODE_PRIVATE).edit().
                    putString(PREF_DAYS_JSON_DATA_KEY, response).commit()
                response
            } else {
                getPreferences(Context.MODE_PRIVATE).getString(PREF_DAYS_JSON_DATA_KEY, "")
                    ?: ""
            }
            val daysData = Json.decodeFromString<List<List<TempDataPoint>>>(jsonStr).map {
                normalizedTempData(it)
            }
            runOnUiThread {
                setContent {
                    PiFanTheme (darkTheme = true) {
                        // A surface container using the 'background' color from the theme
                        Surface(color = Color.Black,
                                modifier = Modifier.fillMaxSize()) {
                            Main(daysData)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PiFanTheme {
        Main(listOf(MyData.Today))
    }
}

fun List<List<TempDataPoint>>.zipWithDays(context: Context): List<Pair<String, List<TempDataPoint>>> =
    List(size) { index ->
        when (index) {
            0 -> context.getString(R.string.today)
            1 -> context.getString(R.string.yesterday)
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    DateTimeFormatter.ofPattern("EEEE").format(
                        LocalDateTime.now().minusDays(index.toLong())
                    ).let {
                        it.first().toUpperCase()+it.substring(1)
                    }
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
            }
        }
    }.zip(this)

@Composable
fun DayGraphBox(index: Int,
                item: Pair<String, List<TempDataPoint>>,
                onClicked: (Int)->Unit) {
    Box(modifier = Modifier
        .background(
            color = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(10)
        )
        .padding(10.dp)
        .width(320.dp)
        .height(300.dp)
        .clickable(true) {
            onClicked(index)
        },
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.first, color = MaterialTheme.colors.onSurface)
            LineChartScreen(item.second)
        }
    }
}

@Composable
fun Main(daysJsonData: List<List<TempDataPoint>>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        itemsIndexed(daysJsonData.zipWithDays(localContext)) { index, item ->
            DayGraphBox(index = index, item = item) {
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            }
        }
    }

}

fun dateToEpoch(dateStr: String): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        date.toEpochSecond(ZoneOffset.UTC)
    } else {
        Long.MAX_VALUE
    }
}

fun TempDataPoint.toTempPoint(): Data.Point {
    return pointOf(dateToEpoch(date).toFloat(), temp)
}

fun requestDaysJsonData(context: Context, callback: (succeed: Boolean, response: String)->Unit) {
    val url = "http://serverpi:60019/all_temps"
    val stringRequest = StringRequest(
        Request.Method.GET, url,
        { response ->
            callback(true, response)
        },
        { error ->
            println("[error] couldn't make request to web service `$url`: $error")
            callback(false, "")
        }
    ).setRetryPolicy(DefaultRetryPolicy(10_000, 1, 1f))
    newRequestQueue(context).add(stringRequest)
}

fun requestDayJsonData(dayOffset: Int, context: Context, callback: (response: String)->Unit) {
    require(dayOffset in 0..7)
    val url = "http://serverpi:60019/temps?dayOffset=$dayOffset"

    val stringRequest = StringRequest(
        Request.Method.GET, url,
        { response ->
            callback(response)
        },
        { println("[error] couldn't make request to web service: `$url`") }
    )
    newRequestQueue(context).add(stringRequest)
}
fun normalizedTempData(data: List<TempDataPoint>) =
    if (data.size>10) {
        val chunkSize = data.size / 10
        data.chunked(chunkSize).map {
            it.reduce { p1, p2 ->
                TempDataPoint(
                    p1.date,
                    (p1.delta + p2.delta) / 2,
                    (p1.level + p2.level) / 2,
                    (p1.temp + p2.temp) / 2f
                )
            }
        }
    } else { data }

fun json2DayData(dayJsonData: String) {
    normalizedTempData(Json.decodeFromString(dayJsonData))
}

@Composable
fun LineChartScreen(dayData: List<TempDataPoint>) {
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
                            LocalDateTime.ofEpochSecond((it.x + minX).toLong(), 0, ZoneOffset.UTC)
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