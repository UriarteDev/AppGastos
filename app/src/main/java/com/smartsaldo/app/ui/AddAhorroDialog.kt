package com.smartsaldo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.smartsaldo.app.databinding.DialogAddAhorroBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAhorroDialog : DialogFragment() {

    private var _binding: DialogAddAhorroBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AhorroViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddAhorroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnCancelar.setOnClickListener { dismiss() }
            btnGuardar.setOnClickListener { guardarAhorro() }
        }
    }

    private fun guardarAhorro() {
        binding.apply {
            val nombre = etNombre.text.toString().trim()
            val meta = etMeta.text.toString().toDoubleOrNull()

            if (nombre.isBlank()) {
                etNombre.error = "Ingrese un nombre"
                return
            }

            if (meta == null || meta <= 0) {
                etMeta.error = "Ingrese una meta válida"
                return
            }

            viewModel.crearAhorro(nombre, meta)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}