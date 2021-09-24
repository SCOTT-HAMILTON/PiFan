package com.example.pifan

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
            TempLineChart(item.second)
        }
    }
}

fun List<List<TempDataPoint>>.zipWithDays(context: Context, offset: Int):
        List<Pair<String, List<TempDataPoint>>> {
    return List(size) { index ->
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
}