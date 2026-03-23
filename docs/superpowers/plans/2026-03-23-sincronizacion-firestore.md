# Sincronización en Tiempo Real — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sincronizar en tiempo real eventos, gastos, lista de compras y mensajes entre los dos teléfonos de los padres, sin servidor propio, usando Firebase Firestore.

**Architecture:** Cada familia tiene un ID único persistente (UUID guardado en SharedPreferences). Ambos teléfonos escriben a Room DB local Y a Firestore simultáneamente a través de un `SyncManager`. Un listener de Firestore en tiempo real detecta cambios del otro teléfono y los aplica al Room DB local. Room sigue siendo la fuente de verdad para la UI.

**Tech Stack:** Firebase Firestore (cloud DB), Firebase BOM 33.x, Kotlin Coroutines, Room DB (cache local sin cambios), SharedPreferences (family ID persistente)

**Costo:** Gratis en Spark plan (50K lecturas/día, 20K escrituras/día — suficiente para uso personal y decenas de familias)

---

## Mapa de archivos

| Archivo | Acción | Responsabilidad |
|---------|--------|-----------------|
| `build.gradle.kts` (raíz) | Modificar | Agregar plugin google-services |
| `app/build.gradle.kts` | Modificar | Aplicar plugin + dependencias Firebase |
| `app/google-services.json` | Crear (manual) | Config Firebase descargada de la consola |
| `FamilyIdManager.kt` | Crear | ID familiar persistente en SharedPreferences |
| `SyncManager.kt` | Crear | Escribe a Room + Firestore; listeners de cambios remotos |
| `PantallaVincular.kt` | Crear | UI para compartir código / ingresar código del otro padre |
| `MainActivity.kt` | Modificar | Init SyncManager, reemplazar DAO calls con SyncManager, agregar pantalla vincular |
| `Daos.kt` / DAOs separados | Modificar | Agregar `eliminarPorId` a 4 DAOs |

---

## Tarea 1 — Setup manual en Firebase Console

> El usuario hace esto en el navegador.

- [ ] **1.1** Ir a **console.firebase.google.com** → "Agregar proyecto"
  - Nombre: `crianza-app`
  - Deshabilitar Google Analytics
  - Click "Crear proyecto"

- [ ] **1.2** En el proyecto → "Agregar app" → ícono Android
  - Package name: `com.tudominio.crianza`
  - Apodo: `Crianza`
  - Click "Registrar app"

- [ ] **1.3** Descargar `google-services.json` → copiarlo a:
  `H:\Users\Ostro\AndroidStudioProjects\Crianza\app\google-services.json`

- [ ] **1.4** Saltear los pasos "Agregar Firebase SDK" del wizard (los hacemos en código)

- [ ] **1.5** En panel de Firebase → "Firestore Database" → "Crear base de datos"
  - Elegir "Iniciar en modo de prueba"
  - Ubicación: `southamerica-east1` (São Paulo) o `nam5`
  - Click "Habilitar"

- [ ] **1.6** (Después de 30 días) Actualizar reglas de seguridad en Firestore → Reglas:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /familias/{familyId}/{document=**} {
      allow read, write: if true;
    }
  }
}
```

---

## Tarea 2 — Agregar dependencias Firebase

- [ ] **2.1** Modificar `build.gradle.kts` (raíz) — agregar la última línea:
```kotlin
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

- [ ] **2.2** Modificar `app/build.gradle.kts` — agregar plugin:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}
```

- [ ] **2.3** En `dependencies {}` de `app/build.gradle.kts`, agregar:
```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-firestore-ktx")
```

- [ ] **2.4** Sync Gradle en Android Studio ("Sync Now")

- [ ] **2.5** Verificar que compila sin errores.

---

## Tarea 3 — FamilyIdManager.kt

- [ ] **3.1** Crear `app/src/main/java/com/tudominio/crianza/FamilyIdManager.kt`:

```kotlin
package com.tudominio.crianza

import android.content.Context
import java.util.UUID

object FamilyIdManager {

    private const val PREFS_NAME = "crianza_family"
    private const val KEY_FAMILY_ID = "family_id"
    private const val KEY_IS_LINKED = "is_linked"

