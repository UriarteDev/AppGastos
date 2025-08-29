package com.example.myapplication.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.db.dao.FinanzasDao
import com.example.myapplication.db.dao.MovimientoDao
import com.example.myapplication.db.dao.UsuarioDao
import com.example.myapplication.db.entities.Finanzas
import com.example.myapplication.db.entities.Movimiento
import com.example.myapplication.db.entities.Usuario

@Database(
    entities = [Finanzas::class, Movimiento::class, Usuario::class],
    version = 2, // << súbelo si cambiaste algo
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun finanzasDao(): FinanzasDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanzas_db"
                )
                    //.fallbackToDestructiveMigration() // solo si quieres resetear sin migración
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}