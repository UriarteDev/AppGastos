package com.smartsaldo.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.R
import com.smartsaldo.app.db.entities.Movimiento

class GastoAdapter(
    private val onEliminarClick: (Movimiento) -> Unit
) : ListAdapter<Movimiento, GastoAdapter.GastoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val gasto = getItem(position)
        holder.bind(gasto, onEliminarClick)
    }

    class GastoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreGasto)
        private val tvMonto: TextView = itemView.findViewById(R.id.tvMontoGasto)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarGasto)

        fun bind(gasto: Movimiento, onEliminarClick: (Movimiento) -> Unit) {
            tvNombre.text = gasto.nombre ?: "Gasto"
            tvMonto.text = "S/ ${gasto.monto}"
            btnEliminar.setOnClickListener { onEliminarClick(gasto) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Movimiento>() {
        override fun areItemsTheSame(oldItem: Movimiento, newItem: Movimiento): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movimiento, newItem: Movimiento): Boolean {
            return oldItem == newItem
        }
    }
}
