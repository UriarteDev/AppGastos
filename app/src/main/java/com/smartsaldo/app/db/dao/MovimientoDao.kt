package com.smartsaldo.app.db.dao

import androidx.room.*
import com.smartsaldo.app.db.entities.Movimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insertarMovimiento(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos WHERE usuarioId = :usuarioId")
    fun obtenerMovimientos(usuarioId: Long): Flow<List<Movimiento>>

    // ✅ Obtener solo gastos
    @Query("SELECT * FROM movimientos WHERE usuarioId = :usuarioId AND tipo = 'GASTO'")
    fun obtenerGastos(usuarioId: Long): Flow<List<Movimiento>>

    // ✅ Eliminar un movimiento
    @Delete
    suspend fun eliminarMovimiento(movimiento: Movimiento)
}
