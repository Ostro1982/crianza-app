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
        ItemCompra::class,
        Documento::class,
        Mensaje::class,
        CategoriaCompra::class,
        Pendiente::class,
        RegistroEdicion::class,
        CustodySchedule::class,
        CategoriaGasto::class
    ],
    version = 22,
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
    abstract fun itemCompraDao(): ItemCompraDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun mensajeDao(): MensajeDao
    abstract fun categoriaCompraDao(): CategoriaCompraDao
    abstract fun pendienteDao(): PendienteDao
    abstract fun registroEdicionDao(): RegistroEdicionDao
    abstract fun custodyScheduleDao(): CustodyScheduleDao
    abstract fun categoriaGastoDao(): CategoriaGastoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v14 → v15: agregar columna "categoria" a gastos
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gastos ADD COLUMN categoria TEXT NOT NULL DEFAULT ''")
            }
        }

        // v15 → v16: agregar autocompensado a registros_tiempo/gastos y tipoCompensacion a compensaciones
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE registros_tiempo ADD COLUMN autocompensado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gastos ADD COLUMN autocompensado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE compensaciones ADD COLUMN tipoCompensacion TEXT NOT NULL DEFAULT 'dinero'")
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

        // v19 → v20: cleanup integraciones (Telegram/Email/WhatsApp) y nuevos campos co-parenting.
        // Recrea configuracion_integracion (SQLite no soporta DROP COLUMN en versiones < 3.35).
        // Borra tabla filtros_email completa (entidad eliminada).
        // Agrega columnas: idPagoFotoUri (gastos) — foto recibo. esEvidencia (mensajes) — para futuro lock.
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS filtros_email")

                db.execSQL("""
                    CREATE TABLE configuracion_integracion_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        notifEventos INTEGER NOT NULL DEFAULT 1,
                        notifGastos INTEGER NOT NULL DEFAULT 1,
                        notifCompensaciones INTEGER NOT NULL DEFAULT 1,
                        notifCompras INTEGER NOT NULL DEFAULT 1,
                        notifCustodia INTEGER NOT NULL DEFAULT 1,
                        frozenDias INTEGER NOT NULL DEFAULT 0,
                        moneda TEXT NOT NULL DEFAULT 'ARS'
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO configuracion_integracion_new (id, notifEventos, notifGastos, notifCompensaciones, notifCompras, notifCustodia, frozenDias, moneda)
                    SELECT id, notifEventos, notifGastos, notifCompensaciones, notifCompras, 1, 0, 'ARS' FROM configuracion_integracion
                """.trimIndent())
                db.execSQL("DROP TABLE configuracion_integracion")
                db.execSQL("ALTER TABLE configuracion_integracion_new RENAME TO configuracion_integracion")

                // Foto recibo gasto (Tier 2 #7)
                db.execSQL("ALTER TABLE gastos ADD COLUMN reciboFotoUri TEXT NOT NULL DEFAULT ''")
            }
        }

        // v21 → v22: categorías gasto editables + gastos recurrentes.
        // Crea tabla categorias_gasto, popula con defaults (CATEGORIAS_GASTO).
        // Agrega columnas frecuenciaDias y origenGastoRecurrente a gastos.
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categorias_gasto (
                        nombre TEXT NOT NULL PRIMARY KEY,
                        emoji TEXT NOT NULL DEFAULT '',
                        orden INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                val defaults = listOf(
                    "Alimentación" to "🛒",
                    "Salud" to "🏥",
                    "Educación" to "📚",
                    "Ropa" to "👕",
                    "Transporte" to "🚗",
                    "Entretenimiento" to "🎭",
                    "Hogar" to "🏠",
                    "Higiene" to "🧴",
                    "Actividades" to "⚽",
                    "Otro" to "📦"
                )
                defaults.forEachIndexed { idx, (n, e) ->
                    db.execSQL("INSERT OR IGNORE INTO categorias_gasto (nombre, emoji, orden) VALUES (?, ?, ?)", arrayOf(n, e, idx))
                }
                db.execSQL("ALTER TABLE gastos ADD COLUMN frecuenciaDias INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gastos ADD COLUMN origenGastoRecurrente TEXT NOT NULL DEFAULT ''")
            }
        }

        // v20 → v21: custody scheduler. Agrega tabla custody_schedules y marca de
        // origen en registros_tiempo para poder regenerar/borrar batch.
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE registros_tiempo ADD COLUMN origenSchedule TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custody_schedules (
                        id TEXT NOT NULL PRIMARY KEY,
                        patron TEXT NOT NULL,
                        fechaInicio TEXT NOT NULL,
                        mesesGenerados INTEGER NOT NULL DEFAULT 6,
                        idPadreA TEXT NOT NULL,
                        idPadreB TEXT NOT NULL,
                        idsHijos TEXT NOT NULL DEFAULT '',
                        horaInicio TEXT NOT NULL DEFAULT '00:00',
                        horaFin TEXT NOT NULL DEFAULT '23:59',
                        fechaCreacion INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
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
                    .addMigrations(
                        MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                        MIGRATION_20_21, MIGRATION_21_22
                    )
                    // Solo destruye en downgrade (improbable). Upgrade requiere migration explícita.
                    .fallbackToDestructiveMigrationOnDowngrade()
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
