package com.smartsaldo.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.smartsaldo.app.db.entities.Categoria
import com.smartsaldo.app.db.entities.TipoTransaccion
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class AddTransaccionDialog : DialogFragment() {

    private var _binding: DialogAddTransaccionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransaccionViewModel by viewModels({ requireParentFragment() })

    private var fechaSeleccionada = Calendar.getInstance()
    private var categorias: List<Categoria> = emptyList()
    private var categoriaSeleccionada: Categoria? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransaccionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        // Configurar fecha inicial
        updateFechaDisplay()
    }

    private fun setupUI() {
        binding.apply {
            // Botones de tipo
            chipGrupoTipo.setOnCheckedChangeListener { _, checkedId ->
                val tipo = if (checkedId == chipGasto.id) {
                    TipoTransaccion.GASTO
                } else {
                    TipoTransaccion.INGRESO
                }
                filtrarCategoriasPorTipo(tipo)
            }

            // Selector de fecha
            btnSeleccionarFecha.setOnClickListener {
                mostrarDatePicker()
            }

            // Selector de hora
            btnSeleccionarHora.setOnClickListener {
                mostrarTimePicker()
            }

            // Botones de acción
            btnCancelar.setOnClickListener {
                dismiss()
            }

            btnGuardar.setOnClickListener {
                guardarTransaccion()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorias.collect { listaCategorias ->
                categorias = listaCategorias
                // Filtrar por tipo inicial (GASTO)
                filtrarCategoriasPorTipo(TipoTransaccion.GASTO)
            }
        }
    }

    private fun filtrarCategoriasPorTipo(tipo: TipoTransaccion) {
        val categoriasFiltradas = categorias.filter {
            it.tipo == tipo.name
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categoriasFiltradas.map { it.nombre }
        )

        binding.spinnerCategoria.setAdapter(adapter)
        binding.spinnerCategoria.setOnItemClickListener { _, _, position, _ ->
            categoriaSeleccionada = categoriasFiltradas[position]
        }
    }

    private fun mostrarDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                fechaSeleccionada.set(Calendar.YEAR, year)
                fechaSeleccionada.set(Calendar.MONTH, month)
                fechaSeleccionada.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateFechaDisplay()
            },
            fechaSeleccionada.get(Calendar.YEAR),
            fechaSeleccionada.get(Calendar.MONTH),
            fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun mostrarTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                fechaSeleccionada.set(Calendar.HOUR_OF_DAY, hourOfDay)
                fechaSeleccionada.set(Calendar.MINUTE, minute)
                updateFechaDisplay()
            },
            fechaSeleccionada.get(Calendar.HOUR_OF_DAY),
            fechaSeleccionada.get(Calendar.MINUTE),
            true // formato 24 horas
        ).show()
    }

    private fun updateFechaDisplay() {
        val fechaFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val horaFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.btnSeleccionarFecha.text = fechaFormat.format(fechaSeleccionada.time)
        binding.btnSeleccionarHora.text = horaFormat.format(fechaSeleccionada.time)
    }

    private fun guardarTransaccion() {
        binding.apply {
            val monto = etMonto.text.toString().toDoubleOrNull()
            val descripcion = etDescripcion.text.toString().trim()
            val notas = etNotas.text.toString().trim().ifBlank { null }

            // Validaciones
            if (monto == null || monto <= 0) {
                etMonto.error = "Ingrese un monto válido"
                return
            }

            if (descripcion.isBlank()) {
                etDescripcion.error = "Ingrese una descripción"
                return
            }

            if (categoriaSeleccionada == null) {
                spinnerCategoria.error = "Seleccione una categoría"
                return
            }

            val tipo = if (chipGrupoTipo.checkedChipId == chipGasto.id) {
                TipoTransaccion.GASTO
            } else {
                TipoTransaccion.INGRESO
            }

            // Guardar transacción
            viewModel.agregarTransaccion(
                monto = monto,
                descripcion = descripcion,
                notas = notas,
                fecha = fechaSeleccionada.timeInMillis,
                categoriaId = categoriaSeleccionada!!.id,
                tipo = tipo
            )

            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Hacer el dialog más ancho
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}