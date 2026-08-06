package ec.edu.istg.controlgastos.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE categorias (
                id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                descripcion TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE gastos (
                id_gasto INTEGER PRIMARY KEY AUTOINCREMENT,
                descripcion TEXT NOT NULL,
                monto REAL NOT NULL CHECK (monto > 0),
                fecha TEXT NOT NULL,
                id_categoria INTEGER NOT NULL,
                moneda TEXT NOT NULL DEFAULT 'USD',
                nota TEXT,
                FOREIGN KEY (id_categoria)
                    REFERENCES categorias(id_categoria)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            )
            """.trimIndent()
        )

        val categoryIds = insertInitialCategories(db)
        insertInitialExpenses(db, categoryIds)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS gastos")
        db.execSQL("DROP TABLE IF EXISTS categorias")
        onCreate(db)
    }

    private fun insertInitialCategories(db: SQLiteDatabase): Map<String, Long> {
        val categories = listOf(
            "Alimentación" to "Comidas, supermercado y bebidas",
            "Transporte" to "Movilidad y transporte",
            "Vivienda" to "Arriendo, servicios y mantenimiento"
        )

        return categories.associate { (name, description) ->
            val values = ContentValues().apply {
                put("nombre", name)
                put("descripcion", description)
            }
            name to db.insertOrThrow("categorias", null, values)
        }
    }

    private fun insertInitialExpenses(
        db: SQLiteDatabase,
        categoryIds: Map<String, Long>
    ) {
        val foodId = categoryIds.getValue("Alimentación")
        val transportId = categoryIds.getValue("Transporte")
        val housingId = categoryIds.getValue("Vivienda")

        val expenses = listOf(
            ExpenseSeed("Compra de supermercado", 42.50, "2026-01-05", foodId, "USD", "Compra semanal"),
            ExpenseSeed("Almuerzo", 8.75, "2026-01-08", foodId, "USD", null),
            ExpenseSeed("Café y refrigerio", 4.25, "2026-01-12", foodId, "USD", null),
            ExpenseSeed("Pasaje de bus", 1.50, "2026-01-06", transportId, "USD", null),
            ExpenseSeed("Combustible", 35.00, "2026-01-10", transportId, "USD", "Carga de combustible"),
            ExpenseSeed("Taxi", 7.00, "2026-01-15", transportId, "USD", null),
            ExpenseSeed("Servicio de internet", 28.00, "2026-01-03", housingId, "USD", null),
            ExpenseSeed("Productos de limpieza", 16.90, "2026-01-18", housingId, "USD", null)
        )

        expenses.forEach { expense ->
            val values = ContentValues().apply {
                put("descripcion", expense.descripcion)
                put("monto", expense.monto)
                put("fecha", expense.fecha)
                put("id_categoria", expense.idCategoria)
                put("moneda", expense.moneda)
                if (expense.nota == null) {
                    putNull("nota")
                } else {
                    put("nota", expense.nota)
                }
            }
            db.insertOrThrow("gastos", null, values)
        }
    }

    private data class ExpenseSeed(
        val descripcion: String,
        val monto: Double,
        val fecha: String,
        val idCategoria: Long,
        val moneda: String,
        val nota: String?
    )

    companion object {
        const val DATABASE_NAME = "control_gastos.db"
        const val DATABASE_VERSION = 1
    }
}
