package com.example.pifan

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.example.pifan.ui.theme.PiFanTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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

    @ExperimentalAnimationApi
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
            val daysData =
                if (jsonStr.isNotEmpty()) {
                    try {
                        Json.decodeFromString<List<List<TempDataPoint>>>(jsonStr).map {
                            normalizedTempData(it)
                        }
                    } catch(e: Exception) {
                        listOf()
                    }
                } else listOf()
            runOnUiThread {
                setContent {
                    val isRefreshing = remember { mutableStateOf(false) }
                    isRefreshing.value = false
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
                                    isRefreshing.value = true
                                    tryUpdatingData()
                                }
                            }
                        }
                        // A surface container using the 'background' color from the theme
                        Surface(color = Color.Black,
                            modifier = Modifier.fillMaxSize()) {
                            NavigationHost(
                                daysData,
                                isRefreshing.value, {
                                    isRefreshing.value = true
                                    tryUpdatingData()
                                },
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

    @ExperimentalAnimationApi
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

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview(showBackground = false)
@Composable
fun DefaultPreview() {
    PiFanTheme {
        Surface(color = Color.Black,
            modifier = Modifier.fillMaxSize()) {
            NavigationHost(listOf(MyData.Today), false, {}, "", 0)
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun NavigationHost(daysData: List<List<TempDataPoint>>,
                   refreshing: Boolean,
                   onRefresh: ()->Unit,
                   defaultServerUrl: String,
                   defaultPortValue: Int,
                   onServerUrlChange: ((String) -> Unit)? = null,
                   onPortValueChange: ((Int) -> Unit)? = null,
                   snackbarHost: @Composable (SnackbarHostState)->Unit = {}){
    val navController = rememberAnimatedNavController()
    var serverUrl by rememberSaveable {
        mutableStateOf(defaultServerUrl) }
    var portValue by rememberSaveable {
        mutableStateOf(defaultPortValue) }
    AnimatedNavHost(navController = navController, startDestination = "MainScreen") {
        composable(
            "MainScreen",
            enterTransition = { initial, _ ->
                when (initial.destination.route) {
                    "PreferencesScreen" ->
                        slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(700))
                    else -> null
                }
            },
            exitTransition = { _, target ->
                when (target.destination.route) {
                    "PreferencesScreen" ->
                        slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(700))
                    else -> null
                }
            }
        ) {
            SwipeRefresh(
                state = rememberSwipeRefreshState(refreshing),
                onRefresh = { onRefresh() },
            ) {
                Main(daysData, navController, snackbarHost = snackbarHost)
            }
        }
        composable(
            "PreferencesScreen",
            enterTransition = { initial, _ ->
                when (initial.destination.route) {
                    "MainScreen" ->
                        slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(700))
                    else -> null
                }
            },
            exitTransition = { _, target ->
                when (target.destination.route) {
                    "MainScreen" ->
                        slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(700))
                    else -> null
                }
            }
        ) {
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