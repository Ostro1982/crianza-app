@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.launch

// Glass card reutilizable
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassWhite)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

private fun generarQrBitmap(texto: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bmp
}

// Dialogo de direccion de copia
@Composable
private fun DialogoDireccionSync(
    otraFamilyId: String,
    otroNombre: String,
    familyId: String,
    onConfirmar: (modo: String) -> Unit,
    onCerrar: () -> Unit
) {
    var modo by remember { mutableStateOf("yo_copio") }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Neutral99,
        title = {
            Text("Direccion de datos", color = Neutral10, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Al vincular, se puede elegir quien copia los datos de quien.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant50
                )
                Spacer(Modifier.height(8.dp))

                // Opcion 1: yo copio del otro
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = modo == "yo_copio",
                        onClick = { modo = "yo_copio" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Indigo80, unselectedColor = NeutralVariant50
                        )
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Yo copio los datos de $otroNombre",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral10, fontWeight = FontWeight.Medium
                        )
                        Text(
                            "El otro ya tiene todo cargado",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralVariant50
                        )
                    }
                }

                // Opcion 2: el otro copia de mi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = modo == "otro_copia",
                        onClick = { modo = "otro_copia" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Indigo80, unselectedColor = NeutralVariant50
                        )
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "$otroNombre copia mis datos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral10, fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Yo ya tengo todo cargado",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralVariant50
                        )
                    }
                }

                // Opcion 3: ambos copian (merge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = modo == "ambos",
                        onClick = { modo = "ambos" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Indigo80, unselectedColor = NeutralVariant50
                        )
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Ambos copian del otro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral10, fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Se combinan datos (sin duplicar familia)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralVariant50
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(modo) },
                shape = RoundedCornerShape(14.dp)
            ) { Text("Vincular") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cancelar", color = NeutralVariant30)
            }
        }
    )
}

