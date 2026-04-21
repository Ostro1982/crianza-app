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
        Pendiente::class,
        RegistroEdicion::class
    ],
    version = 19,
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
    abstract fun registroEdicionDao(): RegistroEdicionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v14 → v15: agregar columna "categoria" a gastos
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gastos ADD COLUMN categoria TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pendientes ADD COLUMN frecuenciaDias INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pendientes ADD COLUMN fechaCompletado INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_tiempo_fecha ON registros_tiempo(fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_tiempo_idPadre ON registros_tiempo(idPadre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_registros_tiempo_idHijo ON registros_tiempo(idHijo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_eventos_fecha ON eventos(fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_gastos_fecha ON gastos(fecha)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_gastos_idPagador ON gastos(idPagador)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pendientes_completado ON pendientes(completado)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pendientes_fechaLimite ON pendientes(fechaLimite)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS registros_edicion (
                        id TEXT NOT NULL PRIMARY KEY,
                        idRegistro TEXT NOT NULL,
                        fechaEdicion INTEGER NOT NULL,
                        fechaAnterior TEXT NOT NULL,
                        horaInicioAnterior TEXT NOT NULL,
                        horaFinAnterior TEXT NOT NULL,
                        nombreHijoAnterior TEXT NOT NULL,
                        autocompensadoAnterior INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crianza_database"
                )
                    .addMigrations(MIGRATION_14_15, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
