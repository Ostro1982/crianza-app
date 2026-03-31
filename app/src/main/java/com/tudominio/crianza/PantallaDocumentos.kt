@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PantallaDocumentos(
    documentos: List<Documento>,
    onAgregar: (Documento) -> Unit,
    onActualizar: (Documento) -> Unit,
    onEliminar: (Documento) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var documentoEditando by remember { mutableStateOf<Documento?>(null) }
    var documentoViendo by remember { mutableStateOf<Documento?>(null) }
    var contenidoDescifrado by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(BgGrad8)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bóveda de documentos", color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogo = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(GlassWhite)
            ) {
                Text(
                    "🔒 Todo el contenido está encriptado con AES-256 usando el chip de seguridad del dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (documentos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay documentos. Agregá contraseñas, datos importantes, fotos, etc.", color = Color.White)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(documentos) { doc ->
                        TarjetaDocumento(
                            documento = doc,
                            onVer = {
                                val texto = EncriptadorDocumentos.desencriptar(doc.contenidoEncriptado, doc.iv)
                                contenidoDescifrado = texto
                                documentoViendo = doc
                            },
                            onEditar = { documentoEditando = doc },
                            onEliminar = { onEliminar(doc) }
                        )
                    }
                }
            }
        }
    }
    }  // Box

    if (mostrarDialogo || documentoEditando != null) {
        DialogoDocumento(
            documento = documentoEditando,
            onDismiss = { mostrarDialogo = false; documentoEditando = null },
            onGuardar = { titulo, categoria, contenido, rutaImagen ->
                val (enc, iv) = EncriptadorDocumentos.encriptar(contenido)
                val doc = if (documentoEditando != null) {
                    documentoEditando!!.copy(
                        titulo = titulo,
                        categoria = categoria,
                        contenidoEncriptado = enc,
                        iv = iv,
                        rutaImagen = rutaImagen,
                        fechaModificacion = System.currentTimeMillis()
                    )
                } else {
                    Documento(
                        id = UUID.randomUUID().toString(),
                        titulo = titulo,
                        categoria = categoria,
                        contenidoEncriptado = enc,
                        iv = iv,
                        rutaImagen = rutaImagen
                    )
                }
                if (documentoEditando != null) onActualizar(doc) else onAgregar(doc)
                mostrarDialogo = false
                documentoEditando = null
            }
        )
    }

    documentoViendo?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentoViendo = null; contenidoDescifrado = null },
            title = { Text(doc.titulo) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (doc.categoria.isNotEmpty()) {
                        Text(doc.categoria, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (doc.rutaImagen.isNotEmpty()) {
                        val bmp = remember(doc.rutaImagen) {
                            try { BitmapFactory.decodeFile(doc.rutaImagen)?.asImageBitmap() } catch (_: Exception) { null }
                        }
                        bmp?.let {
                            Image(
                                painter = BitmapPainter(it),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Text(contenidoDescifrado ?: "❌ No se pudo descifrar el contenido.")
                }
            },
            confirmButton = {
                TextButton(onClick = { documentoViendo = null; contenidoDescifrado = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun TarjetaDocumento(
    documento: Documento,
    onVer: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassWhite)
    ) {
        Column {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null,
                    tint = Color(0xFFC4B5FD), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(documento.titulo, fontWeight = FontWeight.SemiBold, color = Color.White)
                    val info = listOfNotNull(
                        documento.categoria.takeIf { it.isNotEmpty() },
                        "Modificado: ${sdf.format(Date(documento.fechaModificacion))}"
                    ).joinToString(" · ")
                    Text(info, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    if (documento.rutaImagen.isNotEmpty()) {
                        Text("📷 Foto adjunta", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC4B5FD))
                    }
                }
                TextButton(onClick = onVer) { Text("Ver", color = Color(0xFFC4B5FD)) }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFF87171))
                }
            }
            // Miniatura de foto si existe
            if (documento.rutaImagen.isNotEmpty()) {
                val bmp = remember(documento.rutaImagen) {
                    try { BitmapFactory.decodeFile(documento.rutaImagen)?.asImageBitmap() } catch (_: Exception) { null }
                }
                bmp?.let {
                    Image(
                        painter = BitmapPainter(it),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoDocumento(
    documento: Documento?,
    onDismiss: () -> Unit,
    onGuardar: (titulo: String, categoria: String, contenido: String, rutaImagen: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var titulo by remember { mutableStateOf(documento?.titulo ?: "") }
    var categoria by remember { mutableStateOf(documento?.categoria ?: "") }
    var contenido by remember { mutableStateOf("") }
    var rutaImagen by remember { mutableStateOf(documento?.rutaImagen ?: "") }

    LaunchedEffect(documento) {
        if (documento != null && documento.contenidoEncriptado.isNotEmpty()) {
            contenido = EncriptadorDocumentos.desencriptar(
                documento.contenidoEncriptado, documento.iv
            ) ?: ""
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val dir = File(context.filesDir, "boveda_fotos").also { it.mkdirs() }
                    val dest = File(dir, "${UUID.randomUUID()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    rutaImagen = dest.absolutePath
                } catch (_: Exception) {}
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (documento == null) "Nuevo documento" else "Editar documento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Contraseñas, Médico, Seguros…") }
                )
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Texto, contraseña, número de cuenta…") }
                )
                // Sección foto
                OutlinedButton(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (rutaImagen.isEmpty()) "Agregar foto" else "Cambiar foto")
                }
                if (rutaImagen.isNotEmpty()) {
                    val bmp = remember(rutaImagen) {
                        try { BitmapFactory.decodeFile(rutaImagen)?.asImageBitmap() } catch (_: Exception) { null }
                    }
                    bmp?.let {
                        Image(
                            painter = BitmapPainter(it),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(titulo.trim(), categoria.trim(), contenido, rutaImagen) },
                enabled = titulo.isNotBlank() && contenido.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
