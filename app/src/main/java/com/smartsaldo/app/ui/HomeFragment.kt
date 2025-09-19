package com.smartsaldo.app.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartsaldo.app.R
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val transaccionViewModel: TransaccionViewModel by viewModels()

    private lateinit var adapter: TransaccionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupSearch()
    }

    private fun setupRecyclerView() {
        adapter = TransaccionAdapter(
            onEditClick = { transaccion ->
                // Navegar a editar transacción
            },
            onDeleteClick = { transaccion ->
                transaccionViewModel.eliminarTransaccion(transaccion.transaccion)
            }
        )

        binding.recyclerViewTransacciones.apply {
            this.adapter = this@HomeFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        // Observar usuario actual
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.usuario.collect { usuario ->
                usuario?.let {
                    transaccionViewModel.setUsuarioId(it.uid)
                }
            }
        }

        // Observar transacciones
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.transacciones.collect { transacciones ->
                adapter.submitList(transacciones)
                updateEmptyState(transacciones.isEmpty())
            }
        }

        // Observar estadísticas del mes
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.estadisticasDelMes.collect { estadisticas ->
                updateResumenCard(estadisticas)
            }
        }

        // Observar estado de UI
        viewLifecycleOwner.lifecycleScope.launch {
            transaccionViewModel.uiState.collect { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading

                state.message?.let { message ->
                    // Mostrar Snackbar
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .show()
                    transaccionViewModel.limpiarMensaje()
                }

                state.error?.let { error ->
                    // Mostrar error
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, error, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("Reintentar") { /* acción de reintento */ }
                        .show()
                    transaccionViewModel.limpiarMensaje()
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // Recargar datos
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
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

    private fun updateResumenCard(estadisticas: EstadisticaMensual?) {
        if (estadisticas != null) {
            binding.apply {
                tvTotalIngresos.text = "S/ ${String.format("%.2f", estadisticas.totalIngresos)}"
                tvTotalGastos.text = "S/ ${String.format("%.2f", estadisticas.totalGastos)}"
                tvSaldoDisponible.text = "S/ ${String.format("%.2f", estadisticas.saldo)}"

                // Cambiar color según si es positivo o negativo
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