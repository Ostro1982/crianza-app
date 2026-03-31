package com.tudominio.crianza.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Paleta principal — Indigo cálido + Rosa familia + Verde esmeralda ─────────

// Primary — Indigo profundo (confianza, serenidad)
val Indigo10  = Color(0xFF0D0E38)
val Indigo20  = Color(0xFF1A1A6E)
val Indigo30  = Color(0xFF2929A6)
val Indigo40  = Color(0xFF4F46E5)  // Primary light
val Indigo80  = Color(0xFFADAFF8)  // Primary dark
val Indigo90  = Color(0xFFE0E0FF)  // PrimaryContainer light
val Indigo100 = Color(0xFFFFFFFF)

// Secondary — Rosa cálido (amor, familia)
val Rose10  = Color(0xFF3E0019)
val Rose20  = Color(0xFF6B0031)
val Rose30  = Color(0xFF9A1057)
val Rose40  = Color(0xFFBF4B76)   // Secondary light
val Rose80  = Color(0xFFFFB1C7)   // Secondary dark
val Rose90  = Color(0xFFFFD9E4)   // SecondaryContainer light
val Rose100 = Color(0xFFFFFFFF)

// Tertiary — Esmeralda (crecimiento, naturaleza)
val Teal10  = Color(0xFF00201A)
val Teal20  = Color(0xFF003730)
val Teal30  = Color(0xFF005046)
val Teal40  = Color(0xFF2A9D7F)   // Tertiary light
val Teal80  = Color(0xFF5ADBB9)   // Tertiary dark
val Teal90  = Color(0xFFB5F0D8)   // TertiaryContainer light
val Teal100 = Color(0xFFFFFFFF)

// Error
val Red10  = Color(0xFF410002)
val Red40  = Color(0xFFBA1A1A)
val Red80  = Color(0xFFFFB4AB)
val Red90  = Color(0xFFFFDAD6)

// Neutrals
val Neutral10  = Color(0xFF1B1B1F)
val Neutral20  = Color(0xFF2F3033)
val Neutral90  = Color(0xFFE4E1E6)
val Neutral95  = Color(0xFFF3EFF4)
val Neutral99  = Color(0xFFFEFBFF)

val NeutralVariant30 = Color(0xFF48454E)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant60 = Color(0xFF938F99)
val NeutralVariant80 = Color(0xFFCAC4D0)
val NeutralVariant90 = Color(0xFFE7E0EB)

// Colores especiales para gradientes de pantallas
val GradientStart  = Color(0xFF6366F1) // Indigo 500
val GradientMid    = Color(0xFF8B5CF6) // Violet 500
val GradientEnd    = Color(0xFFC084FC) // Purple 400
val GlassWhite     = Color(0x33FFFFFF) // Blanco 20% — glassmorphism
val GlassWhiteHeavy= Color(0x4DFFFFFF) // Blanco 30%

// Fondo gradiente compartido — pantallas internas
val BgGradient = Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))

// Fondo principal — dark navy profundo
val BgGradientMain = Brush.verticalGradient(listOf(
    Color(0xFF0F0E1A),
    Color(0xFF16152B),
    Color(0xFF1C1B3A)
))

// Fondos internos — mismos colores exactos que la tarjeta, dirección vertical
val BgGrad0 = Brush.verticalGradient(listOf(Color(0xFF0A6B5E), Color(0xFF14B8A6))) // teal (Tiempo)
val BgGrad1 = Brush.verticalGradient(listOf(Color(0xFF92400E), Color(0xFFF59E0B))) // ámbar (Calendario)
val BgGrad2 = Brush.verticalGradient(listOf(Color(0xFF991B1B), Color(0xFFF87171))) // rojo (Gastos)
val BgGrad3 = Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF10B981))) // esmeralda (Compensación)
val BgGrad4 = Brush.verticalGradient(listOf(Color(0xFF831843), Color(0xFFF472B6))) // rosa (Compras)
val BgGrad5 = Brush.verticalGradient(listOf(Color(0xFF3F6212), Color(0xFF84CC16))) // lima (Pendientes)
val BgGrad6 = Brush.verticalGradient(listOf(Color(0xFF075985), Color(0xFF38BDF8))) // azul cielo (Mensajes)
val BgGrad7 = Brush.verticalGradient(listOf(Color(0xFF9A3412), Color(0xFFFB923C))) // naranja (Recuerdos)
val BgGrad8 = Brush.verticalGradient(listOf(Color(0xFF4C1D95), Color(0xFFA855F7))) // violeta (Documentos)

// Header pantalla principal
val HeaderCardGrad = Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81)))

// Tarjetas del menú — colores saturados, sin azul/violeta repetido
val CardGrad0 = Brush.linearGradient(listOf(Color(0xFF0A6B5E), Color(0xFF14B8A6))) // teal (Tiempo)
val CardGrad1 = Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFFF59E0B))) // ámbar (Calendario)
val CardGrad2 = Brush.linearGradient(listOf(Color(0xFF991B1B), Color(0xFFF87171))) // rojo (Gastos)
val CardGrad3 = Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF10B981))) // esmeralda (Compensación)
val CardGrad4 = Brush.linearGradient(listOf(Color(0xFF831843), Color(0xFFF472B6))) // rosa (Compras)
val CardGrad5 = Brush.linearGradient(listOf(Color(0xFF3F6212), Color(0xFF84CC16))) // lima (Pendientes)
val CardGrad6 = Brush.linearGradient(listOf(Color(0xFF075985), Color(0xFF38BDF8))) // azul cielo (Mensajes)
val CardGrad7 = Brush.linearGradient(listOf(Color(0xFF9A3412), Color(0xFFFB923C))) // naranja (Recuerdos)
val CardGrad8 = Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFFA855F7))) // violeta (Documentos)
