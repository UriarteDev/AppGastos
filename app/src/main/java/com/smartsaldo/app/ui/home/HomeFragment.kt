package com.smartsaldo.app.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartsaldo.app.R
import kotlinx.coroutines.launch
import com.smartsaldo.app.databinding.FragmentHomeBinding
import com.smartsaldo.app.data.local.dao.EstadisticaMensual
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val transaccionViewModel: TransaccionViewModel by activityViewModels()

    private lateinit var adapter: TransaccionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupSearch()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = TransaccionAdapter(
            onEditClick = { transaccion ->
                val dialog = EditTransaccionDialog.newInstance(transaccion)
                dialog.show(childFragmentManager, "EditTransaccionDialog")
            },
            onDeleteClick = { transaccion ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar transacción")
                    .setMessage("¿Estás seguro de que deseas eliminar '${transaccion.transaccion.descripcion}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        animarEliminacion(transaccion)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        binding.recyclerViewTransacciones.apply {
            this.adapter = this@HomeFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.transacciones.collect { transacciones ->
                adapter.submitList(transacciones)
                updateEmptyState(transacciones.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.estadisticasDelMes.collect { estadisticas ->
                updateResumenCard(estadisticas)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.uiState.collect { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading

                state.message?.let { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    transaccionViewModel.limpiarMensaje()
                }

                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                        .setAction("Reintentar") { }
                        .show()
                    transaccionViewModel.limpiarMensaje()
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(500)
                binding.swipeRefresh.isRefreshing = false
                Snackbar.make(binding.root, "Actualizado ✅", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.swipeRefresh.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.light_blue_600
        )
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { transaccionViewModel.buscar(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    transaccionViewModel.buscar("")
                }
                return true
            }
        })
    }

    private fun setupFab() {
        binding.fabAddTransaccion.setOnClickListener {
            val dialog = AddTransaccionDialog()
            dialog.show(childFragmentManager, "AddTransaccionDialog")
        }
    }

    private fun animarEliminacion(transaccion: com.smartsaldo.app.data.local.entities.TransaccionConCategoria) {
        val position = adapter.currentList.indexOfFirst {
            it.transaccion.id == transaccion.transaccion.id
        }

        if (position == -1) return

        val viewHolder = binding.recyclerViewTransacciones.findViewHolderForAdapterPosition(position)

        if (viewHolder != null && viewHolder is TransaccionAdapter.TransaccionViewHolder) {
            val itemView = viewHolder.itemView

            val animatorSet = AnimatorSet()
            val alphaAnimator = ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0f)
            val translationAnimator = ObjectAnimator.ofFloat(itemView, "translationX", 0f, 100f)

            animatorSet.apply {
                playTogether(alphaAnimator, translationAnimator)
                duration = 300
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        transaccionViewModel.eliminarTransaccion(transaccion.transaccion)
                    }
                })
                start()
            }
        } else {
            transaccionViewModel.eliminarTransaccion(transaccion.transaccion)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateResumenCard(estadisticas: EstadisticaMensual?) {
        if (estadisticas != null) {
            binding.apply {
                tvTotalIngresos.text = "S/ ${String.format("%.2f", estadisticas.totalIngresos)}"
                tvTotalGastos.text = "S/ ${String.format("%.2f", estadisticas.totalGastos)}"
                tvSaldoDisponible.text = "S/ ${String.format("%.2f", estadisticas.saldo)}"

                val colorSaldo = if (estadisticas.saldo >= 0) {
                    R.color.green_500
                } else {
                    R.color.red_500
                }
                tvSaldoDisponible.setTextColor(requireContext().getColor(colorSaldo))
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerViewTransacciones.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerViewTransacciones.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}