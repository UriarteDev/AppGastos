package com.smartsaldo.app.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.smartsaldo.app.db.entities.Usuario

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    suspend fun getUsuarioPorUid(uid: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun getUsuarioPorEmail(email: String): Usuario?

    @Update
    suspend fun updateUsuario(usuario: Usuario)

    @Delete
    suspend fun deleteUsuario(usuario: Usuario)

    @Query("UPDATE usuarios SET isActive = 0")
    suspend fun deactivateAllUsers()

    @Query("SELECT * FROM usuarios WHERE isActive = 1 LIMIT 1")
    suspend fun getUsuarioActivo(): Usuario?
}