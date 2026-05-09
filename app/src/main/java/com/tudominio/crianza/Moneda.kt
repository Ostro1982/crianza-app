package com.tudominio.crianza

import java.text.NumberFormat
import java.util.Locale

/**
 * Formateo de moneda configurable a nivel familia (config.moneda).
 *
 * Soporta los códigos ISO 4217 más comunes para LATAM + USD/EUR.
 * Si la moneda es desconocida, fallback a símbolo genérico "$" + número.
 */
/**
 * Cache global de la moneda actual de la familia. Se setea en MainActivity al
 * cargar la config y al guardarla. Lo leen los formateos sin tener que pasar el
 * código por toda la jerarquía Compose.
 */
object MonedaConfig {
    @Volatile var actual: String = "ARS"
}

object Moneda {

    // Locale por moneda — mantiene formato local correcto (separador de miles, decimales).
    private val locales = mapOf(
        "ARS" to Locale("es", "AR"),
        "MXN" to Locale("es", "MX"),
        "CLP" to Locale("es", "CL"),
        "COP" to Locale("es", "CO"),
        "PEN" to Locale("es", "PE"),
        "UYU" to Locale("es", "UY"),
        "USD" to Locale("en", "US"),
        "EUR" to Locale("es", "ES")
    )

    private val simbolos = mapOf(
        "ARS" to "$",
        "MXN" to "$",
        "CLP" to "$",
        "COP" to "$",
        "PEN" to "S/",
        "UYU" to "\$U",
        "USD" to "US$",
        "EUR" to "€"
    )

    fun simbolo(codigo: String): String = simbolos[codigo.uppercase()] ?: "$"

    /**
     * Formatea un monto con el símbolo y formato de la moneda configurada.
     * CLP/COP no usan decimales; el resto usa 2.
     */
    fun formatear(monto: Double, codigo: String): String {
        val cod = codigo.uppercase()
        val locale = locales[cod] ?: Locale.getDefault()
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = if (cod == "CLP" || cod == "COP") 0 else 2
        nf.maximumFractionDigits = nf.minimumFractionDigits
        return "${simbolo(cod)} ${nf.format(monto)}"
    }

    /** Versión corta para listas/widgets: sin decimales. */
    fun formatearCorto(monto: Double, codigo: String): String {
        val cod = codigo.uppercase()
        val locale = locales[cod] ?: Locale.getDefault()
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        return "${simbolo(cod)} ${nf.format(monto)}"
    }
}
