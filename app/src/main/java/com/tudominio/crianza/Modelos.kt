package com.tudominio.crianza

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "registros_tiempo",
    indices = [Index("fecha"), Index("idPadre"), Index("idHijo")]
)
data class RegistroTiempo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val idHijo: String,
    val nombreHijo: String,
    val idPadre: String,
    val nombrePadre: String,
    val fecha: String, // YYYY-MM-DD
    val horaInicio: String, // HH:MM
    val horaFin: String, // HH:MM
    val fechaCompleta: Long = System.currentTimeMillis(),
    val esTodosLosHijos: Boolean = false,
    val autocompensado: Boolean = false, // No entra a deuda de compensación
    val origenSchedule: String = ""      // ID del CustodySchedule que lo generó. "" = manual.
)

@Entity(tableName = "registros_edicion")
data class RegistroEdicion(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idRegistro: String,
    val fechaEdicion: Long = System.currentTimeMillis(),
    val fechaAnterior: String,
    val horaInicioAnterior: String,
    val horaFinAnterior: String,
    val nombreHijoAnterior: String,
    val autocompensadoAnterior: Boolean = false
)

@Entity(tableName = "hijos")
data class Hijo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val fechaNacimiento: String = "" // YYYY-MM-DD
)

@Entity(tableName = "padres")
data class Padre(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val telefono: String = "",
    val email: String = "",
    // padre, madre, tutor, abuela, abuelo, niñera, familiar
    val rol: String = "padre",
    val fechaNacimiento: String = "" // YYYY-MM-DD
)

@Entity(tableName = "configuracion_tiempo")
data class ConfiguracionTiempo(
    @PrimaryKey
    val id: Int = 1,
    val porcentajePadre1: Int = 50,
    val porcentajePadre2: Int = 50,
    val valorHora: Double = 10.0,
    val aceptadoPadre1: Boolean = false,
    val aceptadoPadre2: Boolean = false,
    val autoAceptar: Boolean = false,
    // "hora", "dia", "semana"
    val tipoValor: String = "dia",
    val aprobadoTipoValor1: Boolean = false,
    val aprobadoTipoValor2: Boolean = false
)

@Entity(
    tableName = "eventos",
    indices = [Index("fecha")]
)
data class Evento(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String = "",
    val fecha: String, // YYYY-MM-DD
    val horaInicio: String? = null, // HH:MM
    val horaFin: String? = null, // HH:MM
    val color: Int = 0,
    val fechaCompleta: Long = System.currentTimeMillis(),
    val ubicacion: String = "",
    val origenEmail: Boolean = false,
    // "va", "no_va", "" (sin respuesta aún)
    val asistenciaPadre1: String = "",
    val asistenciaPadre2: String = ""
)

@Entity(
    tableName = "gastos",
    indices = [Index("fecha"), Index("idPagador")]
)
data class Gasto(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val descripcion: String,
    val monto: Double,
    val fecha: String, // YYYY-MM-DD
    val idPagador: String, // ID del padre que pagó
    val nombrePagador: String,
    val idsHijos: List<String> = emptyList(), // IDs de los hijos involucrados
    val nombresHijos: String = "", // Texto para mostrar
    val dividirAutomatico: Boolean = true,
    val fechaCompleta: Long = System.currentTimeMillis(),
    val categoria: String = "",
    val autocompensado: Boolean = false, // No entra a deuda de compensación
    val reciboFotoUri: String = "", // Foto del recibo (opcional, evidencia legal)
    // Recurrencia: 0 = no recurrente, 7 = semanal, 30 = mensual, 365 = anual
    val frecuenciaDias: Int = 0,
    val origenGastoRecurrente: String = "" // ID del gasto plantilla. "" = manual.
)

val CATEGORIAS_GASTO = listOf(
    "Alimentación", "Salud", "Educación", "Ropa", "Transporte",
    "Entretenimiento", "Hogar", "Higiene", "Actividades", "Otro"
)

@Entity(tableName = "compensaciones")
data class Compensacion(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val fecha: String, // YYYY-MM-DD
    val idPagador: String,
    val nombrePagador: String,
    val idReceptor: String,
    val nombreReceptor: String,
    val horasCompensadas: Double,
    val valorHora: Double,
    val montoTotal: Double,
    val fechaCompleta: Long = System.currentTimeMillis(),
    val aceptadoPadre1: Boolean = false,
    val aceptadoPadre2: Boolean = false,
    val tipoCompensacion: String = "dinero" // "dinero", "horas", "dias"
) {
    val confirmada: Boolean get() = aceptadoPadre1 && aceptadoPadre2
}

