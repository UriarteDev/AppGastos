package com.smartsaldo.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String?,
    val photoURL: String?,
    val provider: String, // "email", "google"
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
