package com.smartsaldo.app.db.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.smartsaldo.app.db.dao.AhorroDao
import com.smartsaldo.app.db.entities.Ahorro
import com.smartsaldo.app.db.entities.AporteAhorro
import kotlinx.coroutines.flow.Flow

class AhorroRepository(private val dao: AhorroDao) {

    private val firestore = FirebaseFirestore.getInstance()

    fun getAhorros(usuarioId: String): Flow<List<Ahorro>> {
        return dao.getAhorros(usuarioId)
    }

    suspend fun insertarAhorro(ahorro: Ahorro): Long {
        val id = dao.insertAhorro(ahorro)
        try {
            firestore.collection("usuarios")
                .document(ahorro.usuarioId)
                .collection("ahorros")
                .document(id.toString())
                .set(mapOf(
                    "id" to id,
                    "nombre" to ahorro.nombre,
                    "metaMonto" to ahorro.metaMonto,
                    "montoActual" to ahorro.montoActual,
                    "createdAt" to ahorro.createdAt,
                    "updatedAt" to ahorro.updatedAt
                ))
                .await()
        } catch (e: Exception) {
            android.util.Log.e("AhorroRepository", "Error guardando ahorro en Firestore", e)
        }
        return id
    }

    suspend fun actualizarAhorro(ahorro: Ahorro) {
        dao.updateAhorro(ahorro)
        try {
            firestore.collection("usuarios")
                .document(ahorro.usuarioId)
                .collection("ahorros")
                .document(ahorro.id.toString())
                .set(mapOf(
                    "id" to ahorro.id,
                    "nombre" to ahorro.nombre,
                    "metaMonto" to ahorro.metaMonto,
                    "montoActual" to ahorro.montoActual,
                    "createdAt" to ahorro.createdAt,
                    "updatedAt" to ahorro.updatedAt
                ))
                .await()
        } catch (e: Exception) {
            android.util.Log.e("AhorroRepository", "Error actualizando ahorro en Firestore", e)
        }
    }

    suspend fun eliminarAhorro(ahorro: Ahorro) {
        dao.deleteAhorro(ahorro)
        try {
            firestore.collection("usuarios")
                .document(ahorro.usuarioId)
                .collection("ahorros")
                .document(ahorro.id.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("AhorroRepository", "Error eliminando ahorro en Firestore", e)
        }
    }

    fun getAportes(ahorroId: Long): Flow<List<AporteAhorro>> {
        return dao.getAportes(ahorroId)
    }

    suspend fun agregarAporte(aporte: AporteAhorro) {
        dao.agregarAporte(aporte)

        try {
            // Guardar el aporte en Firestore
            val aporteId = aporte.id
            firestore.collection("usuarios")
                .document(aporte.usuarioId)
                .collection("ahorros")
                .document(aporte.ahorroId.toString())
                .collection("aportes")
                .document(aporteId.toString())
                .set(mapOf(
                    "id" to aporteId,
                    "ahorroId" to aporte.ahorroId,
                    "monto" to aporte.monto,
                    "nota" to aporte.nota,
                    "fecha" to aporte.fecha
                ))
                .await()

            // Actualizar el ahorro en Firestore
            val ahorro = dao.getAhorroById(aporte.ahorroId)
            ahorro?.let {
                firestore.collection("usuarios")
                    .document(it.usuarioId)
                    .collection("ahorros")
                    .document(it.id.toString())
                    .update(mapOf(
                        "montoActual" to it.montoActual,
                        "updatedAt" to it.updatedAt
                    ))
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("AhorroRepository", "Error guardando aporte en Firestore", e)
        }
    }

    suspend fun getAhorroById(id: Long): Ahorro? {
        return dao.getAhorroById(id)
    }
}