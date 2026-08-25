package ec.edu.istg.controlgastos.data.network

import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String = "USD"
    ): ExchangeRateResponse
}
