package com.smartsaldo.app.ui.categorias

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.smartsaldo.app.R
import com.smartsaldo.app.data.local.entities.Categoria
import com.smartsaldo.app.databinding.DialogAddCategoriaBinding
import com.smartsaldo.app.databinding.FragmentCategoriasBinding
import com.smartsaldo.app.ui.shared.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoriasFragment : Fragment() {

    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!

    private val categoriaViewModel: CategoriaViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var adapter: CategoriaAdapter
    private var categoriaEditando: Categoria? = null
    private var todasLasCategorias: List<Categoria> = emptyList()

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
        setupSwipeRefresh()
        setupSearchView()
    }

    private fun setupRecyclerView() {
        adapter = CategoriaAdapter(
            onDeleteClick = { categoria ->
                mostrarDialogEliminar(categoria)
            },
            onEditClick = { categoria ->
                mostrarDialogEditarCategoria(categoria)
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
                todasLasCategorias = categorias
                adapter.submitList(categorias)
                updateEmptyState(categorias.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            categoriaViewModel.uiState.collect { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading

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
            categoriaEditando = null
            mostrarDialogAgregarCategoria()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // Recargar categorías
            viewLifecycleOwner.lifecycleScope.launch {
                delay(500)
                binding.swipeRefresh.isRefreshing = false
                Snackbar.make(binding.root, "Actualizado ✅", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Configurar colores
        binding.swipeRefresh.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.light_blue_600
        )
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarCategorias(newText ?: "")
                return true
            }
        })
    }

    private fun filtrarCategorias(filtro: String) {
        val categoriasFiltradas = if (filtro.isBlank()) {
            todasLasCategorias
        } else {
            todasLasCategorias.filter { categoria ->
                categoria.nombre.contains(filtro, ignoreCase = true)
            }
        }
        adapter.submitList(categoriasFiltradas)
        updateEmptyState(categoriasFiltradas.isEmpty())
    }

    private fun mostrarDialogAgregarCategoria() {
        val bindingDialog = DialogAddCategoriaBinding.inflate(layoutInflater)
        val view = bindingDialog.root

        val coloresDisponibles = listOf(
            "#FF5722" to "Naranja", "#2196F3" to "Azul", "#9C27B0" to "Púrpura", "#F44336" to "Rojo",
            "#795548" to "Marrón", "#3F51B5" to "Índigo", "#E91E63" to "Rosado", "#4CAF50" to "Verde",
            "#00BCD4" to "Cian", "#FF9800" to "Ámbar", "#607D8B" to "Gris azul", "#8BC34A" to "Verde claro"
        )

        var tipoSeleccionado = "GASTO"
        var colorSeleccionado = coloresDisponibles[0].first

        bindingDialog.radioGasto.isChecked = true
        bindingDialog.radioGasto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) tipoSeleccionado = "GASTO"
        }
        bindingDialog.radioIngreso.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) tipoSeleccionado = "INGRESO"
        }

        val coloresNombres = coloresDisponibles.map { it.second }
        bindingDialog.spinnerColores.apply {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                coloresNombres
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    colorSeleccionado = coloresDisponibles[position].first
                    try {
                        bindingDialog.viewColorPreview.setBackgroundColor(Color.parseColor(colorSeleccionado))
                    } catch (e: Exception) {
                        bindingDialog.viewColorPreview.setBackgroundColor(Color.GRAY)
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            })
        }

        categoriaEditando?.let { categoria ->
            bindingDialog.etNombre.setText(categoria.nombre)
            bindingDialog.etIcono.setText(categoria.icono)

            if (categoria.tipo == "GASTO") {
                bindingDialog.radioGasto.isChecked = true
                tipoSeleccionado = "GASTO"
            } else {
                bindingDialog.radioIngreso.isChecked = true
                tipoSeleccionado = "INGRESO"
            }

            val indexColor = coloresDisponibles.indexOfFirst { it.first == categoria.color }
            if (indexColor >= 0) {
                bindingDialog.spinnerColores.setSelection(indexColor)
                colorSeleccionado = categoria.color
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (categoriaEditando != null) "Editar Categoría" else "Nueva Categoría")
            .setView(view)
            .setPositiveButton(if (categoriaEditando != null) "Guardar" else "Crear") { _, _ ->
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

                if (categoriaEditando != null) {
                    val categoriaActualizada = categoriaEditando!!.copy(
                        nombre = nombre,
                        color = colorSeleccionado,
                        icono = icono,
                        tipo = tipoSeleccionado
                    )
                    categoriaViewModel.actualizarCategoria(categoriaActualizada)
                } else {
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
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditarCategoria(categoria: Categoria) {
        categoriaEditando = categoria
        mostrarDialogAgregarCategoria()
    }

    private fun mostrarDialogEliminar(categoria: Categoria) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar categoría")
            .setMessage("¿Estás seguro de que deseas eliminar '${categoria.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                animarEliminacionCategoria(categoria)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun animarEliminacionCategoria(categoria: Categoria) {
        val position = adapter.currentList.indexOfFirst { it.id == categoria.id }

        if (position == -1) return

        val viewHolder = binding.recyclerViewCategorias.findViewHolderForAdapterPosition(position)

        if (viewHolder != null && viewHolder is CategoriaAdapter.CategoriaViewHolder) {
            val itemView = viewHolder.itemView

            val animatorSet = AnimatorSet()
            val alphaAnimator = ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0f)
            val scaleAnimator = ObjectAnimator.ofFloat(itemView, "scaleX", 1f, 0.8f)
            val translationAnimator = ObjectAnimator.ofFloat(itemView, "translationX", 0f, -100f)

            animatorSet.apply {
                playTogether(alphaAnimator, scaleAnimator, translationAnimator)
                duration = 300
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        categoriaViewModel.eliminarCategoria(categoria)
                    }
                })
                start()
            }
        } else {
            categoriaViewModel.eliminarCategoria(categoria)
        }
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