@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tudominio.crianza.widget.SemillappWidget
import androidx.glance.appwidget.updateAll

class MainActivity : FragmentActivity() {

    // Launcher para el permiso POST_NOTIFICATIONS (Android 13+)
    private val permNotifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted o denied — NotificacionHelper verifica el estado en cada envío */ }

    // Receiver que lanza el instalador cuando termina la descarga del auto-update
    private val downloadReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != -1L && id == ActualizacionChecker.ultimoDownloadId) {
                ActualizacionChecker.instalar(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capturar texto compartido desde otra app (WhatsApp, notas, etc)
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(android.content.Intent.EXTRA_TEXT)?.let { texto ->
                getSharedPreferences("crianza_prefs", MODE_PRIVATE).edit()
                    .putString("pending_share_text", texto)
                    .putLong("pending_share_ts", System.currentTimeMillis())
                    .apply()
            }
        }

        // Canal de notificaciones push
        NotificacionHelper.crearCanal(this)

        // Pedir permiso de notificaciones (solo Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Iniciar worker de recordatorios (eventos del día, pendientes vencidos)
        RecordatoriosWorker.iniciar(this)

        // Worker resumen semanal (domingo 20hs)
        ResumenSemanalWorker.iniciar(this)

        // Receiver para detectar descarga completada del auto-update
        val filter = android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(downloadReceiver, filter)
        }

        setContent {
            CrianzaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var desbloqueado by remember {
                        mutableStateOf(!AppLock.estaHabilitado(this@MainActivity))
                    }
                    if (desbloqueado) {
                        NavegacionApp()
                    } else {
                        PantallaBloqueo(
                            onAutenticar = {
                                AppLock.prompt(
                                    activity = this@MainActivity,
                                    onSuccess = { desbloqueado = true },
                                    onFailure = { }
                                )
                            }
                        )
                        LaunchedEffect(Unit) {
                            AppLock.prompt(
                                activity = this@MainActivity,
                                onSuccess = { desbloqueado = true },
                                onFailure = { }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(downloadReceiver) } catch (_: Exception) {}
    }
}

@Composable
fun NavegacionApp() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val syncManager = remember { SyncManager(context, db) }

    val scope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE) }
    var pantallaActual by rememberSaveable { mutableStateOf("cargando") }
    var modoSeleccionado by remember { mutableStateOf(prefs.getString("modo", "") ?: "") }

    var padres by remember { mutableStateOf(listOf<Padre>()) }
    var hijos by remember { mutableStateOf(listOf<Hijo>()) }
    var idPadreActual by remember { mutableStateOf(prefs.getString("padre_actual_id", "") ?: "") }
    var padreActualFijado by remember { mutableStateOf(prefs.getBoolean("padre_actual_fijado", false)) }
    var registrosTiempo by remember { mutableStateOf(listOf<RegistroTiempo>()) }
    var configuracionTiempo by remember { mutableStateOf(ConfiguracionTiempo()) }
    var eventos by remember { mutableStateOf(listOf<Evento>()) }
    var gastos by remember { mutableStateOf(listOf<Gasto>()) }
    var compensaciones by remember { mutableStateOf(listOf<Compensacion>()) }
    var recuerdos by remember { mutableStateOf(listOf<Recuerdo>()) }
    var configuracionIntegracion by remember { mutableStateOf(ConfiguracionIntegracion()) }
    var filtrosEmail by remember { mutableStateOf(listOf<FiltroEmail>()) }
    var itemsCompra by remember { mutableStateOf(listOf<ItemCompra>()) }
    var documentos by remember { mutableStateOf(listOf<Documento>()) }
    var mensajes by remember { mutableStateOf(listOf<Mensaje>()) }
    var pendientesLista by remember { mutableStateOf(listOf<Pendiente>()) }
    var categoriasCompra by remember { mutableStateOf(listOf<CategoriaCompra>()) }
    var edicionesRegistros by remember { mutableStateOf(mapOf<String, List<RegistroEdicion>>()) }
    val googleAuth = remember { GoogleAuthHelper(context) }
    var usuarioGoogle by remember { mutableStateOf(googleAuth.obtenerUsuarioActual()) }
    // Código de familia único (generado una vez, basado en IDs de padres)
    val codigoFamiliar by remember { mutableStateOf(FamilyIdManager.obtenerFamilyId(context)) }
    // Info de actualización disponible (no null = hay versión nueva)
    var actualizacionInfo by remember { mutableStateOf<ActualizacionChecker.ActualizacionInfo?>(null) }

    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        scope.launch {
            padres = db.familiaDao().obtenerTodosLosPadres()
            hijos = db.familiaDao().obtenerTodosLosHijos()
            registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
            val configExistente = db.configuracionDao().obtenerConfiguracion()
            if (configExistente == null) {
                db.configuracionDao().insertarConfiguracion(ConfiguracionTiempo())
                configuracionTiempo = ConfiguracionTiempo()
            } else {
                configuracionTiempo = configExistente
            }
            eventos = db.eventoDao().obtenerTodosLosEventos()
            gastos = db.gastoDao().obtenerTodosLosGastos()
            compensaciones = db.compensacionDao().obtenerTodasLasCompensaciones()
            recuerdos = db.recuerdoDao().obtenerTodosLosRecuerdos()
            configuracionIntegracion = db.configuracionIntegracionDao().obtener() ?: ConfiguracionIntegracion()
            filtrosEmail = db.filtroEmailDao().obtenerTodos()
            itemsCompra = db.itemCompraDao().obtenerTodos()
            documentos = db.documentoDao().obtenerTodos()
            mensajes = db.mensajeDao().obtenerTodos()
            pendientesLista = db.pendienteDao().obtenerTodos()
            categoriasCompra = db.categoriaCompraDao().obtenerTodas()
            edicionesRegistros = db.registroEdicionDao().obtenerTodos()
                .groupBy { it.idRegistro }

            if (configuracionIntegracion.habilitarTelegram || configuracionIntegracion.habilitarEmail) {
                SincronizacionWorker.iniciar(context)
            }

            // Auto-seleccionar: si el ID guardado no existe en la familia actual, resetear
            if (padres.isNotEmpty()) {
                val idValido = padres.any { it.id == idPadreActual }
                if (!idValido) {
                    idPadreActual = padres.first().id
                    padreActualFijado = false
                    prefs.edit().remove("padre_actual_id").putBoolean("padre_actual_fijado", false).apply()
                }
            }

            // Solo redirigir si todavía está en "cargando" (primera carga, no rotación)
            if (pantallaActual == "cargando") {
                pantallaActual = if (padres.isNotEmpty()) "principal" else "seleccionModo"
            }

            // Iniciar sincronización Firestore (después de setear pantalla)
            syncManager.iniciarListeners(
                onEventosActualizados        = { scope.launch { eventos         = db.eventoDao().obtenerTodosLosEventos() } },
                onGastosActualizados         = { scope.launch { gastos          = db.gastoDao().obtenerTodosLosGastos() } },
                onItemsActualizados          = { scope.launch { itemsCompra     = db.itemCompraDao().obtenerTodos() } },
                onMensajesActualizados       = { scope.launch { mensajes        = db.mensajeDao().obtenerTodos() } },
                onRegistrosActualizados      = { scope.launch { registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros() } },
                onCompensacionesActualizadas = { scope.launch { compensaciones  = db.compensacionDao().obtenerTodasLasCompensaciones() } },
                onPendientesActualizados     = { scope.launch { pendientesLista = db.pendienteDao().obtenerTodos() } },
                onEdicionesActualizadas      = { scope.launch { edicionesRegistros = db.registroEdicionDao().obtenerTodos().groupBy { it.idRegistro } } }
            )

            // Subir datos locales a Firestore si es la primera vez
            scope.launch { syncManager.subirDatosLocalesIfNeeded() }

            // Chequear actualizaciones en background
            scope.launch {
                val info = withContext(Dispatchers.IO) { ActualizacionChecker.verificar() }
                if (info != null) actualizacionInfo = info
            }
        }
    }

    // Diálogo de actualización disponible
    actualizacionInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { actualizacionInfo = null },
            title = { Text("Nueva versión disponible") },
            text = { Text("Versión ${info.tagName} lista para instalar. ¿Descargar ahora?") },
            confirmButton = {
                TextButton(onClick = {
                    ActualizacionChecker.descargar(context, info)
                    actualizacionInfo = null
                }) { Text("Descargar") }
            },
            dismissButton = {
                TextButton(onClick = { actualizacionInfo = null }) { Text("Ahora no") }
            }
        )
    }

    // ── Back handler: en principal no salir, en sub-pantallas volver a principal ──
    BackHandler(enabled = true) {
        when (pantallaActual) {
            "principal" -> { /* No hacer nada — no salir de la app */ }
            "cargando", "seleccionModo" -> { /* No salir */ }
            else -> pantallaActual = "principal"
        }
    }

    when (pantallaActual) {
        "cargando" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        "seleccionModo" -> PantallaSeleccionModo(
            padresExisten = padres.isNotEmpty(),
            onContinuar = { pantallaActual = "principal" },
            onModoSeleccionado = { modo ->
                modoSeleccionado = modo
                prefs.edit().putString("modo", modo).apply()
                pantallaActual = "registroFamilia"
            },
            onGoogle = { pantallaActual = "google" },
            onVincular = { pantallaActual = "vincular" }
        )
        "registroFamilia" -> PantallaRegistroFamilia(
            modo = modoSeleccionado,
            padresExistentes = padres,
            hijosExistentes = hijos,
            onRegistroCompleto = { adultosForm, hijosForm ->
                scope.launch {
                    db.familiaDao().eliminarTodosLosPadres()
                    db.familiaDao().eliminarTodosLosHijos()

                    val nuevosPadres = adultosForm.map { form ->
                        Padre(
                            id = java.util.UUID.randomUUID().toString(),
                            nombre = form.nombre,
                            telefono = form.telefono,
                            email = form.email,
                            rol = form.rol,
                            fechaNacimiento = form.fechaNacimiento
                        )
                    }
                    nuevosPadres.forEach { db.familiaDao().insertarPadre(it) }

                    val nuevosHijos = hijosForm.map { form ->
                        Hijo(
                            id = java.util.UUID.randomUUID().toString(),
                            nombre = form.nombre,
                            fechaNacimiento = form.fechaNacimiento
                        )
                    }
                    nuevosHijos.forEach { db.familiaDao().insertarHijo(it) }

                    padres = db.familiaDao().obtenerTodosLosPadres()
                    hijos = db.familiaDao().obtenerTodosLosHijos()
                    if (idPadreActual.isEmpty()) idPadreActual = padres.firstOrNull()?.id ?: ""

                    pantallaActual = "principal"
                }
            },
            onAtras = {
                pantallaActual = "seleccionModo"
            }
        )
        "principal" -> PantallaPrincipal(
            modo = modoSeleccionado,
            padres = padres,
            hijos = hijos,
            idPadreActual = idPadreActual,
            padreActualFijado = padreActualFijado,
            onCambiarPadreActual = { id ->
                idPadreActual = id
                padreActualFijado = true
                prefs.edit()
                    .putString("padre_actual_id", id)
                    .putBoolean("padre_actual_fijado", true)
                    .apply()
            },
            onTiempo = { pantallaActual = "tiempo" },
            onCalendario = { pantallaActual = "calendario" },
            onGastos = { pantallaActual = "gastos" },
            onCompensacion = { pantallaActual = "compensacion" },
            onRecuerdos = { pantallaActual = "recuerdos" },
            onListaCompras = { pantallaActual = "listaCompras" },
            onDocumentos = { pantallaActual = "documentos" },
            onMensajes = { pantallaActual = "mensajes" },
            onPendientes = { pantallaActual = "pendientes" },
            registrosTiempo = registrosTiempo,
            codigoFamiliar = codigoFamiliar,
            usuarioGoogle = usuarioGoogle,
            onIniciarCustodia = {
                scope.launch {
                    val padre = padres.find { it.id == idPadreActual } ?: return@launch
                    val hoy = obtenerFechaActual()
                    val ahora = obtenerHoraActual()
                    hijos.forEach { hijo ->
                        syncManager.insertarRegistro(RegistroTiempo(
                            idHijo = hijo.id, nombreHijo = hijo.nombre,
                            idPadre = padre.id, nombrePadre = padre.nombre,
                            fecha = hoy, horaInicio = ahora, horaFin = ""
                        ))
                    }
                    registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                }
            },
            onFinalizarCustodia = {
                scope.launch {
                    val hoy = obtenerFechaActual()
                    val ahora = obtenerHoraActual()
                    registrosTiempo.filter {
                        it.fecha == hoy && it.horaFin.isBlank() && it.idPadre == idPadreActual
                    }.forEach { registro ->
                        syncManager.actualizarRegistro(registro.copy(horaFin = ahora))
                    }
                    registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                }
            },
            onConfiguracion = { pantallaActual = "configuracion" },
            onAtras = { pantallaActual = "registroFamilia" },
            onEditarFamilia = { pantallaActual = "registroFamilia" },
            onGoogle = { pantallaActual = "google" },
            onVincular = { pantallaActual = "vincular" },
            onRevincular = {
                scope.launch {
                    FamilyIdManager.desvincular(context)
                    db.eventoDao().eliminarTodos()
                    db.gastoDao().eliminarTodos()
                    db.itemCompraDao().eliminarTodos()
                    db.mensajeDao().eliminarTodos()
                    db.registroTiempoDao().eliminarTodos()
                    db.compensacionDao().eliminarTodos()
                    db.pendienteDao().eliminarTodos()
                    pantallaActual = "vincular"
                }
            },
            onPlanificacion = { pantallaActual = "planificacion" }
        )
        "planificacion" -> {
            PantallaDiasFijos(
                padres = padres,
                hijos = hijos,
                onAgregarEventos = { nuevos, nuevosRegistros ->
                    scope.launch {
                        nuevos.forEach { syncManager.insertarEvento(it) }
                        db.registroTiempoDao().insertarRegistros(nuevosRegistros)
                        eventos = db.eventoDao().obtenerTodosLosEventos()
                        registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                    }
                },
                onEliminarEventosActividad = { ids ->
                    scope.launch {
                        db.eventoDao().eliminarPorIds(ids)
                        eventos = db.eventoDao().obtenerTodosLosEventos()
                    }
                },
                onAtras = {
                    scope.launch { syncManager.subirPlanificacion() }
                    pantallaActual = "principal"
                }
            )
        }
        "tiempo" -> PantallaTiempo(
            hijos = hijos,
            padres = padres,
            registros = registrosTiempo,
            configuracion = configuracionTiempo,
            onAgregarRegistro = { nuevoRegistro ->
                scope.launch {
                    syncManager.insertarRegistro(nuevoRegistro)
                    registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                }
            },
            onAgregarMultiplesRegistros = { nuevosRegistros ->
                scope.launch {
                    nuevosRegistros.forEach { syncManager.insertarRegistro(it) }
                    registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                }
            },
            onEliminarRegistro = { id ->
                scope.launch {
                    val registro = registrosTiempo.find { it.id == id }
                    if (registro != null) {
                        syncManager.eliminarRegistro(registro)
                        registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                    }
                }
            },
            onEditarRegistro = { registroEditado ->
                scope.launch {
                    syncManager.actualizarRegistro(registroEditado)
                    registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                }
            },
            onVerHistorial = { pantallaActual = "historialTiempo" },
            onAtras = { pantallaActual = "principal" }
        )
        "historialTiempo" -> PantallaHistorialTiempo(
            hijos = hijos,
            padres = padres,
            registros = registrosTiempo,
            ediciones = edicionesRegistros,
            onEliminarRegistro = { id ->
                scope.launch {
                    val r = registrosTiempo.find { it.id == id }
                    if (r != null) {
                        db.registroEdicionDao().eliminarPorRegistro(id)
                        syncManager.eliminarRegistro(r)
                        registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                        edicionesRegistros = db.registroEdicionDao().obtenerTodos().groupBy { it.idRegistro }
                    }
                }
            },
            onEditarRegistro = { r ->
                scope.launch {
                    syncManager.actualizarRegistro(r)
                    registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros()
                    edicionesRegistros = db.registroEdicionDao().obtenerTodos().groupBy { it.idRegistro }
                }
            },
            onAtras = { pantallaActual = "tiempo" }
        )
        "resumenTiempo" -> PantallaResumenTiempo(
            hijos = hijos,
            padres = padres,
            registros = registrosTiempo,
            configuracion = configuracionTiempo,
            onGuardarConfiguracion = { nuevaConfig ->
                scope.launch {
                    db.configuracionDao().insertarConfiguracion(nuevaConfig)
                    configuracionTiempo = db.configuracionDao().obtenerConfiguracion() ?: nuevaConfig
                }
            },
            onAtras = {
                pantallaActual = "tiempo"
            }
        )
        "calendario" -> PantallaCalendario(
            eventos = eventos,
            padres = padres,
            onAgregarEvento = { nuevoEvento ->
                scope.launch {
                    syncManager.insertarEvento(nuevoEvento)
                    eventos = db.eventoDao().obtenerTodosLosEventos()
                    SemillappWidget().updateAll(context)
                }
            },
            onEliminarEvento = { id ->
                scope.launch {
                    val evento = eventos.find { it.id == id }
                    if (evento != null) {
                        syncManager.eliminarEvento(evento)
                        eventos = db.eventoDao().obtenerTodosLosEventos()
                        SemillappWidget().updateAll(context)
                    }
                }
            },
            onEliminarEventosBulk = { ids ->
                scope.launch {
                    db.eventoDao().eliminarPorIds(ids)
                    eventos = db.eventoDao().obtenerTodosLosEventos()
                    SemillappWidget().updateAll(context)
                }
            },
            onEditarEvento = { eventoEditado ->
                scope.launch {
                    syncManager.actualizarEvento(eventoEditado)
                    eventos = db.eventoDao().obtenerTodosLosEventos()
                    SemillappWidget().updateAll(context)
                }
            },
            onAtras = {
                pantallaActual = "principal"
            }
        )
        "gastos" -> PantallaGastos(
            gastos = gastos,
            hijos = hijos,
            padres = padres,
            idPadreActual = idPadreActual,
            onAgregarGasto = { nuevoGasto ->
                scope.launch {
                    syncManager.insertarGasto(nuevoGasto)
                    gastos = db.gastoDao().obtenerTodosLosGastos()
                    SemillappWidget().updateAll(context)
                }
            },
            onEliminarGasto = { id ->
                scope.launch {
                    val gasto = gastos.find { it.id == id }
                    if (gasto != null) {
                        syncManager.eliminarGasto(gasto)
                        gastos = db.gastoDao().obtenerTodosLosGastos()
                        SemillappWidget().updateAll(context)
                    }
                }
            },
            onEditarGasto = { gastoEditado ->
                scope.launch {
                    syncManager.actualizarGasto(gastoEditado)
                    gastos = db.gastoDao().obtenerTodosLosGastos()
                    SemillappWidget().updateAll(context)
                }
            },
            onAtras = {
                pantallaActual = "principal"
            }
        )
        "compensacion" -> PantallaCompensacion(
            padres = padres,
            registros = registrosTiempo,
            configuracionTiempo = configuracionTiempo,
            compensaciones = compensaciones,
            gastos = gastos,
            itemsCompra = itemsCompra,
            onRegistrarCompensacion = { nuevaCompensacion ->
                scope.launch {
                    syncManager.insertarCompensacion(nuevaCompensacion)
                    compensaciones = db.compensacionDao().obtenerTodasLasCompensaciones()
                }
            },
            onEliminarCompensacion = { id ->
                scope.launch {
                    val comp = compensaciones.find { it.id == id }
                    if (comp != null) {
                        syncManager.eliminarCompensacion(comp)
                        compensaciones = db.compensacionDao().obtenerTodasLasCompensaciones()
                    }
                }
            },
            onEditarCompensacion = { compEditada ->
                scope.launch {
                    if (compEditada.confirmada) {
                        syncManager.eliminarCompensacion(compEditada)
                    } else {
                        syncManager.actualizarCompensacion(compEditada)
                    }
                    compensaciones = db.compensacionDao().obtenerTodasLasCompensaciones()
                }
            },
            idPadreActual = idPadreActual,
            onGuardarConfiguracion = { nuevaConfig ->
                scope.launch {
                    db.configuracionDao().insertarConfiguracion(nuevaConfig)
                    configuracionTiempo = nuevaConfig
                }
            },
            onAtras = { pantallaActual = "principal" }
        )
        "listaCompras" -> PantallaListaCompras(
            items = itemsCompra,
            padres = padres,
            idPadreActual = idPadreActual,
            categoriasPersonalizadas = categoriasCompra,
            onAgregar = { item -> scope.launch { syncManager.insertarItem(item); itemsCompra = db.itemCompraDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onActualizar = { item -> scope.launch { syncManager.actualizarItem(item); itemsCompra = db.itemCompraDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onEliminar = { item -> scope.launch { syncManager.eliminarItem(item); itemsCompra = db.itemCompraDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onEliminarComprados = { scope.launch { db.itemCompraDao().eliminarComprados(); itemsCompra = db.itemCompraDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onAgregarCategoria = { cat -> scope.launch { db.categoriaCompraDao().insertar(cat); categoriasCompra = db.categoriaCompraDao().obtenerTodas() } },
            onAtras = { pantallaActual = "principal" }
        )
        "documentos" -> PantallaDocumentos(
            documentos = documentos,
            onAgregar = { doc -> scope.launch { db.documentoDao().insertar(doc); documentos = db.documentoDao().obtenerTodos() } },
            onActualizar = { doc -> scope.launch { db.documentoDao().actualizar(doc); documentos = db.documentoDao().obtenerTodos() } },
            onEliminar = { doc -> scope.launch { db.documentoDao().eliminar(doc); documentos = db.documentoDao().obtenerTodos() } },
            onAtras = { pantallaActual = "principal" }
        )
        "mensajes" -> {
            LaunchedEffect(Unit) {
                db.mensajeDao().marcarTodosLeidos()
            }
            PantallaMensajes(
                mensajes = mensajes,
                padres = padres,
                idPadreActual = idPadreActual,
                configuracion = configuracionIntegracion,
                onEnviar = { msg -> scope.launch { syncManager.insertarMensaje(msg); mensajes = db.mensajeDao().obtenerTodos() } },
                onAtras = { pantallaActual = "principal" }
            )
        }
        "pendientes" -> PantallaPendientes(
            pendientes = pendientesLista,
            padres = padres,
            onAgregar = { p -> scope.launch { syncManager.insertarPendiente(p); pendientesLista = db.pendienteDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onActualizar = { p -> scope.launch { syncManager.actualizarPendiente(p); pendientesLista = db.pendienteDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onEliminar = { p -> scope.launch { syncManager.eliminarPendiente(p); pendientesLista = db.pendienteDao().obtenerTodos(); SemillappWidget().updateAll(context) } },
            onAtras = { pantallaActual = "principal" }
        )
        "configuracion" -> PantallaConfiguracion(
            config = configuracionIntegracion,
            filtros = filtrosEmail,
            padres = padres,
            onGuardarConfig = { nueva ->
                scope.launch {
                    db.configuracionIntegracionDao().guardar(nueva)
                    configuracionIntegracion = nueva
                    if (nueva.habilitarTelegram || nueva.habilitarEmail) {
                        SincronizacionWorker.iniciar(context)
                    } else {
                        SincronizacionWorker.detener(context)
                    }
                }
            },
            onAgregarFiltro = { filtro ->
                scope.launch {
                    db.filtroEmailDao().insertar(filtro)
                    filtrosEmail = db.filtroEmailDao().obtenerTodos()
                }
            },
            onEliminarFiltro = { filtro ->
                scope.launch {
                    db.filtroEmailDao().eliminar(filtro)
                    filtrosEmail = db.filtroEmailDao().obtenerTodos()
                }
            },
            onAtras = { pantallaActual = "principal" }
        )
        "vincular", "google" -> {
            // Al abrir vincular: subir planificación y registrar emails de padres
            LaunchedEffect(Unit) {
                syncManager.subirPlanificacion()
                syncManager.registrarEmailsPadres(padres)
            }
            PantallaCuentaVincular(
            usuarioActual = usuarioGoogle,
            codigoFamiliar = codigoFamiliar,
            onIniciarSesion = { user ->
                usuarioGoogle = user
                syncManager.registrarUsuarioGoogle(user)
            },
            onCerrarSesion = { usuarioGoogle = null },
            onVinculado = {
                syncManager.reiniciarListeners()
                scope.launch { syncManager.subirDatosLocalesIfNeeded() }
                pantallaActual = "principal"
            },
            onAtras = { pantallaActual = "principal" },
            onBuscarEmail = { email -> syncManager.buscarUsuarioPorEmail(email) }
        )
        }
        "recuerdos" -> PantallaRecuerdos(
            recuerdos = recuerdos,
            onAgregarRecuerdo = { nuevoRecuerdo ->
                scope.launch {
                    db.recuerdoDao().insertarRecuerdo(nuevoRecuerdo)
                    recuerdos = db.recuerdoDao().obtenerTodosLosRecuerdos()
                }
            },
            onEliminarRecuerdo = { id ->
                scope.launch {
                    val recuerdo = recuerdos.find { it.id == id }
                    if (recuerdo != null) {
                        db.recuerdoDao().eliminarRecuerdo(recuerdo)
                        recuerdos = db.recuerdoDao().obtenerTodosLosRecuerdos()
                    }
                }
            },
            onEditarRecuerdo = { recuerdoEditado ->
                scope.launch {
                    db.recuerdoDao().actualizarRecuerdo(recuerdoEditado)
                    recuerdos = db.recuerdoDao().obtenerTodosLosRecuerdos()
                }
            },
            onAtras = {
                pantallaActual = "principal"
            }
        )
    }
}

@Composable
fun PantallaSeleccionModo(
    padresExisten: Boolean = false,
    onContinuar: () -> Unit = {},
    onModoSeleccionado: (String) -> Unit,
    onGoogle: () -> Unit = {},
    onVincular: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // ── Hero ──────────────────────────────────────────────────────────
            Text("👨‍👩‍👧‍👦", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Crianza",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = Neutral10,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                "Compartida",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = NeutralVariant30,
                    fontWeight = FontWeight.Light
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Tu familia. Tu acuerdo.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = NeutralVariant50
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Botón continuar (solo si ya hay familia registrada) ────────────
            if (padresExisten) {
                Button(
                    onClick = onContinuar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo40
                    )
                ) {
                    Text(
                        "\u25B6  Continuar con mi familia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "\u2500  o crear nueva  \u2500",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeutralVariant50
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Pregunta ──────────────────────────────────────────────────────
            Text(
                "¿Cómo es tu familia?",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Neutral10,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ── Opción 1 ──────────────────────────────────────────────────────
            OpcionModoCard(
                emoji = "🏡",
                titulo = "Vivimos juntos",
                subtitulo = "Coordinamos la crianza bajo el mismo techo",
                onClick = { onModoSeleccionado("juntos") }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Opción 2 ──────────────────────────────────────────────────────
            OpcionModoCard(
                emoji = "🔄",
                titulo = "Vivimos separados",
                subtitulo = "Compartimos la crianza entre dos hogares",
                onClick = { onModoSeleccionado("separados") }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "Podés cambiar esto después en configuración",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NeutralVariant50
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Cuenta / Vincular ────────────────────────────────────────────
            Text(
                "¿Ya tenés cuenta?",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Neutral10
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onGoogle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeutralVariant50)
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onVincular,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeutralVariant50)
                ) {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vincular", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OpcionModoCard(emoji: String, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Indigo90,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Neutral10,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NeutralVariant50
                    )
                )
            }
            Text("\u203A", style = MaterialTheme.typography.titleLarge.copy(color = NeutralVariant50))
        }
    }
}

data class ItemMenuPrincipal(
    val titulo: String,
    val subtitulo: String,
    val icon: ImageVector,
    val colorIndex: Int,
    val onClick: () -> Unit
)

@Composable
fun PantallaPrincipal(
    modo: String,
    padres: List<Padre>,
    hijos: List<Hijo>,
    idPadreActual: String = "",
    padreActualFijado: Boolean = false,
    onCambiarPadreActual: (String) -> Unit = {},
    onTiempo: () -> Unit,
    onCalendario: () -> Unit,
    onGastos: () -> Unit,
    onCompensacion: () -> Unit,
    onRecuerdos: () -> Unit,
    onListaCompras: () -> Unit,
    onDocumentos: () -> Unit,
    onMensajes: () -> Unit,
    onConfiguracion: () -> Unit,
    onPendientes: () -> Unit = {},
    registrosTiempo: List<RegistroTiempo> = emptyList(),
    onIniciarCustodia: () -> Unit = {},
    onFinalizarCustodia: () -> Unit = {},
    codigoFamiliar: String = "",
    usuarioGoogle: UsuarioGoogle? = null,
    onAtras: () -> Unit,
    onEditarFamilia: () -> Unit,
    onGoogle: () -> Unit = {},
    onVincular: () -> Unit = {},
    onRevincular: () -> Unit = {},
    onPlanificacion: () -> Unit = {}
) {
    // ── Formato fecha y nombre ────────────────────────────────────────────────
    val sdfDisplay = remember { java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale("es")) }
    val fechaHoy = remember { sdfDisplay.format(java.util.Date()).replaceFirstChar { it.uppercase() } }
    val nombrePadre = padres.find { it.id == idPadreActual }?.nombre ?: "Usuario"

    // ── Estado dinámico del dashboard ──────────────────────────────────────────
    val context = LocalContext.current
    var eventosSemana by remember { mutableStateOf<Map<String, List<Evento>>>(emptyMap()) }
    var comprasPendientes by remember { mutableStateOf<List<ItemCompra>>(emptyList()) }
    var tareasPendientes by remember { mutableStateOf<List<Pendiente>>(emptyList()) }
    var totalTareas by remember { mutableStateOf(0) }
    var compensacionesPendientes by remember { mutableStateOf(0) }
    var balNetoCompensacion by remember { mutableStateOf(0.0) }
    var deudorNombre by remember { mutableStateOf("") }
    var acreedorNombre by remember { mutableStateOf("") }
    var mensajesNoLeidos by remember { mutableStateOf(0) }
    var ultimoMensaje by remember { mutableStateOf<Mensaje?>(null) }
    var gastosMes by remember { mutableStateOf<List<Gasto>>(emptyList()) }
    var totalGastosMes by remember { mutableStateOf(0.0) }

    // Custodia rápida: detectar si hay registros abiertos hoy para el padre actual
    val hoyStr = remember { obtenerFechaActual() }
    val registrosAbiertos = registrosTiempo.filter {
        it.fecha == hoyStr && it.horaFin.isBlank() && it.idPadre == idPadreActual
    }
    val custodiaActiva = registrosAbiertos.isNotEmpty()
    val custodiaDesde = registrosAbiertos.firstOrNull()?.horaInicio ?: ""

    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        val hoy = obtenerFechaActual()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        cal.time = sdf.parse(hoy)!!
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
        val finSemana = sdf.format(cal.time)

        val todos = withContext(Dispatchers.IO) { db.eventoDao().obtenerTodosLosEventos() }
        eventosSemana = todos
            .filter { it.fecha in hoy..finSemana }
            .groupBy { it.fecha }
            .toSortedMap()

        comprasPendientes = withContext(Dispatchers.IO) {
            db.itemCompraDao().obtenerCompartidos()
        }.filter { !it.comprado }

        val todasTareas = withContext(Dispatchers.IO) { db.pendienteDao().obtenerTodos() }
        totalTareas = todasTareas.size
        tareasPendientes = todasTareas.filter { !it.completado }

        val todasCompensaciones = withContext(Dispatchers.IO) {
            db.compensacionDao().obtenerTodasLasCompensaciones()
        }
        compensacionesPendientes = todasCompensaciones.count { !it.confirmada }

        mensajesNoLeidos = withContext(Dispatchers.IO) { db.mensajeDao().contarNoLeidos() }

        val mensajes = withContext(Dispatchers.IO) { db.mensajeDao().obtenerTodos() }
        ultimoMensaje = mensajes.maxByOrNull { it.fechaCompleta }

        val todosGastos = withContext(Dispatchers.IO) { db.gastoDao().obtenerTodosLosGastos() }
        val mesActual = hoy.substring(0, 7)
        gastosMes = todosGastos.filter { it.fecha.startsWith(mesActual) }.sortedByDescending { it.fechaCompleta }
        totalGastosMes = gastosMes.sumOf { it.monto }

        // Calcular balance neto de compensación (tiempo + gastos + compras - pagos)
        if (padres.size >= 2) {
            val p1 = padres[0]; val p2 = padres[1]
            val todosRegistros = withContext(Dispatchers.IO) { db.registroTiempoDao().obtenerTodosLosRegistros() }
            val registrosComp = todosRegistros.filter { !it.autocompensado }
            val config = withContext(Dispatchers.IO) { db.configuracionDao().obtenerConfiguracion() }
            val valorEf = when (config?.tipoValor) {
                "hora" -> config.valorHora
                "semana" -> config.valorHora / (7 * 24)
                else -> (config?.valorHora ?: 10.0) / 24
            }
            val horasPorPadre = calcularHorasPorPadre(registrosComp)
            val totalH = horasPorPadre.values.sum()
            val h1 = horasPorPadre[p1.id] ?: 0.0
            val pctR1 = if (totalH > 0) (h1 / totalH * 100).toInt() else 0
            val obj1 = config?.porcentajePadre1 ?: 50
            val dif1 = pctR1 - obj1
            val horasDeuda = if (totalH > 0) kotlin.math.abs(dif1) * totalH / 100 else 0.0
            val montoTiempo = horasDeuda * valorEf
            val timeBal = when { dif1 < 0 -> -montoTiempo; dif1 > 0 -> montoTiempo; else -> 0.0 }

            val gastosComp = todosGastos.filter { !it.autocompensado }
            val gP1 = gastosComp.filter { it.idPagador == p1.id }.sumOf { it.monto }
            val gP2 = gastosComp.filter { it.idPagador == p2.id }.sumOf { it.monto }
            val balG = (gP1 - gP2) / 2

            val pagadas = withContext(Dispatchers.IO) { db.itemCompraDao().obtenerCompartidos() }
                .filter { it.precio > 0 && it.comprado }
            val cP1 = pagadas.filter { it.idPagador == p1.id }.sumOf { it.precio }
            val cP2 = pagadas.filter { it.idPagador == p2.id }.sumOf { it.precio }
            val balC = (cP1 - cP2) / 2

            val balPend = todasCompensaciones.filter { !it.confirmada }.sumOf { c ->
                when (c.idPagador) { p1.id -> c.montoTotal; p2.id -> -c.montoTotal; else -> 0.0 }
            }

            balNetoCompensacion = balG + balC + timeBal - balPend
            val deudorP = if (balNetoCompensacion < 0) p1 else p2
            val acreedorP = if (balNetoCompensacion < 0) p2 else p1
            deudorNombre = deudorP.nombre
            acreedorNombre = acreedorP.nombre
        }
    }

    Box(Modifier.fillMaxSize().background(BgGradientMain)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(44.dp))

            // ── Header cálido ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hola, $nombrePadre",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = Neutral10)
                    Text(fechaHoy, style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onConfiguracion) {
                        Icon(Icons.Default.Settings, "Configuración", tint = NeutralVariant50)
                    }
                    var showUserMenu by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(Rose40).clickable { showUserMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nombrePadre.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cuenta Google") },
                                onClick = { showUserMenu = false; onGoogle() },
                                leadingIcon = { Icon(Icons.Default.Person, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Vincular dispositivo") },
                                onClick = { showUserMenu = false; onVincular() },
                                leadingIcon = { Icon(Icons.Default.Sync, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Editar familia") },
                                onClick = { showUserMenu = false; onEditarFamilia() },
                                leadingIcon = { Icon(Icons.Default.MenuBook, null) }
                            )
                            if (FamilyIdManager.estaVinculado(context)) {
                                DropdownMenuItem(
                                    text = { Text("Volver a vincular") },
                                    onClick = { showUserMenu = false; onRevincular() },
                                    leadingIcon = { Icon(Icons.Default.Sync, null) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Selector padre (solo si no está fijado aún) ──────────────────
            if (padres.size >= 2 && !padreActualFijado) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Quién sos?", style = MaterialTheme.typography.labelMedium, color = NeutralVariant50)
                    padres.forEach { padre ->
                        FilterChip(
                            selected = idPadreActual == padre.id,
                            onClick = { onCambiarPadreActual(padre.id) },
                            label = { Text(padre.nombre, fontWeight = if (idPadreActual == padre.id) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (idPadreActual == padre.id) {
                                { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo40.copy(alpha = 0.15f),
                                selectedLabelColor = Indigo30,
                                selectedLeadingIconColor = Indigo30,
                                containerColor = NeutralVariant80.copy(alpha = 0.3f),
                                labelColor = NeutralVariant30
                            )
                        )
                    }
                }
            }

            // ── Hero: Planificación semanal ───────────────────────────────────
            if (hijos.isNotEmpty() && idPadreActual.isNotEmpty() && modo != "juntos") {
                WidgetPlanificacionSemanal(padres = padres, onEditar = onPlanificacion)
            }

            // ── Clima ─────────────────────────────────────────────────────────
            ClimaCard()

            Spacer(Modifier.height(8.dp))

            // ── Mosaico de tarjetas glass ─────────────────────────────────
            // TODO: Permitir al usuario ocultar/mostrar tarjetas del dashboard
            //       (Inbox, Próximamente, Compras, Pendientes, Compensación, Finanzas)
            //       desde Configuración con toggles por módulo.────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Inbox (ancho completo) ────────────────────────────────────
                GlassCard(onClick = onMensajes) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("\uD83D\uDCAC", fontSize = 22.sp)
                            Text("Inbox", style = MaterialTheme.typography.labelLarge,
                                color = NeutralVariant50, fontWeight = FontWeight.SemiBold)
                        }
                        if (mensajesNoLeidos > 0) {
                            WarmBadge("$mensajesNoLeidos nuevo${if (mensajesNoLeidos > 1) "s" else ""}", isAlert = true)
                        }
                    }
                    ultimoMensaje?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildString {
                                append(msg.nombreEmisor); append(" \u2022 ")
                                append(msg.texto.take(60))
                                if (msg.texto.length > 60) append("\u2026")
                            },
                            style = MaterialTheme.typography.bodySmall, color = NeutralVariant30
                        )
                    }
                }

                // ── Próximamente + Compras ────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(onClick = onCalendario, modifier = Modifier.weight(1f).heightIn(min = 140.dp)) {
                        Text("\uD83D\uDCC5", fontSize = 22.sp)
                        Text("Próximamente", style = MaterialTheme.typography.labelLarge,
                            color = NeutralVariant50, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val sdfShort = java.text.SimpleDateFormat("EEE", java.util.Locale("es"))
                        if (eventosSemana.isEmpty()) {
                            Text("Sin eventos", style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                        } else {
                            eventosSemana.entries.take(3).forEach { (fecha, eventos) ->
                                val label = if (fecha == hoyStr) "HOY" else try {
                                    sdfShort.format(sdfIn.parse(fecha)!!).uppercase()
                                } catch (_: Exception) { fecha }
                                eventos.take(1).forEach { ev ->
                                    Text("$label ${ev.horaInicio ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold, color = Neutral10)
                                    Text(ev.titulo, style = MaterialTheme.typography.bodySmall,
                                        color = NeutralVariant30, lineHeight = 16.sp)
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                    GlassCard(onClick = onListaCompras, modifier = Modifier.weight(1f).heightIn(min = 140.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("\uD83D\uDED2", fontSize = 22.sp)
                            Text("Lista de compras", style = MaterialTheme.typography.labelLarge,
                                color = NeutralVariant50, fontWeight = FontWeight.SemiBold)
                            if (comprasPendientes.isNotEmpty()) WarmBadge("${comprasPendientes.size}")
                        }
                        Spacer(Modifier.height(4.dp))
                        if (comprasPendientes.isEmpty()) {
                            Text("Lista vacía", style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                        } else {
                            comprasPendientes.take(3).forEach { item ->
                                Text("\u2610 ${item.descripcion}",
                                    style = MaterialTheme.typography.bodySmall, color = Neutral10)
                            }
                        }
                    }
                }

                // ── Pendientes + Compensación ─────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(onClick = onPendientes, modifier = Modifier.weight(1f).heightIn(min = 140.dp)) {
                        Text("\u2611", fontSize = 22.sp)
                        Text("Pendientes", style = MaterialTheme.typography.labelLarge,
                            color = NeutralVariant50, fontWeight = FontWeight.SemiBold)
                        Text("${tareasPendientes.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Neutral10)
                        if (totalTareas > 0) {
                            Text("de $totalTareas", style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                        }
                        tareasPendientes.firstOrNull()?.let {
                            Text(it.titulo, style = MaterialTheme.typography.bodySmall, color = NeutralVariant30)
                        }
                    }
                    GlassCard(onClick = onCompensacion, modifier = Modifier.weight(1f).heightIn(min = 140.dp)) {
                        Text("\u2696", fontSize = 22.sp)
                        Text("Compensación", style = MaterialTheme.typography.labelLarge,
                            color = NeutralVariant50, fontWeight = FontWeight.SemiBold)
                        if (kotlin.math.abs(balNetoCompensacion) > 0.01) {
                            Text("$${String.format("%,.0f", kotlin.math.abs(balNetoCompensacion))}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Neutral10)
                            Text("$deudorNombre debe",
                                style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                        } else if (compensacionesPendientes > 0) {
                            Text("$compensacionesPendientes",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Neutral10)
                            Text("pendiente${if (compensacionesPendientes > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                        } else {
                            Text("Al día", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = Indigo40)
                        }
                    }
                }

                // ── Finanzas Mes (ancho completo) ─────────────────────────────
                GlassCard(onClick = onGastos) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("\uD83D\uDCB0", fontSize = 22.sp)
                            Text("Finanzas Mes", style = MaterialTheme.typography.labelLarge,
                                color = NeutralVariant50, fontWeight = FontWeight.SemiBold)
                        }
                        Text("$${String.format("%,.0f", totalGastosMes)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Neutral10)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Bottom Nav ────────────────────────────────────────────────────────
        BottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onTiempo = onTiempo,
            onRecuerdos = onRecuerdos,
            onDocumentos = onDocumentos,
            onGastos = onGastos
        )
    }
}

@Composable
fun ClimaCard() {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var clima      by remember { mutableStateOf<ClimaData?>(null) }
    var coordenadas by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun climaUrl() = coordenadas?.let { (lat, lon) ->
        "https://www.accuweather.com/es/search-locations?query=${lat},${lon}"
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val ubicacion = ClimaService.getUbicacion(context)
                if (ubicacion != null) {
                    coordenadas = ubicacion
                    val data = withContext(Dispatchers.IO) {
                        ClimaService.fetchClima(ubicacion.first, ubicacion.second)
                    }
                    if (data != null) {
                        withContext(Dispatchers.IO) { ClimaService.guardarCache(context, data) }
                        clima = data
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { ClimaService.cargarCache(context) }
        if (cached != null) clima = cached

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val ubicacion = ClimaService.getUbicacion(context)
            if (ubicacion != null) {
                coordenadas = ubicacion
                val data = withContext(Dispatchers.IO) {
                    ClimaService.fetchClima(ubicacion.first, ubicacion.second)
                }
                if (data != null) {
                    withContext(Dispatchers.IO) { ClimaService.guardarCache(context, data) }
                    clima = data
                }
            }
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    clima?.let { data ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { climaUrl()?.let { uriHandler.openUri(it) } }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mañana", style = MaterialTheme.typography.labelMedium, color = NeutralVariant50)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(data.mananaIcono, fontSize = 26.sp)
                        Text("${data.mananaTemp}°", style = MaterialTheme.typography.headlineSmall,
                            color = Neutral10, fontWeight = FontWeight.Bold)
                    }
                    Text("7 am", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                }
                Box(modifier = Modifier.height(48.dp).width(1.dp)
                    .background(NeutralVariant80.copy(alpha = 0.5f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tarde", style = MaterialTheme.typography.labelMedium, color = NeutralVariant50)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(data.tardeIcono, fontSize = 26.sp)
                        Text("${data.tardeTemp}°", style = MaterialTheme.typography.headlineSmall,
                            color = Neutral10, fontWeight = FontWeight.Bold)
                    }
                    Text("4 pm", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Ver pronóstico →",
                style = MaterialTheme.typography.labelSmall,
                color = NeutralVariant50,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun WidgetPlanificacionSemanal(
    padres: List<Padre>,
    onEditar: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE) }
    val cicloDias = prefs.getInt("dias_fijos_ciclo", 7)
    val libresAlOtro = prefs.getBoolean("dias_libres_al_otro", false)
    val padreActualId = prefs.getString("padre_actual_id", "") ?: ""
    val otroPadreId = padres.firstOrNull { it.id != padreActualId }?.id ?: ""
    val nombresDias = listOf("L", "M", "X", "J", "V", "S", "D")

    val cal = remember {
        java.util.Calendar.getInstance().also { c ->
            val daysFromMon = (c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            c.add(java.util.Calendar.DAY_OF_YEAR, -daysFromMon)
        }
    }
    val weekDates = remember {
        (0..6).map { i ->
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.DAY_OF_YEAR, i)
            c.get(java.util.Calendar.DAY_OF_MONTH)
        }
    }
    val hoyDow = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    val colorP1 = Teal40
    val colorP2 = Color(0xFF8B4A20)

    fun readDaySlots(schedIdx: Int): List<DaySlot> {
        val raw = prefs.getString("dias_fijos_slots_${cicloDias}_${schedIdx}", null)
        if (raw != null) {
            if (raw.isBlank()) return emptyList()
            return raw.split("|").mapNotNull { s ->
                val ci = s.indexOf(':'); if (ci < 0) null else {
                    val rng = s.substring(ci + 1).split("-")
                    DaySlot(s.substring(0, ci), rng.getOrElse(0) { "" }, rng.getOrElse(1) { "" })
                }
            }
        }
        // backward compat: old single-padre format
        val oldId = (prefs.getString("dias_fijos_schedule_$cicloDias", "") ?: "").split("|").getOrElse(schedIdx) { "" }
        val effId = if (oldId.isBlank() && libresAlOtro) otroPadreId else oldId
        return if (effId.isNotBlank()) listOf(DaySlot(effId, "", "")) else emptyList()
    }

    fun dayMinutes(slots: List<DaySlot>, padreId: String): Int {
        return slots.filter { it.padreId == padreId }.sumOf { slot ->
            val p0 = slot.inicio.trim().split(":"); val p1 = slot.fin.trim().split(":")
            if (p0.size == 2 && p1.size == 2) {
                val im = (p0[0].toIntOrNull() ?: -1) * 60 + (p0[1].toIntOrNull() ?: -1)
                val fm = (p1[0].toIntOrNull() ?: -1) * 60 + (p1[1].toIntOrNull() ?: -1)
                if (im >= 0 && fm > im) fm - im else 24 * 60
            } else 24 * 60
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(NeutralVariant80.copy(0.18f), NeutralVariant90.copy(0.10f))))
            .border(1.dp, Color.White.copy(0.6f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📅 Semana", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = NeutralVariant30)
                IconButton(onClick = onEditar, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Edit, null, tint = NeutralVariant50, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                nombresDias.forEachIndexed { idx, nombre ->
                    val schedIdx = idx % cicloDias
                    val slots = readDaySlots(schedIdx)
                    val minsP0 = dayMinutes(slots, padres.getOrNull(0)?.id ?: "")
                    val minsP1 = dayMinutes(slots, padres.getOrNull(1)?.id ?: "")
                    val totalM = minsP0 + minsP1
                    val esHoy = idx == hoyDow
                    val alpha = if (esHoy) 0.7f else 0.4f

                    val circleBrush: Brush = when {
                        totalM == 0 -> Brush.linearGradient(listOf(NeutralVariant80.copy(if (esHoy) 0.3f else 0.15f), NeutralVariant80.copy(if (esHoy) 0.3f else 0.15f)))
                        minsP0 == 0 -> Brush.linearGradient(listOf(colorP2.copy(alpha), colorP2.copy(alpha)))
                        minsP1 == 0 -> Brush.linearGradient(listOf(colorP1.copy(alpha), colorP1.copy(alpha)))
                        else -> {
                            val f = minsP0.toFloat() / totalM
                            Brush.horizontalGradient(colorStops = arrayOf(0f to colorP1.copy(alpha), f to colorP1.copy(alpha), f to colorP2.copy(alpha), 1f to colorP2.copy(alpha)))
                        }
                    }
                    val borderColor = when {
                        totalM == 0 -> NeutralVariant80.copy(0.4f)
                        minsP0 == 0 -> colorP2
                        minsP1 == 0 -> colorP1
                        else -> colorP1
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            nombre,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (esHoy) Neutral10 else NeutralVariant50,
                            fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(circleBrush)
                                .then(if (esHoy) Modifier.border(1.5.dp, borderColor.copy(0.9f), CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                weekDates[idx].toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (totalM > 0 || esHoy) Color.White.copy(if (esHoy) 1f else 0.9f) else NeutralVariant50,
                                fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(padres.getOrNull(0) to colorP1, padres.getOrNull(1) to colorP2).forEach { (padre, color) ->
                    if (padre != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                            Text(padre.nombre, style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetCustodiaRapida(
    activa: Boolean,
    desde: String,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        NeutralVariant80.copy(alpha = 0.18f),
                        NeutralVariant90.copy(alpha = 0.10f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .clickable(onClick = onToggle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                if (activa) "\uD83C\uDFE0 Con los chicos" else "\uD83C\uDFE0 Sin los chicos",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = NeutralVariant30
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (activa) "Están con vos" else "Tocar para registrar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Neutral10
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (activa) "Desde las $desde \u2022 Tocar para finalizar"
                else "Registrá cuando tenés a los chicos",
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralVariant30
            )
            if (activa) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.06f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(Rose40, Indigo40)))
                    )
                }
            }
        }
    }
}

// ── Helpers UI warm minimal ──────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun WarmBadge(text: String, isAlert: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isAlert) Red90 else Indigo90.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isAlert) Red40 else NeutralVariant30)
    }
}

@Composable
fun SmallIconButton(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
    }
}

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    onTiempo: () -> Unit,
    onRecuerdos: () -> Unit,
    onDocumentos: () -> Unit,
    onGastos: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassWhiteHeavy)
            .padding(top = 1.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.06f)))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onTiempo)) {
                Text("\u23F2", fontSize = 20.sp)
                Text("Tiempo", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onRecuerdos)) {
                Text("\uD83D\uDCF8", fontSize = 20.sp)
                Text("Recuerdos", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onDocumentos)) {
                Text("\uD83D\uDCC4", fontSize = 20.sp)
                Text("Docs", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onGastos)) {
                Text("\uD83D\uDCB0", fontSize = 20.sp)
                Text("Gastos", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaSeleccionModoPreview() {
    CrianzaTheme {
        PantallaSeleccionModo(onModoSeleccionado = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaPrincipalPreview() {
    CrianzaTheme {
        PantallaPrincipal(
            modo = "separados",
            padres = listOf(Padre(id = "1", nombre = "Ana"), Padre(id = "2", nombre = "Carlos")),
            hijos = listOf(Hijo(id = "1", nombre = "Sofía")),
            idPadreActual = "1",
            onTiempo = {}, onCalendario = {}, onGastos = {}, onCompensacion = {},
            onRecuerdos = {}, onListaCompras = {}, onDocumentos = {}, onMensajes = {},
            onConfiguracion = {}, onAtras = {}, onEditarFamilia = {}
        )
    }
}