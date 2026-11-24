package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Invoice
import com.example.data.model.LineItem
import com.example.data.model.Quotation
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun shareInvoicePdf(context: Context, invoice: Invoice) {
        val file = generateDocumentPdf(
            context = context,
            fileName = "${invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf",
            docType = "INVOICE",
            docNumber = invoice.invoiceNumber,
            statusLabel = invoice.statusEnum().label,
            clientName = invoice.clientName,
            clientBusiness = invoice.clientBusiness,
            clientPhone = invoice.clientPhone,
            dateLabel = "Due Date: ${FormatUtils.formatDate(invoice.dueDateMillis)}",
            items = invoice.items,
            subtotal = invoice.subtotal,
            discount = invoice.discountAmount,
            taxPercent = invoice.taxPercent,
            taxAmount = invoice.taxAmount,
            total = invoice.totalAmount,
            paidAmount = invoice.paidAmount,
            notes = invoice.notes
        )
        if (file != null) sharePdfFile(context, file, "Invoice ${invoice.invoiceNumber}")
    }

    fun shareQuotationPdf(context: Context, quotation: Quotation) {
        val file = generateDocumentPdf(
            context = context,
            fileName = "${quotation.quotationNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.pdf",
            docType = "QUOTATION",
            docNumber = quotation.quotationNumber,
            statusLabel = quotation.statusEnum().label,
            clientName = quotation.clientName,
            clientBusiness = quotation.clientBusiness,
            clientPhone = quotation.clientPhone,
            dateLabel = "Valid Until: ${FormatUtils.formatDate(quotation.validUntilMillis)}",
            items = quotation.items,
            subtotal = quotation.subtotal,
            discount = quotation.discountAmount,
            taxPercent = 0.0,
            taxAmount = 0.0,
            total = quotation.totalAmount,
            paidAmount = null,
            notes = quotation.notes
        )
        if (file != null) sharePdfFile(context, file, "Quotation ${quotation.quotationNumber}")
    }

    private fun generateDocumentPdf(
        context: Context,
        fileName: String,
        docType: String,
        docNumber: String,
        statusLabel: String,
        clientName: String,
        clientBusiness: String,
        clientPhone: String,
        dateLabel: String,
        items: List<LineItem>,
        subtotal: Double,
        discount: Double,
        taxPercent: Double,
        taxAmount: Double,
        total: Double,
        paidAmount: Double?,
        notes: String
    ): File? {
        return try {
            val dir = File(context.cacheDir, "documents").apply { mkdirs() }
            val outFile = File(dir, fileName)
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val headerBgPaint = Paint().apply { color = Color.parseColor("#0B132E") }
            val accentPaint = Paint().apply { color = Color.parseColor("#7149D7") }
            val goldPaint = Paint().apply {
                color = Color.parseColor("#E8C148")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val whiteTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val whiteSubPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 10.5f
            }
            val headingPaint = Paint().apply {
                color = Color.parseColor("#0B132E")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 11f
            }
            val mutedPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            // Header Banner
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 118f, headerBgPaint)
            canvas.drawRect(0f, 114f, PAGE_WIDTH.toFloat(), 118f, accentPaint)
            canvas.drawText("Rajeev Digital", 36f, 44f, goldPaint)
            canvas.drawText("WhatsApp Marketing Expert | Grow Your Business Digitally", 36f, 64f, whiteSubPaint)
            canvas.drawText("WhatsApp: +91 8307229361  |  Instagram: @rajeev.digital.ai", 36f, 82f, whiteSubPaint)

            val rightAlignPaint = Paint(whiteTitlePaint).apply { textAlign = Paint.Align.RIGHT }
            val rightSubPaint = Paint(whiteSubPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(docType, PAGE_WIDTH - 36f, 46f, rightAlignPaint)
            canvas.drawText("#$docNumber", PAGE_WIDTH - 36f, 66f, rightSubPaint)
            canvas.drawText("Status: $statusLabel", PAGE_WIDTH - 36f, 84f, rightSubPaint)

            // Client Section
            var y = 152f
            canvas.drawText("BILL TO", 36f, y, mutedPaint)
            canvas.drawText(dateLabel, PAGE_WIDTH - 210f, y, bodyPaint)
            y += 18f
            canvas.drawText(clientName, 36f, y, headingPaint)
            y += 16f
            if (clientBusiness.isNotBlank()) {
                canvas.drawText(clientBusiness, 36f, y, bodyPaint)
                y += 15f
            }
            if (clientPhone.isNotBlank()) {
                canvas.drawText("Phone: $clientPhone", 36f, y, bodyPaint)
                y += 15f
            }

            // Table Header
            y += 16f
            val tableBg = Paint().apply { color = Color.parseColor("#F1F5F9") }
            canvas.drawRect(36f, y, PAGE_WIDTH - 36f, y + 26f, tableBg)
            canvas.drawText("SERVICE", 44f, y + 17f, headingPaint)
            canvas.drawText("QTY", 355f, y + 17f, headingPaint)
            canvas.drawText("RATE", 405f, y + 17f, headingPaint)
            canvas.drawText("AMOUNT", 495f, y + 17f, headingPaint)
            y += 38f

            // Items
            items.take(16).forEach { item ->
                canvas.drawText(item.serviceName.take(42), 44f, y, bodyPaint)
                canvas.drawText(item.quantity.toString(), 360f, y, bodyPaint)
                canvas.drawText(FormatUtils.formatInr(item.unitPrice), 405f, y, bodyPaint)
                canvas.drawText(FormatUtils.formatInr(item.lineTotal), 495f, y, bodyPaint)
                y += 14f
                if (item.description.isNotBlank()) {
                    canvas.drawText(item.description.take(58), 44f, y, mutedPaint)
                    y += 13f
                }
                canvas.drawLine(36f, y + 4f, PAGE_WIDTH - 36f, y + 4f, linePaint)
                y += 16f
            }

            // Summary
            y += 12f
            canvas.drawText("Subtotal:", 385f, y, bodyPaint)
            canvas.drawText(FormatUtils.formatInr(subtotal), 495f, y, bodyPaint)
            y += 18f
            if (discount > 0) {
                canvas.drawText("Discount:", 385f, y, bodyPaint)
                canvas.drawText("-${FormatUtils.formatInr(discount)}", 495f, y, bodyPaint)
                y += 18f
            }
            if (taxAmount > 0) {
                canvas.drawText("Tax (${taxPercent}%):", 385f, y, bodyPaint)
                canvas.drawText(FormatUtils.formatInr(taxAmount), 495f, y, bodyPaint)
                y += 18f
            }
            canvas.drawLine(380f, y - 6f, PAGE_WIDTH - 36f, y - 6f, linePaint)
            canvas.drawText("TOTAL:", 385f, y + 10f, headingPaint)
            canvas.drawText(FormatUtils.formatInr(total), 495f, y + 10f, headingPaint)
            y += 28f

            if (paidAmount != null) {
                canvas.drawText("Paid:", 385f, y, bodyPaint)
                canvas.drawText(FormatUtils.formatInr(paidAmount), 495f, y, bodyPaint)
                y += 18f
                val balance = (total - paidAmount).coerceAtLeast(0.0)
                canvas.drawText("Balance Due:", 385f, y, headingPaint)
                canvas.drawText(FormatUtils.formatInr(balance), 495f, y, headingPaint)
                y += 24f
            }

            if (notes.isNotBlank()) {
                y += 10f
                canvas.drawText("Notes:", 36f, y, headingPaint)
                y += 15f
                canvas.drawText(notes.take(110), 36f, y, mutedPaint)
            }

            // Footer
            canvas.drawLine(36f, PAGE_HEIGHT - 55f, PAGE_WIDTH - 36f, PAGE_HEIGHT - 55f, linePaint)
            canvas.drawText(
                "Generated by Rajeev Digital Agency Management  •  +91 8307229361  •  @rajeev.digital.ai",
                36f,
                PAGE_HEIGHT - 36f,
                mutedPaint
            )

            document.finishPage(page)
            FileOutputStream(outFile).use { stream -> document.writeTo(stream) }
            document.close()
            outFile
        } catch (e: Exception) {
            Toast.makeText(context, "Could not generate PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun sharePdfFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$title - Rajeev Digital")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share $title PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
