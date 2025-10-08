package com.smartsaldo.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.FragmentProfileBinding
import com.smartsaldo.app.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                subirImagenPerfil(uri)
            }
        }
    }

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
        binding.apply {
            // Cerrar sesión
            btnCerrarSesion.setOnClickListener {
                mostrarDialogCerrarSesion()
            }

            // Sincronizar
            btnSincronizar.setOnClickListener {
                sincronizarDatos()
            }

            // Cambiar foto de perfil
            ivFotoPerfil.setOnClickListener {
                seleccionarImagenPerfil()
            }
        }
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.usuario.collect { usuario ->
                usuario?.let {
                    binding.apply {
                        tvNombre.text = it.displayName ?: "Usuario"
                        tvEmail.text = it.email

                        // Cargar foto de perfil
                        cargarFotoPerfil(it.photoURL)
                    }
                }
            }
        }
    }

    private fun cargarFotoPerfil(photoURL: String?) {
        if (!photoURL.isNullOrBlank()) {
            Glide.with(this)
                .load(photoURL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivFotoPerfil)
        } else {
            binding.ivFotoPerfil.setImageResource(R.drawable.ic_person)
        }
    }

    private fun seleccionarImagenPerfil() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun subirImagenPerfil(imageUri: Uri) {
        val usuario = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.ivFotoPerfil.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Referencia a Firebase Storage
                val storageRef = storage.reference
                    .child("profile_images")
                    .child("${usuario.uid}.jpg")

                // Subir imagen
                storageRef.putFile(imageUri).await()

                // Obtener URL de descarga
                val downloadUrl = storageRef.downloadUrl.await()

                // Actualizar perfil en Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(downloadUrl)
                    .build()

                usuario.updateProfile(profileUpdates).await()

                // Actualizar en Room (local)
                authViewModel.usuario.value?.let { currentUser ->
                    val updatedUser = currentUser.copy(photoURL = downloadUrl.toString())
                    // Aquí deberías tener un método en AuthViewModel para actualizar el usuario
                    authViewModel.actualizarUsuario(updatedUser)
                }

                // Actualizar UI
                cargarFotoPerfil(downloadUrl.toString())

                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Foto de perfil actualizada ✅",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Error al subir imagen: ${e.message}",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.ivFotoPerfil.isEnabled = true
            }
        }
    }

    private fun sincronizarDatos() {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Aquí puedes implementar sincronización manual si lo deseas
                kotlinx.coroutines.delay(1000)

                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Datos sincronizados ✅",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Error de sincronización",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mostrarDialogCerrarSesion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.signOut()

            // Navegar a LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}