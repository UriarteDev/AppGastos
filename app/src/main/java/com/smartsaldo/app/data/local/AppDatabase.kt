package com.smartsaldo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsaldo.app.data.local.dao.AhorroDao
import com.smartsaldo.app.data.local.dao.CategoriaDao
import com.smartsaldo.app.data.local.dao.TransaccionDao
import com.smartsaldo.app.data.local.dao.UsuarioDao
import com.smartsaldo.app.data.local.entities.Ahorro
import com.smartsaldo.app.data.local.entities.AporteAhorro
import com.smartsaldo.app.data.local.entities.Categoria
import com.smartsaldo.app.data.local.entities.Transaccion
import com.smartsaldo.app.data.local.entities.Usuario

@Database(
    entities = [Usuario::class, Categoria::class, Transaccion::class, Ahorro::class, AporteAhorro::class],
    version = 8,
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
                database.execSQL(
                    """
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
                """
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `aportes_ahorro` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ahorroId` INTEGER NOT NULL,
                        `monto` REAL NOT NULL,
                        `nota` TEXT,
                        `fecha` INTEGER NOT NULL,
                        FOREIGN KEY(`ahorroId`) REFERENCES `ahorros`(`id`) ON DELETE CASCADE
                    )
                """
                )

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_ahorros_usuarioId` ON `ahorros` (`usuarioId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_aportes_ahorro_ahorroId` ON `aportes_ahorro` (`ahorroId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columna usuarioId a aportes_ahorro
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `aportes_ahorro_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ahorroId` INTEGER NOT NULL,
                        `monto` REAL NOT NULL,
                        `nota` TEXT,
                        `usuarioId` TEXT NOT NULL,
                        `fecha` INTEGER NOT NULL,
                        FOREIGN KEY(`ahorroId`) REFERENCES `ahorros`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`uid`) ON DELETE CASCADE
                    )
                """
                )

                // Copiar datos existentes
                database.execSQL(
                    """
                    INSERT INTO `aportes_ahorro_new` (`id`, `ahorroId`, `monto`, `nota`, `usuarioId`, `fecha`)
                    SELECT `id`, `ahorroId`, `monto`, `nota`, 
                           (SELECT `usuarioId` FROM `ahorros` WHERE `ahorros`.`id` = `aportes_ahorro`.`ahorroId` LIMIT 1),
                           `fecha`
                    FROM `aportes_ahorro`
                """
                )

                // Eliminar tabla vieja
                database.execSQL("DROP TABLE `aportes_ahorro`")

                // Renombrar tabla nueva
                database.execSQL("ALTER TABLE `aportes_ahorro_new` RENAME TO `aportes_ahorro`")

                // Crear índices
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_aportes_ahorro_ahorroId` ON `aportes_ahorro` (`ahorroId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_aportes_ahorro_usuarioId` ON `aportes_ahorro` (`usuarioId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("AppDatabase", "🔄 Migrando de versión 6 a 7: Eliminando foreign key de categorias")

                // 1️⃣ Crear tabla nueva SIN foreign key
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categorias_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `icono` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `esDefault` INTEGER NOT NULL,
                        `usuarioId` TEXT
                    )
                """
                )

                // 2️⃣ Copiar todos los datos de la tabla vieja
                database.execSQL(
                    """
                    INSERT INTO `categorias_new` (`id`, `nombre`, `icono`, `color`, `tipo`, `esDefault`, `usuarioId`)
                    SELECT `id`, `nombre`, `icono`, `color`, `tipo`, `esDefault`, `usuarioId`
                    FROM `categorias`
                """
                )

                // 3️⃣ Eliminar tabla vieja
                database.execSQL("DROP TABLE `categorias`")

                // 4️⃣ Renombrar la nueva tabla
                database.execSQL("ALTER TABLE `categorias_new` RENAME TO `categorias`")

                android.util.Log.d("AppDatabase", "✅ Migración 6→7 completada")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columna 'key' a la tabla 'categorias'
                database.execSQL(
                    """
            ALTER TABLE `categorias` 
            ADD COLUMN `key` TEXT
            """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartsaldo_db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}