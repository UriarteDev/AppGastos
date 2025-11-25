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

            // Sincronizar datos
            sincronizarDesdeFirestore(firebaseUser.uid)
            actualizarKeysDeCategoriasExistentes(firebaseUser.uid)

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

            // Crear categorías predefinidas
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
                // Usuario nuevo: crear categorías predefinidas
                crearCategoriasDefault(usuario.uid)
                try {
                    guardarUsuarioEnFirestore(usuario)
                } catch (e: Exception) {
                    Log.w("AuthRepository", "No se pudo guardar en Firestore: ${e.message}")
                }
            } else {
                // Usuario existente: sincronizar desde Firebase
                try {
                    sincronizarDesdeFirestore(firebaseUser.uid)
                    actualizarKeysDeCategoriasExistentes(firebaseUser.uid)

                    // Si no tiene categorías, crearlas
                    val categoriasExistentes = categoriaDao.getCategorias(usuario.uid).first()
                    if (categoriasExistentes.isEmpty()) {
                        Log.w("AuthRepository", "⚠️ No hay categorías, creando predefinidas...")
                        crearCategoriasDefault(usuario.uid)
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Error sincronizando: ${e.message}")
                    crearCategoriasDefault(usuario.uid)
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

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("setup_completed", false).apply()
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

    suspend fun sincronizarDesdeFirestore(usuarioId: String) {
        try {
            Log.d("AuthRepository", "🔄 Iniciando sincronización para: $usuarioId")

            // 1️⃣ Sincronizar categorías predefinidas desde Firebase
            val categoriasDefaultSnapshot = firestore.collection("usuarios")
                .document(usuarioId)
                .collection("categorias_default")
                .get()
                .await()

            if (categoriasDefaultSnapshot.documents.isNotEmpty()) {
                Log.d("AuthRepository", "🏷️ Categorías predefinidas en Firebase: ${categoriasDefaultSnapshot.documents.size}")

                categoriasDefaultSnapshot.documents.forEach { doc ->
                    try {
                        val key = doc.getString("key")
                        val categoria = Categoria(
                            id = doc.id.toLongOrNull() ?: 0,
                            nombre = doc.getString("nombre") ?: "",
                            icono = doc.getString("icono") ?: "📦",
                            color = doc.getString("color") ?: "#607D8B",
                            tipo = doc.getString("tipo") ?: "GASTO",
                            esDefault = true,
                            usuarioId = null,
                            key = key  // ← AGREGAR ESTA LÍNEA
                        )
                        Log.d("AuthRepository", "📦 Sincronizando: ${categoria.nombre}, key: ${categoria.key}")  // ← AGREGAR ESTE LOG
                        categoriaDao.insertCategoria(categoria)
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "❌ Error sincronizando categoría default: ${doc.id}", e)
                    }
                }
            }

            // 2️⃣ Sincronizar categorías personalizadas
            val categoriasSnapshot = firestore.collection("usuarios")
                .document(usuarioId)
                .collection("categorias")
                .get()
                .await()

            Log.d("AuthRepository", "🏷️ Categorías personalizadas: ${categoriasSnapshot.documents.size}")

            categoriasSnapshot.documents.forEach { doc ->
                try {
                    val categoria = Categoria(
                        id = doc.id.toLongOrNull() ?: 0,
                        nombre = doc.getString("nombre") ?: "",
                        icono = doc.getString("icono") ?: "📦",
                        color = doc.getString("color") ?: "#607D8B",
                        tipo = doc.getString("tipo") ?: "GASTO",
                        esDefault = false,
                        usuarioId = usuarioId
                    )
                    categoriaDao.insertCategoria(categoria)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "❌ Error sincronizando categoría: ${doc.id}", e)
                }
            }

            // 3️⃣ Sincronizar transacciones
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
                } catch (e: Exception) {
                    Log.e("AuthRepository", "❌ Error sincronizando transacción: ${doc.id}", e)
                }
            }

            // 4️⃣ Sincronizar ahorros
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

        // Si ya hay categorías default, no crear nuevas
        if (categoriasExistentes.any { it.esDefault }) {
            Log.d("AuthRepository", "✅ Categorías default ya existen (${categoriasExistentes.filter { it.esDefault }.size})")
            return
        }

        Log.d("AuthRepository", "🆕 Creando categorías default por primera vez...")
        val contextoConIdioma = com.smartsaldo.app.utils.LocaleHelper.onAttach(context)

        val categorias = listOf(
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_comida), icono = "🍔", color = "#FF5722", tipo = "GASTO", esDefault = true, usuarioId = null, key = "food"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_transporte), icono = "🚗", color = "#2196F3", tipo = "GASTO", esDefault = true, usuarioId = null, key = "transport"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_ocio), icono = "🎮", color = "#9C27B0", tipo = "GASTO", esDefault = true, usuarioId = null, key = "leisure"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_salud), icono = "🏥", color = "#F44336", tipo = "GASTO", esDefault = true, usuarioId = null, key = "health"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_casa), icono = "🏠", color = "#795548", tipo = "GASTO", esDefault = true, usuarioId = null, key = "home"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_educacion), icono = "📚", color = "#3F51B5", tipo = "GASTO", esDefault = true, usuarioId = null, key = "education"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_ropa), icono = "👔", color = "#E91E63", tipo = "GASTO", esDefault = true, usuarioId = null, key = "clothing"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_otros_gastos), icono = "📦", color = "#607D8B", tipo = "GASTO", esDefault = true, usuarioId = null, key = "other_expenses"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_sueldo), icono = "💼", color = "#4CAF50", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "salary"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_freelance), icono = "💻", color = "#00BCD4", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "freelance"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_inversiones), icono = "📈", color = "#8BC34A", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "investments"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_regalos), icono = "🎁", color = "#FFEB3B", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "gifts"),
            Categoria(nombre = contextoConIdioma.getString(R.string.cat_otros_ingresos), icono = "💰", color = "#4CAF50", tipo = "INGRESO", esDefault = true, usuarioId = null, key = "other_income")
        )

        categoriaDao.insertCategorias(categorias)
        Log.d("AuthRepository", "✅ ${categorias.size} categorías creadas con keys")

        // Guardar en Firebase
        try {
            categorias.forEachIndexed { index, categoria ->
                val id = (index + 1).toLong()
                firestore.collection("usuarios")
                    .document(usuarioId)
                    .collection("categorias_default")
                    .document(id.toString())
                    .set(mapOf(
                        "id" to id,
                        "nombre" to categoria.nombre,
                        "icono" to categoria.icono,
                        "color" to categoria.color,
                        "tipo" to categoria.tipo,
                        "esDefault" to true,
                        "usuarioId" to usuarioId,
                        "key" to categoria.key
                    ))
                    .await()
            }
            Log.d("AuthRepository", "✅ Categorías guardadas en Firebase")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error guardando en Firebase", e)
        }
    }

    private suspend fun actualizarKeysDeCategoriasExistentes(usuarioId: String) {
        try {
            val categoriasExistentes = categoriaDao.getCategorias(usuarioId).first()
            val categoriasSinKey = categoriasExistentes.filter { it.esDefault && it.key == null }

            if (categoriasSinKey.isEmpty()) {
                Log.d("AuthRepository", "✅ Todas las categorías ya tienen keys")
                return
            }

            Log.d("AuthRepository", "🔄 Actualizando ${categoriasSinKey.size} categorías sin key...")

            // Mapeo de nombres a keys (soporta español e inglés)
            val keysMapping = mapOf(
                "Comida" to "food", "Food" to "food",
                "Transporte" to "transport", "Transport" to "transport",
                "Ocio" to "leisure", "Leisure" to "leisure",
                "Salud" to "health", "Health" to "health",
                "Casa" to "home", "Home" to "home",
                "Educación" to "education", "Education" to "education",
                "Ropa" to "clothing", "Clothing" to "clothing",
                "Otros Gastos" to "other_expenses", "Other Expenses" to "other_expenses",
                "Sueldo" to "salary", "Salary" to "salary",
                "Freelance" to "freelance",
                "Inversiones" to "investments", "Investments" to "investments",
                "Regalos" to "gifts", "Gifts" to "gifts",
                "Otros Ingresos" to "other_income", "Other Income" to "other_income"
            )

            categoriasSinKey.forEach { categoria ->
                val nuevoKey = keysMapping[categoria.nombre]
                if (nuevoKey != null) {
                    Log.d("AuthRepository", "✏️ '${categoria.nombre}' -> key: '$nuevoKey'")
                    categoriaDao.updateCategoria(categoria.copy(key = nuevoKey))
                } else {
                    Log.w("AuthRepository", "⚠️ No se encontró key para: ${categoria.nombre}")
                }
            }

            Log.d("AuthRepository", "✅ Keys actualizados correctamente")
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error actualizando keys", e)
        }
    }
}