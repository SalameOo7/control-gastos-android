package ec.edu.istg.controlgastos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ec.edu.istg.controlgastos.data.DatabaseHelper
import ec.edu.istg.controlgastos.data.GastoConCategoria

class DetalleGastoActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private var gasto: GastoConCategoria? = null
    private var idGasto: Long = INVALID_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_gasto)

        databaseHelper = DatabaseHelper(this)
        idGasto = intent.getLongExtra(EXTRA_ID_GASTO, INVALID_ID)
        if (idGasto == INVALID_ID) {
            mostrarGastoNoEncontrado()
            return
        }

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

    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized && idGasto != INVALID_ID) {
            cargarGasto()
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
