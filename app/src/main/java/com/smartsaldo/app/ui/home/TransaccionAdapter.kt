package com.smartsaldo.app.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.databinding.ItemTransaccionBinding
import com.smartsaldo.app.data.local.entities.TransaccionConCategoria
import com.smartsaldo.app.data.local.entities.TipoTransaccion
import com.smartsaldo.app.utils.CurrencyHelper
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
        holder.bind(getItem(position), onEditClick, onDeleteClick, position)
    }

    class TransaccionViewHolder(
        private val binding: ItemTransaccionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(
            transaccionConCategoria: TransaccionConCategoria,
            onEditClick: (TransaccionConCategoria) -> Unit,
            onDeleteClick: (TransaccionConCategoria) -> Unit,
            position: Int
        ) {
            val transaccion = transaccionConCategoria.transaccion
            val categoria = transaccionConCategoria.categoria

            binding.apply {
                // Resetear animaciones previas
                root.clearAnimation()
                root.animate().cancel()
                root.alpha = 1f
                root.translationX = 0f
                root.scaleX = 1f
                root.scaleY = 1f

                tvDescripcion.text = transaccion.descripcion
                tvCategoria.text = categoria?.nombre ?: "Sin categoría"
                tvFecha.text = dateFormat.format(Date(transaccion.fecha))
                tvHora.text = timeFormat.format(Date(transaccion.fecha))

                val montoTexto = CurrencyHelper.formatAmount(itemView.context, transaccion.monto)
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

                try {
                    val colorCategoria = Color.parseColor(categoria?.color ?: "#808080")
                    viewColorCategoria.setBackgroundColor(colorCategoria)
                } catch (e: Exception) {
                    viewColorCategoria.setBackgroundColor(Color.GRAY)
                }

                if (!transaccion.notas.isNullOrBlank()) {
                    tvNotas.text = transaccion.notas
                    tvNotas.visibility = android.view.View.VISIBLE
                } else {
                    tvNotas.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    animarPulso {
                        onEditClick(transaccionConCategoria)
                    }
                }

                btnEliminar.setOnClickListener {
                    onDeleteClick(transaccionConCategoria)
                }

                animarEntrada(position)
            }
        }

        private fun animarEntrada(position: Int) {
            binding.root.apply {
                alpha = 0f

                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((position * 50).toLong())
                    .start()
            }
        }

        private fun animarPulso(onComplete: () -> Unit) {
            val animatorSet = AnimatorSet()

            val scaleXAnimator = ObjectAnimator.ofFloat(binding.root, "scaleX", 1f, 0.98f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(binding.root, "scaleY", 1f, 0.98f, 1f)

            animatorSet.apply {
                playTogether(scaleXAnimator, scaleYAnimator)
                duration = 200
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onComplete()
                    }
                })
                start()
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