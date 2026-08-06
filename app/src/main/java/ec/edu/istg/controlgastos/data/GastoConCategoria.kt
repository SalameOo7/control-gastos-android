package ec.edu.istg.controlgastos.data

data class GastoConCategoria(
    val idGasto: Long,
    val descripcion: String,
    val monto: Double,
    val fecha: String,
    val idCategoria: Long,
    val nombreCategoria: String,
    val moneda: String,
    val nota: String?
)
