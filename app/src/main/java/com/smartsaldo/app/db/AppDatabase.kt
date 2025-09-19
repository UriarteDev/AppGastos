package com.smartsaldo.app.db

import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsaldo.app.db.dao.UsuarioDao
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsaldo.app.db.dao.*
import com.smartsaldo.app.db.entities.*
import com.smartsaldo.app.db.entities.Usuario

@Database(
    entities = [Usuario::class, Categoria::class, Transaccion::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de versión 3 a 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla categorias
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categorias` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `icono` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `esDefault` INTEGER NOT NULL,
                        `usuarioId` TEXT
                    )
                """)

                // Crear tabla transacciones
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transacciones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `monto` REAL NOT NULL,
                        `descripcion` TEXT NOT NULL,
                        `notas` TEXT,
                        `fecha` INTEGER NOT NULL,
                        `categoriaId` INTEGER NOT NULL,
                        `usuarioId` TEXT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`uid`) ON DELETE CASCADE,
                        FOREIGN KEY(`categoriaId`) REFERENCES `categorias`(`id`) ON DELETE RESTRICT
                    )
                """)

                // Actualizar tabla usuarios
                database.execSQL("ALTER TABLE usuarios ADD COLUMN provider TEXT DEFAULT 'email'")
                database.execSQL("ALTER TABLE usuarios ADD COLUMN photoURL TEXT")
                database.execSQL("ALTER TABLE usuarios ADD COLUMN isActive INTEGER DEFAULT 1")

                // Migrar datos de movimientos a transacciones
                database.execSQL("""
                    INSERT INTO transacciones (monto, descripcion, fecha, categoriaId, usuarioId, tipo, createdAt, updatedAt)
                    SELECT monto, COALESCE(nombre, 'Transacción'), fecha, 1, '1', tipo, fecha, fecha
                    FROM movimientos
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartsaldo_db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}