package com.example.myapplication.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.db.entities.Movimiento
import com.example.myapplication.db.repository.FinanzasRepository

@Dao
interface MovimientoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos WHERE usuarioId = :usuarioId")
    fun obtenerPorUsuario(usuarioId: Long): LiveData<List<Movimiento>>
}