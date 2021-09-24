package com.example.pifan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.chargemap.compose.numberpicker.NumberPicker

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
                        Text(
                            stringResource(R.string.port_number),
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