    fun obtenerFamilyId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_FAMILY_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_FAMILY_ID, id).apply()
        }
        return id
    }

    fun vincularConCodigo(context: Context, codigoCompleto: String): Boolean {
        val limpio = codigoCompleto.trim()
        // Validar formato UUID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx) o al menos 32 hex chars
        val esUuid = limpio.matches(Regex("[0-9a-fA-F\\-]{32,36}"))
        if (!esUuid) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FAMILY_ID, limpio)
            .putBoolean(KEY_IS_LINKED, true)
            .apply()
        return true
    }

    fun estaVinculado(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LINKED, false)
    }
}
```

- [ ] **3.2** Verificar que compila.

---

## Tarea 4 — Arreglar DAOs: `eliminarPorId` + `OnConflictStrategy.REPLACE`

Los listeners de Firestore necesitan eliminar por ID y deben manejar inserts de documentos ya existentes sin crashear.

- [ ] **4.1** En `EventoDao.kt`, hacer DOS cambios:
```kotlin
// Cambiar @Insert a:
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertarEvento(evento: Evento)

// Agregar al final del @Dao:
@Query("DELETE FROM eventos WHERE id = :id")
suspend fun eliminarPorId(id: String)
```
Import necesario si no está: `import androidx.room.OnConflictStrategy`

- [ ] **4.2** En `GastoDao.kt`, mismo patrón:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertarGasto(gasto: Gasto)

@Query("DELETE FROM gastos WHERE id = :id")
suspend fun eliminarPorId(id: String)
```

- [ ] **4.3** En `ItemCompraDao`, agregar:
```kotlin
@Query("DELETE FROM items_compra WHERE id = :id")
suspend fun eliminarPorId(id: String)
```

- [ ] **4.4** En `MensajeDao`, agregar:
```kotlin
@Query("DELETE FROM mensajes WHERE id = :id")
suspend fun eliminarPorId(id: String)
```

- [ ] **4.5** Verificar que compila.

---

## Tarea 5 — SyncManager.kt

- [ ] **5.1** Crear `app/src/main/java/com/tudominio/crianza/SyncManager.kt`:

