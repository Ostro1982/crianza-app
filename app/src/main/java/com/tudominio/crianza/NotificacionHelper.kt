package com.tudominio.crianza

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centraliza la creación de canales y el envío de notificaciones push.
 * Las notificaciones se muestran cuando el otro integrante agrega datos
 * vía Telegram, WhatsApp o Email.
 */
object NotificacionHelper {

    const val CANAL_ID = "crianza_notificaciones"
    private const val CANAL_NOMBRE = "Crianza — Actividad familiar"
    private const val CANAL_DESC = "Notificaciones de eventos, gastos y compras agregados por el otro integrante"

    private val contadorId = AtomicInteger(1000)

    /**
     * Crea el canal de notificaciones. Llamar en MainActivity.onCreate().
     * En Android < O (API 26) no hace nada.
     */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CANAL_DESC
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(canal)
        }
    }

    /**
     * Envía una notificación push. Verifica el permiso en Android 13+.
     */
    fun notificar(context: Context, titulo: String, cuerpo: String) {
        // Verificar permiso POST_NOTIFICATIONS (requerido desde Android 13 / API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        // Intent para abrir la app al tocar la notificación
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(contadorId.getAndIncrement(), notif)
    }

    // ── Wrappers por tipo ──────────────────────────────────────────────────────

    fun notificarEvento(context: Context, titulo: String, fecha: String) =
        notificar(context, "📅 Nuevo evento: $titulo", "Fecha: $fecha")

    fun notificarGasto(context: Context, descripcion: String, monto: Double) =
        notificar(context, "💳 Nuevo gasto registrado", "$descripcion — $${"%.2f".format(monto)}")

    fun notificarCompensacion(context: Context, monto: Double) =
        notificar(context, "⚖️ Nueva compensación", "Se ha registrado una compensación de $${"%.2f".format(monto)}")

    fun notificarItemCompra(context: Context, descripcion: String) =
        notificar(context, "🛒 Nuevo ítem en compras", descripcion)

    fun notificarActualizacion(context: Context, tagName: String) =
        notificar(context, "🔄 Actualización disponible", "Nueva versión $tagName lista para instalar")
}
