package com.smartsaldo.app.db.repository

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.smartsaldo.app.db.dao.UsuarioDao
import com.smartsaldo.app.db.dao.CategoriaDao
import com.smartsaldo.app.db.dao.TransaccionDao
import com.smartsaldo.app.db.dao.AhorroDao
import com.smartsaldo.app.db.entities.*
import kotlinx.coroutines.tasks.await
import android.content.Context
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val usuarioDao: UsuarioDao,
    private val categoriaDao: CategoriaDao,
    private val transaccionDao: TransaccionDao,
    private val ahorroDao: AhorroDao,
    private val context: Context
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("590560104917-ju6in6ave70sib1s4fjshkndpi56bhlv.apps.googleusercontent.com")
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
                provider = "email",
                isActive = true
            )

            usuarioDao.deactivateAllUsers()
            usuarioDao.insertOrUpdate(usuario)

            // Sincronizar desde Firestore
            sincronizarDesdeFirestore(firebaseUser.uid)

            Result.success(usuario)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error en signInWithEmail", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<Usuario> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Usuario nulo")

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val usuario = Usuario(
                uid = firebaseUser.uid,
                email = firebaseUser.email!!,
                displayName = displayName,
                photoURL = null,
                provider = "email",
                isActive = true
            )

            usuarioDao.deactivateAllUsers()
            usuarioDao.insertOrUpdate(usuario)

            crearCategoriasDefault(usuario.uid)

            // Intentar guardar en Firestore (no bloquear si falla)
            try {
                guardarUsuarioEnFirestore(usuario)
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "No se pudo guardar en Firestore: ${e.message}")
            }

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Usuario> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Usuario nulo")

            val usuario = Usuario(
                uid = firebaseUser.uid,
                email = firebaseUser.email!!,
                displayName = firebaseUser.displayName,
                photoURL = firebaseUser.photoUrl?.toString(),
                provider = "google",
                isActive = true
            )

            // Guardar localmente
            usuarioDao.deactivateAllUsers()
            usuarioDao.insertOrUpdate(usuario)

            if (result.additionalUserInfo?.isNewUser == true) {
                // Usuario nuevo - crear categorías primero
                crearCategoriasDefault(usuario.uid)

                // Intentar guardar en Firestore (no bloquear si falla)
                try {
                    guardarUsuarioEnFirestore(usuario)
                } catch (e: Exception) {
                    android.util.Log.w("AuthRepository", "No se pudo guardar en Firestore: ${e.message}")
                }
            } else {
                // Usuario existente, intentar sincronizar desde Firestore
                try {
                    sincronizarDesdeFirestore(firebaseUser.uid)
                } catch (e: Exception) {
                    android.util.Log.w("AuthRepository", "No se pudo sincronizar desde Firestore: ${e.message}")
                }
            }

            Result.success(usuario)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut().await()
        usuarioDao.deactivateAllUsers()
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun guardarUsuarioEnFirestore(usuario: Usuario) {
        firestore.collection("usuarios")
            .document(usuario.uid)
            .set(mapOf(
                "email" to usuario.email,
                "displayName" to usuario.displayName,
                "photoURL" to usuario.photoURL,
                "provider" to usuario.provider,
                "createdAt" to usuario.createdAt
            ))
            .await()
    }

    suspend fun sincronizarTransaccionesAFirestore(usuarioId: String) {
        try {
            val transacciones = transaccionDao.getTransaccionesConCategoria(usuarioId).first()

            val batch = firestore.batch()
            transacciones.forEach { transaccionConCategoria ->
                val doc = firestore.collection("usuarios")
                    .document(usuarioId)
                    .collection("transacciones")
                    .document(transaccionConCategoria.transaccion.id.toString())

                batch.set(doc, mapOf(
                    "monto" to transaccionConCategoria.transaccion.monto,
                    "descripcion" to transaccionConCategoria.transaccion.descripcion,
                    "notas" to transaccionConCategoria.transaccion.notas,
                    "fecha" to transaccionConCategoria.transaccion.fecha,
                    "categoriaId" to transaccionConCategoria.transaccion.categoriaId,
                    "tipo" to transaccionConCategoria.transaccion.tipo.name,
                    "createdAt" to transaccionConCategoria.transaccion.createdAt,
                    "updatedAt" to transaccionConCategoria.transaccion.updatedAt
                ))
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sincronizarAhorrosAFirestore(usuarioId: String) {
        try {
            val ahorros = ahorroDao.getAhorros(usuarioId).first()

            val batch = firestore.batch()
            ahorros.forEach { ahorro ->
                val doc = firestore.collection("usuarios")
                    .document(usuarioId)
                    .collection("ahorros")
                    .document(ahorro.id.toString())

                batch.set(doc, mapOf(
                    "nombre" to ahorro.nombre,
                    "metaMonto" to ahorro.metaMonto,
                    "montoActual" to ahorro.montoActual,
                    "createdAt" to ahorro.createdAt,
                    "updatedAt" to ahorro.updatedAt
                ))
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sincronizarDesdeFirestore(usuarioId: String) {
        try {
            val transaccionesSnapshot = firestore.collection("usuarios")
                .document(usuarioId)
                .collection("transacciones")
                .get()
                .await()

            transaccionesSnapshot.documents.forEach { doc ->
                val transaccion = Transaccion(
                    id = doc.id.toLongOrNull() ?: 0,
                    monto = doc.getDouble("monto") ?: 0.0,
                    descripcion = doc.getString("descripcion") ?: "",
                    notas = doc.getString("notas"),
                    fecha = doc.getLong("fecha") ?: 0,
                    categoriaId = doc.getLong("categoriaId") ?: 0,
                    usuarioId = usuarioId,
                    tipo = TipoTransaccion.valueOf(doc.getString("tipo") ?: "GASTO"),
                    createdAt = doc.getLong("createdAt") ?: 0,
                    updatedAt = doc.getLong("updatedAt") ?: 0
                )
                transaccionDao.insertTransaccion(transaccion)
            }

            val ahorrosSnapshot = firestore.collection("usuarios")
                .document(usuarioId)
                .collection("ahorros")
                .get()
                .await()

            ahorrosSnapshot.documents.forEach { doc ->
                val ahorro = Ahorro(
                    id = doc.id.toLongOrNull() ?: 0,
                    nombre = doc.getString("nombre") ?: "",
                    metaMonto = doc.getDouble("metaMonto") ?: 0.0,
                    montoActual = doc.getDouble("montoActual") ?: 0.0,
                    usuarioId = usuarioId,
                    createdAt = doc.getLong("createdAt") ?: 0,
                    updatedAt = doc.getLong("updatedAt") ?: 0
                )
                ahorroDao.insertAhorro(ahorro)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun crearCategoriasDefault(usuarioId: String) {
        val categorias = listOf(
            Categoria(nombre = "Comida", icono = "🍔", color = "#FF5722", tipo = "GASTO", esDefault = true),
            Categoria(nombre = "Transporte", icono = "🚗", color = "#2196F3", tipo = "GASTO", esDefault = true),
            Categoria(nombre = "Ocio", icono = "🎮", color = "#9C27B0", tipo = "GASTO", esDefault = true),
            Categoria(nombre = "Salud", icono = "🏥", color = "#F44336", tipo = "GASTO", esDefault = true),
            Categoria(nombre = "Sueldo", icono = "💼", color = "#4CAF50", tipo = "INGRESO", esDefault = true),
            Categoria(nombre = "Freelance", icono = "💻", color = "#00BCD4", tipo = "INGRESO", esDefault = true)
        )
        categoriaDao.insertCategorias(categorias)
    }
}