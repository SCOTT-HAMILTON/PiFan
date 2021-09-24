package com.example.pifan

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pifan.ui.theme.PiFanTheme
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