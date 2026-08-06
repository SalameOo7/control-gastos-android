package ec.edu.istg.controlgastos.data

data class Gasto(
    val idGasto: Long = 0,
    val descripcion: String,
    val monto: Double,
    val fecha: String,
    val idCategoria: Long,
    val moneda: String = "USD",
    val nota: String? = null
)
