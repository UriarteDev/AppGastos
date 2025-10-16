package com.smartsaldo.app.data.repository

import kotlinx.coroutines.tasks.await
import com.smartsaldo.app.data.local.dao.EstadisticaMensual
import com.smartsaldo.app.data.local.dao.TotalPorCategoria
import com.smartsaldo.app.data.local.dao.TransaccionDao
import kotlinx.coroutines.flow.Flow
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import com.smartsaldo.app.data.local.entities.TipoTransaccion
import com.smartsaldo.app.data.local.entities.Transaccion
import com.smartsaldo.app.data.local.entities.TransaccionConCategoria

class TransaccionRepository(private val dao: TransaccionDao) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getTransacciones(usuarioId: String): Flow<List<TransaccionConCategoria>> {
        return dao.getTransaccionesConCategoria(usuarioId)
    }

    fun getTransaccionesDelMes(usuarioId: String, year: Int, month: Int): Flow<List<TransaccionConCategoria>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val inicioMes = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val finMes = calendar.timeInMillis

        return dao.getTransaccionesPorFecha(usuarioId, inicioMes, finMes)
    }

    fun getTransaccionesPorFecha(usuarioId: String, fechaInicio: Long, fechaFin: Long): Flow<List<TransaccionConCategoria>> {
        return dao.getTransaccionesPorFecha(usuarioId, fechaInicio, fechaFin)
    }

    fun buscarTransacciones(usuarioId: String, busqueda: String): Flow<List<TransaccionConCategoria>> {
        return dao.buscarTransacciones(usuarioId, busqueda)
    }

    fun getTransaccionesPorCategoria(usuarioId: String, categoriaId: Long): Flow<List<TransaccionConCategoria>> {
        return dao.getTransaccionesPorCategoria(usuarioId, categoriaId)
    }

    suspend fun insertarTransaccion(transaccion: Transaccion): Long {
        val id = dao.insertTransaccion(transaccion)

        try {
            firestore.collection("usuarios")
                .document(transaccion.usuarioId)
                .collection("transacciones")
                .document(id.toString())
                .set(transaccion.copy(id = id))
                .await()
        } catch (e: Exception) {
            android.util.Log.e("TransaccionRepository", "Error guardando en Firestore", e)
        }

        return id
    }

    suspend fun actualizarTransaccion(transaccion: Transaccion) {
        val transaccionActualizada = transaccion.copy(updatedAt = System.currentTimeMillis())
        dao.updateTransaccion(transaccionActualizada)

        try {
            firestore.collection("usuarios")
                .document(transaccion.usuarioId)
                .collection("transacciones")
                .document(transaccionActualizada.id.toString())
                .set(transaccionActualizada)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("TransaccionRepository", "Error actualizando en Firestore", e)
        }
    }

    suspend fun eliminarTransaccion(transaccion: Transaccion) {
        dao.deleteTransaccion(transaccion)

        try {
            firestore.collection("usuarios")
                .document(transaccion.usuarioId)
                .collection("transacciones")
                .document(transaccion.id.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("TransaccionRepository", "Error eliminando de Firestore", e)
        }
    }

    suspend fun getTransaccionPorId(id: Long): Transaccion? {
        return dao.getTransaccionPorId(id)
    }

    fun getEstadisticasMensuales(usuarioId: String): Flow<List<EstadisticaMensual>> {
        return dao.getEstadisticasMensuales(usuarioId)
    }

    fun getTotalPorCategoria(
        usuarioId: String,
        year: Int,
        month: Int,
        tipo: TipoTransaccion
    ): Flow<List<TotalPorCategoria>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val inicioMes = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val finMes = calendar.timeInMillis

        return dao.getTotalPorCategoria(usuarioId, inicioMes, finMes, tipo)
    }
}