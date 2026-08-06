package ec.edu.istg.controlgastos

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.istg.controlgastos.data.Categoria
import ec.edu.istg.controlgastos.data.DatabaseHelper
import ec.edu.istg.controlgastos.data.Gasto
import java.text.SimpleDateFormat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_gasto)

        databaseHelper = DatabaseHelper(this)
        enlazarVistas()
        cargarCategorias()
        configurarMonedas()

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
            databaseHelper.insertarGasto(gasto)
            Toast.makeText(this, R.string.form_saved, Toast.LENGTH_SHORT).show()
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
        databaseHelper.close()
        super.onDestroy()
    }
}
