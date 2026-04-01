package com.tudominio.crianza

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RegistroTiempo::class,
        Hijo::class,
        Padre::class,
        ConfiguracionTiempo::class,
        Evento::class,
        Gasto::class,
        Compensacion::class,
        Recuerdo::class,
        ConfiguracionIntegracion::class,
        FiltroEmail::class,
        ItemCompra::class,
        Documento::class,
        Mensaje::class,
        CategoriaCompra::class,
        Pendiente::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familiaDao(): FamiliaDao
    abstract fun registroTiempoDao(): RegistroTiempoDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun eventoDao(): EventoDao
    abstract fun gastoDao(): GastoDao
    abstract fun compensacionDao(): CompensacionDao
    abstract fun recuerdoDao(): RecuerdoDao
    abstract fun configuracionIntegracionDao(): ConfiguracionIntegracionDao
    abstract fun filtroEmailDao(): FiltroEmailDao
    abstract fun itemCompraDao(): ItemCompraDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun mensajeDao(): MensajeDao
    abstract fun categoriaCompraDao(): CategoriaCompraDao
    abstract fun pendienteDao(): PendienteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v14 → v15: agregar columna "categoria" a gastos
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gastos ADD COLUMN categoria TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crianza_database"
                )
                    .addMigrations(MIGRATION_14_15)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
