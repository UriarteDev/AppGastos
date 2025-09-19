package com.smartsaldo.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.databinding.ItemTransaccionBinding
import com.smartsaldo.app.db.entities.TransaccionConCategoria
import com.smartsaldo.app.db.entities.TipoTransaccion
import java.text.SimpleDateFormat
import java.util.*

class TransaccionAdapter(
    private val onEditClick: (TransaccionConCategoria) -> Unit,
    private val onDeleteClick: (TransaccionConCategoria) -> Unit
) : ListAdapter<TransaccionConCategoria, TransaccionAdapter.TransaccionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaccionViewHolder {
        val binding = ItemTransaccionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransaccionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransaccionViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick)
    }

    class TransaccionViewHolder(
        private val binding: ItemTransaccionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(
            transaccionConCategoria: TransaccionConCategoria,
            onEditClick: (TransaccionConCategoria) -> Unit,
            onDeleteClick: (TransaccionConCategoria) -> Unit
        ) {
            val transaccion = transaccionConCategoria.transaccion
            val categoria = transaccionConCategoria.categoria

            binding.apply {
                // Información básica
                tvDescripcion.text = transaccion.descripcion
                tvCategoria.text = categoria.nombre
                tvFecha.text = dateFormat.format(Date(transaccion.fecha))
                tvHora.text = timeFormat.format(Date(transaccion.fecha))

                // Monto con color según tipo
                val montoTexto = "S/ ${String.format("%.2f", transaccion.monto)}"
                when (transaccion.tipo) {
                    TipoTransaccion.INGRESO -> {
                        tvMonto.text = "+$montoTexto"
                        tvMonto.setTextColor(Color.parseColor("#4CAF50"))
                    }
                    TipoTransaccion.GASTO -> {
                        tvMonto.text = "-$montoTexto"
                        tvMonto.setTextColor(Color.parseColor("#F44336"))
                    }
                }

                // Color de categoría
                try {
                    val colorCategoria = Color.parseColor(categoria.color)
                    viewColorCategoria.setBackgroundColor(colorCategoria)
                } catch (e: Exception) {
                    viewColorCategoria.setBackgroundColor(Color.GRAY)
                }

                // Notas (opcional)
                if (!transaccion.notas.isNullOrBlank()) {
                    tvNotas.text = transaccion.notas
                    tvNotas.visibility = android.view.View.VISIBLE
                } else {
                    tvNotas.visibility = android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener { onEditClick(transaccionConCategoria) }
                btnEliminar.setOnClickListener { onDeleteClick(transaccionConCategoria) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransaccionConCategoria>() {
        override fun areItemsTheSame(
            oldItem: TransaccionConCategoria,
            newItem: TransaccionConCategoria
        ): Boolean {
            return oldItem.transaccion.id == newItem.transaccion.id
        }

        override fun areContentsTheSame(
            oldItem: TransaccionConCategoria,
            newItem: TransaccionConCategoria
        ): Boolean {
            return oldItem == newItem
        }
    }
}