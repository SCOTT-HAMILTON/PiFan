package com.example.pifan

import kotlinx.serialization.Serializable

@Serializable
data class TempDataPoint(val date: String,
                         val delta: Int,
                         val level: Int,
                         val temp: Float)
class MyData {
    companion object {
        val Today = listOf(
            TempDataPoint(
                date = "2021-09-15T00:06:37",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:08:07",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:09:38",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:11:09",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:12:40",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:14:11",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:15:41",
                delta = 0,
                level = 3,
                temp = 53f
            ),
            TempDataPoint(
                date = "2021-09-15T00:17:12",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:18:43",
                delta = 1,
                level = 3,
                temp = 54f
            ),
            TempDataPoint(
                date = "2021-09-15T00:20:14",
                delta = 2,
                level = 3,
                temp = 53f
            )
        )
    }
}