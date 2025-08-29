package com.example.myapplication.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.db.entities.Finanzas

@Dao
interface FinanzasDao {

    @Query("SELECT * FROM finanzas ORDER BY id DESC LIMIT 1")
    fun getUltimaFinanza(): LiveData<Finanzas?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(finanzas: Finanzas)

    @Update
    suspend fun update(finanzas: Finanzas)

    @Delete
    suspend fun delete(finanzas: Finanzas)
}
