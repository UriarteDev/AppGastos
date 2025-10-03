package com.smartsaldo.app.db

import android.content.Context
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
    entities = [Usuario::class, Categoria::class, Transaccion::class, Ahorro::class, AporteAhorro::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao
    abstract fun ahorroDao(): AhorroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ahorros` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `metaMonto` REAL NOT NULL,
                        `montoActual` REAL NOT NULL,
                        `usuarioId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`uid`) ON DELETE CASCADE
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `aportes_ahorro` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ahorroId` INTEGER NOT NULL,
                        `monto` REAL NOT NULL,
                        `nota` TEXT,
                        `fecha` INTEGER NOT NULL,
                        FOREIGN KEY(`ahorroId`) REFERENCES `ahorros`(`id`) ON DELETE CASCADE
                    )
                """)

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ahorros_usuarioId` ON `ahorros` (`usuarioId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_aportes_ahorro_ahorroId` ON `aportes_ahorro` (`ahorroId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartsaldo_db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}