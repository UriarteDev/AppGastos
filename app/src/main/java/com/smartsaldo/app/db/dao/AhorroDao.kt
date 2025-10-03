package com.smartsaldo.app.db.dao

import androidx.room.*
import com.smartsaldo.app.db.entities.Ahorro
import com.smartsaldo.app.db.entities.AporteAhorro
import kotlinx.coroutines.flow.Flow

@Dao
interface AhorroDao {

    @Query("SELECT * FROM ahorros WHERE usuarioId = :usuarioId ORDER BY createdAt DESC")
    fun getAhorros(usuarioId: String): Flow<List<Ahorro>>

    @Query("SELECT * FROM ahorros WHERE id = :id")
    suspend fun getAhorroById(id: Long): Ahorro?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAhorro(ahorro: Ahorro): Long

    @Update
    suspend fun updateAhorro(ahorro: Ahorro)

    @Delete
    suspend fun deleteAhorro(ahorro: Ahorro)

    @Query("SELECT * FROM aportes_ahorro WHERE ahorroId = :ahorroId ORDER BY fecha DESC")
    fun getAportes(ahorroId: Long): Flow<List<AporteAhorro>>

    @Insert
    suspend fun insertAporte(aporte: AporteAhorro): Long

    @Transaction
    suspend fun agregarAporte(aporte: AporteAhorro) {
        insertAporte(aporte)
        val ahorro = getAhorroById(aporte.ahorroId) ?: return
        val nuevoMonto = ahorro.montoActual + aporte.monto
        updateAhorro(ahorro.copy(montoActual = nuevoMonto, updatedAt = System.currentTimeMillis()))
    }
}