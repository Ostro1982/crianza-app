package com.tudominio.crianza

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class ClimaData(
    val mananaTemp: Int,
    val mananaIcono: String,
    val tardeTemp: Int,
    val tardeIcono: String
)

object ClimaService {

    private val client = OkHttpClient()

    fun codigoAIcono(code: Int): String = when (code) {
        0            -> "☀️"
        1, 2         -> "🌤"
        3            -> "☁️"
        45, 48       -> "🌫"
        51, 53, 55   -> "🌦"
        61, 63, 65   -> "🌧"
        71, 73, 75,
        77           -> "🌨"
        80, 81, 82   -> "🌦"
        95, 96, 99   -> "⛈"
        else         -> "🌡"
    }

    /**
     * Obtiene la ubicación: primero intenta getLastKnownLocation, si es null
     * solicita una actualización fresca (máximo 10 segundos de espera).
     */
    @Suppress("DEPRECATION")
    suspend fun getUbicacion(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Intento rápido con última ubicación conocida
        try {
            val last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (last != null) return last.latitude to last.longitude
        } catch (_: SecurityException) { return null }

        // Fallback: solicitar ubicación fresca, esperar hasta 10 segundos
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        if (cont.isActive) cont.resume(loc.latitude to loc.longitude)
                    }
                    override fun onProviderDisabled(provider: String) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
                try {
                    lm.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper()
                    )
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                } catch (_: SecurityException) {
                    cont.resume(null)
                }
            }
        }
    }

    /** Llama a Open-Meteo y retorna temperaturas de 7am y 4pm de hoy. */
    fun fetchClima(lat: Double, lon: Double): ClimaData? {
        return try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&hourly=temperature_2m,weathercode" +
                "&timezone=auto&forecast_days=1"
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            val body = resp.body?.string() ?: return null
            val hourly = JSONObject(body).getJSONObject("hourly")
            val temps = hourly.getJSONArray("temperature_2m")
            val codes = hourly.getJSONArray("weathercode")
            ClimaData(
                mananaTemp  = temps.getDouble(7).roundToInt(),
                mananaIcono = codigoAIcono(codes.getInt(7)),
                tardeTemp   = temps.getDouble(16).roundToInt(),
                tardeIcono  = codigoAIcono(codes.getInt(16))
            )
        } catch (_: Exception) { null }
    }

    fun guardarCache(context: Context, data: ClimaData) {
        context.getSharedPreferences("clima_cache", Context.MODE_PRIVATE).edit().apply {
            putInt("manana_temp", data.mananaTemp)
            putString("manana_icono", data.mananaIcono)
            putInt("tarde_temp", data.tardeTemp)
            putString("tarde_icono", data.tardeIcono)
            putLong("ts", System.currentTimeMillis())
            apply()
        }
    }

    /** Devuelve datos cacheados si tienen menos de 1 hora. */
    fun cargarCache(context: Context): ClimaData? {
        val p = context.getSharedPreferences("clima_cache", Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - p.getLong("ts", 0L) > 60 * 60 * 1000L) return null
        return ClimaData(
            mananaTemp  = p.getInt("manana_temp", 0),
            mananaIcono = p.getString("manana_icono", "🌡") ?: "🌡",
            tardeTemp   = p.getInt("tarde_temp", 0),
            tardeIcono  = p.getString("tarde_icono", "🌡") ?: "🌡"
        )
    }
}
