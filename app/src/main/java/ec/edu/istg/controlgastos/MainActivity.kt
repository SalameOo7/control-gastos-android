package ec.edu.istg.controlgastos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ec.edu.istg.controlgastos.data.DatabaseHelper
import ec.edu.istg.controlgastos.data.repository.ExchangeRateRepository
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var gastoAdapter: GastoAdapter
    private lateinit var recyclerViewGastos: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var textViewEmptyList: TextView
    private lateinit var textViewRatesStatus: TextView
    private lateinit var textViewRatesUpdate: TextView
    private lateinit var textViewRateEur: TextView
    private lateinit var textViewRateCop: TextView
    private lateinit var textViewRateMxn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)
        exchangeRateRepository = ExchangeRateRepository(this)

        gastoAdapter = GastoAdapter(emptyList()) { idGasto ->
            startActivity(
                Intent(this, DetalleGastoActivity::class.java)
                    .putExtra(DetalleGastoActivity.EXTRA_ID_GASTO, idGasto)
            )
        }

        recyclerViewGastos = findViewById(R.id.recyclerViewGastos)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        textViewEmptyList = findViewById(R.id.textViewEmptyList)
        textViewRatesStatus = findViewById(R.id.textViewRatesStatus)
        textViewRatesUpdate = findViewById(R.id.textViewRatesUpdate)
        textViewRateEur = findViewById(R.id.textViewRateEur)
        textViewRateCop = findViewById(R.id.textViewRateCop)
        textViewRateMxn = findViewById(R.id.textViewRateMxn)

        recyclerViewGastos.layoutManager = LinearLayoutManager(this)
        recyclerViewGastos.adapter = gastoAdapter

        swipeRefreshLayout.setOnRefreshListener {
            refrescarDatos()
        }

        findViewById<Button>(R.id.buttonNuevoGasto).setOnClickListener {
            startActivity(Intent(this, FormularioGastoActivity::class.java))
        }

        findViewById<ImageButton>(R.id.imageButtonLogout).setOnClickListener {
            cerrarSesion()
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
        cargarTasasDeCambio()
    }

    private fun refrescarDatos() {
        if (::databaseHelper.isInitialized) {
            cargarGastos()
        }
        lifecycleScope.launch {
            consultarTasas()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun cargarGastos() {
        val gastos = databaseHelper.obtenerGastosConCategoria()
        gastoAdapter.actualizarLista(gastos)
        textViewEmptyList.visibility = if (gastos.isEmpty()) View.VISIBLE else View.GONE
        recyclerViewGastos.visibility = if (gastos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun cargarTasasDeCambio() {
        lifecycleScope.launch {
            consultarTasas()
        }
    }

    private suspend fun consultarTasas() {
        val result = exchangeRateRepository.getExchangeRates("USD")
        textViewRatesStatus.text = if (result.isFromCache) {
            getString(R.string.rates_status_cached)
        } else {
            getString(R.string.rates_status_online)
        }

        textViewRatesUpdate.text = getString(
            R.string.rates_updated,
            result.lastUpdated ?: "N/D"
        )

        val eur = result.rates["EUR"] ?: 0.0
        val cop = result.rates["COP"] ?: 0.0
        val mxn = result.rates["MXN"] ?: 0.0

        textViewRateEur.text = if (eur > 0) String.format(Locale.US, "%.3f", eur) else "--"
        textViewRateCop.text = if (cop > 0) String.format(Locale.US, "%,.1f", cop) else "--"
        textViewRateMxn.text = if (mxn > 0) String.format(Locale.US, "%.2f", mxn) else "--"
    }

    private fun cerrarSesion() {
        getSharedPreferences(LoginActivity.PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .remove(LoginActivity.USER_NAME_KEY)
            .apply()

        Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroy()
    }
}
