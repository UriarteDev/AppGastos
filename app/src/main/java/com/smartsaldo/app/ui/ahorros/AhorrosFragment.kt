package com.smartsaldo.app.ui.ahorros

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.FragmentAhorrosBinding
import com.smartsaldo.app.data.local.entities.Ahorro
import com.smartsaldo.app.ui.shared.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AhorrosFragment : Fragment() {

    private var _binding: FragmentAhorrosBinding? = null
    private val binding get() = _binding!!

    private val ahorroViewModel: AhorroViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var adapter: AhorroAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAhorrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupFAB()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        adapter = AhorroAdapter(
            onAporteClick = { ahorro ->
                mostrarDialogAporte(ahorro)
            },
            onDeleteClick = { ahorro ->
                mostrarDialogEliminar(ahorro)
            }
        )

        binding.recyclerViewAhorros.apply {
            this.adapter = this@AhorrosFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            ahorroViewModel.ahorros.collect { ahorros ->
                adapter.submitList(ahorros)
                updateEmptyState(ahorros.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            ahorroViewModel.uiState.collect { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading

                state.message?.let { messageKey ->
                    val message = when (messageKey) {
                        "meta_creada" -> getString(R.string.meta_creada)
                        "meta_eliminada" -> getString(R.string.meta_eliminada)
                        "aporte_registrado" -> getString(R.string.aporte_registrado)
                        else -> messageKey
                    }
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    ahorroViewModel.limpiarMensaje()
                }

                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    ahorroViewModel.limpiarMensaje()
                }
            }
        }
    }

    private fun setupFAB() {
        binding.fabAddAhorro.setOnClickListener {
            val dialog = AddAhorroDialog()
            dialog.show(childFragmentManager, "AddAhorroDialog")
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(500)
                binding.swipeRefresh.isRefreshing = false
                Snackbar.make(binding.root, getString(R.string.actualizado), Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.swipeRefresh.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.light_blue_600
        )
    }

    private fun mostrarDialogAporte(ahorro: Ahorro) {
        val dialog = AddAporteDialog.newInstance(ahorro.id)
        dialog.show(childFragmentManager, "AddAporteDialog")
    }

    private fun mostrarDialogEliminar(ahorro: Ahorro) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.eliminar_meta_ahorro))
            .setMessage(getString(R.string.seguro_eliminar_meta, ahorro.nombre))
            .setPositiveButton(getString(R.string.eliminar)) { _, _ ->
                animarEliminacionAhorro(ahorro)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun animarEliminacionAhorro(ahorro: Ahorro) {
        val position = adapter.currentList.indexOfFirst { it.id == ahorro.id }

        if (position == -1) return

        val viewHolder = binding.recyclerViewAhorros.findViewHolderForAdapterPosition(position)

        if (viewHolder != null && viewHolder is AhorroAdapter.AhorroViewHolder) {
            val itemView = viewHolder.itemView

            val animatorSet = AnimatorSet()
            val alphaAnimator = ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0f)
            val scaleAnimator = ObjectAnimator.ofFloat(itemView, "scaleY", 1f, 0.3f)
            val translationAnimator = ObjectAnimator.ofFloat(itemView, "translationY", 0f, -100f)

            animatorSet.apply {
                playTogether(alphaAnimator, scaleAnimator, translationAnimator)
                duration = 350
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        ahorroViewModel.eliminarAhorro(ahorro)
                    }
                })
                start()
            }
        } else {
            ahorroViewModel.eliminarAhorro(ahorro)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerViewAhorros.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerViewAhorros.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}