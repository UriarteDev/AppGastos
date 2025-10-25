package com.smartsaldo.app.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.smartsaldo.app.R
import com.smartsaldo.app.data.local.dao.*
import com.smartsaldo.app.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

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

            // ✅ SINCRONIZAR DESDE FIRESTORE
            sincronizarDesdeFirestore(firebaseUser.uid)

            Result.success(usuario)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en signInWithEmail", e)
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

            try {
                guardarUsuarioEnFirestore(usuario)
            } catch (e: Exception) {
                Log.w("AuthRepository", "No se pudo guardar en Firestore: ${e.message}")
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

            usuarioDao.deactivateAllUsers()
            usuarioDao.insertOrUpdate(usuario)

            if (result.additionalUserInfo?.isNewUser == true) {
                crearCategoriasDefault(usuario.uid)
                try {
                    guardarUsuarioEnFirestore(usuario)
                } catch (e: Exception) {
                    Log.w("AuthRepository", "No se pudo guardar en Firestore: ${e.message}")
                }
            } else {
                // ✅ SINCRONIZAR DESDE FIRESTORE
                try {
                    sincronizarDesdeFirestore(firebaseUser.uid)
                } catch (e: Exception) {
                    Log.w("AuthRepository", "No se pudo sincronizar desde Firestore: ${e.message}")
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

    // ✅ MÉTODO MEJORADO DE SINCRONIZACIÓN (ahora público)
    suspend fun sincronizarDesdeFirestore(usuarioId: String) {
        try {
            Log.d("AuthRepository", "🔄 Iniciando sincronización para usuario: $usuarioId")

            // Sincronizar transacciones
            val transaccionesSnapshot = firestore.collection("usuarios")
                .document(usuarioId)
                .collection("transacciones")
                .get()
                .await()

            Log.d("AuthRepository", "📦 Transacciones encontradas: ${transaccionesSnapshot.documents.size}")

            transaccionesSnapshot.documents.forEach { doc ->
                try {
                    val transaccion = Transaccion(
                        id = doc.id.toLongOrNull() ?: 0,
                        monto = doc.getDouble("monto") ?: 0.0,
                        descripcion = doc.getString("descripcion") ?: "",
                        notas = doc.getString("notas"),
                        fecha = doc.getLong("fecha") ?: System.currentTimeMillis(),
                        categoriaId = doc.getLong("categoriaId") ?: 1,
                        usuarioId = usuarioId,
                        tipo = TipoTransaccion.valueOf(doc.getString("tipo") ?: "GASTO"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                    transaccionDao.insertTransaccion(transaccion)
                    Log.d("AuthRepository", "✅ Transacción sincronizada: ${transaccion.descripcion}")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "❌ Error sincronizando transacción: ${doc.id}", e)
                }
            }

            // Sincronizar ahorros
            val ahorrosSnapshot = firestore.collection("usuarios")
                .document(usuarioId)
                .collection("ahorros")
                .get()
                .await()

            Log.d("AuthRepository", "💰 Ahorros encontrados: ${ahorrosSnapshot.documents.size}")

            ahorrosSnapshot.documents.forEach { doc ->
                try {
                    val ahorro = Ahorro(
                        id = doc.id.toLongOrNull() ?: 0,
                        nombre = doc.getString("nombre") ?: "",
                        metaMonto = doc.getDouble("metaMonto") ?: 0.0,
                        montoActual = doc.getDouble("montoActual") ?: 0.0,
                        usuarioId = usuarioId,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                    ahorroDao.insertAhorro(ahorro)
                    Log.d("AuthRepository", "✅ Ahorro sincronizado: ${ahorro.nombre}")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "❌ Error sincronizando ahorro: ${doc.id}", e)
                }
            }

            Log.d("AuthRepository", "✅ Sincronización completada")

        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error en sincronización", e)
            throw e
        }
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

    private suspend fun crearCategoriasDefault(usuarioId: String) {
        val categoriasExistentes = categoriaDao.getCategorias(usuarioId).first()
        if (categoriasExistentes.isNotEmpty()) {
            return
        }

        val categorias = listOf(
            Categoria(
                nombre = context.getString(R.string.cat_comida),
                icono = "🍔",
                color = "#FF5722",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_transporte),
                icono = "🚗",
                color = "#2196F3",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_ocio),
                icono = "🎮",
                color = "#9C27B0",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_salud),
                icono = "🏥",
                color = "#F44336",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_casa),
                icono = "🏠",
                color = "#795548",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_educacion),
                icono = "📚",
                color = "#3F51B5",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_ropa),
                icono = "👔",
                color = "#E91E63",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_otros_gastos),
                icono = "📦",
                color = "#607D8B",
                tipo = "GASTO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_sueldo),
                icono = "💼",
                color = "#4CAF50",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_freelance),
                icono = "💻",
                color = "#00BCD4",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_inversiones),
                icono = "📈",
                color = "#8BC34A",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_regalos),
                icono = "🎁",
                color = "#FFEB3B",
                tipo = "INGRESO",
                esDefault = true
            ),
            Categoria(
                nombre = context.getString(R.string.cat_otros_ingresos),
                icono = "💰",
                color = "#4CAF50",
                tipo = "INGRESO",
                esDefault = true
            )
        )

        categoriaDao.insertCategorias(categorias)
    }
}