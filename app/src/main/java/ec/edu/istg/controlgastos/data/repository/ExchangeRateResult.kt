package ec.edu.istg.controlgastos.data.repository

data class ExchangeRateResult(
    val baseCode: String,
    val rates: Map<String, Double>,
    val isFromCache: Boolean,
    val lastUpdated: String?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
