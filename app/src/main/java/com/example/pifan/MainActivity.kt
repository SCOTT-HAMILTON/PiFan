package com.example.pifan

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import com.chargemap.compose.numberpicker.NumberPicker
import com.example.pifan.ui.theme.PiFanTheme
import io.github.staakk.cchart.Chart
import io.github.staakk.cchart.axis.axisDrawer
import io.github.staakk.cchart.axis.horizontalAxisRenderer
import io.github.staakk.cchart.axis.verticalAxisRenderer
import io.github.staakk.cchart.data.*
import io.github.staakk.cchart.label.*
import io.github.staakk.cchart.renderer.*
import io.github.staakk.cchart.util.TextAlignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URI
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    companion object {
        private const val PREF_DAYS_JSON_DATA_KEY = "pref_days_json_data"
        private const val PREF_SERVER_URL_KEY = "pref_server_url"
        private const val PREF_PORT_VALUE_KEY = "pref_port_value"
        private const val DEFAULT_SERVER_URL = "https://example.rpi-fan.api"
        private const val DEFAULT_PORT_VALUE = 8085
    }
    private var currentServerUrl = ""
    private var currentPortValue = 1000

    @ExperimentalSerializationApi
    @ExperimentalFoundationApi
    private fun tryUpdatingData() {
        requestDaysJsonData(this, currentServerUrl, currentPortValue) { succeed, response, error ->
            val jsonStr = if (succeed) {
                getPreferences(Context.MODE_PRIVATE).edit().
                putString(PREF_DAYS_JSON_DATA_KEY, response).apply()
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
                        val snackBarHostState = remember { SnackbarHostState() }
                        if (!succeed && error != null) {
                            LaunchedEffect(snackBarHostState) {
                                println("Showing SnackBar !")
                                if (snackBarHostState.showSnackbar(
                                    error,
                                    "Retry",
                                    SnackbarDuration.Long
                                ) == SnackbarResult.ActionPerformed) {
                                    tryUpdatingData()
                                }
                            }
                        }
                        // A surface container using the 'background' color from the theme
                        Surface(color = Color.Black,
                            modifier = Modifier.fillMaxSize()) {
                            NavigationHost(daysData,
                                currentServerUrl,
                                currentPortValue,
                                onServerUrlChange = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        println("Writing new current Server Url: $it")
                                        currentServerUrl = it
                                        getPreferences(Context.MODE_PRIVATE).edit()
                                            .putString(PREF_SERVER_URL_KEY, it).apply()
                                    }
                                }, onPortValueChange = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        println("Writing new port value: $it")
                                        currentPortValue = it
                                        getPreferences(Context.MODE_PRIVATE).edit()
                                            .putInt(PREF_PORT_VALUE_KEY, it).apply()
                                    }
                                }, snackbarHost = {
                                    SnackbarHost(hostState = snackBarHostState) { data ->
                                        Snackbar(
                                            snackbarData = data,
                                            backgroundColor = Color.Black,
                                            contentColor = Color.White
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @ExperimentalSerializationApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentServerUrl = getPreferences(Context.MODE_PRIVATE)
            .getString(PREF_SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        currentPortValue = getPreferences(Context.MODE_PRIVATE)
            .getInt(PREF_PORT_VALUE_KEY, DEFAULT_PORT_VALUE)
        println("currentPortValue: $currentPortValue")
        setContent {
            Surface(color = Color.Black,
                modifier = Modifier.fillMaxSize()) {
            }
        }
        tryUpdatingData()
    }
}

@ExperimentalFoundationApi
@Preview(showBackground = false)
@Composable
fun DefaultPreview() {
    PiFanTheme {
        Surface(color = Color.Black,
            modifier = Modifier.fillMaxSize()) {
            NavigationHost(listOf(MyData.Today), "", 0)
        }
    }
}
//
@ExperimentalFoundationApi
@Composable
fun NavigationHost(daysData: List<List<TempDataPoint>>,
                   defaultServerUrl: String,
                   defaultPortValue: Int,
                   onServerUrlChange: ((String) -> Unit)? = null,
                   onPortValueChange: ((Int) -> Unit)? = null,
                   snackbarHost: @Composable (SnackbarHostState)->Unit = {}){
    val navController = rememberNavController()
    var serverUrl by rememberSaveable {
        mutableStateOf(defaultServerUrl) }
    var portValue by rememberSaveable {
        mutableStateOf(defaultPortValue) }
    NavHost(navController = navController, startDestination = "MainScreen") {
        composable("MainScreen") {
            Main(daysData, navController, snackbarHost = snackbarHost)
        }
        composable("PreferencesScreen") {
            PreferencesScreen(
                navController, serverUrl,
                onServerUrlChange = {
                    onServerUrlChange?.invoke(it)
                    serverUrl = it
                }, portValue,
                onPortValueChange = {
                    onPortValueChange?.invoke(it)
                    portValue = it
                },
                snackbarHost = snackbarHost
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview(showBackground = true)
@Composable
fun PreferencesScreen(navController: NavHostController? = null,
                      serverUrl: String = "",
                      onServerUrlChange: ((String)->Unit)? = null,
                      portValue: Int = 1000,
                      onPortValueChange: ((Int)->Unit)? = null,
                      snackbarHost: @Composable (SnackbarHostState)->Unit = {}) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = snackbarHost,
        topBar = {
          TopAppBar(
              title = { Text(stringResource(R.string.preferences)) },
              navigationIcon = {
                  IconButton(onClick = { navController?.navigate("MainScreen") }) {
                      Icon(Icons.Filled.ArrowBack, contentDescription = "Localized description")
                  }
              },
          )
        },
        backgroundColor = MaterialTheme.colors.surface,
        content = {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 50.dp))
                        TextField(
                            value = serverUrl,
                            onValueChange = {
                                onServerUrlChange?.invoke(it)
                            },
                            label = { Text(stringResource(R.string.server_url)) },
                            placeholder = { Text("https://myserver.rpi-fan.api") },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colors.onSurface,
                                fontSize = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.port_number),
                                color = MaterialTheme.colors.onSurface)
                            Spacer(
                                modifier = Modifier
                                    .padding(end = 50.dp)
                            )
                            NumberPicker(
                                value = portValue,
                                range = 1_000..1_000_000,
                                onValueChange = { value ->
                                    onPortValueChange?.invoke(value)
                                },
                                textStyle = TextStyle(color = MaterialTheme.colors.onSurface)
                            )
                        }
                    }
            }
        }
    )
}

