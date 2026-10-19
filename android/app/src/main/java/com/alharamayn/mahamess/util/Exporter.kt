package com.alharamayn.mahamess.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.alharamayn.mahamess.data.AppSettings
import com.alharamayn.mahamess.data.Units
import com.alharamayn.mahamess.data.InvoiceEntity
import com.alharamayn.mahamess.data.InvoiceLineEntity
import java.io.File
import java.io.FileOutputStream

object Exporter {

    private fun exportsDir(context: Context): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun writeText(context: Context, name: String, content: String): File {
        val file = File(exportsDir(context), name)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mime: String, title: String, packageName: String? = null) {
        val uri = uriFor(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            packageName?.let { setPackage(it) }
        }
        context.startActivity(if (packageName != null) intent else Intent.createChooser(intent, title))
    }

    fun shareText(context: Context, text: String, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        if (packageName != null) intent.setPackage(packageName)
        context.startActivity(if (packageName != null) intent else Intent.createChooser(intent, "مشاركة"))
    }

    fun whatsapp(context: Context, phone: String, text: String) {
        val digits = phone.replace(Regex("[^0-9]"), "")
        val normalized = when {
            digits.isEmpty() -> ""
            digits.startsWith("963") -> digits
            digits.startsWith("0") -> "963" + digits.drop(1)
            else -> "963$digits"
        }
        val uri = if (normalized.isEmpty()) {
            Uri.parse("https://wa.me/?text=${Uri.encode(text)}")
        } else {
            Uri.parse("https://wa.me/$normalized?text=${Uri.encode(text)}")
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp"))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    /* ================= PDF الفاتورة ================= */
    fun invoicePdf(
        context: Context,
        invoice: InvoiceEntity,
        lines: List<InvoiceLineEntity>,
        settings: AppSettings,
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val margin = 36f
        val width = 595f
        var y = 46f

        val brown = Color.rgb(78, 52, 46)
        val gold = Color.rgb(212, 160, 23)
        val grey = Color.rgb(120, 100, 85)

        fun paint(size: Float, color: Int = brown, bold: Boolean = false): Paint {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = color
            p.textSize = size
            p.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            return p
        }

        // خلفية علوية
        canvas.drawRect(0f, 0f, width, 104f, paint(1f, gold).apply { color = Color.rgb(255, 248, 225) })
        canvas.drawRect(0f, 100f, width, 104f, paint(1f, gold))

        val shopPaint = paint(23f, brown, true).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(settings.shopName, width / 2f, 44f, shopPaint)
        val subPaint = paint(10f, grey).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("إدارة الفواتير والحسابات", width / 2f, 60f, subPaint)
        canvas.drawText("${settings.branch} — ${settings.address} ${if (settings.phone.isNotBlank()) "— ${settings.phone}" else ""}", width / 2f, 76f, subPaint)
        y = 130f

        val titlePaint = paint(14f, brown, true)
        canvas.drawText("فاتورة مبيعات رقم ${invoice.no}", width - margin, y, titlePaint.apply { textAlign = Paint.Align.RIGHT })
        val bodyPaint = paint(10f, brown)
        y += 18f
        canvas.drawText("التاريخ: ${Dates.format(invoice.date)}", width - margin, y, bodyPaint.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("الزبون: ${invoice.partyName}", margin, y, bodyPaint.apply { textAlign = Paint.Align.LEFT })
        y += 16f
        canvas.drawText("العملة: ${Money.name(invoice.currency)}", width - margin, y, bodyPaint.apply { textAlign = Paint.Align.RIGHT })
        if (invoice.currency == Money.USD) {
            canvas.drawText("سعر الصرف: ${Money.formatRate(invoice.rate)} ل.س", margin, y, bodyPaint)
        }
        y += 20f

        // رأس الجدول
        canvas.drawRect(margin, y, width - margin, y + 22f, paint(1f, Color.rgb(255, 243, 205)))
        val th = paint(10f, brown, true)
        canvas.drawText("الصنف", width - margin - 6f, y + 15f, th.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("الكمية", width / 2f + 20f, y + 15f, th.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("السعر", width / 2f - 90f, y + 15f, th.apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("المجموع", margin + 10f, y + 15f, th.apply { textAlign = Paint.Align.LEFT })
        y += 26f

        val rowPaint = paint(10f, brown)
        val num = paint(10f, brown)
        lines.forEach { line ->
            val qty = line.qtyMilli / 1000.0
            val total = Math.round(line.qtyMilli * line.unitPrice / 1000.0) - line.discount
            canvas.drawText(line.name, width - margin - 6f, y, rowPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(
                "$qty ${Units.short(line.unit)}",
                width / 2f + 20f, y, num.apply { textAlign = Paint.Align.CENTER },
            )
            canvas.drawText(Money.formatBare(line.unitPrice, invoice.currency), width / 2f - 90f, y, num.apply { textAlign = Paint.Align.CENTER })
            canvas.drawText(Money.formatBare(total, invoice.currency), margin + 10f, y, num.apply { textAlign = Paint.Align.LEFT })
            y += 6f
            canvas.drawLine(margin, y, width - margin, y, paint(0.5f, Color.rgb(230, 220, 205)))
            y += 16f
        }

        y += 8f
        fun totalRow(label: String, value: String, bold: Boolean = false, color: Int = brown) {
            val p = paint(if (bold) 13f else 10f, color, bold)
            canvas.drawText(label, width - margin, y, p.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(value, margin, y, paint(if (bold) 13f else 10f, color, bold).apply { textAlign = Paint.Align.LEFT })
            y += if (bold) 22f else 17f
        }
        canvas.drawLine(margin, y, width - margin, y, paint(1f, gold))
        y += 18f
        totalRow("المجموع الفرعي", Money.format(invoice.subtotal, invoice.currency))
        if (invoice.discount > 0) totalRow("الخصم", "- " + Money.format(invoice.discount, invoice.currency))
        totalRow("الإجمالي", Money.format(invoice.total, invoice.currency), true, gold)
        totalRow("المدفوع", Money.format(invoice.paid, invoice.currency))
        totalRow("المتبقي", Money.format(invoice.remaining, invoice.currency), true, brown)

        y += 14f
        val footPaint = paint(9f, grey).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(settings.invoiceFooter, width / 2f, y, footPaint)
        y += 14f
        canvas.drawText("${settings.shopName} — ${settings.phone}", width / 2f, y, footPaint)
        y += 13f
        canvas.drawText("صدرت من تطبيق محامص الحرمين", width / 2f, y, footPaint)

        if (invoice.status == "cancelled") {
            val stamp = paint(34f, Color.rgb(198, 40, 40), true).apply { textAlign = Paint.Align.CENTER; alpha = 90 }
            canvas.drawText("ملغاة", width / 2f, 420f, stamp)
        }

        doc.finishPage(page)
        val file = File(exportsDir(context), "فاتورة-${invoice.no}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /* ================= PDF تقرير ================= */
    fun reportPdf(
        context: Context,
        settings: AppSettings,
        from: String,
        to: String,
        rows: List<Triple<String, String, String>>,
        title: String = "تقرير الفترة",
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val brown = Color.rgb(78, 52, 46)
        val gold = Color.rgb(212, 160, 23)
        val grey = Color.rgb(120, 100, 85)

        fun paint(size: Float, color: Int = brown, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

        canvas.drawRect(0f, 0f, 595f, 92f, paint(1f, Color.rgb(255, 248, 225)))
        canvas.drawRect(0f, 90f, 595f, 94f, paint(1f, gold))
        canvas.drawText(settings.shopName, 297f, 42f, paint(22f, brown, true).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText(title, 297f, 62f, paint(11f, grey).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("من $from إلى $to", 297f, 78f, paint(9f, grey).apply { textAlign = Paint.Align.CENTER })

        var y = 130f
        canvas.drawRect(36f, y - 16f, 559f, y + 8f, paint(1f, Color.rgb(255, 243, 205)))
        canvas.drawText("البند", 550f, y, paint(11f, brown, true).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("ليرة سورية", 300f, y, paint(11f, brown, true).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("دولار", 80f, y, paint(11f, brown, true).apply { textAlign = Paint.Align.CENTER })
        y += 26f
        rows.forEach { (label, syp, usd) ->
            canvas.drawText(label, 550f, y, paint(10f).apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(syp, 300f, y, paint(10f).apply { textAlign = Paint.Align.CENTER })
            canvas.drawText(usd, 80f, y, paint(10f).apply { textAlign = Paint.Align.CENTER })
            y += 6f
            canvas.drawLine(36f, y, 559f, y, paint(0.5f, Color.rgb(230, 220, 205)))
            y += 16f
            if (y > 780f) return@forEach
        }
        canvas.drawText("سعر الصرف المستخدم: ${Money.formatRate(settings.rate)} ل.س", 550f, 800f, paint(9f, grey).apply { textAlign = Paint.Align.RIGHT })
        doc.finishPage(page)
        val file = File(exportsDir(context), "تقرير-$from-$to.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun csv(rows: List<List<String>>): String {
        val sb = StringBuilder("\uFEFF")
        rows.forEach { row ->
            sb.append(row.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" })
            sb.append("\r\n")
        }
        return sb.toString()
    }
}
