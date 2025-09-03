package com.smartsaldo.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsaldo.app.db.dao.FinanzasDao
import com.smartsaldo.app.db.dao.GastoDao
import com.smartsaldo.app.db.dao.MovimientoDao
import com.smartsaldo.app.db.dao.UsuarioDao
import com.smartsaldo.app.db.entities.Finanzas
import com.smartsaldo.app.db.entities.Gasto
import com.smartsaldo.app.db.entities.Movimiento
import com.smartsaldo.app.db.entities.Usuario

@Database(
    entities = [Usuario::class, Finanzas::class, Movimiento::class, Gasto::class],
    version = 3, // subimos versión
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun finanzasDao(): FinanzasDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun gastoDao(): GastoDao

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
                    .fallbackToDestructiveMigration() // importante
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