@Composable
fun Main(daysData: List<List<TempDataPoint>>,
         navController: NavHostController,
         snackbarHost: @Composable (SnackbarHostState)->Unit = {}) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = snackbarHost,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("PreferencesScreen") }
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "")
            }
        }
        ,backgroundColor = Color.Black,
        content = {
            DaysList(daysData)
        }
    )
}

fun List<List<TempDataPoint>>.zipWithDays(context: Context, offset: Int): List<Pair<String, List<TempDataPoint>>> =
    List(size) { index ->
        when (index+offset) {
            0 -> context.getString(R.string.today)
            1 -> context.getString(R.string.yesterday)
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    DateTimeFormatter.ofPattern("EEEE").format(
                        LocalDateTime.now().minusDays((index+offset).toLong())
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
fun DaysList(daysJsonData: List<List<TempDataPoint>>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current
    val maxDate = daysJsonData.flatMap { dayData ->
        dayData.map { dateToEpoch(it.date) }
    }.maxByOrNull { it }?.let {
        LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC)
    }
    val now = LocalDateTime.now()
    val offset = if (maxDate?.isBefore(now.withDayOfMonth(now.dayOfMonth-1)) == true) {
        val period = Period.between(maxDate.toLocalDate(), now.toLocalDate())
        period.days
    } else { 0 }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxSize()) {
        itemsIndexed(daysJsonData.zipWithDays(localContext, offset)) { index, item ->
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

fun requestDaysJsonData(context: Context,
                        serverUrl: String,
                        portValue: Int,
                        callback: (succeed: Boolean, response: String, error: String?)->Unit) {
    val url = URI(serverUrl).let {
        URI(it.scheme, it.userInfo, it.host, portValue, "/all_temps", it.query, it.fragment)
    }
    println("serverUrl: $serverUrl, portValue: $portValue, final Url: `$url`")
    val stringRequest = StringRequest(
        Request.Method.GET, url.toString(),
        { response ->
            callback(true, response, null)
        },
        { error ->
            println("[error] couldn't make request to web service `$url`: $error")
            callback(false, "", error.message)
        }
    ).setRetryPolicy(DefaultRetryPolicy(10_000, 1, 1f))
    newRequestQueue(context).add(stringRequest)
}

fun normalizedTempData(data: List<TempDataPoint>): List<TempDataPoint> {
    return if (data.size > 10) {
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
    } else {
        data
    }
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