@Entity(tableName = "recuerdos")
data class Recuerdo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String,
    val fecha: String, // YYYY-MM-DD
    val imagenUri: String? = null, // Path o URI de la imagen
    val fechaCompleta: Long = System.currentTimeMillis()
)

data class ConfiguracionCompensacion(
    val valorHora: Double = 10.0
)

@Entity(tableName = "items_compra")
data class ItemCompra(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val descripcion: String,
    val cantidad: String = "1",
    val unidad: String = "",
    val categoria: String = "",
    val subcategoria: String = "",
    val precio: Double = 0.0,
    val comprado: Boolean = false,
    val agregadoPor: String = "",
    val idPagador: String = "", // ID del padre que pagó/pagará
    val fechaCompleta: Long = System.currentTimeMillis(),
    val esPrivado: Boolean = false,
    val idPropietario: String = ""
)

@Entity(tableName = "categorias_compra")
data class CategoriaCompra(
    @PrimaryKey val nombre: String,
    val subcategorias: String = "" // pipe-separated: "Leche|Pan|Huevos"
)

@Entity(tableName = "categorias_gasto")
data class CategoriaGasto(
    @PrimaryKey val nombre: String,
    val emoji: String = "",
    val orden: Int = 0
)

@Entity(tableName = "documentos")
data class Documento(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String = "",
    val categoria: String = "",
    val contenidoEncriptado: String = "",
    val iv: String = "",
    val rutaImagen: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaModificacion: Long = System.currentTimeMillis()
)

@Entity(tableName = "mensajes")
data class Mensaje(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idEmisor: String,
    val nombreEmisor: String,
    val texto: String,
    val fechaCompleta: Long = System.currentTimeMillis(),
    val leido: Boolean = false
)

@Entity(tableName = "configuracion_integracion")
data class ConfiguracionIntegracion(
    @PrimaryKey val id: Int = 1,
    // Notificaciones push por tipo
    val notifEventos: Boolean = true,
    val notifGastos: Boolean = true,
    val notifCompensaciones: Boolean = true,
    val notifCompras: Boolean = true,
    // Reminders custodia (Tier 2 #10)
    val notifCustodia: Boolean = true,
    // Modo frozen: días después de los cuales registros pasados quedan bloqueados (0 = desactivado)
    val frozenDias: Int = 0,
    // Moneda familiar (ISO 4217: ARS, MXN, CLP, COP, PEN, USD, EUR, etc.)
    val moneda: String = "ARS"
)

@Entity(
    tableName = "pendientes",
    indices = [Index("completado"), Index("fechaLimite")]
)
data class Pendiente(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val completado: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaLimite: String = "", // YYYY-MM-DD opcional
    val asignadoA: String = "",   // nombre del padre
    // Recurrencia: 0 = no recurrente, 1 = diario, 7 = semanal, 30 = mensual
    val frecuenciaDias: Int = 0,
    val fechaCompletado: Long = 0L  // timestamp ms, para saber cuándo reactivar
)

/**
 * Plantilla de custodia que genera registros de tiempo en lote.
 * Patrones soportados:
 *  "223"        — 2-2-3 (clásico de coparenting)
 *  "5050"       — semana on / semana off
 *  "5225"       — 5-2-2-5
 *  "2255"       — 2-2-5-5
 *  "wknd_alt"   — fines de semana alternos (Lun-Vie A; Sáb-Dom alterna)
 */
@Entity(tableName = "custody_schedules")
data class CustodySchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val patron: String,
    val fechaInicio: String,        // YYYY-MM-DD del Día 0 del ciclo
    val mesesGenerados: Int = 6,
    val idPadreA: String,
    val idPadreB: String,
    val idsHijos: String = "",      // CSV de IDs hijos. "" = todos.
    val horaInicio: String = "00:00",
    val horaFin: String = "23:59",
    val fechaCreacion: Long = System.currentTimeMillis()
)

// Modelo para solicitudes de vinculación entre familias (no persistido en Room)
data class SolicitudVinculacion(
    val id: String = UUID.randomUUID().toString(),
    val deFamilyId: String = "",
    val deNombre: String = "",
    val paraFamilyId: String = "",
    val paraNombre: String = "",
    val modoSync: String = "mis_datos",
    val estado: String = "pendiente" // pendiente, aceptada, rechazada
)
