package com.smartsaldo.app.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.smartsaldo.app.R
import com.smartsaldo.app.data.local.dao.CategoriaDao
import com.smartsaldo.app.data.local.entities.Categoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class CategoriaRepository(
    private val dao: CategoriaDao,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getCategorias(usuarioId: String): Flow<List<Categoria>> {
        return dao.getCategorias(usuarioId)
    }

    fun getCategoriasPorTipo(tipo: String, usuarioId: String): Flow<List<Categoria>> {
        return dao.getCategoriasPorTipo(tipo, usuarioId)
    }

    suspend fun eliminarCategoriasDefault(usuarioId: String) {
        dao.deleteCategoriasDefault(usuarioId)
    }

    suspend fun insertarCategoria(categoria: Categoria): Long {
        val id = dao.insertCategoria(categoria)

        // ✅ Guardar en Firebase si es personalizada
        if (!categoria.esDefault && categoria.usuarioId != null) {
            try {
                firestore.collection("usuarios")
                    .document(categoria.usuarioId)
                    .collection("categorias")
                    .document(id.toString())
                    .set(mapOf(
                        "id" to id,
                        "nombre" to categoria.nombre,
                        "icono" to categoria.icono,
                        "color" to categoria.color,
                        "tipo" to categoria.tipo,
                        "esDefault" to false
                    ))
                    .await()
                android.util.Log.d("CategoriaRepository", "✅ Categoría guardada en Firebase")
            } catch (e: Exception) {
                android.util.Log.e("CategoriaRepository", "Error guardando en Firebase", e)
            }
        }

        return id
    }

    suspend fun insertarCategorias(categorias: List<Categoria>) {
        dao.insertCategorias(categorias)
    }

    suspend fun actualizarCategoria(categoria: Categoria) {
        dao.updateCategoria(categoria)

        // ✅ Actualizar en Firebase si es personalizada
        if (!categoria.esDefault && categoria.usuarioId != null) {
            try {
                firestore.collection("usuarios")
                    .document(categoria.usuarioId)
                    .collection("categorias")
                    .document(categoria.id.toString())
                    .set(mapOf(
                        "id" to categoria.id,
                        "nombre" to categoria.nombre,
                        "icono" to categoria.icono,
                        "color" to categoria.color,
                        "tipo" to categoria.tipo,
                        "esDefault" to false
                    ))
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("CategoriaRepository", "Error actualizando en Firebase", e)
            }
        }
    }

    suspend fun eliminarCategoria(categoria: Categoria) {
        dao.deleteCategoria(categoria)

        // ✅ Eliminar de Firebase si es personalizada
        if (!categoria.esDefault && categoria.usuarioId != null) {
            try {
                firestore.collection("usuarios")
                    .document(categoria.usuarioId)
                    .collection("categorias")
                    .document(categoria.id.toString())
                    .delete()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("CategoriaRepository", "Error eliminando de Firebase", e)
            }
        }
    }

    suspend fun eliminarCategoriasPersonalizadas(usuarioId: String) {
        dao.deleteCategoriasPersonalizadas(usuarioId)
    }

    suspend fun recrearCategoriasDefault(usuarioId: String) {
        android.util.Log.d("CategoriaRepository", "🔄 Recreando categorías con nuevo idioma...")

        // Forzar actualización del contexto con el nuevo idioma
        val contextoActualizado = com.smartsaldo.app.utils.LocaleHelper.onAttach(context)

        // Obtener las categorías actuales
        val categoriasActuales = dao.getCategorias(usuarioId).first()
        val categoriasDefault = categoriasActuales.filter { it.esDefault }

        if (categoriasDefault.isEmpty()) {
            crearCategoriasDefault(usuarioId)
            return
        }

        // Mapeo de índices a strings (usando contexto actualizado)
        val nombresNuevos = listOf(
            contextoActualizado.getString(R.string.cat_comida),
            contextoActualizado.getString(R.string.cat_transporte),
            contextoActualizado.getString(R.string.cat_ocio),
            contextoActualizado.getString(R.string.cat_salud),
            contextoActualizado.getString(R.string.cat_casa),
            contextoActualizado.getString(R.string.cat_educacion),
            contextoActualizado.getString(R.string.cat_ropa),
            contextoActualizado.getString(R.string.cat_otros_gastos),
            contextoActualizado.getString(R.string.cat_sueldo),
            contextoActualizado.getString(R.string.cat_freelance),
            contextoActualizado.getString(R.string.cat_inversiones),
            contextoActualizado.getString(R.string.cat_regalos),
            contextoActualizado.getString(R.string.cat_otros_ingresos)
        )

        android.util.Log.d("CategoriaRepository", "📝 Nuevos nombres: ${nombresNuevos.take(3)}")

        // Actualizar cada categoría
        categoriasDefault.sortedBy { it.id }.forEachIndexed { index, categoria ->
            if (index < nombresNuevos.size) {
                val nuevoNombre = nombresNuevos[index]
                android.util.Log.d("CategoriaRepository", "Actualizando '${categoria.nombre}' → '$nuevoNombre'")

                val categoriaActualizada = categoria.copy(nombre = nuevoNombre)
                dao.updateCategoria(categoriaActualizada)
            }
        }

        android.util.Log.d("CategoriaRepository", "✅ ${categoriasDefault.size} categorías actualizadas")
    }

    suspend fun crearCategoriasDefault(usuarioId: String) {
        val categoriasExistentes = dao.getCategorias(usuarioId).first()
        val tieneCategoriasDefault = categoriasExistentes.any { it.esDefault }

        if (tieneCategoriasDefault) {
            android.util.Log.d("CategoriaRepository", "✅ Categorías predefinidas ya existen")
            return
        }

        val categoriasDefault = listOf(
            Categoria(nombre = context.getString(R.string.cat_comida), icono = "🍔", color = "#FF5722", tipo = "GASTO", esDefault = true, usuarioId = null, key = "food"),
            Categoria(nombre = context.getString(R.string.cat_transporte), icono = "🚗", color = "#2196F3", tipo = "GASTO", esDefault = true, usuarioId = null, key = "transport"),
            Categoria(nombre = context.getString(R.string.cat_ocio), icono = "🎮", color = "#9C27B0", tipo = "GASTO", esDefault = true, usuarioId = null, key = "leisure"),
            Categoria(nombre = context.getString(R.string.cat_salud), icono = "🏥", color = "#F44336", tipo = "GASTO", esDefault = true, usuarioId = null, key = "health"),
            Categoria(nombre = context.getString(R.string.cat_casa), icono = "🏠", color = "#795548", tipo = "GASTO", esDefault = true, usuarioId = null, key = "home"),
            Categoria(nombre = context.getString(R.string.cat_educacion), icono = "📚", color = "#3F51B5", tipo = "GASTO", esDefault = true, usuarioId = null, key = "education"),
            Categoria(nombre = context.getString(R.string.cat_ropa), icono = "👔", color = "#E91E63", tipo = "GASTO", esDefault = true, usuarioId = null, key = "clothing"),
            Categoria(nombre = context.getString(R.string.cat_otros_gastos), icono = "📦", color = "#607D8B", tipo = "GASTO", esDefault = true, usuarioId = null, key = "other_expenses"),
            Categoria(nombre = context.getString(R.string.cat_sueldo), icono = "💼", color = "#4CAF50", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "salary"),
            Categoria(nombre = context.getString(R.string.cat_freelance), icono = "💻", color = "#00BCD4", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "freelance"),
            Categoria(nombre = context.getString(R.string.cat_inversiones), icono = "📈", color = "#8BC34A", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "investments"),
            Categoria(nombre = context.getString(R.string.cat_regalos), icono = "🎁", color = "#FFEB3B", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "gifts"),
            Categoria(nombre = context.getString(R.string.cat_otros_ingresos), icono = "💰", color = "#4CAF50", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "other_income")
        )

        insertarCategorias(categoriasDefault)
        android.util.Log.d("CategoriaRepository", "✅ Categorías predefinidas creadas")
    }
}