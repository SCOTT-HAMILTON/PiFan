package com.example.pifan

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException

fun requestDaysJsonData(context: Context,
                        serverUrl: String,
                        portValue: Int,
                        callback: suspend (succeed: Boolean, response: String, error: String?)->Unit) {
    val url = try {
        URI(serverUrl).let {
            URI(it.scheme, it.userInfo, it.host, portValue, "/all_temps", it.query, it.fragment)
        }
    } catch (e: URISyntaxException) {
        CoroutineScope(Dispatchers.IO).launch {
            callback(false, "", e.message)
        }
        return
    }
    println("serverUrl: $serverUrl, portValue: $portValue, final Url: `$url`")
    val stringRequest = StringRequest(
        Request.Method.GET, url.toString(),
        { response ->
            CoroutineScope(Dispatchers.IO).launch {
                callback(true, response, null)
            }
        },
        { error ->
            println("[error] couldn't make request to web service `$url`: $error")
            CoroutineScope(Dispatchers.IO).launch {
                callback(false, "", error.message)
            }
        }
    ).setRetryPolicy(DefaultRetryPolicy(10_000, 1, 1f))
    Volley.newRequestQueue(context).add(stringRequest)
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