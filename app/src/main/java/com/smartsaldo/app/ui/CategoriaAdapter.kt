package com.smartsaldo.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.databinding.ItemCategoriaBinding
import com.smartsaldo.app.db.entities.Categoria

class CategoriaAdapter(
    private val onDeleteClick: (Categoria) -> Unit
) : ListAdapter<Categoria, CategoriaAdapter.CategoriaViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    class CategoriaViewHolder(
        private val binding: ItemCategoriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            categoria: Categoria,
            onDeleteClick: (Categoria) -> Unit
        ) {
            binding.apply {
                tvNombreCategoria.text = categoria.nombre
                tvTipo.text = categoria.tipo
                tvIcono.text = categoria.icono

                // Aplicar color de fondo
                try {
                    val color = Color.parseColor(categoria.color)
                    viewColorCategoria.setBackgroundColor(color)
                } catch (e: Exception) {
                    viewColorCategoria.setBackgroundColor(Color.GRAY)
                }

                // Badge para categorías predefinidas
                if (categoria.esDefault) {
                    tvBadge.visibility = android.view.View.VISIBLE
                    tvBadge.text = "Predefinida"
                } else {
                    tvBadge.visibility = android.view.View.GONE
                }

                // Solo permitir eliminar categorías personalizadas
                btnEliminar.isEnabled = !categoria.esDefault
                btnEliminar.alpha = if (categoria.esDefault) 0.5f else 1.0f

                btnEliminar.setOnClickListener {
                    onDeleteClick(categoria)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Categoria>() {
        override fun areItemsTheSame(oldItem: Categoria, newItem: Categoria): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Categoria, newItem: Categoria): Boolean {
            return oldItem == newItem
        }
    }
}