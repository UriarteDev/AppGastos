package com.smartsaldo.app.ui

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
import com.smartsaldo.app.db.entities.Categoria
import com.smartsaldo.app.db.entities.TipoTransaccion
import com.smartsaldo.app.db.entities.TransaccionConCategoria
import com.smartsaldo.app.databinding.DialogAddTransaccionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class EditTransaccionDialog : DialogFragment() {

    private var _binding: DialogAddTransaccionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransaccionViewModel by activityViewModels()

    private var fechaSeleccionada = Calendar.getInstance()
    private var categorias: List<Categoria> = emptyList()
    private var categoriaSeleccionada: Categoria? = null
    private var transaccionConCategoria: TransaccionConCategoria? = null

    companion object {
        private const val ARG_TRANSACCION_ID = "transaccion_id"
        private const val ARG_CATEGORIA_ID = "categoria_id"
        private const val ARG_MONTO = "monto"
        private const val ARG_DESCRIPCION = "descripcion"
        private const val ARG_NOTAS = "notas"
        private const val ARG_FECHA = "fecha"
        private const val ARG_TIPO = "tipo"

        fun newInstance(transaccionConCategoria: TransaccionConCategoria): EditTransaccionDialog {
            return EditTransaccionDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TRANSACCION_ID, transaccionConCategoria.transaccion.id)
                    putLong(ARG_CATEGORIA_ID, transaccionConCategoria.transaccion.categoriaId)
                    putDouble(ARG_MONTO, transaccionConCategoria.transaccion.monto)
                    putString(ARG_DESCRIPCION, transaccionConCategoria.transaccion.descripcion)
                    putString(ARG_NOTAS, transaccionConCategoria.transaccion.notas)
                    putLong(ARG_FECHA, transaccionConCategoria.transaccion.fecha)
                    putString(ARG_TIPO, transaccionConCategoria.transaccion.tipo.name)
                }
            }
        }
    }

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
        precargarDatos()
    }

    private fun setupUI() {
        binding.apply {
            chipGrupoTipo.setOnCheckedChangeListener { _, checkedId ->
                val tipo = if (checkedId == chipGasto.id) {
                    TipoTransaccion.GASTO
                } else {
                    TipoTransaccion.INGRESO
                }
                filtrarCategoriasPorTipo(tipo)
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
                guardarTransaccion()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorias.collect { listaCategorias ->
                categorias = listaCategorias
                val tipo = if (binding.chipGrupoTipo.checkedChipId == binding.chipGasto.id) {
                    TipoTransaccion.GASTO
                } else {
                    TipoTransaccion.INGRESO
                }
                filtrarCategoriasPorTipo(tipo)
            }
        }
    }

    private fun precargarDatos() {
        val args = arguments ?: return
        val transaccionId = args.getLong(ARG_TRANSACCION_ID)
        val categoriaId = args.getLong(ARG_CATEGORIA_ID)
        val monto = args.getDouble(ARG_MONTO)
        val descripcion = args.getString(ARG_DESCRIPCION, "")
        val notas = args.getString(ARG_NOTAS, "")
        val fecha = args.getLong(ARG_FECHA)
        val tipo = args.getString(ARG_TIPO, "GASTO")

        binding.apply {
            if (tipo == TipoTransaccion.GASTO.name) {
                chipGrupoTipo.check(chipGasto.id)
            } else {
                chipGrupoTipo.check(chipIngreso.id)
            }

            etMonto.setText(String.format("%.2f", monto))
            etDescripcion.setText(descripcion)
            etNotas.setText(notas)

            fechaSeleccionada.timeInMillis = fecha
            updateFechaDisplay()

            filtrarCategoriasPorTipo(TipoTransaccion.valueOf(tipo))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categorias.collect { cats ->
                val categoria = cats.find { it.id == categoriaId }
                if (categoria != null) {
                    categoriaSeleccionada = categoria
                    binding.spinnerCategoria.setText(categoria.nombre, false)
                }
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
            true
        ).show()
    }

    private fun updateFechaDisplay() {
        val fechaFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val horaFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.btnSeleccionarFecha.text = fechaFormat.format(fechaSeleccionada.time)
        binding.btnSeleccionarHora.text = horaFormat.format(fechaSeleccionada.time)
    }

    private fun guardarTransaccion() {
        val args = arguments ?: return
        val transaccionId = args.getLong(ARG_TRANSACCION_ID)

        binding.apply {
            val monto = etMonto.text.toString().toDoubleOrNull()
            val descripcion = etDescripcion.text.toString().trim()
            val notas = etNotas.text.toString().trim().ifBlank { null }

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

            viewModel.editarTransaccion(
                id = transaccionId,
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