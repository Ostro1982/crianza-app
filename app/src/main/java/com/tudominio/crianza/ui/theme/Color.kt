package com.tudominio.crianza.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Paleta Oliva Cálida — warm minimal, fondo beige-verde, cards blancos ─────

// Primary — Verde oliva (confianza, naturaleza)
val Indigo10  = Color(0xFF1A2E10)
val Indigo20  = Color(0xFF2D4A1E)
val Indigo30  = Color(0xFF3E6628)
val Indigo40  = Color(0xFF4A7A2A)   // Primary — olive green
val Indigo80  = Color(0xFF8BBF6A)   // Primary dark variant
val Indigo90  = Color(0xFFD4EDCA)   // PrimaryContainer light
val Indigo100 = Color(0xFFFFFFFF)

// Secondary — Ámbar dorado (calidez, hogar)
val Rose10  = Color(0xFF2A1E08)
val Rose20  = Color(0xFF4A3510)
val Rose30  = Color(0xFF6B5020)
val Rose40  = Color(0xFF8B7640)     // Secondary — warm amber
val Rose80  = Color(0xFFD4B870)     // Secondary dark
val Rose90  = Color(0xFFF0E8D0)     // SecondaryContainer light
val Rose100 = Color(0xFFFFFFFF)

// Tertiary — Verde salvia (frescura, calma)
val Teal10  = Color(0xFF0A2018)
val Teal20  = Color(0xFF183828)
val Teal30  = Color(0xFF2A5040)
val Teal40  = Color(0xFF4A7A5A)     // Tertiary — sage green
val Teal80  = Color(0xFF7AB88A)     // Tertiary dark
val Teal90  = Color(0xFFD0ECDA)     // TertiaryContainer light
val Teal100 = Color(0xFFFFFFFF)

// Error
val Red10  = Color(0xFF410002)
val Red40  = Color(0xFFC05050)
val Red80  = Color(0xFFE8A0A0)
val Red90  = Color(0xFFFDE0E0)

// Neutrals — Cálidos beige (valores light literales)
private val Neutral10Light  = Color(0xFF2D2A1E)
private val Neutral20Light  = Color(0xFF4A4636)
private val Neutral90Light  = Color(0xFFE8E2D6)
private val Neutral95Light  = Color(0xFFF2ECD8)
private val Neutral99Light  = Color(0xFFF8F4EA)
private val NeutralVariant30Light = Color(0xFF5A5636)
private val NeutralVariant50Light = Color(0xFF8A8460)
private val NeutralVariant60Light = Color(0xFF9A946E)
private val NeutralVariant80Light = Color(0xFFD0C8A8)
private val NeutralVariant90Light = Color(0xFFE8E0C8)

// Valores dark — invertidos para legibilidad sobre fondos oscuros
private val Neutral10Dark  = Color(0xFFEAE4D6)   // texto principal claro
private val Neutral20Dark  = Color(0xFFD0CAB8)
private val Neutral90Dark  = Color(0xFF2A2820)
private val Neutral95Dark  = Color(0xFF1F1D14)
private val Neutral99Dark  = Color(0xFF14130E)
private val NeutralVariant30Dark = Color(0xFFC4BE9C)
private val NeutralVariant50Dark = Color(0xFFA09A78)
private val NeutralVariant60Dark = Color(0xFF8E886A)
private val NeutralVariant80Dark = Color(0xFF4A4636)
private val NeutralVariant90Dark = Color(0xFF302C20)

// @Composable getters — usados en pantallas. Se adaptan según LocalIsDark.
val Neutral10: Color  @Composable get() = if (LocalIsDark.current) Neutral10Dark else Neutral10Light
val Neutral20: Color  @Composable get() = if (LocalIsDark.current) Neutral20Dark else Neutral20Light
val Neutral90: Color  @Composable get() = if (LocalIsDark.current) Neutral90Dark else Neutral90Light
val Neutral95: Color  @Composable get() = if (LocalIsDark.current) Neutral95Dark else Neutral95Light
val Neutral99: Color  @Composable get() = if (LocalIsDark.current) Neutral99Dark else Neutral99Light
val NeutralVariant30: Color @Composable get() = if (LocalIsDark.current) NeutralVariant30Dark else NeutralVariant30Light
val NeutralVariant50: Color @Composable get() = if (LocalIsDark.current) NeutralVariant50Dark else NeutralVariant50Light
val NeutralVariant60: Color @Composable get() = if (LocalIsDark.current) NeutralVariant60Dark else NeutralVariant60Light
val NeutralVariant80: Color @Composable get() = if (LocalIsDark.current) NeutralVariant80Dark else NeutralVariant80Light
val NeutralVariant90: Color @Composable get() = if (LocalIsDark.current) NeutralVariant90Dark else NeutralVariant90Light

