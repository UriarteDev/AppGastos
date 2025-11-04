package com.smartsaldo.app.data.repository


import com.smartsaldo.app.data.local.dao.CategoriaDao
import com.smartsaldo.app.data.local.entities.Categoria
import kotlinx.coroutines.flow.Flow

class CategoriaRepository(private val dao: CategoriaDao) {

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
        return dao.insertCategoria(categoria)
    }

    suspend fun insertarCategorias(categorias: List<Categoria>) {
        dao.insertCategorias(categorias)
    }

    suspend fun actualizarCategoria(categoria: Categoria) {
        dao.updateCategoria(categoria)
    }

    suspend fun eliminarCategoria(categoria: Categoria) {
        dao.deleteCategoria(categoria)
    }

    suspend fun eliminarCategoriasPersonalizadas(usuarioId: String) {
        dao.deleteCategoriasPersonalizadas(usuarioId)
    }

    suspend fun crearCategoriasDefault(usuarioId: String) {
        val categoriasDefault = listOf(
            // 🍕 GASTOS
            Categoria(
                nombre = "Comida",
                icono = "restaurant",
                color = "#FF5722",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Transporte",
                icono = "directions_car",
                color = "#2196F3",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Ocio",
                icono = "sports_esports",
                color = "#9C27B0",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Salud",
                icono = "local_hospital",
                color = "#F44336",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Casa",
                icono = "home",
                color = "#795548",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Educación",
                icono = "school",
                color = "#3F51B5",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Ropa",
                icono = "checkroom",
                color = "#E91E63",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = "Otros Gastos",
                icono = "category",
                color = "#607D8B",
                tipo = "GASTO",
                esDefault = true
            ),

            // 💰 INGRESOS
            Categoria(
                nombre = "Sueldo",
                icono = "work",
                color = "#4CAF50",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = "Freelance",
                icono = "computer",
                color = "#00BCD4",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = "Inversiones",
                icono = "trending_up",
                color = "#8BC34A",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = "Regalos",
                icono = "card_giftcard",
                color = "#FFEB3B",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = "Otros Ingresos",
                icono = "attach_money",
                color = "#4CAF50",
                tipo = "INGRESO",
                esDefault = true
            )
        )

        insertarCategorias(categoriasDefault)
    }
}