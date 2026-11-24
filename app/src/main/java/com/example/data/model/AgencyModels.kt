package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ServiceCategory(val label: String) {
    DIGITAL_MARKETING("Digital Marketing"),
    CONTENT_CREATION("Content Creation"),
    AI_AUTOMATION("AI & Automation"),
    WEBSITE_ECOMMERCE("Website & E-commerce"),
    SOFTWARE_SOLUTIONS("Software & Business Solutions"),
    CONSULTING("Consulting");

    companion object {
        fun fromName(raw: String?): ServiceCategory =
            entries.firstOrNull { it.name == raw } ?: DIGITAL_MARKETING
    }
}

enum class BillingType(val label: String, val suffix: String) {
    MONTHLY("Monthly", "/month"),
    ONE_TIME("One-Time", ""),
    PER_SESSION("Per Session", "/session"),
    CUSTOM_QUOTE("Custom Quote", "");

    companion object {
        fun fromName(raw: String?): BillingType =
            entries.firstOrNull { it.name == raw } ?: CUSTOM_QUOTE
    }
}

enum class LeadStatus(val label: String) {
    NEW("New"),
    CONTACTED("Contacted"),
    INTERESTED("Interested"),
    FOLLOW_UP("Follow-up"),
    CONVERTED("Converted"),
    LOST("Lost");

    companion object {
        fun fromName(raw: String?): LeadStatus =
            entries.firstOrNull { it.name == raw } ?: NEW
    }
}

enum class ProjectStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold");

    companion object {
        fun fromName(raw: String?): ProjectStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    companion object {
        fun fromName(raw: String?): TaskPriority =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromName(raw: String?): TaskStatus =
            entries.firstOrNull { it.name == raw } ?: TODO
    }
}

enum class InvoiceStatus(val label: String) {
    UNPAID("Unpaid"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid");

    companion object {
        fun fromName(raw: String?): InvoiceStatus =
            entries.firstOrNull { it.name == raw } ?: UNPAID
    }
}

enum class QuotationStatus(val label: String) {
    DRAFT("Draft"),
    SENT("Sent"),
    ACCEPTED("Accepted"),
    DECLINED("Declined");

    companion object {
        fun fromName(raw: String?): QuotationStatus =
            entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

enum class EmployeeRole(val label: String) {
    ADMIN("Admin"),
    EMPLOYEE("Employee");

    companion object {
        fun fromName(raw: String?): EmployeeRole =
            entries.firstOrNull { it.name == raw } ?: EMPLOYEE
    }
}

enum class ActivityCategory(val label: String) {
    LEAD("Lead"),
    CLIENT("Client"),
    PROJECT("Project"),
    TASK("Task"),
    INVOICE("Invoice"),
    QUOTATION("Quotation"),
    SERVICE("Service"),
    TEAM("Team");

    companion object {
        fun fromName(raw: String?): ActivityCategory =
            entries.firstOrNull { it.name == raw } ?: PROJECT
    }
}

@Keep
@IgnoreExtraProperties
data class AgencyProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Rajeev",
    val email: String = "",
    val businessName: String = "Rajeev Digital",
    val tagline: String = "WhatsApp Marketing Expert | Grow Your Business Digitally",
    val whatsappNumber: String = "+91 8307229361",
    val instagramHandle: String = "@rajeev.digital.ai",
    val currency: String = "INR",
    val dateFormat: String = "dd MMM yyyy",
    val themeMode: String = "DARK_NAVY",
    val notificationsEnabled: Boolean = true,
    val seededDefaults: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AgencyService(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = ServiceCategory.DIGITAL_MARKETING.name,
    val price: Double = 0.0,
    val fixedPrice: Boolean = false,
    val billingType: String = BillingType.CUSTOM_QUOTE.name,
    val description: String = "",
    val active: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): ServiceCategory = ServiceCategory.fromName(category)
    fun billingEnum(): BillingType = BillingType.fromName(billingType)
}

@Keep
@IgnoreExtraProperties
data class Lead(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val businessName: String = "",
    val source: String = "Instagram",
    val interestedServices: List<String> = emptyList(),
    val status: String = LeadStatus.NEW.name,
    val notes: String = "",
    val followUpDateMillis: Long? = null,
    val convertedClientId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): LeadStatus = LeadStatus.fromName(status)
}

@Keep
@IgnoreExtraProperties
data class Client(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val businessName: String = "",
    val phone: String = "",
    val email: String = "",
    val selectedServices: List<String> = emptyList(),
    val notes: String = "",
    val leadId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val deadlineMillis: Long = System.currentTimeMillis() + 14L * 86_400_000L,
    val budget: Double = 0.0,
    val selectedServices: List<String> = emptyList(),
    val progress: Long = 0L,
    val status: String = ProjectStatus.PENDING.name,
    val assignedEmployeeId: String = "",
    val assignedEmployeeName: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ProjectStatus = ProjectStatus.fromName(status)
}

@Keep
@IgnoreExtraProperties
data class AgencyTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.TODO.name,
    val dueDateMillis: Long = System.currentTimeMillis() + 2L * 86_400_000L,
    val assignedEmployeeId: String = "",
    val assignedEmployeeName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun priorityEnum(): TaskPriority = TaskPriority.fromName(priority)
    fun statusEnum(): TaskStatus = TaskStatus.fromName(status)
}

@Keep
@IgnoreExtraProperties
data class LineItem(
    val serviceName: String = "",
    val description: String = "",
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0
) {
    val lineTotal: Double get() = quantity.coerceAtLeast(1L) * unitPrice.coerceAtLeast(0.0)

    fun toMap(): Map<String, Any> = mapOf(
        "serviceName" to serviceName.take(100),
        "description" to description.take(300),
        "quantity" to quantity.coerceAtLeast(1L),
        "unitPrice" to unitPrice.coerceAtLeast(0.0)
    )
}

@Keep
@IgnoreExtraProperties
data class PaymentEntry(
    val amount: Double = 0.0,
    val method: String = "UPI",
    val note: String = "",
    val paidAtMillis: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "amount" to amount.coerceAtLeast(0.0),
        "method" to method.take(40),
        "note" to note.take(200),
        "paidAtMillis" to paidAtMillis
    )
}

@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientBusiness: String = "",
    val items: List<LineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val status: String = InvoiceStatus.UNPAID.name,
    val dueDateMillis: Long = System.currentTimeMillis() + 7L * 86_400_000L,
    val paymentHistory: List<PaymentEntry> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): InvoiceStatus = InvoiceStatus.fromName(status)
    val balanceDue: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
}

@Keep
@IgnoreExtraProperties
data class Quotation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quotationNumber: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientBusiness: String = "",
    val items: List<LineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = QuotationStatus.DRAFT.name,
    val validUntilMillis: Long = System.currentTimeMillis() + 14L * 86_400_000L,
    val notes: String = "",
    val convertedProjectId: String = "",
    val convertedInvoiceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): QuotationStatus = QuotationStatus.fromName(status)
}

@Keep
@IgnoreExtraProperties
data class Employee(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val designation: String = "",
    val role: String = EmployeeRole.EMPLOYEE.name,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): EmployeeRole = EmployeeRole.fromName(role)
}

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val detail: String = "",
    val category: String = ActivityCategory.PROJECT.name,
    val relatedId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun categoryEnum(): ActivityCategory = ActivityCategory.fromName(category)
}