// ── Glass / overlay utilities ────────────────────────────────────────────────
// En dark cambia a un blanco sutil sobre fondo oscuro (queda como gris claro).
val GlassWhite: Color
    @Composable get() = if (LocalIsDark.current) Color(0x33FFFFFF) else Color(0xB8FFFFFF)
val GlassWhiteHeavy: Color
    @Composable get() = if (LocalIsDark.current) Color(0x55FFFFFF) else Color(0xD9FFFFFF)

// Orbe colors (decorativos, gradientes difusos de fondo)
val OrbOlive   = Color(0x40648646) // verde oliva difuso
val OrbAmber   = Color(0x30A09640) // ámbar dorado difuso
val OrbSage    = Color(0x30649064) // salvia difuso

// Gradientes de fondo principal
val GradientStart  = Color(0xFFF0EAD4)
val GradientMid    = Color(0xFFDDE0C0)
val GradientEnd    = Color(0xFFC4CFAA)

// ── Light gradients (privados, se acceden via @Composable getters) ────────────
private val BgGradientMainLight = Brush.verticalGradient(listOf(
    Color(0xFFF0EAD4),
    Color(0xFFDDE0C0),
    Color(0xFFC4CFAA),
    Color(0xFFD8D4B8),
    Color(0xFFEFE8D2)
))
private val BgGradientLight   = Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))
private val BgGrad0Light = Brush.verticalGradient(listOf(Color(0xFFD4EDCA), Color(0xFFC0D8B0)))
private val BgGrad1Light = Brush.verticalGradient(listOf(Color(0xFFF0E8D0), Color(0xFFD8CCA8)))
private val BgGrad2Light = Brush.verticalGradient(listOf(Color(0xFFEDE8D8), Color(0xFFD8D0B8)))
private val BgGrad3Light = Brush.verticalGradient(listOf(Color(0xFFD0ECDA), Color(0xFFB8D8C0)))
private val BgGrad4Light = Brush.verticalGradient(listOf(Color(0xFFE8E4D0), Color(0xFFD4CCAC)))
private val BgGrad5Light = Brush.verticalGradient(listOf(Color(0xFFE0ECC0), Color(0xFFC8D8A0)))
private val BgGrad6Light = Brush.verticalGradient(listOf(Color(0xFFDCE8D4), Color(0xFFC4D4BC)))
private val BgGrad7Light = Brush.verticalGradient(listOf(Color(0xFFF0E0D0), Color(0xFFD8C0A8)))
private val BgGrad8Light = Brush.verticalGradient(listOf(Color(0xFFE4E0D0), Color(0xFFD0C8B0)))
private val HeaderCardGradLight = Brush.linearGradient(listOf(Color(0xFFE8E0C8), Color(0xFFD8D4B8)))

