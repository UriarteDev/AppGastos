package com.smartsaldo.app.db.repository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartsaldo.app.db.dao.UsuarioDao
import com.smartsaldo.app.db.dao.CategoriaDao
import com.smartsaldo.app.db.entities.Usuario
import com.smartsaldo.app.db.entities.Categoria
import kotlinx.coroutines.tasks.await
import android.content.Context

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao,
    private val categoriaDao: CategoriaDao,
    private val context: Context
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("TU_WEB_CLIENT_ID_AQUI") // ⚡ Obtener de Firebase Console
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    suspend fun signInWithEmail(email: String, password: String): Result<Usuario> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Usuario nulo")

            val usuario = Usuario(
                uid = firebaseUser.uid,
                email = firebaseUser.email!!,
                displayName = firebaseUser.displayName,
                photoURL = firebaseUser.photoUrl?.toString(),
                provider = "email"
            )

            // Guardar en Room si no existe
            usuarioDao.insertOrUpdate(usuario)

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<Usuario> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Usuario nulo")

            // Actualizar perfil
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val usuario = Usuario(
                uid = firebaseUser.uid,
                email = firebaseUser.email!!,
                displayName = displayName,
                photoURL = null,
                provider = "email"
            )

            // Guardar en Room
            usuarioDao.insertOrUpdate(usuario)

            // Crear categorías predefinidas para el nuevo usuario
            crearCategoriasDefault(usuario.uid)

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(): Result<Usuario> {
        return try {
            val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw Exception("No hay cuenta de Google")

            val idToken = googleSignInAccount.idToken ?: throw Exception("Token nulo")
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Usuario nulo")

            val usuario = Usuario(
                uid = firebaseUser.uid,
                email = firebaseUser.email!!,
                displayName = firebaseUser.displayName,
                photoURL = firebaseUser.photoUrl?.toString(),
                provider = "google"
            )

            // Guardar en Room
            usuarioDao.insertOrUpdate(usuario)

            // Crear categorías si es nuevo usuario
            if (result.additionalUserInfo?.isNewUser == true) {
                crearCategoriasDefault(usuario.uid)
            }

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut().await()
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun crearCategoriasDefault(usuarioId: String) {
        val categoriasDefault = listOf(
            // Gastos
            Categoria(nombre = "Comida", icono = "restaurant", color = "#FF5722", tipo = "GASTO"),
            Categoria(nombre = "Transporte", icono = "directions_car", color = "#2196F3", tipo = "GASTO"),
            Categoria(nombre = "Ocio", icono = "sports_esports", color = "#9C27B0", tipo = "GASTO"),
            Categoria(nombre = "Salud", icono = "local_hospital", color = "#F44336", tipo = "GASTO"),
            Categoria(nombre = "Casa", icono = "home", color = "#795548", tipo = "GASTO"),
            Categoria(nombre = "Educación", icono = "school", color = "#3F51B5", tipo = "GASTO"),
            Categoria(nombre = "Ropa", icono = "checkroom", color = "#E91E63", tipo = "GASTO"),
            Categoria(nombre = "Otros", icono = "category", color = "#607D8B", tipo = "GASTO"),

            // Ingresos
            Categoria(nombre = "Sueldo", icono = "work", color = "#4CAF50", tipo = "INGRESO"),
            Categoria(nombre = "Freelance", icono = "computer", color = "#00BCD4", tipo = "INGRESO"),
            Categoria(nombre = "Inversiones", icono = "trending_up", color = "#8BC34A", tipo = "INGRESO"),
            Categoria(nombre = "Regalos", icono = "card_giftcard", color = "#FFEB3B", tipo = "INGRESO"),
            Categoria(nombre = "Otros", icono = "attach_money", color = "#4CAF50", tipo = "INGRESO")
        )

        categoriaDao.insertCategorias(categoriasDefault)
    }
}