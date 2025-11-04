package com.smartsaldo.app.ui.ahorros

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.ItemAhorroBinding
import com.smartsaldo.app.data.local.entities.Ahorro
import com.smartsaldo.app.utils.CurrencyHelper

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
        holder.bind(getItem(position), onAporteClick, onDeleteClick, position)
    }

    class AhorroViewHolder(
        private val binding: ItemAhorroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            ahorro: Ahorro,
            onAporteClick: (Ahorro) -> Unit,
            onDeleteClick: (Ahorro) -> Unit,
            position: Int
        ) {
            binding.apply {
                // IMPORTANTE: Resetear ANTES de hacer cualquier cosa
                root.clearAnimation()
                root.animate().cancel()

                // Asegurar valores normales por defecto
                root.scaleX = 1f
                root.scaleY = 1f
                root.alpha = 1f
                root.translationX = 0f
                root.translationY = 0f

                tvNombreAhorro.text = ahorro.nombre
                tvMontoActual.text = CurrencyHelper.formatAmount(itemView.context, ahorro.montoActual)
                tvMetaMonto.text = "${itemView.context.getString(R.string.meta)}: ${CurrencyHelper.formatAmount(itemView.context, ahorro.metaMonto)}"

                // Calcular porcentaje
                val porcentaje = if (ahorro.metaMonto > 0) {
                    ((ahorro.montoActual / ahorro.metaMonto) * 100).toInt()
                } else 0

                progressBar.progress = porcentaje.coerceIn(0, 100)
                tvPorcentaje.text = "$porcentaje%"

                // Click para agregar aporte con animación
                btnAgregar.setOnClickListener {
                    animarPulso {
                        onAporteClick(ahorro)
                    }
                }

                // Click para eliminar
                btnEliminar.setOnClickListener {
                    onDeleteClick(ahorro)
                }

                // IMPORTANTE: Post para asegurar que el layout esté listo ANTES de animar
                root.post {
                    animarEntrada(position)
                }
            }
        }

        private fun animarEntrada(position: Int) {
            binding.root.apply {
                // Configurar estado inicial de la animación
                alpha = 0f
                translationY = 50f
                scaleX = 0.9f
                scaleY = 0.9f

                // Iniciar animación
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(450)
                    .setStartDelay((position * 80).toLong())
                    .withEndAction {
                        // Asegurar valores finales
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                        translationY = 0f
                    }
                    .start()
            }
        }

        private fun animarPulso(onComplete: () -> Unit) {
            val animatorSet = AnimatorSet()

            val scaleXAnimator = ObjectAnimator.ofFloat(binding.root, "scaleX", 1f, 0.97f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(binding.root, "scaleY", 1f, 0.97f, 1f)

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

    class DiffCallback : DiffUtil.ItemCallback<Ahorro>() {
        override fun areItemsTheSame(oldItem: Ahorro, newItem: Ahorro): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ahorro, newItem: Ahorro): Boolean {
            return oldItem == newItem
        }
    }
}