// ── Dark variants — mismas familias de hue pero con luminosidad muy baja ──────
private val BgGradientMainDark = Brush.verticalGradient(listOf(
    Color(0xFF1A1A12),
    Color(0xFF181812),
    Color(0xFF14161A),
    Color(0xFF161614),
    Color(0xFF1A1A12)
))
private val BgGradientDark   = Brush.verticalGradient(listOf(Color(0xFF1A1A12), Color(0xFF161812), Color(0xFF14160F)))
private val BgGrad0Dark = Brush.verticalGradient(listOf(Color(0xFF1A2A14), Color(0xFF12200E))) // verde
private val BgGrad1Dark = Brush.verticalGradient(listOf(Color(0xFF2A220E), Color(0xFF1F1A0A))) // ámbar
private val BgGrad2Dark = Brush.verticalGradient(listOf(Color(0xFF1F1D15), Color(0xFF181610))) // beige
private val BgGrad3Dark = Brush.verticalGradient(listOf(Color(0xFF142820), Color(0xFF0E1F18))) // salvia
private val BgGrad4Dark = Brush.verticalGradient(listOf(Color(0xFF1F1D12), Color(0xFF18160E))) // arena
private val BgGrad5Dark = Brush.verticalGradient(listOf(Color(0xFF1A220E), Color(0xFF12190A))) // lima
private val BgGrad6Dark = Brush.verticalGradient(listOf(Color(0xFF14241A), Color(0xFF0E1A12))) // verde pálido
private val BgGrad7Dark = Brush.verticalGradient(listOf(Color(0xFF241810), Color(0xFF1A120A))) // naranja
private val BgGrad8Dark = Brush.verticalGradient(listOf(Color(0xFF1D1A12), Color(0xFF15120C))) // lino
private val HeaderCardGradDark = Brush.linearGradient(listOf(Color(0xFF1F1D14), Color(0xFF18160F)))

// ── Public Composable getters — eligen variante según LocalIsDark ─────────────
val BgGradientMain: Brush
    @Composable get() = if (LocalIsDark.current) BgGradientMainDark else BgGradientMainLight
val BgGradient: Brush
    @Composable get() = if (LocalIsDark.current) BgGradientDark else BgGradientLight
val BgGrad0: Brush @Composable get() = if (LocalIsDark.current) BgGrad0Dark else BgGrad0Light
val BgGrad1: Brush @Composable get() = if (LocalIsDark.current) BgGrad1Dark else BgGrad1Light
val BgGrad2: Brush @Composable get() = if (LocalIsDark.current) BgGrad2Dark else BgGrad2Light
val BgGrad3: Brush @Composable get() = if (LocalIsDark.current) BgGrad3Dark else BgGrad3Light
val BgGrad4: Brush @Composable get() = if (LocalIsDark.current) BgGrad4Dark else BgGrad4Light
val BgGrad5: Brush @Composable get() = if (LocalIsDark.current) BgGrad5Dark else BgGrad5Light
val BgGrad6: Brush @Composable get() = if (LocalIsDark.current) BgGrad6Dark else BgGrad6Light
val BgGrad7: Brush @Composable get() = if (LocalIsDark.current) BgGrad7Dark else BgGrad7Light
val BgGrad8: Brush @Composable get() = if (LocalIsDark.current) BgGrad8Dark else BgGrad8Light
val HeaderCardGrad: Brush @Composable get() = if (LocalIsDark.current) HeaderCardGradDark else HeaderCardGradLight

// Tarjetas del menú — tonos suaves, cada módulo con identidad. Stay constant; bien visibles en ambos modos.
val CardGrad0 = Brush.linearGradient(listOf(Color(0xFF6B8E3A), Color(0xFF8AAE4A))) // verde oliva (Tiempo)
val CardGrad1 = Brush.linearGradient(listOf(Color(0xFF8B7640), Color(0xFFB8960A))) // ámbar (Calendario)
val CardGrad2 = Brush.linearGradient(listOf(Color(0xFFC05050), Color(0xFFD87070))) // rojo cálido (Gastos)
val CardGrad3 = Brush.linearGradient(listOf(Color(0xFF4A7A5A), Color(0xFF6A9A70))) // salvia (Compensación)
val CardGrad4 = Brush.linearGradient(listOf(Color(0xFFA06080), Color(0xFFC08090))) // rosa seco (Compras)
val CardGrad5 = Brush.linearGradient(listOf(Color(0xFF6A8A30), Color(0xFF8AAA40))) // lima (Pendientes)
val CardGrad6 = Brush.linearGradient(listOf(Color(0xFF4A7AAE), Color(0xFF6A9AC8))) // azul cielo (Mensajes)
val CardGrad7 = Brush.linearGradient(listOf(Color(0xFFA07040), Color(0xFFC09060))) // naranja tierra (Recuerdos)
val CardGrad8 = Brush.linearGradient(listOf(Color(0xFF7A60A0), Color(0xFF9A80B8))) // lavanda (Documentos)

// FAB gradiente — oliva
val FabGradient = Brush.linearGradient(listOf(Color(0xFF6B8E3A), Color(0xFF4A7A2A)))
