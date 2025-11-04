package com.smartsaldo.app.ui.categorias

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.ItemCategoriaBinding
import com.smartsaldo.app.data.local.entities.Categoria

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
        holder.bind(getItem(position), onDeleteClick, onEditClick, position)
    }

    class CategoriaViewHolder(
        private val binding: ItemCategoriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            categoria: Categoria,
            onDeleteClick: (Categoria) -> Unit,
            onEditClick: (Categoria) -> Unit,
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
                    tvBadge.text = itemView.context.getString(R.string.predefinida)
                    tvBadge.isClickable = false
                    tvBadge.isFocusable = false
                    btnEliminar.isEnabled = false
                    btnEliminar.alpha = 0.5f
                } else {
                    tvBadge.visibility = android.view.View.GONE
                    btnEliminar.isEnabled = true
                    btnEliminar.alpha = 1.0f
                }

                // Click para editar con animación
                root.setOnClickListener {
                    if (!categoria.esDefault) {
                        animarPulso {
                            onEditClick(categoria)
                        }
                    }
                }

                // Click para eliminar
                btnEliminar.setOnClickListener {
                    onDeleteClick(categoria)
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
                scaleX = 0.8f
                scaleY = 0.8f

                // Iniciar animación
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setStartDelay((position * 60).toLong())
                    .withEndAction {
                        // Asegurar valores finales
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                    }
                    .start()
            }
        }

        private fun animarPulso(onComplete: () -> Unit) {
            val animatorSet = AnimatorSet()

            val scaleXAnimator = ObjectAnimator.ofFloat(binding.root, "scaleX", 1f, 0.95f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(binding.root, "scaleY", 1f, 0.95f, 1f)

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

    class DiffCallback : DiffUtil.ItemCallback<Categoria>() {
        override fun areItemsTheSame(oldItem: Categoria, newItem: Categoria): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Categoria, newItem: Categoria): Boolean {
            return oldItem == newItem
        }
    }
}