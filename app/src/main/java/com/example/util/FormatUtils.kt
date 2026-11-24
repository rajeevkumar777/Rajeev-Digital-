package com.example.util

import com.example.data.model.AgencyService
import com.example.data.model.BillingType
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object FormatUtils {

    private val indianLocale = Locale.forLanguageTag("en-IN")

    fun formatInr(amount: Double): String {
        val isNegative = amount < 0
        val scaled = kotlin.math.round(kotlin.math.abs(amount) * 100.0).toLong()
        val whole = scaled / 100L
        val paise = scaled % 100L
        val wholeStr = whole.toString()
        val groupedWhole = if (wholeStr.length <= 3) {
            wholeStr
        } else {
            val lastThree = wholeStr.takeLast(3)
            val rest = wholeStr.dropLast(3)
            val restGrouped = rest.reversed().chunked(2).joinToString(",").reversed()
            "$restGrouped,$lastThree"
        }
        val decimalSuffix = if (paise > 0L) ".${paise.toString().padStart(2, '0')}" else ""
        return "${if (isNegative) "-" else ""}₹$groupedWhole$decimalSuffix"
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
