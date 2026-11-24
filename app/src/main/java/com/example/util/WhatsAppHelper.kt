package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.Lead
import com.example.data.model.Project
import com.example.data.model.Quotation

object WhatsAppHelper {

    const val AGENCY_WHATSAPP_NUMBER = "+91 8307229361"
    const val AGENCY_INSTAGRAM = "@rajeev.digital.ai"

    fun sanitizePhoneForWa(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.isEmpty() -> "918307229361"
            digits.length == 10 -> "91$digits"
            else -> digits
        }
    }

    fun openWhatsAppChat(context: Context, rawPhone: String, message: String) {
        val waPhone = sanitizePhoneForWa(rawPhone)
        val encoded = Uri.encode(message)
        val uri = Uri.parse("https://wa.me/$waPhone?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open WhatsApp or browser.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAgencyWhatsAppSupport(context: Context) {
        openWhatsAppChat(
            context = context,
            rawPhone = AGENCY_WHATSAPP_NUMBER,
            message = "Hello Rajeev Digital! Connecting from the Rajeev Digital Agency App."
        )
    }

    fun sendLeadFollowUp(context: Context, lead: Lead) {
        val services = if (lead.interestedServices.isNotEmpty()) {
            lead.interestedServices.joinToString(", ")
        } else {
            "our digital marketing & growth services"
        }
        val msg = buildString {
            append("Hi ${lead.name}, this is Rajeev Digital ($AGENCY_INSTAGRAM).\n")
            append("Following up regarding your inquiry for $services")
            if (lead.businessName.isNotBlank()) append(" for ${lead.businessName}")
            append(".\nLet us know a convenient time to connect and scale your business digitally!\n")
            append("WhatsApp: $AGENCY_WHATSAPP_NUMBER")
        }
        openWhatsAppChat(context, lead.phone, msg)
    }

    fun sendClientMessage(context: Context, client: Client, customNote: String = "") {
        val msg = if (customNote.isNotBlank()) {
            customNote
        } else {
            "Hi ${client.name}, greeting from Rajeev Digital ($AGENCY_WHATSAPP_NUMBER). Reaching out with an update for ${client.businessName.ifBlank { "your account" }}."
        }
        openWhatsAppChat(context, client.phone, msg)
    }

    fun sendProjectUpdate(context: Context, project: Project, clientPhone: String) {
        val msg = buildString {
            append("*Project Update — Rajeev Digital*\n")
            append("Project: ${project.name}\n")
            append("Client: ${project.clientName}\n")
            append("Status: ${project.statusEnum().label}\n")
            append("Progress: ${project.progress}%\n")
            append("Deadline: ${FormatUtils.formatDate(project.deadlineMillis)}\n")
            if (project.notes.isNotBlank()) append("Notes: ${project.notes}\n")
            append("\nRegards,\nRajeev Digital | $AGENCY_WHATSAPP_NUMBER")
        }
        openWhatsAppChat(context, clientPhone.ifBlank { AGENCY_WHATSAPP_NUMBER }, msg)
    }

    fun sendQuotation(context: Context, quotation: Quotation) {
        val msg = buildString {
            append("*QUOTATION: ${quotation.quotationNumber}*\n")
            append("From: *Rajeev Digital* ($AGENCY_INSTAGRAM)\n")
            append("To: ${quotation.clientName}")
            if (quotation.clientBusiness.isNotBlank()) append(" (${quotation.clientBusiness})")
            append("\n\n*Proposed Services:*\n")
            quotation.items.forEachIndexed { idx, item ->
                append("${idx + 1}. ${item.serviceName} x${item.quantity} — ${FormatUtils.formatInr(item.lineTotal)}\n")
                if (item.description.isNotBlank()) append("   _${item.description}_\n")
            }
            append("\nSubtotal: ${FormatUtils.formatInr(quotation.subtotal)}\n")
            if (quotation.discountAmount > 0) {
                append("Discount: -${FormatUtils.formatInr(quotation.discountAmount)}\n")
            }
            append("*Total Quote: ${FormatUtils.formatInr(quotation.totalAmount)}*\n")
            append("Valid Until: ${FormatUtils.formatDate(quotation.validUntilMillis)}\n")
            if (quotation.notes.isNotBlank()) append("Notes: ${quotation.notes}\n")
            append("\nReply to approve and launch your project!\nWhatsApp: $AGENCY_WHATSAPP_NUMBER")
        }
        openWhatsAppChat(context, quotation.clientPhone.ifBlank { AGENCY_WHATSAPP_NUMBER }, msg)
    }

    fun sendInvoice(context: Context, invoice: Invoice) {
        val msg = buildString {
            append("*INVOICE: ${invoice.invoiceNumber}*\n")
            append("From: *Rajeev Digital* ($AGENCY_WHATSAPP_NUMBER)\n")
            append("Client: ${invoice.clientName}")
            if (invoice.clientBusiness.isNotBlank()) append(" (${invoice.clientBusiness})")
            append("\nStatus: ${invoice.statusEnum().label}\n\n")
            append("*Services:*\n")
            invoice.items.forEachIndexed { idx, item ->
                append("${idx + 1}. ${item.serviceName} x${item.quantity} — ${FormatUtils.formatInr(item.lineTotal)}\n")
            }
            append("\nSubtotal: ${FormatUtils.formatInr(invoice.subtotal)}\n")
            if (invoice.discountAmount > 0) append("Discount: -${FormatUtils.formatInr(invoice.discountAmount)}\n")
            if (invoice.taxAmount > 0) append("Tax (${invoice.taxPercent}%): ${FormatUtils.formatInr(invoice.taxAmount)}\n")
            append("*Total Amount: ${FormatUtils.formatInr(invoice.totalAmount)}*\n")
            append("Paid: ${FormatUtils.formatInr(invoice.paidAmount)}\n")
            append("*Balance Due: ${FormatUtils.formatInr(invoice.balanceDue)}*\n")
            append("Due Date: ${FormatUtils.formatDate(invoice.dueDateMillis)}\n")
            append("\nThank you for partnering with Rajeev Digital!")
        }
        openWhatsAppChat(context, invoice.clientPhone.ifBlank { AGENCY_WHATSAPP_NUMBER }, msg)
    }
}
