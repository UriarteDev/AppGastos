package com.smartsaldo.app.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.data.local.entities.Usuario
import com.smartsaldo.app.data.repository.AuthRepository
import com.smartsaldo.app.data.local.dao.UsuarioDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioDao: UsuarioDao
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                // Verificar si hay un usuario activo en Room
                val usuarioLocal = usuarioDao.getUsuarioActivo()

                // Verificar si hay un usuario en Firebase
                val currentUser = authRepository.getCurrentUser()

                if (currentUser != null && usuarioLocal != null) {
                    _usuario.value = usuarioLocal
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithEmail(email, password)
            if (result.isSuccess) {
                _usuario.value = result.getOrNull()
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Error de login")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUpWithEmail(email, password, displayName)
            if (result.isSuccess) {
                _usuario.value = result.getOrNull()
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Error de registro")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.signInWithGoogle(idToken)

                if (result.isSuccess) {
                    val usuario = result.getOrNull()
                    _usuario.value = usuario

                    kotlinx.coroutines.delay(100)
                    _authState.value = AuthState.Authenticated
                } else {
                    val error = result.exceptionOrNull()
                    error?.printStackTrace()
                    _authState.value = AuthState.Error(
                        error?.message ?: "Error con Google"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _usuario.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Actualiza el usuario en la base de datos local y en el StateFlow
     */
    fun actualizarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                usuarioDao.insertOrUpdate(usuario)
                _usuario.value = usuario
                android.util.Log.d("AuthViewModel", "Usuario actualizado: ${usuario.photoURL}")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error actualizando usuario", e)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepository.resetPassword(email)
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}