```kotlin
package com.tudominio.crianza

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SyncManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val familyId = FamilyIdManager.obtenerFamilyId(context)
    private val fs = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun col(nombre: String) = fs.collection("familias/$familyId/$nombre")

    // ── Serialización ────────────────────────────────────────────────────────

    private fun Evento.toMap() = mapOf(
        "id" to id, "titulo" to titulo, "descripcion" to descripcion,
        "fecha" to fecha, "horaInicio" to (horaInicio ?: ""), "horaFin" to (horaFin ?: ""),
        "color" to color, "fechaCompleta" to fechaCompleta, "ubicacion" to ubicacion,
        "origenEmail" to origenEmail, "asistenciaPadre1" to asistenciaPadre1,
        "asistenciaPadre2" to asistenciaPadre2
    )

    private fun Gasto.toMap() = mapOf(
        "id" to id, "descripcion" to descripcion, "monto" to monto, "fecha" to fecha,
        "idPagador" to idPagador, "nombrePagador" to nombrePagador,
        "idsHijos" to idsHijos, "nombresHijos" to nombresHijos,
        "dividirAutomatico" to dividirAutomatico, "fechaCompleta" to fechaCompleta
    )

    private fun ItemCompra.toMap() = mapOf(
        "id" to id, "descripcion" to descripcion, "cantidad" to cantidad,
        "unidad" to unidad, "categoria" to categoria, "subcategoria" to subcategoria,
        "precio" to precio, "comprado" to comprado, "agregadoPor" to agregadoPor,
        "idPagador" to idPagador, "fechaCompleta" to fechaCompleta,
        "esPrivado" to esPrivado, "idPropietario" to idPropietario
    )

    private fun Mensaje.toMap() = mapOf(
        "id" to id, "idEmisor" to idEmisor, "nombreEmisor" to nombreEmisor,
        "texto" to texto, "fechaCompleta" to fechaCompleta, "leido" to leido
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toEvento(): Evento? = try {
        Evento(
            id = this["id"] as? String ?: return null,
            titulo = this["titulo"] as? String ?: "",
            descripcion = this["descripcion"] as? String ?: "",
            fecha = this["fecha"] as? String ?: "",
            horaInicio = (this["horaInicio"] as? String)?.ifEmpty { null },
            horaFin = (this["horaFin"] as? String)?.ifEmpty { null },
            color = (this["color"] as? Long)?.toInt() ?: 0,
            fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
            ubicacion = this["ubicacion"] as? String ?: "",
            origenEmail = this["origenEmail"] as? Boolean ?: false,
            asistenciaPadre1 = this["asistenciaPadre1"] as? String ?: "",
            asistenciaPadre2 = this["asistenciaPadre2"] as? String ?: ""
        )
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toGasto(): Gasto? = try {
        Gasto(
            id = this["id"] as? String ?: return null,
            descripcion = this["descripcion"] as? String ?: "",
            monto = this["monto"] as? Double ?: 0.0,
            fecha = this["fecha"] as? String ?: "",
            idPagador = this["idPagador"] as? String ?: "",
            nombrePagador = this["nombrePagador"] as? String ?: "",
            idsHijos = (this["idsHijos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            nombresHijos = this["nombresHijos"] as? String ?: "",
            dividirAutomatico = this["dividirAutomatico"] as? Boolean ?: true,
            fechaCompleta = this["fechaCompleta"] as? Long ?: 0L
        )
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toItemCompra(): ItemCompra? = try {
        ItemCompra(
            id = this["id"] as? String ?: return null,
            descripcion = this["descripcion"] as? String ?: "",
            cantidad = this["cantidad"] as? String ?: "1",
            unidad = this["unidad"] as? String ?: "",
            categoria = this["categoria"] as? String ?: "",
            subcategoria = this["subcategoria"] as? String ?: "",
            precio = this["precio"] as? Double ?: 0.0,
            comprado = this["comprado"] as? Boolean ?: false,
            agregadoPor = this["agregadoPor"] as? String ?: "",
            idPagador = this["idPagador"] as? String ?: "",
            fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
            esPrivado = this["esPrivado"] as? Boolean ?: false,
            idPropietario = this["idPropietario"] as? String ?: ""
        )
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toMensaje(): Mensaje? = try {
        Mensaje(
            id = this["id"] as? String ?: return null,
            idEmisor = this["idEmisor"] as? String ?: "",
            nombreEmisor = this["nombreEmisor"] as? String ?: "",
            texto = this["texto"] as? String ?: "",
            fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
            leido = this["leido"] as? Boolean ?: false
        )
    } catch (e: Exception) { null }

    // ── Escrituras ────────────────────────────────────────────────────────────

    suspend fun insertarEvento(evento: Evento) {
        db.eventoDao().insertarEvento(evento)
        col("eventos").document(evento.id).set(evento.toMap())
    }

    suspend fun actualizarEvento(evento: Evento) {
        db.eventoDao().actualizarEvento(evento)
        col("eventos").document(evento.id).set(evento.toMap())
    }

    suspend fun eliminarEvento(evento: Evento) {
        db.eventoDao().eliminarEvento(evento)
        col("eventos").document(evento.id).delete()
    }

    suspend fun insertarGasto(gasto: Gasto) {
        db.gastoDao().insertarGasto(gasto)
        col("gastos").document(gasto.id).set(gasto.toMap())
    }

    suspend fun actualizarGasto(gasto: Gasto) {
        db.gastoDao().actualizarGasto(gasto)
        col("gastos").document(gasto.id).set(gasto.toMap())
    }

    suspend fun eliminarGasto(gasto: Gasto) {
        db.gastoDao().eliminarGasto(gasto)
        col("gastos").document(gasto.id).delete()
    }

    suspend fun insertarItem(item: ItemCompra) {
        db.itemCompraDao().insertar(item)
        if (!item.esPrivado) col("items_compra").document(item.id).set(item.toMap())
    }

    suspend fun actualizarItem(item: ItemCompra) {
        db.itemCompraDao().actualizar(item)
        if (!item.esPrivado) col("items_compra").document(item.id).set(item.toMap())
    }

    suspend fun eliminarItem(item: ItemCompra) {
        db.itemCompraDao().eliminar(item)
        col("items_compra").document(item.id).delete()
    }

    suspend fun insertarMensaje(mensaje: Mensaje) {
        db.mensajeDao().insertar(mensaje)
        col("mensajes").document(mensaje.id).set(mensaje.toMap())
    }

    // ── Listeners en tiempo real ──────────────────────────────────────────────

    fun iniciarListeners(
        onEventosActualizados: () -> Unit,
        onGastosActualizados: () -> Unit,
        onItemsActualizados: () -> Unit,
        onMensajesActualizados: () -> Unit
    ) {
        col("eventos").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    val data = change.document.data
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            data.toEvento()?.let { db.eventoDao().insertarEvento(it) }
                        DocumentChange.Type.REMOVED ->
                            db.eventoDao().eliminarPorId(change.document.id)
                    }
                }
                onEventosActualizados()
            }
        }

        col("gastos").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    val data = change.document.data
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            data.toGasto()?.let { db.gastoDao().insertarGasto(it) }
                        DocumentChange.Type.REMOVED ->
                            db.gastoDao().eliminarPorId(change.document.id)
                    }
                }
                onGastosActualizados()
            }
        }

        col("items_compra").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    val data = change.document.data
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            data.toItemCompra()?.let { db.itemCompraDao().insertar(it) }
                        DocumentChange.Type.REMOVED ->
                            db.itemCompraDao().eliminarPorId(change.document.id)
                    }
                }
                onItemsActualizados()
            }
        }

        col("mensajes").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    val data = change.document.data
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            data.toMensaje()?.let { db.mensajeDao().insertar(it) }
                        DocumentChange.Type.REMOVED ->
                            db.mensajeDao().eliminarPorId(change.document.id)
                    }
                }
                onMensajesActualizados()
            }
        }

        Log.d("SyncManager", "Listeners activos para familia: $familyId")
    }
}
```

