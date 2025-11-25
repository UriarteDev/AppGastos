package com.smartsaldo.app.utils

import android.content.Context
import com.smartsaldo.app.R

object CategoriaHelper {

    /**
     * Obtiene el nombre traducido de una categoría predefinida según su key
     */
    fun getNombreTraducido(context: Context, key: String?): String? {
        return when (key) {
            "food" -> context.getString(R.string.cat_comida)
            "transport" -> context.getString(R.string.cat_transporte)
            "leisure" -> context.getString(R.string.cat_ocio)
            "health" -> context.getString(R.string.cat_salud)
            "home" -> context.getString(R.string.cat_casa)
            "education" -> context.getString(R.string.cat_educacion)
            "clothing" -> context.getString(R.string.cat_ropa)
            "other_expenses" -> context.getString(R.string.cat_otros_gastos)
            "salary" -> context.getString(R.string.cat_sueldo)
            "freelance" -> context.getString(R.string.cat_freelance)
            "investments" -> context.getString(R.string.cat_inversiones)
            "gifts" -> context.getString(R.string.cat_regalos)
            "other_income" -> context.getString(R.string.cat_otros_ingresos)
            else -> null // Categoría personalizada
        }
    }

    /**
     * Obtiene el nombre a mostrar: traducido si es predefinida, o nombre original si es personalizada
     */
    fun getNombreMostrar(context: Context, categoria: com.smartsaldo.app.data.local.entities.Categoria): String {
        android.util.Log.d("CategoriaHelper", "📝 Categoría: ${categoria.nombre}, key: ${categoria.key}, esDefault: ${categoria.esDefault}")

        return if (categoria.esDefault && categoria.key != null) {
            val nombreTraducido = getNombreTraducido(context, categoria.key)
            android.util.Log.d("CategoriaHelper", "✅ Traducido: $nombreTraducido")
            nombreTraducido ?: categoria.nombre
        } else {
            android.util.Log.d("CategoriaHelper", "⚠️ Usando nombre original: ${categoria.nombre}")
            categoria.nombre
        }
    }
}