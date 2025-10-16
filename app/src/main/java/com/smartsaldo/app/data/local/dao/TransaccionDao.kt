package com.smartsaldo.app.data.local.dao

import androidx.room.*
import com.smartsaldo.app.data.local.entities.TipoTransaccion
import com.smartsaldo.app.data.local.entities.Transaccion
import com.smartsaldo.app.data.local.entities.TransaccionConCategoria
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaccionDao {

    @Query("""
        SELECT t.*, 
               c.id as categoria_id,
               c.nombre as categoria_nombre, 
               c.icono as categoria_icono,
               c.color as categoria_color,
               c.tipo as categoria_tipo,
               c.esDefault as categoria_esDefault,
               c.usuarioId as categoria_usuarioId
        FROM transacciones t
        LEFT JOIN categorias c ON t.categoriaId = c.id
        WHERE t.usuarioId = :usuarioId
        ORDER BY t.fecha DESC
    """)
    fun getTransaccionesConCategoria(usuarioId: String): Flow<List<TransaccionConCategoria>>

    @Query("""
        SELECT t.*, 
               c.id as categoria_id,
               c.nombre as categoria_nombre, 
               c.icono as categoria_icono,
               c.color as categoria_color,
               c.tipo as categoria_tipo,
               c.esDefault as categoria_esDefault,
               c.usuarioId as categoria_usuarioId
        FROM transacciones t 
        LEFT JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId 
        AND t.fecha BETWEEN :fechaInicio AND :fechaFin
        ORDER BY t.fecha DESC
    """)
    fun getTransaccionesPorFecha(
        usuarioId: String,
        fechaInicio: Long,
        fechaFin: Long
    ): Flow<List<TransaccionConCategoria>>

    @Query("""
        SELECT t.*, 
               c.id as categoria_id,
               c.nombre as categoria_nombre, 
               c.icono as categoria_icono,
               c.color as categoria_color,
               c.tipo as categoria_tipo,
               c.esDefault as categoria_esDefault,
               c.usuarioId as categoria_usuarioId
        FROM transacciones t 
        LEFT JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId AND t.categoriaId = :categoriaId
        ORDER BY t.fecha DESC
    """)
    fun getTransaccionesPorCategoria(
        usuarioId: String,
        categoriaId: Long
    ): Flow<List<TransaccionConCategoria>>

    @Query("""
        SELECT t.*, 
               c.id as categoria_id,
               c.nombre as categoria_nombre, 
               c.icono as categoria_icono,
               c.color as categoria_color,
               c.tipo as categoria_tipo,
               c.esDefault as categoria_esDefault,
               c.usuarioId as categoria_usuarioId
        FROM transacciones t 
        LEFT JOIN categorias c ON t.categoriaId = c.id 
        WHERE t.usuarioId = :usuarioId 
        AND (t.descripcion LIKE '%' || :busqueda || '%' OR t.notas LIKE '%' || :busqueda || '%')
        ORDER BY t.fecha DESC
    """)
    fun buscarTransacciones(
        usuarioId: String,
        busqueda: String
    ): Flow<List<TransaccionConCategoria>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: Transaccion): Long

    @Update
    suspend fun updateTransaccion(transaccion: Transaccion)

    @Delete
    suspend fun deleteTransaccion(transaccion: Transaccion)

    @Query("SELECT * FROM transacciones WHERE id = :id")
    suspend fun getTransaccionPorId(id: Long): Transaccion?

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