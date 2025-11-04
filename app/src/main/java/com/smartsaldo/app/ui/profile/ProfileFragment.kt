package com.smartsaldo.app.ui.profile

import android.app.Activity
import android.content.Context
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
import com.smartsaldo.app.ui.categorias.CategoriaViewModel
import com.smartsaldo.app.ui.shared.AuthViewModel
import com.smartsaldo.app.utils.CurrencyHelper
import com.smartsaldo.app.utils.LocaleHelper
import com.smartsaldo.app.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
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
            btnCerrarSesion.setOnClickListener {
                mostrarDialogCerrarSesion()
            }

            btnSincronizar.setOnClickListener {
                sincronizarDatos()
            }

            ivFotoPerfil.setOnClickListener {
                seleccionarImagenPerfil()
            }

            btnCambiarIdioma.setOnClickListener {
                mostrarDialogCambiarIdioma()
            }
            btnCambiarMoneda.setOnClickListener {
                mostrarDialogCambiarMoneda()
            }
        }
    }

    private fun mostrarDialogCambiarMoneda() {
        val monedas = CurrencyHelper.Currency.values()
        val nombresMonedas = monedas.map { getString(it.nameResId) }.toTypedArray()

        val monedaActual = CurrencyHelper.getSelectedCurrency(requireContext())
        val selectedIndex = monedas.indexOf(monedaActual)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.seleccionar_moneda))
            .setSingleChoiceItems(nombresMonedas, selectedIndex) { dialog, which ->
                val monedaSeleccionada = monedas[which]
                CurrencyHelper.setSelectedCurrency(requireContext(), monedaSeleccionada)

                Snackbar.make(
                    binding.root,
                    getString(R.string.moneda_cambiada, getString(monedaSeleccionada.nameResId)),
                    Snackbar.LENGTH_SHORT
                ).show()

                dialog.dismiss()

                // Reiniciar actividad para aplicar cambios
                requireActivity().recreate()
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun mostrarDialogCambiarIdioma() {
        val idiomas = arrayOf("Español", "English")
        val idiomasCodigos = arrayOf("es", "en")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.seleccionar_idioma))
            .setItems(idiomas) { _, which ->
                cambiarIdioma(idiomasCodigos[which])
            }
            .show()
    }

    private fun cambiarIdioma(codigoIdioma: String) {
        // Guardar idioma usando LocaleHelper
        LocaleHelper.setLocale(requireContext(), codigoIdioma)

        // ✅ Recrear categorías predefinidas
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                authViewModel.usuario.value?.let { usuario ->
                    // Usar el CategoriaViewModel inyectado
                    val categoriaViewModel: CategoriaViewModel by activityViewModels()
                    categoriaViewModel.recrearCategoriasDefault(usuario.uid)

                    delay(500) // Esperar que se recreen
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error recreando categorías", e)
            }
        }

        // Reiniciar la app
        requireActivity().packageManager
            .getLaunchIntentForPackage(requireActivity().packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
                requireActivity().finish()
            } ?: requireActivity().recreate()
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
            Snackbar.make(binding.root, getString(R.string.error_usuario_no_autenticado), Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.ivFotoPerfil.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Comprimir imagen
                val compressedData = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    comprimirImagen(imageUri)
                }

                // Subir a carpeta profile_images
                val fileName = "${usuario.uid}.jpg"
                val storageRef = storage.reference
                    .child("profile_images")
                    .child(fileName)

                // Metadata básico
                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                // Subir archivo
                val uploadTask = storageRef.putBytes(compressedData, metadata)

                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Obtener URL de descarga
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Actualizar Firebase Auth
                        val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUrl)
                            .build()

                        usuario.updateProfile(profileUpdate).addOnSuccessListener {
                            // Actualizar Room local
                            viewLifecycleOwner.lifecycleScope.launch {
                                authViewModel.usuario.value?.let { currentUser ->
                                    val updatedUser = currentUser.copy(photoURL = downloadUrl.toString())
                                    authViewModel.actualizarUsuario(updatedUser)
                                }

                                // Actualizar UI
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    cargarFotoPerfil(downloadUrl.toString())
                                    binding.progressBar.visibility = View.GONE
                                    binding.ivFotoPerfil.isEnabled = true

                                    Snackbar.make(binding.root, getString(R.string.foto_actualizada), Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }.addOnFailureListener { e ->
                            mostrarError(getString(R.string.error_actualizando_perfil, e.message ?: ""))
                        }
                    }.addOnFailureListener { e ->
                        mostrarError(getString(R.string.error_obteniendo_url, e.message ?: ""))
                    }
                }.addOnFailureListener { e ->
                    val mensaje = when {
                        e.message?.contains("Unable to obtain", ignoreCase = true) == true ->
                            getString(R.string.error_storage_config)
                        e.message?.contains("permission", ignoreCase = true) == true ->
                            getString(R.string.error_sin_permisos)
                        e.message?.contains("network", ignoreCase = true) == true ->
                            getString(R.string.error_red)
                        else -> getString(R.string.error_subir_imagen, e.message ?: "")
                    }
                    mostrarError(mensaje)
                }

            } catch (e: Exception) {
                mostrarError(getString(R.string.error_procesar_imagen, e.message ?: ""))
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            binding.progressBar.visibility = View.GONE
            binding.ivFotoPerfil.isEnabled = true
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
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
                // Verificar internet
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.sin_conexion_internet),
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val usuario = authViewModel.usuario.value
                if (usuario == null) {
                    Snackbar.make(binding.root, "Usuario no encontrado", Snackbar.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                // Sincronizar bidireccional
                authViewModel.sincronizarDatos() // Descargar
                kotlinx.coroutines.delay(1000)
                authViewModel.sincronizarTransaccionesAFirestore(usuario.uid) // Subir transacciones
                authViewModel.sincronizarAhorrosAFirestore(usuario.uid) // Subir ahorros

                Snackbar.make(
                    binding.root,
                    getString(R.string.datos_sincronizados),
                    Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mostrarDialogCerrarSesion() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.cerrar_sesion))
            .setMessage(getString(R.string.estas_seguro_cerrar_sesion))
            .setPositiveButton(getString(R.string.cerrar_sesion)) { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun cerrarSesion() {
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.signOut()

            // ✅ Resetear setup_completed
            val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("setup_completed", false).apply()

            // Ir a WelcomeSetupActivity
            val intent = Intent(requireContext(), com.smartsaldo.app.ui.welcome.WelcomeSetupActivity::class.java)
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