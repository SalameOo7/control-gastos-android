package ec.edu.istg.controlgastos.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ec.edu.istg.controlgastos.data.network.ExchangeRateApiService
import ec.edu.istg.controlgastos.data.network.ExchangeRateResponse
import ec.edu.istg.controlgastos.data.network.RetrofitClient
import ec.edu.istg.controlgastos.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExchangeRateRepository(
    private val context: Context,
    private val apiService: ExchangeRateApiService = RetrofitClient.apiService
) {

    private val preferences by lazy {
        context.getSharedPreferences(PREFS_EXCHANGE_RATES, Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    suspend fun getExchangeRates(
        baseCurrency: String = "USD",
        forceRefresh: Boolean = false
    ): ExchangeRateResult = withContext(Dispatchers.IO) {
        val hasInternet = NetworkUtils.isNetworkAvailable(context)

        if (hasInternet) {
            try {
                val response = apiService.getLatestRates(baseCurrency)
                if (response.result.equals("success", ignoreCase = true)) {
                    guardarEnCache(response)
                    return@withContext ExchangeRateResult(
                        baseCode = response.baseCode,
                        rates = response.rates,
                        isFromCache = false,
                        lastUpdated = response.timeLastUpdateUtc,
                        isSuccess = true
                    )
                }
            } catch (exception: Exception) {
                // Fallback to cache if network call fails
            }
        }

        // Recuperar desde caché
        val cache = cargarDeCache(baseCurrency)
        if (cache != null) {
            return@withContext ExchangeRateResult(
                baseCode = cache.baseCode,
                rates = cache.rates,
                isFromCache = true,
                lastUpdated = cache.timeLastUpdateUtc,
                isSuccess = true,
                errorMessage = if (!hasInternet) "Sin conexión a internet" else "Error al consultar API"
            )
        }

        // Si no hay caché disponible, proveer tasas base de respaldo
        val fallbackRates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "COP" to 4150.0,
            "MXN" to 19.50,
            "PEN" to 3.75,
            "BRL" to 5.45
        )

        ExchangeRateResult(
            baseCode = baseCurrency,
            rates = fallbackRates,
            isFromCache = true,
            lastUpdated = "Tasas predeterminadas (offline)",
            isSuccess = true,
            errorMessage = "Sin conexión y sin caché previo"
        )
    }

    private fun guardarEnCache(response: ExchangeRateResponse) {
        preferences.edit()
            .putString(KEY_CACHED_BASE_CODE, response.baseCode)
            .putString(KEY_CACHED_RATES_JSON, gson.toJson(response.rates))
            .putString(KEY_CACHED_LAST_UPDATE, response.timeLastUpdateUtc)
            .putLong(KEY_CACHED_TIMESTAMP_MS, System.currentTimeMillis())
            .apply()
    }

    private fun cargarDeCache(baseCurrency: String): ExchangeRateResponse? {
        val ratesJson = preferences.getString(KEY_CACHED_RATES_JSON, null) ?: return null
        val baseCode = preferences.getString(KEY_CACHED_BASE_CODE, baseCurrency) ?: baseCurrency
        val lastUpdate = preferences.getString(KEY_CACHED_LAST_UPDATE, null) ?: "Caché local"

        return try {
            val type = object : TypeToken<Map<String, Double>>() {}.type
            val rates: Map<String, Double> = gson.fromJson(ratesJson, type)
            ExchangeRateResponse(
                result = "cached",
                baseCode = baseCode,
                timeLastUpdateUtc = lastUpdate,
                rates = rates
            )
        } catch (e: Exception) {
            null
        }
    }

    fun convertir(
        monto: Double,
        monedaOrigen: String,
        monedaDestino: String,
        rates: Map<String, Double>
    ): Double? {
        if (monedaOrigen.equals(monedaDestino, ignoreCase = true)) {
            return monto
        }

        val tasaOrigen = rates[monedaOrigen.uppercase()] ?: if (monedaOrigen.equals("USD", ignoreCase = true)) 1.0 else null
        val tasaDestino = rates[monedaDestino.uppercase()] ?: if (monedaDestino.equals("USD", ignoreCase = true)) 1.0 else null

        if (tasaOrigen == null || tasaDestino == null || tasaOrigen <= 0.0) {
            return null
        }

        val montoEnUsd = monto / tasaOrigen
        return montoEnUsd * tasaDestino
    }

    companion object {
        private const val PREFS_EXCHANGE_RATES = "control_gastos_exchange_rates"
        private const val KEY_CACHED_BASE_CODE = "cached_base_code"
        private const val KEY_CACHED_RATES_JSON = "cached_rates_json"
        private const val KEY_CACHED_LAST_UPDATE = "cached_last_update"
        private const val KEY_CACHED_TIMESTAMP_MS = "cached_timestamp_ms"
    }
}
