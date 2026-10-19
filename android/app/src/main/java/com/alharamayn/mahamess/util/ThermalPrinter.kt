package com.alharamayn.mahamess.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.alharamayn.mahamess.data.AppSettings
import com.alharamayn.mahamess.data.InvoiceEntity
import com.alharamayn.mahamess.data.InvoiceLineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

object ThermalPrinter {

    private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

    fun isBluetoothReady(context: Context): Boolean {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        return adapter != null && adapter.isEnabled && hasPerm
    }

    fun requiresPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    fun bondedDevices(context: Context): List<BluetoothDevice> {
        if (!isBluetoothReady(context)) return emptyList()
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** رسم الفاتورة كصورة أحادية لدعم العربية على الطابعات الحرارية */
    fun renderReceipt(
        invoice: InvoiceEntity,
        lines: List<InvoiceLineEntity>,
        settings: AppSettings,
    ): Bitmap {
        val width = 384
        var height = 480 + lines.size * 46
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        var y = 28f
        fun center(text: String, size: Float, bold: Boolean = false) {
            paint.textSize = size
            paint.typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, width / 2f, y, paint)
            y += size + 8
        }
        fun row(right: String, left: String, size: Float = 19f) {
            paint.textSize = size
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(right, width - 10f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(left, 10f, y, paint)
            y += size + 6
        }
        fun divider() {
            paint.strokeWidth = 1f
            canvas.drawLine(8f, y, width - 8f, y, paint)
            y += 10
        }

        center(settings.shopName, 28f, true)
        center("فاتورة مبيعات", 20f)
        if (settings.phone.isNotBlank()) center(settings.phone, 16f)
        divider()
        row("رقم: ${invoice.no}", "")
        row("التاريخ: ${Dates.format(invoice.date)}", "")
        row("الزبون: ${invoice.partyName}", "")
        divider()
        lines.forEach { line ->
            paint.textSize = 19f
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(line.name, width - 10f, y, paint)
            y += 23
            paint.textSize = 17f
            val qty = line.qtyMilli / 1000.0
            canvas.drawText("$qty ${Units.short(line.unit)} × ${Money.formatBare(line.unitPrice, invoice.currency)}", width - 10f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            val total = Math.round(line.qtyMilli * line.unitPrice / 1000.0) - line.discount
            canvas.drawText(Money.formatBare(total, invoice.currency), 10f, y, paint)
            y += 25
        }
        divider()
        row("الإجمالي", Money.format(invoice.total, invoice.currency), 22f)
        row("المدفوع", Money.format(invoice.paid, invoice.currency))
        row("المتبقي", Money.format(invoice.remaining, invoice.currency))
        divider()
        center(settings.invoiceFooter, 15f)
        center("شكراً لزيارتكم", 17f)
        y += 24

        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, y.toInt().coerceAtMost(bitmap.height))
        return cropped
    }

    private fun raster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val data = ByteArray(bytesPerRow * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                if (lum < 128) {
                    data[y * bytesPerRow + (x / 8)] = (data[y * bytesPerRow + (x / 8)].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }
        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (bytesPerRow and 0xFF).toByte(), ((bytesPerRow shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(), ((height shr 8) and 0xFF).toByte(),
        )
        return header + data
    }

    @Suppress("MissingPermission")
    suspend fun print(context: Context, device: BluetoothDevice, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
            socket.connect()
            val out: OutputStream = socket.outputStream
            out.write(byteArrayOf(0x1B, 0x40))
            val payload = raster(bitmap)
            var offset = 0
            while (offset < payload.size) {
                val len = minOf(180, payload.size - offset)
                out.write(payload, offset, len)
                offset += len
            }
            out.write(byteArrayOf(0x1B, 0x64, 0x03, 0x1D, 0x56, 0x42, 0x00))
            out.flush()
            "تم إرسال الفاتورة إلى الطابعة"
        } catch (e: Exception) {
            "تعذر الاتصال بالطابعة: ${e.message}"
        } finally {
            try { socket?.close() } catch (e: Exception) { }
        }
    }
}