- [ ] **5.2** Verificar que compila.

---

## Tarea 6 — PantallaVincular.kt

- [ ] **6.1** Crear `app/src/main/java/com/tudominio/crianza/PantallaVincular.kt`:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.tudominio.crianza

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PantallaVincular(
    onVinculado: () -> Unit,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val familyId = remember { FamilyIdManager.obtenerFamilyId(context) }
    var codigoIngresado by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vincular dispositivos") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tu código de familia", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        familyId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT,
                                "Instala Crianza y usá este código para sincronizar nuestra familia:\n$familyId")
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir código"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir código")
                    }
                }
            }

            Divider()
            Text("— o ingresá el código del otro padre —",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)

            OutlinedTextField(
                value = codigoIngresado,
                onValueChange = { codigoIngresado = it.trim(); error = null },
                label = { Text("Código del otro padre") },
                placeholder = { Text("Pegá el UUID completo aquí") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                supportingText = error?.let { { Text(it) } }
            )

            Button(
                onClick = {
                    if (FamilyIdManager.vincularConCodigo(context, codigoIngresado)) {
                        mostrarConfirmacion = true
                    } else {
                        error = "Código inválido. Debe ser el UUID completo."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = codigoIngresado.length >= 8
            ) {
                Text("Vincular con este código")
            }

            Text(
                "Una vez vinculados, eventos, gastos, compras y mensajes se sincronizan automáticamente entre ambos teléfonos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Vinculado") },
            text = { Text("Este teléfono ahora sincroniza con la familia del código ingresado. Reiniciá la app para que los cambios tomen efecto.") },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmacion = false; onVinculado() }) {
                    Text("Entendido")
                }
            }
        )
    }
}
```

- [ ] **6.2** Verificar que compila.

---

## Tarea 7 — Integrar SyncManager en MainActivity

- [ ] **7.1** En `NavegacionApp()`, después de `val db = remember { ... }`, agregar:
```kotlin
val syncManager = remember { SyncManager(context, db) }
```

- [ ] **7.2** En el `LaunchedEffect(Unit)`, dentro del bloque `scope.launch { ... }`, los listeners deben iniciarse DESPUÉS de la línea `pantallaActual = if (padres.isNotEmpty()) ...`. Buscar esa línea y agregar justo debajo:
```kotlin
// Navegar según estado guardado
pantallaActual = if (padres.isNotEmpty()) "principal" else "seleccionModo"

