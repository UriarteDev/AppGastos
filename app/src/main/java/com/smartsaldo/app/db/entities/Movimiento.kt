package com.smartsaldo.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: String, // "INGRESO", "GASTO_FIJO", "AHORRO", "GASTO"
    val monto: Float,
    val fecha: Long = System.currentTimeMillis(),
    val usuarioId: Long,
    val nombre: String? = null
)
