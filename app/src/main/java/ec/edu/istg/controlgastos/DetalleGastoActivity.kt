package ec.edu.istg.controlgastos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ec.edu.istg.controlgastos.data.DatabaseHelper
import ec.edu.istg.controlgastos.data.GastoConCategoria
import ec.edu.istg.controlgastos.data.repository.ExchangeRateRepository
import ec.edu.istg.controlgastos.data.repository.ExchangeRateResult
import kotlinx.coroutines.launch

class DetalleGastoActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private var gasto: GastoConCategoria? = null
    private var idGasto: Long = INVALID_ID
    private var exchangeRateResult: ExchangeRateResult? = null

    private lateinit var spinnerMonedaConversion: Spinner
    private lateinit var textViewMontoConvertido: TextView
    private lateinit var textViewInfoTasa: TextView
    private lateinit var textViewConversionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_gasto)

        databaseHelper = DatabaseHelper(this)
        exchangeRateRepository = ExchangeRateRepository(this)

        idGasto = intent.getLongExtra(EXTRA_ID_GASTO, INVALID_ID)
        if (idGasto == INVALID_ID) {
            mostrarGastoNoEncontrado()
            return
        }

        enlazarVistas()
        configurarSpinnerConversion()

        findViewById<Button>(R.id.buttonEditarGasto).setOnClickListener {
            startActivity(
                Intent(this, FormularioGastoActivity::class.java)
                    .putExtra(FormularioGastoActivity.EXTRA_ID_GASTO, idGasto)
            )
        }
        findViewById<Button>(R.id.buttonEliminarGasto).setOnClickListener {
            confirmarEliminacion()
        }
    }

    private fun enlazarVistas() {
        spinnerMonedaConversion = findViewById(R.id.spinnerMonedaConversion)
        textViewMontoConvertido = findViewById(R.id.textViewMontoConvertido)
        textViewInfoTasa = findViewById(R.id.textViewInfoTasa)
        textViewConversionStatus = findViewById(R.id.textViewConversionStatus)
    }

    private fun configurarSpinnerConversion() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.conversion_currencies,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerMonedaConversion.adapter = adapter

        spinnerMonedaConversion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                actualizarMontoConvertido()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized && idGasto != INVALID_ID) {
            cargarGasto()
            cargarTasasYConversion()
        }
    }

    private fun cargarGasto() {
        gasto = databaseHelper.obtenerGastosConCategoria()
            .firstOrNull { it.idGasto == idGasto }

        val gastoActual = gasto
        if (gastoActual == null) {
            mostrarGastoNoEncontrado()
            return
        }

        findViewById<TextView>(R.id.textViewDetalleDescripcion).text = gastoActual.descripcion
        findViewById<TextView>(R.id.textViewDetalleMonto).text = getString(
            R.string.item_amount,
            gastoActual.monto,
            gastoActual.moneda
        ).replace(",", ".")
        findViewById<TextView>(R.id.textViewDetalleFecha).text = gastoActual.fecha
        findViewById<TextView>(R.id.textViewDetalleCategoria).text = gastoActual.nombreCategoria
        findViewById<TextView>(R.id.textViewDetalleMoneda).text = gastoActual.moneda
        findViewById<TextView>(R.id.textViewDetalleNota).text =
            gastoActual.nota ?: getString(R.string.detail_no_note)

        // Seleccionar por defecto moneda distinta a la del gasto
        if (gastoActual.moneda.equals("USD", ignoreCase = true)) {
            val eurIndex = (0 until spinnerMonedaConversion.adapter.count).firstOrNull {
                spinnerMonedaConversion.adapter.getItem(it)?.toString() == "EUR"
            } ?: 0
            spinnerMonedaConversion.setSelection(eurIndex)
        } else {
            val usdIndex = (0 until spinnerMonedaConversion.adapter.count).firstOrNull {
                spinnerMonedaConversion.adapter.getItem(it)?.toString() == "USD"
            } ?: 0
            spinnerMonedaConversion.setSelection(usdIndex)
        }
    }

    private fun cargarTasasYConversion() {
        lifecycleScope.launch {
            val result = exchangeRateRepository.getExchangeRates("USD")
            exchangeRateResult = result

            val statusText = if (result.isFromCache) {
                getString(R.string.rates_status_cached) + " • " + (result.lastUpdated ?: "Caché")
            } else {
                getString(R.string.rates_status_online) + " • " + getString(R.string.detail_conversion_source)
            }
            textViewConversionStatus.text = statusText

            actualizarMontoConvertido()
        }
    }

    private fun actualizarMontoConvertido() {
        val gastoActual = gasto ?: return
        val result = exchangeRateResult ?: return
        val monedaDestino = spinnerMonedaConversion.selectedItem?.toString() ?: "EUR"

        val montoConvertido = exchangeRateRepository.convertir(
            monto = gastoActual.monto,
            monedaOrigen = gastoActual.moneda,
            monedaDestino = monedaDestino,
            rates = result.rates
        )

        val tasaUnitaria = exchangeRateRepository.convertir(
            monto = 1.0,
            monedaOrigen = gastoActual.moneda,
            monedaDestino = monedaDestino,
            rates = result.rates
        )

        if (montoConvertido != null && tasaUnitaria != null) {
            textViewMontoConvertido.text = getString(
                R.string.detail_converted_amount,
                montoConvertido,
                monedaDestino
            ).replace(",", ".")

            textViewInfoTasa.text = getString(
                R.string.detail_exchange_rate_info,
                gastoActual.moneda,
                tasaUnitaria,
                monedaDestino
            ).replace(",", ".")
        } else {
            textViewMontoConvertido.text = getString(R.string.detail_conversion_unavailable)
            textViewInfoTasa.text = ""
        }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.detail_delete_title)
            .setMessage(R.string.detail_delete_message)
            .setNegativeButton(R.string.detail_delete_cancel, null)
            .setPositiveButton(R.string.detail_delete_confirm) { _, _ ->
                eliminarGasto()
            }
            .show()
    }

    private fun eliminarGasto() {
        val filasEliminadas = databaseHelper.eliminarGasto(idGasto)
        if (filasEliminadas > 0) {
            Toast.makeText(this, R.string.detail_deleted, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            mostrarGastoNoEncontrado()
        }
    }

    private fun mostrarGastoNoEncontrado() {
        Toast.makeText(this, R.string.detail_not_found, Toast.LENGTH_LONG).show()
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ID_GASTO = "extra_id_gasto"
        private const val INVALID_ID = -1L
    }
}
