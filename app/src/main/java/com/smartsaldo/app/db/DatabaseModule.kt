package com.smartsaldo.app.db

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.smartsaldo.app.db.AppDatabase
import com.smartsaldo.app.db.repository.*
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
        return AppDatabase.getDatabase(context)
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
    fun provideAhorroRepository(
        ahorroDao: com.smartsaldo.app.db.dao.AhorroDao
    ): AhorroRepository {
        return AhorroRepository(ahorroDao)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        usuarioDao: com.smartsaldo.app.db.dao.UsuarioDao,
        categoriaDao: com.smartsaldo.app.db.dao.CategoriaDao,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(firebaseAuth, usuarioDao, categoriaDao, context)
    }

    @Provides
    @Singleton
    fun provideTransaccionRepository(
        transaccionDao: com.smartsaldo.app.db.dao.TransaccionDao
    ): TransaccionRepository {
        return TransaccionRepository(transaccionDao)
    }

    @Provides
    @Singleton
    fun provideCategoriaRepository(
        categoriaDao: com.smartsaldo.app.db.dao.CategoriaDao
    ): CategoriaRepository {
        return CategoriaRepository(categoriaDao)
    }
}