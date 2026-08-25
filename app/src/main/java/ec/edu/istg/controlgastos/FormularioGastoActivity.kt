package ec.edu.istg.controlgastos

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import ec.edu.istg.controlgastos.data.Categoria
import ec.edu.istg.controlgastos.data.DatabaseHelper
import ec.edu.istg.controlgastos.data.Gasto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FormularioGastoActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var categorias: List<Categoria>
    private lateinit var editTextDescripcion: EditText
    private lateinit var editTextMonto: EditText
    private lateinit var editTextFecha: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerMoneda: Spinner
    private lateinit var editTextNota: EditText
    private var idGastoEnEdicion: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_gasto)

        databaseHelper = DatabaseHelper(this)
        enlazarVistas()
        cargarCategorias()
        configurarMonedas()
        cargarGastoSiCorresponde()

        findViewById<Button>(R.id.buttonGuardar).setOnClickListener {
            guardarGasto()
        }
        findViewById<Button>(R.id.buttonCancelar).setOnClickListener {
            finish()
        }
    }

    private fun enlazarVistas() {
        editTextDescripcion = findViewById(R.id.editTextDescripcion)
        editTextMonto = findViewById(R.id.editTextMonto)
        editTextFecha = findViewById(R.id.editTextFecha)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerMoneda = findViewById(R.id.spinnerMoneda)
        editTextNota = findViewById(R.id.editTextNota)

        editTextFecha.isFocusable = false
        editTextFecha.isClickable = true
        editTextFecha.setOnClickListener {
            mostrarSelectorFecha()
        }
        findViewById<TextInputLayout>(R.id.textInputLayoutFecha)?.setStartIconOnClickListener {
            mostrarSelectorFecha()
        }
    }

    private fun mostrarSelectorFecha() {
        val fechaTexto = editTextFecha.text.toString().trim()
        val calendario = Calendar.getInstance()

        if (esFechaValida(fechaTexto)) {
            try {
                val partes = fechaTexto.split("-")
                if (partes.size == 3) {
                    val anio = partes[0].toInt()
                    val mes = partes[1].toInt() - 1
                    val dia = partes[2].toInt()
                    calendario.set(anio, mes, dia)
                }
            } catch (_: Exception) {}
        }

        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val fechaFormateada = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, dia)
                editTextFecha.setText(fechaFormateada)
                editTextFecha.error = null
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun cargarGastoSiCorresponde() {
        if (!intent.hasExtra(EXTRA_ID_GASTO)) {
            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            editTextFecha.setText(hoy)
            return
        }

        val idGasto = intent.getLongExtra(EXTRA_ID_GASTO, INVALID_ID)
        val gasto = databaseHelper.obtenerGastoPorId(idGasto)
        if (gasto == null) {
            Toast.makeText(this, R.string.form_gasto_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        idGastoEnEdicion = idGasto
        findViewById<TextView>(R.id.textViewFormTitle).setText(R.string.form_edit_title)
        editTextDescripcion.setText(gasto.descripcion)
        editTextMonto.setText(gasto.monto.toString())
        editTextFecha.setText(gasto.fecha)
        editTextNota.setText(gasto.nota.orEmpty())

        val categoriaPosition = categorias.indexOfFirst {
            it.idCategoria == gasto.idCategoria
        }
        if (categoriaPosition >= 0) {
            spinnerCategoria.setSelection(categoriaPosition)
        }

        val monedaPosition = (0 until spinnerMoneda.adapter.count).firstOrNull { position ->
            spinnerMoneda.adapter.getItem(position)?.toString() == gasto.moneda
        } ?: -1
        if (monedaPosition >= 0) {
            spinnerMoneda.setSelection(monedaPosition)
        }
    }

    private fun cargarCategorias() {
        categorias = databaseHelper.obtenerCategorias()
        val nombresCategorias = categorias.map { it.nombre }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresCategorias
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCategoria.adapter = adapter
    }

    private fun configurarMonedas() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.currency_options,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerMoneda.adapter = adapter
    }

    private fun guardarGasto() {
        val descripcion = editTextDescripcion.text.toString().trim()
        val montoTexto = editTextMonto.text.toString().trim()
        val fecha = editTextFecha.text.toString().trim()

        if (descripcion.isEmpty()) {
            editTextDescripcion.error = getString(R.string.form_required_field)
            editTextDescripcion.requestFocus()
            return
        }

        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            editTextMonto.error = getString(R.string.form_positive_amount)
            editTextMonto.requestFocus()
            return
        }

        if (!esFechaValida(fecha)) {
            editTextFecha.error = getString(R.string.form_invalid_date)
            editTextFecha.requestFocus()
            return
        }

        if (categorias.isEmpty()) {
            Toast.makeText(this, R.string.form_no_categories, Toast.LENGTH_LONG).show()
            return
        }

        val gasto = Gasto(
            descripcion = descripcion,
            monto = monto,
            fecha = fecha,
            idCategoria = categorias[spinnerCategoria.selectedItemPosition].idCategoria,
            moneda = spinnerMoneda.selectedItem.toString(),
            nota = editTextNota.text.toString().trim().ifEmpty { null }
        )

        try {
            val idGasto = idGastoEnEdicion
            if (idGasto == null) {
                databaseHelper.insertarGasto(gasto)
                Toast.makeText(this, R.string.form_saved, Toast.LENGTH_SHORT).show()
            } else {
                val filasActualizadas = databaseHelper.actualizarGasto(
                    gasto.copy(idGasto = idGasto)
                )
                if (filasActualizadas == 0) {
                    Toast.makeText(this, R.string.form_update_error, Toast.LENGTH_LONG).show()
                    return
                }
                Toast.makeText(this, R.string.form_updated, Toast.LENGTH_SHORT).show()
            }
            setResult(RESULT_OK)
            finish()
        } catch (exception: Exception) {
            Toast.makeText(this, R.string.form_save_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun esFechaValida(fecha: String): Boolean {
        if (!fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            return false
        }

        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }.parse(fecha) != null
        } catch (exception: Exception) {
            false
        }
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
