package com.smartsaldo.app.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartsaldo.app.db.entities.Gasto

@Dao
interface GastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gasto: Gasto)

    @Query("SELECT * FROM gastos WHERE usuarioId = :usuarioId")
    fun getGastosUsuario(usuarioId: Long): LiveData<List<Gasto>>

    @Query("SELECT SUM(precio) FROM gastos WHERE usuarioId = :usuarioId")
    fun getTotalGastos(usuarioId: Long): LiveData<Float?>

    @Delete
    suspend fun delete(gasto: Gasto)
}