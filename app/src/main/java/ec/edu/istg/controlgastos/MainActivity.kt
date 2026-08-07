package ec.edu.istg.controlgastos

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.istg.controlgastos.data.DatabaseHelper

class MainActivity : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var gastoAdapter: GastoAdapter
    private lateinit var recyclerViewGastos: RecyclerView
    private lateinit var textViewEmptyList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)
        gastoAdapter = GastoAdapter(emptyList()) { idGasto ->
            startActivity(
                Intent(this, DetalleGastoActivity::class.java)
                    .putExtra(DetalleGastoActivity.EXTRA_ID_GASTO, idGasto)
            )
        }

        recyclerViewGastos = findViewById(R.id.recyclerViewGastos)
        textViewEmptyList = findViewById(R.id.textViewEmptyList)
        recyclerViewGastos.layoutManager = LinearLayoutManager(this)
        recyclerViewGastos.adapter = gastoAdapter

        findViewById<Button>(R.id.buttonNuevoGasto).setOnClickListener {
            startActivity(Intent(this, FormularioGastoActivity::class.java))
        }

        val userName = getSharedPreferences(
            LoginActivity.PREFERENCES_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.USER_NAME_KEY, getString(R.string.default_user_name))
        findViewById<TextView>(R.id.textViewWelcome).text =
            getString(R.string.welcome_user, userName)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized) {
            cargarGastos()
        }
    }

    private fun cargarGastos() {
        val gastos = databaseHelper.obtenerGastosConCategoria()
        gastoAdapter.actualizarLista(gastos)
        textViewEmptyList.visibility = if (gastos.isEmpty()) View.VISIBLE else View.GONE
        recyclerViewGastos.visibility = if (gastos.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroy()
    }
}
