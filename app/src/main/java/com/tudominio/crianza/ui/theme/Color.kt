package com.tudominio.crianza.ui.theme

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

// Neutrals — Cálidos beige
val Neutral10  = Color(0xFF2D2A1E)   // Texto principal
val Neutral20  = Color(0xFF4A4636)
val Neutral90  = Color(0xFFE8E2D6)
val Neutral95  = Color(0xFFF2ECD8)
val Neutral99  = Color(0xFFF8F4EA)   // Fondo principal — beige cálido

val NeutralVariant30 = Color(0xFF5A5636)
val NeutralVariant50 = Color(0xFF8A8460)
val NeutralVariant60 = Color(0xFF9A946E)
val NeutralVariant80 = Color(0xFFD0C8A8)
val NeutralVariant90 = Color(0xFFE8E0C8)

// ── Glass / overlay utilities ────────────────────────────────────────────────
val GlassWhite       = Color(0xB8FFFFFF) // 72% — card base (warm translucent white)
val GlassWhiteHeavy  = Color(0xD9FFFFFF) // 85% — card hover / active

// Orbe colors (decorativos, gradientes difusos de fondo)
val OrbOlive   = Color(0x40648646) // verde oliva difuso
val OrbAmber   = Color(0x30A09640) // ámbar dorado difuso
val OrbSage    = Color(0x30649064) // salvia difuso

// Gradientes de fondo principal
val GradientStart  = Color(0xFFF0EAD4)
val GradientMid    = Color(0xFFDDE0C0)
val GradientEnd    = Color(0xFFC4CFAA)

// Fondo principal — gradiente cálido beige→verde suave
val BgGradientMain = Brush.verticalGradient(listOf(
    Color(0xFFF0EAD4),
    Color(0xFFDDE0C0),
    Color(0xFFC4CFAA),
    Color(0xFFD8D4B8),
    Color(0xFFEFE8D2)
))

// Fondo compartido pantallas internas
val BgGradient = Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))

// Fondos internos — todos en la familia beige/verde/oliva cálido
val BgGrad0 = Brush.verticalGradient(listOf(Color(0xFFD4EDCA), Color(0xFFC0D8B0))) // verde suave (Tiempo)
val BgGrad1 = Brush.verticalGradient(listOf(Color(0xFFF0E8D0), Color(0xFFD8CCA8))) // ámbar (Calendario)
val BgGrad2 = Brush.verticalGradient(listOf(Color(0xFFEDE8D8), Color(0xFFD8D0B8))) // beige cálido (Gastos)
val BgGrad3 = Brush.verticalGradient(listOf(Color(0xFFD0ECDA), Color(0xFFB8D8C0))) // salvia (Compensación)
val BgGrad4 = Brush.verticalGradient(listOf(Color(0xFFE8E4D0), Color(0xFFD4CCAC))) // arena (Compras)
val BgGrad5 = Brush.verticalGradient(listOf(Color(0xFFE0ECC0), Color(0xFFC8D8A0))) // lima (Pendientes)
val BgGrad6 = Brush.verticalGradient(listOf(Color(0xFFDCE8D4), Color(0xFFC4D4BC))) // verde pálido (Mensajes)
val BgGrad7 = Brush.verticalGradient(listOf(Color(0xFFF0E0D0), Color(0xFFD8C0A8))) // naranja tierra (Recuerdos)
val BgGrad8 = Brush.verticalGradient(listOf(Color(0xFFE4E0D0), Color(0xFFD0C8B0))) // lino (Documentos/Información)

// Header card
val HeaderCardGrad = Brush.linearGradient(listOf(Color(0xFFE8E0C8), Color(0xFFD8D4B8)))

// Tarjetas del menú — tonos suaves, cada módulo con identidad
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
