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
    private val onDeleteClick: (Categoria) -> Unit,
    private val onEditClick: (Categoria) -> Unit
) : ListAdapter<Categoria, CategoriaAdapter.CategoriaViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick, onEditClick)
    }

    class CategoriaViewHolder(
        private val binding: ItemCategoriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            categoria: Categoria,
            onDeleteClick: (Categoria) -> Unit,
            onEditClick: (Categoria) -> Unit
        ) {
            binding.apply {
                tvNombreCategoria.text = categoria.nombre
                tvTipo.text = categoria.tipo
                tvIcono.text = categoria.icono

                try {
                    val color = Color.parseColor(categoria.color)
                    viewColorCategoria.setBackgroundColor(color)
                } catch (e: Exception) {
                    viewColorCategoria.setBackgroundColor(Color.GRAY)
                }

                if (categoria.esDefault) {
                    tvBadge.visibility = android.view.View.VISIBLE
                    tvBadge.text = "Predefinida"
                    tvBadge.isClickable = false
                    tvBadge.isFocusable = false
                    btnEliminar.isEnabled = false
                    btnEliminar.alpha = 0.5f
                } else {
                    tvBadge.visibility = android.view.View.GONE
                    btnEliminar.isEnabled = true
                    btnEliminar.alpha = 1.0f
                }

                btnEliminar.setOnClickListener {
                    onDeleteClick(categoria)
                }

                root.setOnClickListener {
                    if (!categoria.esDefault) {
                        onEditClick(categoria)
                    }
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