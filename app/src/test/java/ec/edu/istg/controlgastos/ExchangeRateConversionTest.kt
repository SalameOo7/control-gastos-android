package ec.edu.istg.controlgastos

import com.google.gson.Gson
import ec.edu.istg.controlgastos.data.network.ExchangeRateResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateConversionTest {

    @Test
    fun testExchangeRateResponseParsing() {
        val sampleJson = """
            {
                "result": "success",
                "base_code": "USD",
                "time_last_update_utc": "Tue, 25 Aug 2026 00:00:00 +0000",
                "rates": {
                    "USD": 1.0,
                    "EUR": 0.85,
                    "COP": 4100.0,
                    "MXN": 19.5
                }
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(sampleJson, ExchangeRateResponse::class.java)

        assertNotNull(response)
        assertEquals("success", response.result)
        assertEquals("USD", response.baseCode)
        assertEquals(4, response.rates.size)
        assertEquals(0.85, response.rates["EUR"] ?: 0.0, 0.0001)
    }

    @Test
    fun testCurrencyConversionLogic() {
        val rates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.85,
            "COP" to 4000.0
        )

        // 100 USD to EUR -> 85 EUR
        val usdAmount = 100.0
        val tasaEur = rates["EUR"] ?: 1.0
        val convertedEur = (usdAmount / (rates["USD"] ?: 1.0)) * tasaEur
        assertEquals(85.0, convertedEur, 0.001)

        // 85 EUR to COP -> (85 / 0.85) * 4000 = 100 * 4000 = 400,000 COP
        val tasaOrigen = rates["EUR"] ?: 1.0
        val tasaDestino = rates["COP"] ?: 1.0
        val convertedCop = (85.0 / tasaOrigen) * tasaDestino
        assertEquals(400000.0, convertedCop, 0.001)
    }
}
