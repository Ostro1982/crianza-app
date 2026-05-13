# Graph Report - .  (2026-04-19)

## Corpus Check
- Corpus is ~46,915 words - fits in a single context window. You may not need a graph.

## Summary
- 432 nodes · 390 edges · 51 communities detected
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Sincronización Firestore|Sincronización Firestore]]
- [[_COMMUNITY_DAOs Compras y Mensajes|DAOs Compras y Mensajes]]
- [[_COMMUNITY_Base de Datos y Family ID|Base de Datos y Family ID]]
- [[_COMMUNITY_DAOs Configuración|DAOs Configuración]]
- [[_COMMUNITY_AppDatabase Room|AppDatabase Room]]
- [[_COMMUNITY_MainActivity y UI|MainActivity y UI]]
- [[_COMMUNITY_Modelos de Datos|Modelos de Datos]]
- [[_COMMUNITY_Pantalla Compensación|Pantalla Compensación]]
- [[_COMMUNITY_Utilidades y Cálculos|Utilidades y Cálculos]]
- [[_COMMUNITY_DAO Gastos|DAO Gastos]]
- [[_COMMUNITY_DAO Eventos|DAO Eventos]]
- [[_COMMUNITY_DAO Integración|DAO Integración]]
- [[_COMMUNITY_Notificaciones|Notificaciones]]
- [[_COMMUNITY_FamilyIdManager Vinculación|FamilyIdManager Vinculación]]
- [[_COMMUNITY_DAO Mensajes|DAO Mensajes]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]

## God Nodes (most connected - your core abstractions)
1. `SyncManager` - 38 edges
2. `AppDatabase` - 15 edges
3. `SyncManager` - 13 edges
4. `ItemCompraDao` - 11 edges
5. `GastoDao` - 10 edges
6. `Plan SincronizaciÃ³n en Tiempo Real Firestore` - 10 edges
7. `EventoDao` - 9 edges
8. `NotificacionHelper` - 9 edges
9. `MensajeDao` - 9 edges
10. `RegistroTiempoDao` - 8 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Sincronización Firestore"
Cohesion: 0.05
Nodes (2): ListenerCallbacks, SyncManager

### Community 1 - "DAOs Compras y Mensajes"
Cohesion: 0.07
Nodes (4): CategoriaCompraDao, DocumentoDao, ItemCompraDao, PendienteDao

### Community 2 - "Base de Datos y Family ID"
Cohesion: 0.1
Nodes (27): AppDatabase, EventoDao, Family ID (UUID Ãºnico por familia), FamilyIdManager, Fase 2: Sincronizar registros_tiempo, compensaciones, recuerdos, Fase 3: Auto-descarga padres e hijos al vincular, Fase 4: Migrar a reglas de seguridad definitivas Firestore, Fase 5: Badge visual para cambios en tiempo real (+19 more)

### Community 3 - "DAOs Configuración"
Cohesion: 0.08
Nodes (4): ConfiguracionDao, FamiliaDao, RecuerdoDao, RegistroTiempoDao

### Community 4 - "AppDatabase Room"
Cohesion: 0.11
Nodes (1): AppDatabase

### Community 5 - "MainActivity y UI"
Cohesion: 0.11
Nodes (2): ItemMenuPrincipal, MainActivity

### Community 6 - "Modelos de Datos"
Cohesion: 0.11
Nodes (17): CategoriaCompra, Compensacion, ConfiguracionCompensacion, ConfiguracionIntegracion, ConfiguracionTiempo, Documento, Evento, FiltroEmail (+9 more)

### Community 7 - "Pantalla Compensación"
Cohesion: 0.11
Nodes (0): 

### Community 8 - "Utilidades y Cálculos"
Cohesion: 0.14
Nodes (1): Periodo

### Community 9 - "DAO Gastos"
Cohesion: 0.18
Nodes (1): GastoDao

### Community 10 - "DAO Eventos"
Cohesion: 0.2
Nodes (1): EventoDao

### Community 11 - "DAO Integración"
Cohesion: 0.2
Nodes (2): ConfiguracionIntegracionDao, FiltroEmailDao

### Community 12 - "Notificaciones"
Cohesion: 0.2
Nodes (1): NotificacionHelper

### Community 13 - "FamilyIdManager Vinculación"
Cohesion: 0.22
Nodes (1): FamilyIdManager

### Community 14 - "DAO Mensajes"
Cohesion: 0.22
Nodes (1): MensajeDao

### Community 15 - "Community 15"
Cohesion: 0.22
Nodes (0): 

