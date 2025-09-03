package com.smartsaldo.app.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.smartsaldo.app.db.entities.Usuario

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: Usuario): Long {
        return TODO("Provide the return value")
    }
    suspend fun update(usuario: Usuario) {
    }

    @Delete
    suspend fun delete(usuario: Usuario) {
    }

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Usuario? {
        return TODO("Provide the return value")
    }

    @Query("SELECT * FROM usuarios ORDER BY id LIMIT 1")
    suspend fun getPrimero(): Usuario? {
        return TODO("Provide the return value")
    }

    /**
     * Devuelve el primer usuario, y si no hay, crea uno por defecto.
     */
    @Transaction
    suspend fun getOrCreateDefault(): Usuario {
        val actual = getPrimero()
        if (actual != null) return actual
        val nuevo = Usuario(nombre = "Usuario")
        val newId = insert(nuevo).toLong()
        return getById(newId)!!
    }
}