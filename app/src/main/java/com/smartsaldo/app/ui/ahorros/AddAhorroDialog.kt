package com.smartsaldo.app.ui.ahorros

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.DialogAddAhorroBinding
import com.smartsaldo.app.utils.ValidationUtils.clearError
import com.smartsaldo.app.utils.ValidationUtils.validateAmount
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
            etNombre.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutNombre.clearError()
            }

            etMeta.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutMeta.clearError()
            }

            btnCancelar.setOnClickListener { dismiss() }
            btnGuardar.setOnClickListener {
                if (validateAhorro()) {
                    guardarAhorro()
                }
            }
        }
    }

    private fun validateAhorro(): Boolean {
        binding.apply {
            val isNombreValid = layoutNombre.run {
                val nombre = etNombre.text.toString().trim()
                when {
                    nombre.isBlank() -> {
                        error = getString(R.string.ingrese_nombre_meta)
                        false
                    }
                    nombre.length < 3 -> {
                        error = getString(R.string.nombre_meta_muy_corto)
                        false
                    }
                    nombre.length > 50 -> {
                        error = getString(R.string.nombre_meta_muy_largo)
                        false
                    }
                    else -> {
                        clearError()
                        true
                    }
                }
            }

            val isMetaValid = layoutMeta.validateAmount(maxAmount = 999999999.99)

            return isNombreValid && isMetaValid
        }
    }

    private fun guardarAhorro() {
        binding.apply {
            val nombre = etNombre.text.toString().trim()
            val meta = etMeta.text.toString().toDoubleOrNull() ?: return

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