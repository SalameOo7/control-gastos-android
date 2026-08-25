package ec.edu.istg.controlgastos.data.network

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(
    @SerializedName("result")
    val result: String,
    @SerializedName("base_code")
    val baseCode: String,
    @SerializedName("time_last_update_utc")
    val timeLastUpdateUtc: String,
    @SerializedName("rates")
    val rates: Map<String, Double>
)
