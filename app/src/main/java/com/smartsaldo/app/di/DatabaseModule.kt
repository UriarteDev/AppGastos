package com.smartsaldo.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartsaldo.app.data.local.AppDatabase
import com.smartsaldo.app.data.local.dao.AhorroDao
import com.smartsaldo.app.data.local.dao.CategoriaDao
import com.smartsaldo.app.data.local.dao.TransaccionDao
import com.smartsaldo.app.data.local.dao.UsuarioDao
import com.smartsaldo.app.data.repository.AhorroRepository
import com.smartsaldo.app.data.repository.AuthRepository
import com.smartsaldo.app.data.repository.CategoriaRepository
import com.smartsaldo.app.data.repository.TransaccionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.Companion.getDatabase(context)
    }

    @Provides
    fun provideUsuarioDao(database: AppDatabase) = database.usuarioDao()

    @Provides
    fun provideCategoriaDao(database: AppDatabase) = database.categoriaDao()

    @Provides
    fun provideTransaccionDao(database: AppDatabase) = database.transaccionDao()

    @Provides
    fun provideAhorroDao(database: AppDatabase) = database.ahorroDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAhorroRepository(
        ahorroDao: AhorroDao
    ): AhorroRepository {
        return AhorroRepository(ahorroDao)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        usuarioDao: UsuarioDao,
        categoriaDao: CategoriaDao,
        transaccionDao: TransaccionDao,
        ahorroDao: AhorroDao,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(
            firebaseAuth,
            firestore,
            usuarioDao,
            categoriaDao,
            transaccionDao,
            ahorroDao,
            context
        )
    }

    @Provides
    @Singleton
    fun provideTransaccionRepository(
        transaccionDao: TransaccionDao
    ): TransaccionRepository {
        return TransaccionRepository(transaccionDao)
    }

    @Provides
    @Singleton
    fun provideCategoriaRepository(
        categoriaDao: CategoriaDao
    ): CategoriaRepository {
        return CategoriaRepository(categoriaDao)
    }
}