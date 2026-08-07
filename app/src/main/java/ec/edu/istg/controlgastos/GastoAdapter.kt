package ec.edu.istg.controlgastos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ec.edu.istg.controlgastos.data.GastoConCategoria
import java.util.Locale

class GastoAdapter(
    gastosIniciales: List<GastoConCategoria>,
    private val onGastoSeleccionado: (Long) -> Unit
) : RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {

    private var gastos: List<GastoConCategoria> = gastosIniciales

    fun actualizarLista(nuevosGastos: List<GastoConCategoria>) {
        gastos = nuevosGastos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        holder.bind(gastos[position])
    }

    override fun getItemCount(): Int = gastos.size

    inner class GastoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewDescripcion: TextView = itemView.findViewById(R.id.textViewItemDescripcion)
        private val textViewCategoria: TextView = itemView.findViewById(R.id.textViewItemCategoria)
        private val textViewMonto: TextView = itemView.findViewById(R.id.textViewItemMonto)
        private val textViewFecha: TextView = itemView.findViewById(R.id.textViewItemFecha)

        fun bind(gasto: GastoConCategoria) {
            textViewDescripcion.text = gasto.descripcion
            textViewCategoria.text = itemView.context.getString(
                R.string.item_category,
                gasto.nombreCategoria
            )
            textViewMonto.text = itemView.context.getString(
                R.string.item_amount,
                gasto.monto,
                gasto.moneda
            ).replace(",", ".")
            textViewFecha.text = itemView.context.getString(R.string.item_date, gasto.fecha)
            itemView.contentDescription = gasto.descripcion
            itemView.setOnClickListener {
                onGastoSeleccionado(gasto.idGasto)
            }
        }
    }
}
