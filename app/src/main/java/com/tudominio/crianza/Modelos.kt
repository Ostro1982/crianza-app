package com.tudominio.crianza

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "registros_tiempo")
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
    val esTodosLosHijos: Boolean = false
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

@Entity(tableName = "eventos")
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

@Entity(tableName = "gastos")
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
    val fechaCompleta: Long = System.currentTimeMillis()
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
    val aceptadoPadre2: Boolean = false
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
    val telegramBotToken: String = "",
    val telegramChatIdPadre1: String = "",
    val telegramChatIdPadre2: String = "",
    val emailHost: String = "imap.gmail.com",
    val emailPort: Int = 993,
    val emailUser: String = "",
    val emailPassword: String = "",
    val habilitarTelegram: Boolean = false,
    val habilitarEmail: Boolean = false,
    val ultimoUpdateIdTelegram: Long = 0L,
    val ultimaRevisionEmail: Long = 0L,
    // WhatsApp
    val whatsappTelefonoPadre1: String = "",
    val whatsappTelefonoPadre2: String = "",
    val habilitarWhatsApp: Boolean = false,
    // Nombres de grupos escolares separados por coma
    // Los mensajes de texto libre de estos grupos se analizan para detectar eventos
    val whatsappGruposEscuela: String = "",
    // Notificaciones push por tipo
    val notifEventos: Boolean = true,
    val notifGastos: Boolean = true,
    val notifCompensaciones: Boolean = true,
    val notifCompras: Boolean = true
)

@Entity(tableName = "filtros_email")
data class FiltroEmail(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tipo: String, // "remitente" o "asunto"
    val valor: String,
    val activo: Boolean = true
)
