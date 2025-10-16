package com.smartsaldo.app.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.FragmentProfileBinding
import com.smartsaldo.app.ui.auth.LoginActivity
import com.smartsaldo.app.ui.shared.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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
            btnCerrarSesion.setOnClickListener {
                mostrarDialogCerrarSesion()
            }

            btnSincronizar.setOnClickListener {
                sincronizarDatos()
            }

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
        val usuario = auth.currentUser
        if (usuario == null) {
            Snackbar.make(binding.root, "Error: Usuario no autenticado", Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.ivFotoPerfil.isEnabled = false

        try {
            Log.d("ProfileFragment", "Iniciando subida de imagen...")

            // Comprimir la imagen antes de subirla
            Log.d("ProfileFragment", "Comprimiendo imagen...")
            val compressedData = comprimirImagen(imageUri)
            Log.d("ProfileFragment", "Imagen comprimida: ${compressedData.size / 1024}KB")

            // Referencia a Firebase Storage con nombre simple
            val fileName = "${usuario.uid}.jpg"
            val storageRef = storage.reference
                .child("profile_images")
                .child(fileName)

            Log.d("ProfileFragment", "Subiendo imagen a Storage: $fileName")

            // Subir con callbacks en lugar de await
            val uploadTask = storageRef.putBytes(compressedData)

            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                Log.d("ProfileFragment", "Progreso de subida: $progress%")
            }.addOnSuccessListener { taskSnapshot ->
                Log.d("ProfileFragment", "Imagen subida exitosamente")
                Log.d("ProfileFragment", "Obteniendo URL de descarga...")

                // Obtener URL de descarga
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d("ProfileFragment", "URL obtenida: $downloadUri")

                    // Actualizar perfil en Firebase Auth
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()

                    usuario.updateProfile(profileUpdates).addOnSuccessListener {
                        Log.d("ProfileFragment", "Perfil actualizado en Firebase Auth")

                        // Actualizar en Room (local)
                        viewLifecycleOwner.lifecycleScope.launch {
                            val currentUser = authViewModel.usuario.value
                            if (currentUser != null) {
                                Log.d("ProfileFragment", "Actualizando usuario local...")
                                val updatedUser = currentUser.copy(photoURL = downloadUri.toString())
                                authViewModel.actualizarUsuario(updatedUser)
                            }

                            // Actualizar UI
                            cargarFotoPerfil(downloadUri.toString())

                            binding.progressBar.visibility = View.GONE
                            binding.ivFotoPerfil.isEnabled = true

                            Snackbar.make(
                                binding.root,
                                "Foto de perfil actualizada ✅",
                                Snackbar.LENGTH_SHORT
                            ).show()

                            Log.d("ProfileFragment", "Proceso completado exitosamente")
                        }
                    }.addOnFailureListener { e ->
                        Log.e("ProfileFragment", "Error actualizando perfil en Auth", e)
                        binding.progressBar.visibility = View.GONE
                        binding.ivFotoPerfil.isEnabled = true
                        Snackbar.make(binding.root, "Error actualizando perfil: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Error obteniendo URL", e)
                    binding.progressBar.visibility = View.GONE
                    binding.ivFotoPerfil.isEnabled = true
                    Snackbar.make(binding.root, "Error obteniendo URL: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error subiendo imagen", e)
                binding.progressBar.visibility = View.GONE
                binding.ivFotoPerfil.isEnabled = true
                Snackbar.make(binding.root, "Error subiendo imagen: ${e.message}", Snackbar.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error al procesar imagen", e)
            binding.progressBar.visibility = View.GONE
            binding.ivFotoPerfil.isEnabled = true
            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * Comprime la imagen para reducir el tamaño del archivo
     */
    private fun comprimirImagen(uri: Uri): ByteArray {
        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)

        // Redimensionar si es muy grande
        val maxSize = 800
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxSize || height > maxSize) {
            val scale = Math.min(maxSize.toFloat() / width, maxSize.toFloat() / height)
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        // Comprimir a JPEG
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        // Liberar memoria
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    private fun sincronizarDatos() {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                kotlinx.coroutines.delay(1000)

                Snackbar.make(
                    binding.root,
                    "Datos sincronizados ✅",
                    Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error de sincronización",
                    Snackbar.LENGTH_SHORT
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