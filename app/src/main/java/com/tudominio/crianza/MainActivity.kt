@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

class MainActivity : ComponentActivity() {

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

        // Canal de notificaciones push
        NotificacionHelper.crearCanal(this)

        // Pedir permiso de notificaciones (solo Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
                    NavegacionApp()
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
    var pantallaActual by remember { mutableStateOf("cargando") }
    var modoSeleccionado by remember { mutableStateOf(prefs.getString("modo", "") ?: "") }

    var padres by remember { mutableStateOf(listOf<Padre>()) }
    var hijos by remember { mutableStateOf(listOf<Hijo>()) }
    var idPadreActual by remember { mutableStateOf("") }
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
    var usuarioGoogle by remember { mutableStateOf<UsuarioGoogle?>(null) }
    // Código de familia único (generado una vez, basado en IDs de padres)
    val codigoFamiliar by remember { mutableStateOf(java.util.UUID.randomUUID().toString().take(8).uppercase()) }
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

            if (configuracionIntegracion.habilitarTelegram || configuracionIntegracion.habilitarEmail) {
                SincronizacionWorker.iniciar(context)
            }

            // Navegar según estado guardado
            pantallaActual = if (padres.isNotEmpty()) "principal" else "seleccionModo"

            // Iniciar sincronización Firestore (después de setear pantalla)
            syncManager.iniciarListeners(
                onEventosActualizados        = { scope.launch { eventos         = db.eventoDao().obtenerTodosLosEventos() } },
                onGastosActualizados         = { scope.launch { gastos          = db.gastoDao().obtenerTodosLosGastos() } },
                onItemsActualizados          = { scope.launch { itemsCompra     = db.itemCompraDao().obtenerTodos() } },
                onMensajesActualizados       = { scope.launch { mensajes        = db.mensajeDao().obtenerTodos() } },
                onRegistrosActualizados      = { scope.launch { registrosTiempo = db.registroTiempoDao().obtenerTodosLosRegistros() } },
                onCompensacionesActualizadas = { scope.launch { compensaciones  = db.compensacionDao().obtenerTodasLasCompensaciones() } },
                onPendientesActualizados     = { scope.launch { pendientesLista = db.pendienteDao().obtenerTodos() } }
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
            }
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
            onCambiarPadreActual = { id -> idPadreActual = id },
            onTiempo = { pantallaActual = "tiempo" },
            onCalendario = { pantallaActual = "calendario" },
            onGastos = { pantallaActual = "gastos" },
            onCompensacion = { pantallaActual = "compensacion" },
            onRecuerdos = { pantallaActual = "recuerdos" },
            onListaCompras = { pantallaActual = "listaCompras" },
            onDocumentos = { pantallaActual = "documentos" },
            onMensajes = { pantallaActual = "mensajes" },
            onPendientes = { pantallaActual = "pendientes" },
            onConfiguracion = { pantallaActual = "configuracion" },
            onAtras = { pantallaActual = "registroFamilia" },
            onEditarFamilia = { pantallaActual = "registroFamilia" },
            onGoogle = { pantallaActual = "google" },
            onVincular = { pantallaActual = "vincular" }
        )
        "tiempo" -> PantallaTiempo(
            hijos = hijos,
            padres = padres,
            registros = registrosTiempo,
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
            onVerResumen = {
                pantallaActual = "resumenTiempo"
            },
            onAtras = {
                pantallaActual = "principal"
            }
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
                }
            },
            onEliminarEvento = { id ->
                scope.launch {
                    val evento = eventos.find { it.id == id }
                    if (evento != null) {
                        syncManager.eliminarEvento(evento)
                        eventos = db.eventoDao().obtenerTodosLosEventos()
                    }
                }
            },
            onEditarEvento = { eventoEditado ->
                scope.launch {
                    syncManager.actualizarEvento(eventoEditado)
                    eventos = db.eventoDao().obtenerTodosLosEventos()
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
            onAgregarGasto = { nuevoGasto ->
                scope.launch {
                    syncManager.insertarGasto(nuevoGasto)
                    gastos = db.gastoDao().obtenerTodosLosGastos()
                }
            },
            onEliminarGasto = { id ->
                scope.launch {
                    val gasto = gastos.find { it.id == id }
                    if (gasto != null) {
                        syncManager.eliminarGasto(gasto)
                        gastos = db.gastoDao().obtenerTodosLosGastos()
                    }
                }
            },
            onEditarGasto = { gastoEditado ->
                scope.launch {
                    syncManager.actualizarGasto(gastoEditado)
                    gastos = db.gastoDao().obtenerTodosLosGastos()
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
            onAgregar = { item -> scope.launch { syncManager.insertarItem(item); itemsCompra = db.itemCompraDao().obtenerTodos() } },
            onActualizar = { item -> scope.launch { syncManager.actualizarItem(item); itemsCompra = db.itemCompraDao().obtenerTodos() } },
            onEliminar = { item -> scope.launch { syncManager.eliminarItem(item); itemsCompra = db.itemCompraDao().obtenerTodos() } },
            onEliminarComprados = { scope.launch { db.itemCompraDao().eliminarComprados(); itemsCompra = db.itemCompraDao().obtenerTodos() } },
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
        "mensajes" -> PantallaMensajes(
            mensajes = mensajes,
            padres = padres,
            idPadreActual = idPadreActual,
            configuracion = configuracionIntegracion,
            onEnviar = { msg -> scope.launch { syncManager.insertarMensaje(msg); mensajes = db.mensajeDao().obtenerTodos() } },
            onAtras = { pantallaActual = "principal" }
        )
        "pendientes" -> PantallaPendientes(
            pendientes = pendientesLista,
            padres = padres,
            onAgregar = { p -> scope.launch { syncManager.insertarPendiente(p); pendientesLista = db.pendienteDao().obtenerTodos() } },
            onActualizar = { p -> scope.launch { syncManager.actualizarPendiente(p); pendientesLista = db.pendienteDao().obtenerTodos() } },
            onEliminar = { p -> scope.launch { syncManager.eliminarPendiente(p); pendientesLista = db.pendienteDao().obtenerTodos() } },
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
        "vincular" -> PantallaVincular(
            onVinculado = { pantallaActual = "principal" },
            onAtras = { pantallaActual = "principal" },
            onBuscarEmail = { email -> syncManager.buscarUsuarioPorEmail(email) }
        )
        "google" -> PantallaGoogle(
            usuarioActual = usuarioGoogle,
            codigoFamiliar = codigoFamiliar,
            onIniciarSesion = { user ->
                usuarioGoogle = user
                syncManager.registrarUsuarioGoogle(user)
            },
            onCerrarSesion = { usuarioGoogle = null },
            onAtras = { pantallaActual = "principal" }
        )
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
    onModoSeleccionado: (String) -> Unit
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
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                "Compartida",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Light
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Tu familia. Tu acuerdo.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.6f)
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
                        containerColor = Color.White.copy(alpha = 0.25f)
                    )
                ) {
                    Text(
                        "▶  Continuar con mi familia",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "─  o crear nueva  ─",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.45f)
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
                    color = Color.White.copy(alpha = 0.9f),
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
                    color = Color.White.copy(alpha = 0.4f)
                ),
                textAlign = TextAlign.Center
            )
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.2f),
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(alpha = 0.5f)))
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
    onAtras: () -> Unit,
    onEditarFamilia: () -> Unit,
    onGoogle: () -> Unit = {},
    onVincular: () -> Unit = {}
) {
    // Primarios: las 3 funciones principales, se muestran en fila de 3 mas grandes
    val menuItemsPrimarios = listOf(
        ItemMenuPrincipal("Tiempo",       "Registros diarios",   Icons.Default.AccessTime,           0, onTiempo),
        ItemMenuPrincipal("Calendario",   "Eventos y citas",     Icons.Default.CalendarMonth,        1, onCalendario),
        ItemMenuPrincipal("Gastos",       "Control de pagos",    Icons.Default.AccountBalanceWallet, 2, onGastos),
    )
    // Secundarios: las demas, en grid de 2 columnas
    val menuItemsSecundarios = listOf(
        ItemMenuPrincipal("Compensación", "Balance y deudas",    Icons.Default.Balance,              3, onCompensacion),
        ItemMenuPrincipal("Compras",      "Lista compartida",    Icons.Default.ShoppingCart,         4, onListaCompras),
        ItemMenuPrincipal("Pendientes",   "Tareas por hacer",    Icons.Default.CheckCircle,          5, onPendientes),
        ItemMenuPrincipal("Mensajes",     "Comunicación",        Icons.Default.Chat,                 6, onMensajes),
        ItemMenuPrincipal("Recuerdos",    "Momentos especiales", Icons.Default.MenuBook,             7, onRecuerdos),
        ItemMenuPrincipal("Documentos",   "Bóveda privada",      Icons.Default.Lock,                 8, onDocumentos),
    )

    // ── Estado dinámico del dashboard ──────────────────────────────────────────
    val context = LocalContext.current
    var eventosSemana by remember { mutableStateOf<Map<String, List<Evento>>>(emptyMap()) }
    var comprasPendientes by remember { mutableStateOf<List<ItemCompra>>(emptyList()) }
    var tareasPendientes by remember { mutableStateOf<List<Pendiente>>(emptyList()) }
    var compensacionesPendientes by remember { mutableStateOf(0) }
    var mensajesNoLeidos by remember { mutableStateOf(0) }

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

        tareasPendientes = withContext(Dispatchers.IO) {
            db.pendienteDao().obtenerPendientes()
        }

        compensacionesPendientes = withContext(Dispatchers.IO) {
            db.compensacionDao().obtenerTodasLasCompensaciones()
        }.count { !it.confirmada }

        mensajesNoLeidos = withContext(Dispatchers.IO) { db.mensajeDao().contarNoLeidos() }
    }

    Box(Modifier.fillMaxSize().background(BgGradientMain)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crianza Compartida",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onConfiguracion) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = Color.White)
                    }
                    TextButton(onClick = onEditarFamilia) { Text("Familia", color = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Selector "Soy yo" compacto ────────────────────────────────────
            if (padres.size >= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Soy yo:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    padres.forEach { padre ->
                        FilterChip(
                            selected = idPadreActual == padre.id,
                            onClick = { onCambiarPadreActual(padre.id) },
                            label = { Text(padre.nombre, fontWeight = if (idPadreActual == padre.id) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (idPadreActual == padre.id) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.12f),
                                labelColor = Color.White.copy(alpha = 0.75f)
                            )
                        )
                    }
                }
            }

            // ── Widget de clima ───────────────────────────────────────────────
            ClimaCard()

            // ── Widgets dinámicos ─────────────────────────────────────────────
            WidgetEstaSemana(
                eventosPorDia = eventosSemana,
                onClick = onCalendario
            )
            WidgetPendientes(
                compras = comprasPendientes,
                tareas = tareasPendientes,
                onClickCompras = onListaCompras,
                onClickPendientes = onPendientes
            )
            if (compensacionesPendientes > 0 || mensajesNoLeidos > 0) {
                WidgetAccionPrioritaria(
                    compensacionesPendientes = compensacionesPendientes,
                    mensajesNoLeidos = mensajesNoLeidos,
                    onClickCompensacion = onCompensacion,
                    onClickMensajes = onMensajes
                )
            }

            // ── Grid de funciones con animación de entrada ─────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila primaria: 3 funciones principales, mas grandes
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    menuItemsPrimarios.forEachIndexed { index, item ->
                        TarjetaMenuPrincipalAnimada(
                            item = item,
                            modifier = Modifier.weight(1f),
                            delayMs = index * 60,
                            esPrimaria = true
                        )
                    }
                }

                // Filas secundarias: grid 2 columnas, mas compactas
                menuItemsSecundarios.chunked(2).forEachIndexed { filaIdx, fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fila.forEachIndexed { colIdx, item ->
                            TarjetaMenuPrincipalAnimada(
                                item = item,
                                modifier = Modifier.weight(1f),
                                delayMs = (3 + filaIdx * 2 + colIdx) * 60,
                                esPrimaria = false
                            )
                        }
                        if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Sección inferior ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Cuenta / Co-padre
                    OutlinedButton(
                        onClick = onGoogle,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Cuenta", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("Co-padre", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                        }
                    }
                    // Divisor vertical
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(52.dp)
                            .align(Alignment.CenterVertically)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    // Botón Vincular
                    OutlinedButton(
                        onClick = onVincular,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Vincular", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("Dispositivos", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    }  // Box
}

@Composable
fun ClimaCard() {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var clima      by remember { mutableStateOf<ClimaData?>(null) }
    var coordenadas by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun windyUrl() = coordenadas?.let { (lat, lon) ->
        "https://www.windy.com/?${lat},${lon},10"
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassWhite)
                .clickable { windyUrl()?.let { uriHandler.openUri(it) } }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mañana", style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(data.mananaIcono, fontSize = 26.sp)
                        Text("${data.mananaTemp}°", style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("7 am", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f))
                }
                Box(modifier = Modifier.height(48.dp).width(1.dp)
                    .background(Color.White.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tarde", style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(data.tardeIcono, fontSize = 26.sp)
                        Text("${data.tardeTemp}°", style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("4 pm", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f))
                }
            }
            Text(
                "Ver pronóstico →",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun WidgetEstaSemana(
    eventosPorDia: Map<String, List<Evento>>,
    onClick: () -> Unit
) {
    val gradienteVerde = Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF10B981)))
    val sdfIn = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val sdfOut = remember { java.text.SimpleDateFormat("EEE d", java.util.Locale("es")) }
    val hoy = remember { obtenerFechaActual() }
    val totalEventos = eventosPorDia.values.sumOf { it.size }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradienteVerde)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f)))
        Box(
            Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Esta semana",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (totalEventos > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "$totalEventos",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text("›", style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.5f))
                    }
                }
                if (eventosPorDia.isEmpty()) {
                    Text(
                        "Sin eventos esta semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                } else {
                    eventosPorDia.entries.take(5).forEach { (fecha, eventos) ->
                        val label = if (fecha == hoy) "Hoy" else {
                            try { sdfOut.format(sdfIn.parse(fecha)!!).replaceFirstChar { it.uppercase() } }
                            catch (_: Exception) { fecha }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (fecha == hoy) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.widthIn(min = 50.dp)
                            )
                            Column {
                                eventos.take(2).forEach { evento ->
                                    Text(
                                        buildString {
                                            evento.horaInicio?.let { append("$it ") }
                                            append(evento.titulo)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                                if (eventos.size > 2) {
                                    Text("+${eventos.size - 2} más", style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPendientes(
    compras: List<ItemCompra>,
    tareas: List<Pendiente>,
    onClickCompras: () -> Unit,
    onClickPendientes: () -> Unit
) {
    val total = compras.size + tareas.size
    val gradienteRosa = Brush.linearGradient(listOf(Color(0xFF831843), Color(0xFFF472B6)))
    val onClick = if (tareas.isNotEmpty()) onClickPendientes else onClickCompras

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradienteRosa)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f)))
        Box(
            Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pendientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (total > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "$total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.5f))
                }
            }
            if (total == 0) {
                Text(
                    "Todo al día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            } else {
                // Tareas primero
                tareas.take(2).forEach { tarea ->
                    Text(
                        "○ ${tarea.titulo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                // Luego compras
                val restantes = 3 - tareas.take(2).size
                if (restantes > 0 && compras.isNotEmpty()) {
                    compras.take(restantes).forEach { item ->
                        Text(
                            "🛒 ${item.descripcion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
                val ocultos = total - minOf(total, 3)
                if (ocultos > 0) {
                    Text(
                        "+$ocultos más",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetAccionPrioritaria(
    compensacionesPendientes: Int,
    mensajesNoLeidos: Int,
    onClickCompensacion: () -> Unit,
    onClickMensajes: () -> Unit
) {
    val onClick = if (compensacionesPendientes > 0) onClickCompensacion else onClickMensajes
    val gradienteRojo = Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFFF97316)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradienteRojo)
            .clickable(onClick = onClick)
    ) {
        // Glassmorphic overlay
        Box(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.04f)))
        // Decorative circle
        Box(
            Modifier
                .size(70.dp)
                .align(Alignment.TopEnd)
                .offset(x = 15.dp, y = (-15).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Acción prioritaria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("›", style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.5f))
            }
            if (compensacionesPendientes > 0) {
                Text(
                    "$compensacionesPendientes compensación${if (compensacionesPendientes > 1) "es" else ""} sin confirmar",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (mensajesNoLeidos > 0) {
                Text(
                    "$mensajesNoLeidos mensaje${if (mensajesNoLeidos > 1) "s" else ""} sin leer",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun TarjetaMenuPrincipalAnimada(
    item: ItemMenuPrincipal,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    esPrimaria: Boolean = false
) {
    val gradients = listOf(
        CardGrad0, CardGrad1, CardGrad2, CardGrad3,
        CardGrad4, CardGrad5, CardGrad6, CardGrad7, CardGrad8
    )
    val gradient = gradients[item.colorIndex % gradients.size]

    // Animacion de entrada
    val animAlpha = remember { Animatable(0f) }
    val animOffset = remember { Animatable(30f) }
    val animScale = remember { Animatable(0.88f) }

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        launch {
            animAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            animOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            animScale.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }

    val cornerRadius = if (esPrimaria) 22.dp else 24.dp
    val iconSize = if (esPrimaria) 32.dp else 40.dp

    Box(
        modifier = modifier
            .then(if (esPrimaria) Modifier.height(130.dp) else Modifier.aspectRatio(1f))
            .graphicsLayer {
                alpha = animAlpha.value
                translationY = animOffset.value * density
                scaleX = animScale.value
                scaleY = animScale.value
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient)
            .clickable(onClick = item.onClick)
    ) {
        // Circulo decorativo grande
        Box(
            Modifier
                .size(if (esPrimaria) 80.dp else 90.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )
        // Segundo circulo decorativo mas chico
        Box(
            Modifier
                .size(if (esPrimaria) 40.dp else 50.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-12).dp, y = 12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        // Glassmorphic overlay sutil
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.04f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (esPrimaria) 10.dp else 16.dp, vertical = if (esPrimaria) 16.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono con fondo glassmorphic
            Box(
                modifier = Modifier
                    .size(if (esPrimaria) 48.dp else 56.dp)
                    .clip(RoundedCornerShape(if (esPrimaria) 14.dp else 16.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.titulo,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.height(if (esPrimaria) 10.dp else 14.dp))
            Text(
                item.titulo,
                style = if (esPrimaria) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (!esPrimaria) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    item.subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    item.subtitulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Mantener para compatibilidad si se usa en previews
@Composable
fun TarjetaMenuPrincipal(item: ItemMenuPrincipal, modifier: Modifier = Modifier) {
    TarjetaMenuPrincipalAnimada(item = item, modifier = modifier)
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