package com.smartsaldo.app.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ahorros",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["uid"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["usuarioId"])]
)
data class Ahorro(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val metaMonto: Double,
    val montoActual: Double = 0.0,
    val usuarioId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "aportes_ahorro",
    foreignKeys = [
        ForeignKey(
            entity = Ahorro::class,
            parentColumns = ["id"],
            childColumns = ["ahorroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ahorroId"])]
)
data class AporteAhorro(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ahorroId: Long,
    val monto: Double,
    val nota: String?,
    val fecha: Long = System.currentTimeMillis()
)