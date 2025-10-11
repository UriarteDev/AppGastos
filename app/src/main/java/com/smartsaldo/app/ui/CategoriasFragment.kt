package com.smartsaldo.app.ui

import android.graphics.Color
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
import com.smartsaldo.app.databinding.FragmentCategoriasBinding
import com.smartsaldo.app.db.entities.Categoria
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoriasFragment : Fragment() {

    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!

    private val categoriaViewModel: CategoriaViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var adapter: CategoriaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupFAB()
    }

    private fun setupRecyclerView() {
        adapter = CategoriaAdapter(
            onDeleteClick = { categoria ->
                mostrarDialogEliminar(categoria)
            }
        )

        binding.recyclerViewCategorias.apply {
            this.adapter = this@CategoriasFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.usuario.collect { usuario ->
                usuario?.let {
                    categoriaViewModel.setUsuarioId(it.uid)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            categoriaViewModel.categorias.collect { categorias ->
                adapter.submitList(categorias)
                updateEmptyState(categorias.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            categoriaViewModel.uiState.collect { state ->
                state.message?.let { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    categoriaViewModel.limpiarMensaje()
                }

                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    categoriaViewModel.limpiarMensaje()
                }
            }
        }
    }

    private fun setupFAB() {
        binding.fabAddCategoria.setOnClickListener {
            mostrarDialogAgregarCategoria()
        }
    }

    private fun mostrarDialogAgregarCategoria() {
        val bindingDialog = com.smartsaldo.app.databinding.DialogAddCategoriaBinding.inflate(layoutInflater)
        val view = bindingDialog.root

        val coloresDisponibles = listOf(
            "#FF5722" to "Naranja",  // Comida
            "#2196F3" to "Azul",     // Transporte
            "#9C27B0" to "Púrpura",  // Ocio
            "#F44336" to "Rojo",     // Salud
            "#795548" to "Marrón",   // Casa
            "#3F51B5" to "Índigo",   // Educación
            "#E91E63" to "Rosado",   // Ropa
            "#4CAF50" to "Verde",    // Sueldo
            "#00BCD4" to "Cian",     // Freelance
            "#FF9800" to "Ámbar",    // Otros
            "#607D8B" to "Gris azul",// Varios
            "#8BC34A" to "Verde claro" // Inversiones
        )

        var tipoSeleccionado = "GASTO"
        var colorSeleccionado = coloresDisponibles[0].first

        // Configurar radio buttons para tipo
        bindingDialog.radioGasto.isChecked = true
        bindingDialog.radioGasto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) tipoSeleccionado = "GASTO"
        }
        bindingDialog.radioIngreso.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) tipoSeleccionado = "INGRESO"
        }

        // Configurar spinner de colores
        val coloresNombres = coloresDisponibles.map { it.second }
        bindingDialog.spinnerColores.apply {
            val adapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                coloresNombres
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    colorSeleccionado = coloresDisponibles[position].first
                    try {
                        bindingDialog.viewColorPreview.setBackgroundColor(Color.parseColor(colorSeleccionado))
                    } catch (e: Exception) {
                        bindingDialog.viewColorPreview.setBackgroundColor(Color.GRAY)
                    }
                }

                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            })
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Categoría")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = bindingDialog.etNombre.text.toString().trim()
                val icono = bindingDialog.etIcono.text.toString().trim()

                if (nombre.isBlank()) {
                    Snackbar.make(binding.root, "Ingrese un nombre", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (icono.isBlank()) {
                    Snackbar.make(binding.root, "Ingrese un emoji", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val usuarioId = authViewModel.usuario.value?.uid ?: return@setPositiveButton

                val categoria = Categoria(
                    nombre = nombre,
                    color = colorSeleccionado,
                    icono = icono,
                    tipo = tipoSeleccionado,
                    esDefault = false,
                    usuarioId = usuarioId
                )

                categoriaViewModel.crearCategoria(categoria)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEliminar(categoria: Categoria) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar categoría")
            .setMessage("¿Estás seguro de que deseas eliminar '${categoria.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                categoriaViewModel.eliminarCategoria(categoria)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerViewCategorias.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerViewCategorias.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}