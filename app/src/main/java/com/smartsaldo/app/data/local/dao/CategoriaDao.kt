package com.smartsaldo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartsaldo.app.data.local.entities.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias WHERE esDefault = 1 OR usuarioId = :usuarioId")
    fun getCategorias(usuarioId: String): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE tipo = :tipo AND (esDefault = 1 OR usuarioId = :usuarioId)")
    fun getCategoriasPorTipo(tipo: String, usuarioId: String): Flow<List<Categoria>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoria(categoria: Categoria): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategorias(categorias: List<Categoria>)

    @Update
    suspend fun updateCategoria(categoria: Categoria)

    @Delete
    suspend fun deleteCategoria(categoria: Categoria)

    @Query("DELETE FROM categorias WHERE usuarioId = :usuarioId AND esDefault = 0")
    suspend fun deleteCategoriasPersonalizadas(usuarioId: String)
}