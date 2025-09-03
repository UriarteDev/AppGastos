package com.smartsaldo.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finanzas")
data class Finanzas(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saldo: Float,
    val gastoFijo: Float,
    val ahorro: Float,
    val usuarioId: Long
)