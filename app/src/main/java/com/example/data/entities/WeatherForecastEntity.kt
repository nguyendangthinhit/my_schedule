package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bảng dữ liệu dự báo thời tiết cho 7 ngày liên tiếp kể từ ngày hôm nay.
 * Bảng này tự động cập nhật và làm mới khi bước sang ngày mới.
 */
@Entity(tableName = "weather_forecasts")
data class WeatherForecastEntity(
    @PrimaryKey val date: String,             // Định dạng ISO: "yyyy-MM-dd"
    val tempAvg: Int,                         // Nhiệt độ trung bình (°C)
    val tempMin: Int,                         // Nhiệt độ thấp nhất (°C)
    val tempMax: Int,                         // Nhiệt độ cao nhất (°C)
    val weatherEmoji: String,                 // Emoji thời tiết (☀️, ⛅, 🌧️, ⛈️,...)
    val conditionDescription: String,         // Mô tả trạng thái (Nắng ráo, Mưa rào nhẹ, Có mây,...)
    val remainingDayForecast: String? = null, // Dự báo thời tiết thời gian còn lại của ngày (cho ngày hôm nay)
    val updatedAt: Long = System.currentTimeMillis()
)
