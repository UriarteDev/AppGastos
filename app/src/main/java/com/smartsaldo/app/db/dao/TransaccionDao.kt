package com.smartsaldo.app.db.dao

import androidx.room.*
import com.smartsaldo.app.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaccionDao {

    // ✅ Obtener transacciones con categoría
    @Query("""
        SELECT t.*, c.nombre as categoriaNombre, c.icono, c.color, c.tipo as categoriaTipo
        FROM transacciones t 
        INNER JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId 
        ORDER BY t.fecha DESC, t.createdAt DESC
    """)
    fun getTransaccionesConCategoria(usuarioId: String): Flow<List<TransaccionConCategoria>>

    // ✅ Filtrar por rango de fechas
    @Query("""
        SELECT t.*, c.nombre as categoriaNombre, c.icono, c.color, c.tipo as categoriaTipo
        FROM transacciones t 
        INNER JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId 
        AND t.fecha BETWEEN :fechaInicio AND :fechaFin
        ORDER BY t.fecha DESC
    """)
    fun getTransaccionesPorFecha(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long
    ): Flow<List<TransaccionConCategoria>>

    // ✅ Filtrar por categoría
    @Query("""
        SELECT t.*, c.nombre as categoriaNombre, c.icono, c.color, c.tipo as categoriaTipo
        FROM transacciones t 
        INNER JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId AND t.categoriaId = :categoriaId
        ORDER BY t.fecha DESC
    """)
    fun getTransaccionesPorCategoria(
        usuarioId: String,
        categoriaId: Long
    ): Flow<List<TransaccionConCategoria>>

    // ✅ Buscar por descripción
    @Query("""
        SELECT t.*, c.nombre as categoriaNombre, c.icono, c.color, c.tipo as categoriaTipo
        FROM transacciones t 
        INNER JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId 
        AND (t.descripcion LIKE '%' || :busqueda || '%' OR t.notas LIKE '%' || :busqueda || '%')
        ORDER BY t.fecha DESC
    """)
    fun buscarTransacciones(
        usuarioId: String,
        busqueda: String
    ): Flow<List<TransaccionConCategoria>>

    // ✅ CRUD básico
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: Transaccion): Long

    @Update
    suspend fun updateTransaccion(transaccion: Transaccion)

    @Delete
    suspend fun deleteTransaccion(transaccion: Transaccion)

    @Query("SELECT * FROM transacciones WHERE id = :id")
    suspend fun getTransaccionPorId(id: Long): Transaccion?

    // ✅ Estadísticas mensuales
    @Query("""
        SELECT 
            strftime('%Y-%m', datetime(fecha/1000, 'unixepoch')) as mes,
            SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END) as totalIngresos,
            SUM(CASE WHEN tipo = 'GASTO' THEN monto ELSE 0 END) as totalGastos,
            COUNT(*) as totalTransacciones
        FROM transacciones 
        WHERE usuarioId = :usuarioId 
        GROUP BY strftime('%Y-%m', datetime(fecha/1000, 'unixepoch'))
        ORDER BY mes DESC
    """)
    fun getEstadisticasMensuales(usuarioId: String): Flow<List<EstadisticaMensual>>

    // ✅ Total por categoría (para gráfico)
    @Query("""
        SELECT 
            c.nombre as categoria,
            c.color,
            c.icono,
            SUM(t.monto) as total,
            COUNT(t.id) as cantidad
        FROM transacciones t 
        INNER JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId 
        AND t.fecha BETWEEN :fechaInicio AND :fechaFin
        AND t.tipo = :tipo
        GROUP BY c.id, c.nombre, c.color, c.icono
        ORDER BY total DESC
    """)
    fun getTotalPorCategoria(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long,
        tipo: TipoTransaccion
    ): Flow<List<TotalPorCategoria>>
}
data class EstadisticaMensual(
    val mes: String,
    val totalIngresos: Double,
    val totalGastos: Double,
    val totalTransacciones: Int
) {
    val saldo: Double get() = totalIngresos - totalGastos
}

data class TotalPorCategoria(
    val categoria: String,
    val color: String,
    val icono: String,
    val total: Double,
    val cantidad: Int
)