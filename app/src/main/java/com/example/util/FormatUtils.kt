package com.example.util

import com.example.data.model.AgencyService
import com.example.data.model.BillingType
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object FormatUtils {

    private val indianLocale = Locale("en", "IN")

    fun formatInr(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            minimumFractionDigits = 0
        }
        return "₹${formatter.format(amount)}"
    }

    fun formatCompactInr(amount: Double): String {
        return when {
            amount >= 10_000_000 -> String.format(indianLocale, "₹%.1fCr", amount / 10_000_000.0)
            amount >= 100_000 -> String.format(indianLocale, "₹%.1fL", amount / 100_000.0)
            amount >= 1_000 -> String.format(indianLocale, "₹%.1fk", amount / 1_000.0)
            else -> "₹${amount.roundToLong()}"
        }
    }

    fun formatServicePrice(service: AgencyService): String {
        if (!service.fixedPrice || service.billingEnum() == BillingType.CUSTOM_QUOTE || service.price <= 0.0) {
            return "Custom Quote"
        }
        return "${formatInr(service.price)}${service.billingEnum().suffix}"
    }

    fun formatDate(millis: Long?, pattern: String = "dd MMM yyyy"): String {
        if (millis == null || millis <= 0L) return "Not set"
        return try {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
        } catch (_: Exception) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    fun formatTimestamp(ts: Timestamp?, pattern: String = "dd MMM yyyy"): String {
        val date = ts?.toDate() ?: return "Just now"
        return try {
            SimpleDateFormat(pattern, Locale.getDefault()).format(date)
        } catch (_: Exception) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }

    fun formatTimestampWithTime(ts: Timestamp?): String {
        val date = ts?.toDate() ?: return "Just now"
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(date)
    }
}