// Iniciar sincronización Firestore (después de setear pantalla para evitar updates prematuros)
syncManager.iniciarListeners(
    onEventosActualizados  = { scope.launch { eventos     = db.eventoDao().obtenerTodosLosEventos() } },
    onGastosActualizados   = { scope.launch { gastos      = db.gastoDao().obtenerTodosLosGastos() } },
    onItemsActualizados    = { scope.launch { itemsCompra = db.itemCompraDao().obtenerTodos() } },
    onMensajesActualizados = { scope.launch { mensajes    = db.mensajeDao().obtenerTodos() } }
)
```

- [ ] **7.3** Agregar pantalla al `when (pantallaActual)`:
```kotlin
"vincular" -> PantallaVincular(
    onVinculado = { pantallaActual = "principal" },
    onAtras = { pantallaActual = "principal" }
)
```

- [ ] **7.4** En `MainActivity.kt`, buscar el bloque que empieza con `"principal" -> PantallaPrincipal(`. Está en el `when (pantallaActual)`. Agregar el parámetro `onVincular` al final, antes del cierre `)`:
```kotlin
"principal" -> PantallaPrincipal(
    // ... parámetros existentes ...
    onGoogle = { pantallaActual = "google" },
    onVincular = { pantallaActual = "vincular" }   // AGREGAR ESTA LÍNEA
)
```

- [ ] **7.5** En la firma de la función `PantallaPrincipal()` (buscar `fun PantallaPrincipal(`), agregar el nuevo parámetro al final antes del cierre `)`):
```kotlin
onGoogle: () -> Unit = {},
onVincular: () -> Unit = {}   // AGREGAR ESTA LÍNEA
```
Luego, en la lista `val menuItems = listOf(...)` dentro de la misma función, agregar al final de la lista:
```kotlin
ItemMenuPrincipal("Sincronizar", "Vincular dispositivos", Icons.Default.Sync, 8, onVincular),
```
Agregar import al principio del archivo: `import androidx.compose.material.icons.filled.Sync`

Si hay un `@Preview` de `PantallaPrincipal` al final del archivo, agregarle también `onVincular = {}` para que no rompa la preview.

- [ ] **7.6** Reemplazar en la sección `"calendario"`:
```kotlin
// db.eventoDao().insertarEvento(it) → syncManager.insertarEvento(it)
// db.eventoDao().actualizarEvento(it) → syncManager.actualizarEvento(it)
// db.eventoDao().eliminarEvento(it) → syncManager.eliminarEvento(it)
```

- [ ] **7.7** Reemplazar en `"gastos"`:
```kotlin
// db.gastoDao().insertarGasto(it) → syncManager.insertarGasto(it)
// db.gastoDao().actualizarGasto(it) → syncManager.actualizarGasto(it)
// db.gastoDao().eliminarGasto(it) → syncManager.eliminarGasto(it)
```

- [ ] **7.8** Reemplazar en `"listaCompras"`:
```kotlin
// db.itemCompraDao().insertar(it) → syncManager.insertarItem(it)
// db.itemCompraDao().actualizar(it) → syncManager.actualizarItem(it)
// db.itemCompraDao().eliminar(it) → syncManager.eliminarItem(it)
```

- [ ] **7.9** Reemplazar en `"mensajes"`:
```kotlin
// db.mensajeDao().insertar(msg) → syncManager.insertarMensaje(msg)
```

- [ ] **7.10** Build completo. Resolver errores si hay.

---

## Tarea 8 — Commit y push

- [ ] **8.1** En PowerShell o terminal:
```bash
cd "H:\Users\Ostro\AndroidStudioProjects\Crianza"
git add .
git commit -m "feat: sincronizacion en tiempo real via Firebase Firestore"
git push
```

---

## Tarea 9 — Prueba end-to-end

- [ ] **9.1** Instalar en celular A
- [ ] **9.2** Instalar en celular B
- [ ] **9.3** Celular A: Menú → "Sincronizar" → copiar el UUID completo
- [ ] **9.4** Celular B: Menú → "Sincronizar" → pegar UUID → "Vincular"
- [ ] **9.5** Celular A: agregar un evento
- [ ] **9.6** Celular B: verificar que el evento aparece en segundos
- [ ] **9.7** Celular B: tachar un ítem de compras
- [ ] **9.8** Celular A: verificar que aparece tachado

---

## Fases futuras

- **Fase 2:** Sincronizar `registros_tiempo`, `compensaciones`, `recuerdos` (sin fotos)
- **Fase 3:** Al vincular, descargar automáticamente los `padres` e `hijos` del otro teléfono
- **Fase 4:** Migrar de modo prueba a reglas de seguridad definitivas en Firestore
- **Fase 5:** Badge visual en la UI cuando llega un cambio del otro padre en tiempo real
