package com.smartsaldo.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val icono: String,
    val color: String,
    val tipo: String, // "GASTO" o "INGRESO"
    val esDefault: Boolean = true,
    val usuarioId: String? = null // null para categorías predefinidas, no-null para personalizadas
)