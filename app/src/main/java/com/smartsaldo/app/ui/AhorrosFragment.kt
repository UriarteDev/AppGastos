package com.smartsaldo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartsaldo.app.databinding.FragmentAhorrosBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AhorrosFragment : Fragment() {

    private var _binding: FragmentAhorrosBinding? = null
    private val binding get() = _binding!!

    private val ahorroViewModel: AhorroViewModel by activityViewModels()
    private val transaccionViewModel: TransaccionViewModel by activityViewModels()

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
    }

    private fun setupRecyclerView() {
        adapter = AhorroAdapter(
            onAporteClick = { ahorro ->
                mostrarDialogAporte(ahorro)
            },
            onDeleteClick = { ahorro ->
                ahorroViewModel.eliminarAhorro(ahorro)
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
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            ahorroViewModel.uiState.collect { state ->
                state.message?.let { message ->
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .show()
                    ahorroViewModel.limpiarMensaje()
                }

                state.error?.let { error ->
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, error, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
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

    private fun mostrarDialogAporte(ahorro: com.smartsaldo.app.db.entities.Ahorro) {
        val dialog = AddAporteDialog.newInstance(ahorro.id)
        dialog.show(childFragmentManager, "AddAporteDialog")
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