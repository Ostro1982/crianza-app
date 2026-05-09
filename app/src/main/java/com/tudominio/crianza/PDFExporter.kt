package com.tudominio.crianza

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generación de PDFs con valor de evidencia. Cada PDF incluye:
 * - Header con marca Nesty, familia y rango.
 * - Cuerpo tabular de la evidencia (custodia o gastos).
 * - Footer en la última página con SHA-256 del contenido del cuerpo + fecha/hora gen + familyId.
 *
 * El hash se calcula sobre el texto serializado del reporte (no del PDF binario), de
 * forma que se pueda regenerar y comparar desde el registro Firestore vía oficio.
 *
 * El PDF NO está firmado digitalmente (eso requiere CA reconocida). El valor legal
 * surge de poder cotejar el hash contra los datos almacenados en Firestore con
 * `serverTimestamp` inmutable, mediante oficio del juzgado al proveedor del servicio.
 */
object PDFExporter {

    private const val PAGE_W = 595 // A4 portrait points
    private const val PAGE_H = 842
    private const val MARGEN = 36

    private val sdfDoc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())

    // ── Custodia ─────────────────────────────────────────────────────────────

    suspend fun exportarCustodia(
        context: Context,
        destino: Uri,
        registros: List<RegistroTiempo>,
        padres: List<Padre>,
        hijos: List<Hijo>,
        familyId: String,
        rango: Rango
    ): Result<Unit> = runCatching {
        val filtrados = registros.filter { it.fecha in rango.desde..rango.hasta }
            .sortedBy { it.fecha }

        // Texto canonicalizado para hash. Cualquier alteración cambia el hash.
        val cuerpoTexto = buildString {
            append("CUSTODIA|familia=$familyId|desde=${rango.desde}|hasta=${rango.hasta}\n")
            filtrados.forEach { r ->
                append("${r.fecha}|${r.idHijo}|${r.idPadre}|${r.horaInicio}|${r.horaFin}|${r.autocompensado}\n")
            }
        }
        val hash = sha256(cuerpoTexto)

        // Conteo días por padre (días distintos con al menos 1 registro)
        val diasPorPadre = filtrados.groupBy { it.idPadre }
            .mapValues { (_, regs) -> regs.map { it.fecha }.toSet().size }
        val totalDias = diasPorPadre.values.sum().coerceAtLeast(1)

        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        var page = doc.startPage(info)
        var canvas = page.canvas
        var y = MARGEN

        val pTitulo = Paint().apply { color = Color.BLACK; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pSubt = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val pTexto = Paint().apply { color = Color.BLACK; textSize = 10f }
        val pHead = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pFooter = Paint().apply { color = Color.GRAY; textSize = 8f }

        canvas.drawText("Resumen de días con los chicos — Nesty", MARGEN.toFloat(), (y + 14).toFloat(), pTitulo); y += 24
        canvas.drawText("Familia: $familyId", MARGEN.toFloat(), y.toFloat(), pSubt); y += 14
        canvas.drawText("Período: ${rango.desde} a ${rango.hasta}", MARGEN.toFloat(), y.toFloat(), pSubt); y += 18

        // Resumen porcentajes
        canvas.drawText("Resumen del período", MARGEN.toFloat(), y.toFloat(), pHead); y += 14
        padres.forEach { p ->
            val dias = diasPorPadre[p.id] ?: 0
            val pct = if (totalDias > 0) dias * 100 / totalDias else 0
            canvas.drawText("${p.nombre}: $dias días ($pct%)", MARGEN.toFloat(), y.toFloat(), pTexto); y += 13
        }
        y += 10

        // Tabla
        canvas.drawText("Detalle (cronológico)", MARGEN.toFloat(), y.toFloat(), pHead); y += 14
        canvas.drawText("Fecha", MARGEN.toFloat(), y.toFloat(), pHead)
        canvas.drawText("Hijo", (MARGEN + 90).toFloat(), y.toFloat(), pHead)
        canvas.drawText("Padre", (MARGEN + 220).toFloat(), y.toFloat(), pHead)
        canvas.drawText("Horario", (MARGEN + 350).toFloat(), y.toFloat(), pHead)
        canvas.drawText("Autocomp.", (MARGEN + 460).toFloat(), y.toFloat(), pHead)
        y += 12

        for (r in filtrados) {
            if (y > PAGE_H - 80) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
                canvas = page.canvas; y = MARGEN
            }
            canvas.drawText(r.fecha, MARGEN.toFloat(), y.toFloat(), pTexto)
            canvas.drawText(r.nombreHijo.take(18), (MARGEN + 90).toFloat(), y.toFloat(), pTexto)
            canvas.drawText(r.nombrePadre.take(18), (MARGEN + 220).toFloat(), y.toFloat(), pTexto)
            canvas.drawText("${r.horaInicio}–${r.horaFin}", (MARGEN + 350).toFloat(), y.toFloat(), pTexto)
            if (r.autocompensado) canvas.drawText("Sí", (MARGEN + 460).toFloat(), y.toFloat(), pTexto)
            y += 12
        }

        // Footer con hash en la última página
        val genTime = sdfDoc.format(Date())
        canvas.drawText("Generado: $genTime", MARGEN.toFloat(), (PAGE_H - 30).toFloat(), pFooter)
        canvas.drawText("Firma SHA-256: $hash", MARGEN.toFloat(), (PAGE_H - 18).toFloat(), pFooter)
        canvas.drawText("Generado por Nesty. La firma SHA-256 permite verificar el contenido.",
            MARGEN.toFloat(), (PAGE_H - 8).toFloat(), pFooter)
        doc.finishPage(page)

        val bytes = ByteArrayOutputStream().also { doc.writeTo(it) }.toByteArray()
        doc.close()

        context.contentResolver.openOutputStream(destino)?.use { it.write(bytes) }
            ?: error("No se pudo abrir destino")
        Unit
    }

    // ── Gastos ───────────────────────────────────────────────────────────────

    suspend fun exportarGastos(
        context: Context,
        destino: Uri,
        gastos: List<Gasto>,
        compensaciones: List<Compensacion>,
        familyId: String,
        rango: Rango,
        moneda: String
    ): Result<Unit> = runCatching {
        val filtrados = gastos.filter { it.fecha in rango.desde..rango.hasta }
            .sortedBy { it.fecha }
        val totalAcumulado = filtrados.sumOf { it.monto }

        val cuerpoTexto = buildString {
            append("GASTOS|familia=$familyId|desde=${rango.desde}|hasta=${rango.hasta}|moneda=$moneda\n")
            filtrados.forEach { g ->
                append("${g.id}|${g.fecha}|${g.descripcion}|${g.idPagador}|${g.monto}|${g.categoria}|${g.autocompensado}|recibo=${g.reciboFotoUri.isNotEmpty()}\n")
            }
            append("---\n")
            compensaciones.forEach { c ->
                append("COMP|${c.id}|${c.fechaCompleta}|${c.montoTotal}|${c.aceptadoPadre1}|${c.aceptadoPadre2}|${c.tipoCompensacion}\n")
            }
        }
        val hash = sha256(cuerpoTexto)

        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        var page = doc.startPage(info)
        var canvas = page.canvas
        var y = MARGEN

        val pTitulo = Paint().apply { color = Color.BLACK; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pSubt = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val pTexto = Paint().apply { color = Color.BLACK; textSize = 10f }
        val pHead = Paint().apply { color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val pFooter = Paint().apply { color = Color.GRAY; textSize = 8f }

        canvas.drawText("Resumen de gastos — Nesty", MARGEN.toFloat(), (y + 14).toFloat(), pTitulo); y += 24
        canvas.drawText("Familia: $familyId", MARGEN.toFloat(), y.toFloat(), pSubt); y += 14
        canvas.drawText("Período: ${rango.desde} a ${rango.hasta}    Moneda: $moneda", MARGEN.toFloat(), y.toFloat(), pSubt); y += 18

        canvas.drawText("Total acumulado: ${Moneda.formatear(totalAcumulado, moneda)}", MARGEN.toFloat(), y.toFloat(), pHead); y += 18

        canvas.drawText("Detalle de gastos", MARGEN.toFloat(), y.toFloat(), pHead); y += 14
        canvas.drawText("Fecha", MARGEN.toFloat(), y.toFloat(), pHead)
        canvas.drawText("Descripción", (MARGEN + 70).toFloat(), y.toFloat(), pHead)
        canvas.drawText("Pagador", (MARGEN + 290).toFloat(), y.toFloat(), pHead)
        canvas.drawText("Monto", (MARGEN + 400).toFloat(), y.toFloat(), pHead)
        canvas.drawText("Recibo", (MARGEN + 480).toFloat(), y.toFloat(), pHead)
        y += 12

        for (g in filtrados) {
            if (y > PAGE_H - 80) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
                canvas = page.canvas; y = MARGEN
            }
            canvas.drawText(g.fecha, MARGEN.toFloat(), y.toFloat(), pTexto)
            canvas.drawText(g.descripcion.take(34), (MARGEN + 70).toFloat(), y.toFloat(), pTexto)
            canvas.drawText(g.nombrePagador.take(16), (MARGEN + 290).toFloat(), y.toFloat(), pTexto)
            canvas.drawText(Moneda.formatear(g.monto, moneda), (MARGEN + 400).toFloat(), y.toFloat(), pTexto)
            if (g.reciboFotoUri.isNotEmpty()) canvas.drawText("Sí", (MARGEN + 480).toFloat(), y.toFloat(), pTexto)
            y += 12
        }

        // Compensaciones aceptadas (evidencia de saldos cerrados)
        if (compensaciones.isNotEmpty()) {
            y += 10
            if (y > PAGE_H - 100) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
                canvas = page.canvas; y = MARGEN
            }
            canvas.drawText("Compensaciones registradas", MARGEN.toFloat(), y.toFloat(), pHead); y += 14
            canvas.drawText("ID", MARGEN.toFloat(), y.toFloat(), pHead)
            canvas.drawText("Fecha", (MARGEN + 100).toFloat(), y.toFloat(), pHead)
            canvas.drawText("Monto", (MARGEN + 200).toFloat(), y.toFloat(), pHead)
            canvas.drawText("Confirmada", (MARGEN + 320).toFloat(), y.toFloat(), pHead)
            canvas.drawText("Tipo", (MARGEN + 420).toFloat(), y.toFloat(), pHead)
            y += 12
            val sdfFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (c in compensaciones.sortedByDescending { it.fechaCompleta }) {
                if (y > PAGE_H - 80) {
                    doc.finishPage(page)
                    page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
                    canvas = page.canvas; y = MARGEN
                }
                canvas.drawText(c.id.take(8), MARGEN.toFloat(), y.toFloat(), pTexto)
                canvas.drawText(sdfFecha.format(Date(c.fechaCompleta)), (MARGEN + 100).toFloat(), y.toFloat(), pTexto)
                canvas.drawText(Moneda.formatear(c.montoTotal, moneda), (MARGEN + 200).toFloat(), y.toFloat(), pTexto)
                canvas.drawText(if (c.confirmada) "Sí (ambos)" else "No", (MARGEN + 320).toFloat(), y.toFloat(), pTexto)
                canvas.drawText(c.tipoCompensacion, (MARGEN + 420).toFloat(), y.toFloat(), pTexto)
                y += 12
            }
        }

        // Footer
        val genTime = sdfDoc.format(Date())
        canvas.drawText("Generado: $genTime", MARGEN.toFloat(), (PAGE_H - 30).toFloat(), pFooter)
        canvas.drawText("Firma SHA-256: $hash", MARGEN.toFloat(), (PAGE_H - 18).toFloat(), pFooter)
        canvas.drawText("Generado por Nesty. La firma SHA-256 permite verificar el contenido.",
            MARGEN.toFloat(), (PAGE_H - 8).toFloat(), pFooter)
        doc.finishPage(page)

        val bytes = ByteArrayOutputStream().also { doc.writeTo(it) }.toByteArray()
        doc.close()

        context.contentResolver.openOutputStream(destino)?.use { it.write(bytes) }
            ?: error("No se pudo abrir destino")
        Unit
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(s.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    data class Rango(val desde: String, val hasta: String) {
        companion object {
            fun mesActual(): Rango {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val desde = sdf.format(cal.time)
                cal.add(java.util.Calendar.MONTH, 1)
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                val hasta = sdf.format(cal.time)
                return Rango(desde, hasta)
            }
            fun ultimosMeses(meses: Int): Rango {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val cal = java.util.Calendar.getInstance()
                val hasta = sdf.format(cal.time)
                cal.add(java.util.Calendar.MONTH, -meses)
                val desde = sdf.format(cal.time)
                return Rango(desde, hasta)
            }
            fun todo(): Rango = Rango("0000-01-01", "9999-12-31")
        }
    }
}
