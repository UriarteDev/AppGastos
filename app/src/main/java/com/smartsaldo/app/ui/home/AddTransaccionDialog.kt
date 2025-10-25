package com.smartsaldo.app.ui.home


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.smartsaldo.app.R
import com.smartsaldo.app.data.local.entities.Categoria
import com.smartsaldo.app.data.local.entities.TipoTransaccion
import com.smartsaldo.app.databinding.DialogAddTransaccionBinding
import com.smartsaldo.app.utils.ValidationUtils.clearError
import com.smartsaldo.app.utils.ValidationUtils.validateAmount
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AddTransaccionDialog : DialogFragment() {

    private var _binding: DialogAddTransaccionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransaccionViewModel by activityViewModels()

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
        updateFechaDisplay()
    }

    private fun setupUI() {
        binding.apply {
            // Limpiar errores al escribir/enfocar
            etMonto.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutMonto.clearError()
            }

            etDescripcion.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutDescripcion.clearError()
            }

            chipGrupoTipo.setOnCheckedChangeListener { _, checkedId ->
                val tipo = if (checkedId == chipGasto.id) {
                    TipoTransaccion.GASTO
                } else {
                    TipoTransaccion.INGRESO
                }
                filtrarCategoriasPorTipo(tipo)
                layoutCategoria.clearError()
            }

            btnSeleccionarFecha.setOnClickListener {
                mostrarDatePicker()
            }

            btnSeleccionarHora.setOnClickListener {
                mostrarTimePicker()
            }

            btnCancelar.setOnClickListener {
                dismiss()
            }

            btnGuardar.setOnClickListener {
                if (validateTransaccion()) {
                    guardarTransaccion()
                }
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorias.collect { listaCategorias ->
                categorias = listaCategorias
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
            binding.layoutCategoria.clearError()
        }
    }

    private fun validateTransaccion(): Boolean {
        binding.apply {
            // Validar monto
            val isMontoValid = layoutMonto.run {
                val monto = etMonto.text.toString().toDoubleOrNull()
                when {
                    monto == null -> {
                        error = getString(com.smartsaldo.app.R.string.ingrese_monto)
                        false
                    }
                    monto <= 0 -> {
                        error = getString(com.smartsaldo.app.R.string.monto_mayor_cero)
                        false
                    }
                    else -> {
                        clearError()
                        true
                    }
                }
            }

            // Validar descripción
            val isDescripcionValid = layoutDescripcion.run {
                val desc = etDescripcion.text.toString().trim()
                when {
                    desc.isBlank() -> {
                        error = getString(com.smartsaldo.app.R.string.ingrese_descripcion)
                        false
                    }
                    desc.length < 3 -> {
                        error = getString(com.smartsaldo.app.R.string.descripcion_muy_corta)
                        false
                    }
                    else -> {
                        clearError()
                        true
                    }
                }
            }

            // Validar categoría
            val isCategoriaValid = layoutCategoria.run {
                if (categoriaSeleccionada == null) {
                    error = getString(com.smartsaldo.app.R.string.seleccione_categoria)
                    false
                } else {
                    clearError()
                    true
                }
            }

            // Validar notas (opcional pero si existe, validar longitud)
            val isNotasValid = etNotas.text.toString().let { notas ->
                if (notas.isNotBlank() && notas.length > 500) {
                    Snackbar.make(root, getString(R.string.nota_muy_larga), Snackbar.LENGTH_SHORT).show()
                    false
                } else {
                    true
                }
            }

            return isMontoValid && isDescripcionValid && isCategoriaValid && isNotasValid
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
            true
        ).show()
    }

    private fun updateFechaDisplay() {
        val fechaFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val horaFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.btnSeleccionarFecha.text = fechaFormat.format(fechaSeleccionada.time)
        binding.btnSeleccionarHora.text = horaFormat.format(fechaSeleccionada.time)
    }

    private fun guardarTransaccion() {
        binding.apply {
            val monto = etMonto.text.toString().toDoubleOrNull() ?: return
            val descripcion = etDescripcion.text.toString().trim()
            val notas = etNotas.text.toString().trim().ifBlank { null }

            val tipo = if (chipGrupoTipo.checkedChipId == chipGasto.id) {
                TipoTransaccion.GASTO
            } else {
                TipoTransaccion.INGRESO
            }

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