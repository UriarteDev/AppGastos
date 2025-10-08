package com.smartsaldo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.smartsaldo.app.databinding.DialogAddAporteBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddAporteDialog : DialogFragment() {

    private var _binding: DialogAddAporteBinding? = null
    private val binding get() = _binding!!

    private val ahorroViewModel: AhorroViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private var ahorroId: Long = 0

    companion object {
        private const val ARG_AHORRO_ID = "ahorro_id"

        fun newInstance(ahorroId: Long): AddAporteDialog {
            return AddAporteDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_AHORRO_ID, ahorroId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ahorroId = arguments?.getLong(ARG_AHORRO_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddAporteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnCancelar.setOnClickListener { dismiss() }
            btnGuardar.setOnClickListener { guardarAporte() }
        }
    }

    private fun guardarAporte() {
        binding.apply {
            val monto = etMonto.text.toString().toDoubleOrNull()
            val nota = etNota.text.toString().trim().ifBlank { null }

            if (monto == null || monto <= 0) {
                etMonto.error = "Ingrese un monto válido"
                return
            }

            // Obtener el usuarioId del AuthViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                authViewModel.usuario.collect { usuario ->
                    if (usuario != null) {
                        ahorroViewModel.agregarAporte(
                            ahorroId = ahorroId,
                            monto = monto,
                            nota = nota,
                            usuarioId = usuario.uid
                        )
                        dismiss()
                    } else {
                        etMonto.error = "Error: Usuario no autenticado"
                    }
                }
            }
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