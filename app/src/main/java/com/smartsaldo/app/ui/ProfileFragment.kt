package com.smartsaldo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.smartsaldo.app.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeUser()
    }

    private fun setupUI() {
        binding.btnCerrarSesion.setOnClickListener {
            authViewModel.signOut()
        }

        binding.btnSincronizar.setOnClickListener {
            // Implementar sincronización manual si lo deseas
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "Sincronización en progreso...",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.usuario.collect { usuario ->
                usuario?.let {
                    binding.tvNombre.text = it.displayName ?: "Usuario"
                    binding.tvEmail.text = it.email

                    // Cargar foto si existe
                    it.photoURL?.let { url ->
                        // Aquí puedes usar Glide o Picasso para cargar la imagen
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}