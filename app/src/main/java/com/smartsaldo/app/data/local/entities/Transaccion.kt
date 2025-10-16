package com.smartsaldo.app.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transacciones",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["uid"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["categoriaId"]),
        Index(value = ["fecha"])
    ]
)
data class Transaccion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monto: Double,
    val descripcion: String,
    val notas: String?,
    val fecha: Long, // timestamp
    val categoriaId: Long,
    val usuarioId: String,
    val tipo: TipoTransaccion, // enum
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TipoTransaccion {
    INGRESO, GASTO
}

// ===== DATA CLASS PARA VISTA CON CATEGORIA =====
data class TransaccionConCategoria(
    @Embedded val transaccion: Transaccion,

    @Embedded(prefix = "categoria_")
    val categoria: Categoria?
)