### Community 16 - "Community 16"
Cohesion: 0.25
Nodes (2): ClimaData, ClimaService

### Community 17 - "Community 17"
Cohesion: 0.25
Nodes (1): CompensacionDao

### Community 18 - "Community 18"
Cohesion: 0.25
Nodes (3): ComandoParsado, MensajeParsado, TelegramService

### Community 19 - "Community 19"
Cohesion: 0.25
Nodes (0): 

### Community 20 - "Community 20"
Cohesion: 0.29
Nodes (2): GoogleAuthHelper, UsuarioGoogle

### Community 21 - "Community 21"
Cohesion: 0.29
Nodes (2): CalendarioDispositivo, GoogleCalendarService

### Community 22 - "Community 22"
Cohesion: 0.29
Nodes (2): EventoIcal, ICalParser

### Community 23 - "Community 23"
Cohesion: 0.29
Nodes (1): WhatsAppListenerService

### Community 24 - "Community 24"
Cohesion: 0.33
Nodes (2): ActualizacionChecker, ActualizacionInfo

### Community 25 - "Community 25"
Cohesion: 0.33
Nodes (2): EmailParsado, EmailService

### Community 26 - "Community 26"
Cohesion: 0.33
Nodes (0): 

### Community 27 - "Community 27"
Cohesion: 0.33
Nodes (0): 

### Community 28 - "Community 28"
Cohesion: 0.33
Nodes (2): HijoFormulario, PersonaFormulario

### Community 29 - "Community 29"
Cohesion: 0.33
Nodes (1): SincronizacionWorker

### Community 30 - "Community 30"
Cohesion: 0.4
Nodes (1): DetectorEventosNaturales

### Community 31 - "Community 31"
Cohesion: 0.4
Nodes (1): EncriptadorDocumentos

### Community 32 - "Community 32"
Cohesion: 0.4
Nodes (0): 

### Community 33 - "Community 33"
Cohesion: 0.4
Nodes (0): 

### Community 34 - "Community 34"
Cohesion: 0.5
Nodes (1): Converters

### Community 35 - "Community 35"
Cohesion: 0.5
Nodes (0): 

### Community 36 - "Community 36"
Cohesion: 0.5
Nodes (0): 

### Community 37 - "Community 37"
Cohesion: 0.5
Nodes (1): RecordatoriosWorker

### Community 38 - "Community 38"
Cohesion: 0.67
Nodes (1): ExampleInstrumentedTest

### Community 39 - "Community 39"
Cohesion: 0.67
Nodes (0): 

### Community 40 - "Community 40"
Cohesion: 0.67
Nodes (0): 

### Community 41 - "Community 41"
Cohesion: 0.67
Nodes (1): ProcesadorComandos

### Community 42 - "Community 42"
Cohesion: 0.67
Nodes (1): ExampleUnitTest

### Community 43 - "Community 43"
Cohesion: 1.0
Nodes (0): 

### Community 44 - "Community 44"
Cohesion: 1.0
Nodes (0): 

### Community 45 - "Community 45"
Cohesion: 1.0
Nodes (0): 

### Community 46 - "Community 46"
Cohesion: 1.0
Nodes (0): 

### Community 47 - "Community 47"
Cohesion: 1.0
Nodes (0): 

### Community 48 - "Community 48"
Cohesion: 1.0
Nodes (0): 

### Community 49 - "Community 49"
Cohesion: 1.0
Nodes (0): 

### Community 50 - "Community 50"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **45 isolated node(s):** `ActualizacionInfo`, `ClimaData`, `EmailParsado`, `UsuarioGoogle`, `CalendarioDispositivo` (+40 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 43`** (2 nodes): `PantallaGoogle.kt`, `PantallaGoogle()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 44`** (2 nodes): `PantallaVincular.kt`, `PantallaVincular()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 45`** (2 nodes): `Theme.kt`, `CrianzaTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 46`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 47`** (1 nodes): `settings.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 48`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 49`** (1 nodes): `Color.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 50`** (1 nodes): `Type.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MensajeDao` connect `DAO Mensajes` to `DAOs Compras y Mensajes`?**
  _High betweenness centrality (0.003) - this node is a cross-community bridge._
- **What connects `ActualizacionInfo`, `ClimaData`, `EmailParsado` to the rest of the system?**
  _45 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Sincronización Firestore` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `DAOs Compras y Mensajes` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Base de Datos y Family ID` be split into smaller, more focused modules?**
  _Cohesion score 0.1 - nodes in this community are weakly interconnected._
- **Should `DAOs Configuración` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._
- **Should `AppDatabase Room` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._