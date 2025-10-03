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
                .set(ahorro)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
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
                .set(ahorro)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
        }
    }

    fun getAportes(ahorroId: Long): Flow<List<AporteAhorro>> {
        return dao.getAportes(ahorroId)
    }

    suspend fun agregarAporte(aporte: AporteAhorro) {
        dao.agregarAporte(aporte)
        try {
            firestore.collection("usuarios")
                .document(aporte.usuarioId)
                .collection("ahorros")
                .document(aporte.ahorroId.toString())
                .collection("aportes")
                .document(aporte.id.toString())
                .set(aporte)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAhorroById(id: Long): Ahorro? {
        return dao.getAhorroById(id)
    }
}
