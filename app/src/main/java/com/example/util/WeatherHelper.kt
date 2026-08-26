package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object WeatherHelper {
    private const val PREFS_NAME = "app_weather_prefs"
    private const val KEY_WEATHER_ENABLED = "weather_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Trạng thái bật/tắt hiển thị thời tiết (mặc định là true).
     */
    fun isWeatherEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WEATHER_ENABLED, true)
    }

    fun setWeatherEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WEATHER_ENABLED, enabled).apply()
        com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    /**
     * Kiểm tra quyền vị trí (Fine hoặc Coarse) đã được cấp hay chưa.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Kiểm tra xem thiết bị đã bật định vị (GPS / Network Location) trong phần Cài đặt hệ thống hay chưa.
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val isGpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val isNetworkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return isGpsEnabled || isNetworkEnabled
    }

    /**
     * Lấy tọa độ vị trí gần nhất của thiết bị nếu có quyền và dịch vụ bật.
     * Trả về Pair(latitude, longitude), hoặc null nếu chưa lấy được.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        var bestLocation: Location? = null
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLoc != null) bestLocation = gpsLoc
            }
            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (netLoc != null) bestLocation = netLoc
            }
            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                val passiveLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (passiveLoc != null) bestLocation = passiveLoc
            }
        } catch (_: Exception) {
            // Ignore security exception or disabled providers
        }

        return bestLocation?.let { Pair(it.latitude, it.longitude) }
    }

    /**
     * Mở màn hình cài đặt bật định vị trên thiết bị.
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val appSettingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appSettingsIntent)
        }
    }
}
