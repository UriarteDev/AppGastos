package com.smartsaldo.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.databinding.ItemAhorroBinding
import com.smartsaldo.app.db.entities.Ahorro

class AhorroAdapter(
    private val onAporteClick: (Ahorro) -> Unit,
    private val onDeleteClick: (Ahorro) -> Unit
) : ListAdapter<Ahorro, AhorroAdapter.AhorroViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AhorroViewHolder {
        val binding = ItemAhorroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AhorroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AhorroViewHolder, position: Int) {
        holder.bind(getItem(position), onAporteClick, onDeleteClick)
    }

    class AhorroViewHolder(
        private val binding: ItemAhorroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            ahorro: Ahorro,
            onAporteClick: (Ahorro) -> Unit,
            onDeleteClick: (Ahorro) -> Unit
        ) {
            binding.apply {
                tvNombreAhorro.text = ahorro.nombre
                tvMontoActual.text = "S/ ${String.format("%.2f", ahorro.montoActual)}"
                tvMetaMonto.text = "Meta: S/ ${String.format("%.2f", ahorro.metaMonto)}"

                // Calcular porcentaje
                val porcentaje = if (ahorro.metaMonto > 0) {
                    ((ahorro.montoActual / ahorro.metaMonto) * 100).toInt()
                } else 0

                progressBar.progress = porcentaje.coerceIn(0, 100)
                tvPorcentaje.text = "$porcentaje%"

                btnAgregar.setOnClickListener { onAporteClick(ahorro) }
                btnEliminar.setOnClickListener { onDeleteClick(ahorro) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Ahorro>() {
        override fun areItemsTheSame(oldItem: Ahorro, newItem: Ahorro): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ahorro, newItem: Ahorro): Boolean {
            return oldItem == newItem
        }
    }
}