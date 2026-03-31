package com.tudominio.crianza

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Escucha notificaciones de WhatsApp y procesa comandos y mensajes de grupos escolares.
 *
 * SETUP REQUERIDO:
 * 1. El usuario debe habilitar el acceso a notificaciones:
 *    Ajustes → Notificaciones → Acceso a notificaciones → Crianza
 * 2. Configurar los números de WhatsApp de cada padre en Ajustes → Integración.
 * 3. Opcional: configurar nombres de grupos escolares para detección automática de eventos.
 *
 * LIMITACIÓN: Solo puede leer el texto que aparece en la notificación.
 * Para mensajes con fotos/imágenes, WhatsApp muestra "📷 Imagen" sin contenido real.
 * En ese caso se crea un recordatorio para revisar manualmente.
 *
 * DOS MODOS:
 * - Comandos explícitos (/gasto, /tiempo, etc.): funcionan desde cualquier chat
 * - Grupos escolares: mensajes de texto libre son analizados para detectar eventos
 */
class WhatsAppListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val WHATSAPP_PKG = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PKG = "com.whatsapp.w4b"

        // Patrones de notificación de foto en WhatsApp
        private val PATRONES_FOTO = setOf(
            "📷", "📷 foto", "📷 imagen", "📸", "📸 foto",
            "foto", "imagen", "image", "photo", "sticker", "gif",
            "📷 sticker", "📷 gif"
        )

        /**
         * Abre WhatsApp con un mensaje pre-cargado al número dado.
         * El número debe estar en formato internacional sin +: "5491112345678"
         */
        fun enviarMensaje(context: Context, telefono: String, texto: String) {
            val url = "https://wa.me/$telefono?text=${Uri.encode(texto)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("WhatsApp", "WhatsApp no instalado", e)
            }
        }

        /**
         * Verifica si el acceso a notificaciones está habilitado.
         */
        fun accesoHabilitado(context: Context): Boolean {
            val habilitados = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return habilitados.contains(context.packageName)
        }

        /**
         * Abre la pantalla de configuración de acceso a notificaciones.
         */
        fun abrirConfiguracion(context: Context) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Palabras clave que activan comandos sin necesidad del prefijo /.
     * Se mapean al comando /equivalente para reutilizar parsearComando().
     */
    private val ALIAS_COMANDOS = mapOf(
        "gasto" to "/gasto",
        "gasté" to "/gasto",
        "gaste" to "/gasto",
        "compra" to "/compra",
        "comprar" to "/compra",
        "calendario" to "/evento",
        "pendiente" to "/pendiente"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg != WHATSAPP_PKG && pkg != WHATSAPP_BUSINESS_PKG) return

        val extras = sbn.notification.extras ?: return
        val texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: return
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return

        // ── Normalizar: detectar comandos con o sin / ─────────────────────────
        val textoComando = if (texto.startsWith("/")) {
            texto
        } else {
            val primeraPalabra = texto.split("\\s+".toRegex()).firstOrNull()?.lowercase() ?: return
            val prefijo = ALIAS_COMANDOS[primeraPalabra] ?: return
            // Reemplazar la primera palabra por el comando con /
            prefijo + texto.substring(primeraPalabra.length)
        }

        Log.d("WhatsApp", "Comando de $titulo: $textoComando")
        scope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val config = db.configuracionIntegracionDao().obtener() ?: return@launch
                if (!config.habilitarWhatsApp) return@launch
                val padres = db.familiaDao().obtenerTodosLosPadres()
                val hijos = db.familiaDao().obtenerTodosLosHijos()
                if (padres.isEmpty()) return@launch

                val idPadre = identificarPadre(titulo, padres, config)
                val syncManager = SyncManager(applicationContext, db)
                val mensajeFictico = TelegramService.MensajeParsado(
                    chatId = "", updateId = 0L, texto = textoComando, idPadre = idPadre
                )
                val comando = TelegramService(config).parsearComando(mensajeFictico) ?: return@launch
                val respuesta = ProcesadorComandos.procesar(comando, padres, hijos, db, config, syncManager)
                Log.d("WhatsApp", "Respuesta: $respuesta")

                // Notificación local con el resultado
                NotificacionHelper.notificarComandoWhatsApp(applicationContext, respuesta)
            } catch (e: Exception) {
                Log.e("WhatsApp", "Error procesando comando", e)
            }
        }

        // Si el texto original no era un comando, continuar con detección de grupos
        if (texto.startsWith("/") || ALIAS_COMANDOS.containsKey(
                texto.split("\\s+".toRegex()).firstOrNull()?.lowercase() ?: ""
            )) return

        // ── Path 2: grupos escolares — detección automática de eventos ─────────
        scope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val config = db.configuracionIntegracionDao().obtener() ?: return@launch
                if (!config.habilitarWhatsApp) return@launch
                if (config.whatsappGruposEscuela.isBlank()) return@launch

                val grupos = config.whatsappGruposEscuela
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                // Verificar si la notificación viene de un grupo escolar configurado
                val grupoCoincide = grupos.any { grupo ->
                    titulo.equals(grupo, ignoreCase = true) ||
                    titulo.contains(grupo, ignoreCase = true)
                }
                if (!grupoCoincide) return@launch

                val hoy = obtenerFechaActual()  // de Utils.kt — compatible minSdk 24

                // Detectar notificaciones de foto: crear recordatorio para revisar manualmente
                val esNotificacionFoto = texto.lowercase() in PATRONES_FOTO ||
                    (texto.length <= 15 && texto.trimStart().startsWith("📷"))

                val comandoTexto = if (esNotificacionFoto) {
                    Log.d("WhatsApp", "Foto en grupo '$titulo' — creando recordatorio")
                    "/evento Revisar foto en $titulo $hoy"
                } else {
                    DetectorEventosNaturales.extraer(texto) ?: return@launch
                }

                // Extraer título y fecha del comando para dedup
                val partes = comandoTexto.removePrefix("/evento ").split(" ")
                val fechaIdx = partes.indexOfFirst { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }
                if (fechaIdx < 0) return@launch
                val eventoTitulo = partes.take(fechaIdx).joinToString(" ")
                val eventoFecha = partes[fechaIdx]

                if (db.eventoDao().contarDuplicado(eventoTitulo, eventoFecha) > 0) {
                    Log.d("WhatsApp", "Evento duplicado ignorado: $eventoTitulo ($eventoFecha)")
                    return@launch
                }

                val mensajeFictico = TelegramService.MensajeParsado(
                    chatId = "", updateId = 0L, texto = comandoTexto, idPadre = "padre1"
                )
                val comando = TelegramService(config).parsearComando(mensajeFictico) ?: return@launch
                val padres = db.familiaDao().obtenerTodosLosPadres()
                val hijos = db.familiaDao().obtenerTodosLosHijos()
                ProcesadorComandos.procesar(comando, padres, hijos, db, config)
                Log.d("WhatsApp", "Evento escolar creado desde grupo '$titulo': $eventoTitulo ($eventoFecha)")

                // Notificación push de evento escolar
                if (config.notifEventos) {
                    NotificacionHelper.notificarEvento(applicationContext, eventoTitulo, eventoFecha)
                }

            } catch (e: Exception) {
                Log.e("WhatsApp", "Error procesando grupo escolar", e)
            }
        }
    }

    /**
     * Identifica qué padre envió el mensaje según el nombre del contacto o teléfono configurado.
     */
    private fun identificarPadre(
        remitente: String,
        padres: List<Padre>,
        config: ConfiguracionIntegracion
    ): String = when {
        remitente.contains(padres[0].nombre, ignoreCase = true) ||
        (config.whatsappTelefonoPadre1.isNotEmpty() && remitente.contains(config.whatsappTelefonoPadre1)) -> "padre1"
        padres.size > 1 && (
            remitente.contains(padres[1].nombre, ignoreCase = true) ||
            (config.whatsappTelefonoPadre2.isNotEmpty() && remitente.contains(config.whatsappTelefonoPadre2))
        ) -> "padre2"
        else -> "padre1"
    }
}
