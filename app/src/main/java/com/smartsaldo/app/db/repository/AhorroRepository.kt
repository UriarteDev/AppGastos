package com.smartsaldo.app.db.repository

import com.smartsaldo.app.db.dao.AhorroDao
import com.smartsaldo.app.db.entities.Ahorro
import com.smartsaldo.app.db.entities.AporteAhorro
import kotlinx.coroutines.flow.Flow

class AhorroRepository(private val dao: AhorroDao) {

    fun getAhorros(usuarioId: String): Flow<List<Ahorro>> {
        return dao.getAhorros(usuarioId)
    }

    suspend fun insertarAhorro(ahorro: Ahorro): Long {
        return dao.insertAhorro(ahorro)
    }

    suspend fun actualizarAhorro(ahorro: Ahorro) {
        dao.updateAhorro(ahorro)
    }

    suspend fun eliminarAhorro(ahorro: Ahorro) {
        dao.deleteAhorro(ahorro)
    }

    fun getAportes(ahorroId: Long): Flow<List<AporteAhorro>> {
        return dao.getAportes(ahorroId)
    }

    suspend fun agregarAporte(aporte: AporteAhorro) {
        dao.agregarAporte(aporte)
    }

    suspend fun getAhorroById(id: Long): Ahorro? {
        return dao.getAhorroById(id)
    }
}