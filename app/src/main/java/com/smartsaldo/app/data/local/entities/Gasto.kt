package com.smartsaldo.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos")
data class Gasto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val precio: Float,
    val usuarioId: Long
)