@Composable
fun PantallaCuentaVincular(
    usuarioActual: UsuarioGoogle?,
    codigoFamiliar: String,
    onIniciarSesion: (UsuarioGoogle) -> Unit,
    onCerrarSesion: () -> Unit,
    onVinculado: () -> Unit,
    onAtras: () -> Unit,
    onBuscarEmail: suspend (String) -> Pair<String, String>? = { null }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuth = remember { GoogleAuthHelper(context) }
    val familyId = remember { FamilyIdManager.obtenerFamilyId(context) }

    var cargando by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var emailIngresado by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var resultadoBusqueda by remember { mutableStateOf<Pair<String, String>?>(null) }
    var noEncontrado by remember { mutableStateOf(false) }

    var codigoIngresado by remember { mutableStateOf("") }
    var errorCodigo by remember { mutableStateOf<String?>(null) }
    var exitoCodigo by remember { mutableStateOf(false) }

    var mostrarQr by remember { mutableStateOf(false) }

    // Estado para dialogo de direccion — (familyId del otro, nombre)
    var pendienteVincular by remember { mutableStateOf<Pair<String, String>?>(null) }
    var vinculadoExito by remember { mutableStateOf(false) }
    var vinculandoCargando by remember { mutableStateOf(false) }
    var vinculandoError by remember { mutableStateOf<String?>(null) }

    fun ejecutarVinculacion(otraFamilyId: String, modo: String) {
        vinculandoCargando = true
        vinculandoError = null
        scope.launch {
            try {
                val sm = SyncManager(context, AppDatabase.getInstance(context))
                when (modo) {
                    "yo_copio" -> {
                        // Adopto su familyId y descargo sus datos (sin subir los mios primero)
                        FamilyIdManager.vincularConCodigo(context, otraFamilyId)
                        sm.descargarDatosDeFamilia()
                    }
                    "otro_copia" -> {
                        // Subo mis datos a su familia para que los descargue (no cambio mi familyId)
                        sm.subirDatosAFamilia(otraFamilyId)
                    }
                    "ambos" -> {
                        // Subo mis datos a la otra familia, adopto su familyId, descargo sus datos
                        sm.subirDatosAFamilia(otraFamilyId)
                        FamilyIdManager.vincularConCodigo(context, otraFamilyId)
                        sm.descargarDatosDeFamilia()
                    }
                }
                vinculadoExito = true
                onVinculado()
            } catch (e: Exception) {
                vinculandoError = "No se pudo vincular: ${e.message ?: "Error de red"}"
            } finally {
                vinculandoCargando = false
            }
        }
    }

    // Scanner QR
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val codigo = result.contents
        if (codigo != null) {
            if (codigo == familyId) {
                Toast.makeText(context, "Ese es tu propio codigo", Toast.LENGTH_SHORT).show()
            } else if (codigo.matches(Regex("[0-9a-fA-F\\-]{32,36}"))) {
                pendienteVincular = Pair(codigo, "otro padre")
            } else {
                Toast.makeText(context, "QR invalido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dialogo de direccion
    pendienteVincular?.let { (otroFid, otroNombre) ->
        DialogoDireccionSync(
            otraFamilyId = otroFid,
            otroNombre = otroNombre,
            familyId = familyId,
            onConfirmar = { modo ->
                pendienteVincular = null
                ejecutarVinculacion(otroFid, modo)
            },
            onCerrar = { pendienteVincular = null }
        )
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Mi cuenta", color = Neutral10, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atras", tint = NeutralVariant30)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pv ->

        // ── Pantalla principal ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── CUENTA GOOGLE ────────────────────────────────────────────────
            GlassCard {
                if (usuarioActual == null) {
                    Icon(
                        Icons.Default.AccountCircle, contentDescription = null,
                        modifier = Modifier.size(56.dp), tint = NeutralVariant50
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Conecta tu cuenta Google",
                        style = MaterialTheme.typography.titleMedium,
                        color = Neutral10, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Para que el otro padre/madre pueda encontrarte por email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!googleAuth.configurado) { errorMsg = "Google Sign-In no configurado."; return@Button }
                            scope.launch {
                                cargando = true; errorMsg = null
                                googleAuth.iniciarSesion(context as Activity)
                                    .onSuccess { onIniciarSesion(it) }
                                    .onFailure { errorMsg = it.message }
                                cargando = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !cargando,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Iniciar sesion con Google")
                    }
                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Red80, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Icon(
                        Icons.Default.AccountCircle, contentDescription = null,
                        modifier = Modifier.size(56.dp), tint = Indigo80
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        usuarioActual.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Neutral10
                    )
                    Text(
                        usuarioActual.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { scope.launch { googleAuth.cerrarSesion(); onCerrarSesion() } },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeutralVariant30)
                    ) { Text("Cerrar sesion", style = MaterialTheme.typography.labelMedium) }
                }
            }

            // ── VINCULAR ─────────────────────────────────────────────────────
            Text(
                "Vincular con el otro padre/madre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Neutral10
            )

            // ── Estado de vinculacion ────────────────────────────────────────
            if (vinculandoCargando) {
                GlassCard {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Vinculando, espera un momento...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50,
                        textAlign = TextAlign.Center
                    )
                }
            }
            vinculandoError?.let { err ->
                GlassCard {
                    Text(err, color = Red80, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vinculandoError = null }) { Text("Cerrar") }
                }
            }
            if (vinculadoExito) {
                GlassCard {
                    Text(
                        "Vinculado correctamente!",
                        color = Indigo80, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Los datos se sincronizaran automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50
                    )
                }
            }

            // ── Codigo de familia + QR ───────────────────────────────────────
            GlassCard {
                Text("Tu codigo de familia", style = MaterialTheme.typography.labelMedium, color = NeutralVariant30)
                Spacer(Modifier.height(4.dp))
                Text(
                    familyId,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant50, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))

                // QR
                if (mostrarQr) {
                    val qrBitmap = remember(familyId) { generarQrBitmap(familyId) }
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR codigo de familia",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "El otro padre escanea este QR desde su app",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Botones
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Codigo familia", familyId))
                            Toast.makeText(context, "Codigo copiado", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral10)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copiar", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { mostrarQr = !mostrarQr },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Neutral10)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (mostrarQr) "Ocultar QR" else "Mostrar QR", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { GoogleAuthHelper.compartirApp(context, familyId) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir invitacion", style = MaterialTheme.typography.labelMedium)
                }
            }

            // ── Escanear QR ──────────────────────────────────────────────────
            GlassCard {
                Text("Escanear QR del otro padre", style = MaterialTheme.typography.labelMedium, color = NeutralVariant30)
                Spacer(Modifier.height(4.dp))
                Text(
                    "El otro padre muestra su QR y vos lo escaneas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant50, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("Escanea el QR del otro padre/madre")
                            setBeepEnabled(false)
                            setOrientationLocked(true)
                        }
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Escanear QR")
                }
            }

            // ── Buscar por email ─────────────────────────────────────────────
            GlassCard {
                Text("Buscar por email", style = MaterialTheme.typography.labelMedium, color = NeutralVariant30)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailIngresado,
                    onValueChange = { emailIngresado = it.trim(); noEncontrado = false; resultadoBusqueda = null },
                    label = { Text("Email del otro padre/madre", color = NeutralVariant50) },
                    placeholder = { Text("ejemplo@gmail.com", color = NeutralVariant50.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Neutral10,
                        unfocusedTextColor = Neutral10,
                        focusedBorderColor = Indigo80,
                        unfocusedBorderColor = NeutralVariant50.copy(alpha = 0.4f),
                        cursorColor = Indigo80
                    )
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            buscando = true; noEncontrado = false; resultadoBusqueda = null
                            val r = onBuscarEmail(emailIngresado)
                            if (r == null) noEncontrado = true else resultadoBusqueda = r
                            buscando = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = emailIngresado.contains("@") && !buscando,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (buscando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar")
                }

                if (noEncontrado) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No se encontro. El otro padre debe abrir la app e iniciar sesion con Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Red80, textAlign = TextAlign.Center
                    )
                }
            }

            // ── Resultado email encontrado ───────────────────────────────────
            resultadoBusqueda?.let { (nombre, fid) ->
                GlassCard {
                    Text("Encontrado", style = MaterialTheme.typography.labelMedium, color = Indigo80)
                    Spacer(Modifier.height(4.dp))
                    Text(nombre, style = MaterialTheme.typography.titleMedium, color = Neutral10, fontWeight = FontWeight.Bold)
                    Text(emailIngresado, style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)

                    Spacer(Modifier.height(12.dp))

                    if (fid == familyId) {
                        Text("Ya estan vinculados", color = Indigo80, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { pendienteVincular = Pair(fid, nombre) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Re-sincronizar datos")
                        }
                    } else {
                        Button(
                            onClick = { pendienteVincular = Pair(fid, nombre) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Vincular con $nombre")
                        }
                    }
                }
            }

            // ── Buscar por codigo ────────────────────────────────────────────
            GlassCard {
                Text("Ingresar codigo manualmente", style = MaterialTheme.typography.labelMedium, color = NeutralVariant30)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = codigoIngresado,
                    onValueChange = { codigoIngresado = it.trim(); errorCodigo = null; exitoCodigo = false },
                    label = { Text("Codigo de familia", color = NeutralVariant50) },
                    placeholder = { Text("ej: a1b2c3d4-...", color = NeutralVariant50.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Neutral10,
                        unfocusedTextColor = Neutral10,
                        focusedBorderColor = Indigo80,
                        unfocusedBorderColor = NeutralVariant50.copy(alpha = 0.4f),
                        cursorColor = Indigo80
                    )
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val limpio = codigoIngresado.trim()
                        if (limpio == familyId) {
                            errorCodigo = "Ese es tu propio codigo de familia."
                        } else if (!limpio.matches(Regex("[0-9a-fA-F\\-]{32,36}"))) {
                            errorCodigo = "Codigo invalido. Debe ser un UUID."
                        } else {
                            pendienteVincular = Pair(limpio, "otro padre")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = codigoIngresado.length >= 32,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Vincular")
                }

                errorCodigo?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Red80, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
    } // Box
}
