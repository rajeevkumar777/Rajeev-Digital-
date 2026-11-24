package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityLog
import com.example.data.model.AgencyProfile
import com.example.data.model.AgencyService
import com.example.data.model.AgencyTask
import com.example.data.model.BillingType
import com.example.data.model.Client
import com.example.data.model.Employee
import com.example.data.model.EmployeeRole
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.data.model.Lead
import com.example.data.model.LeadStatus
import com.example.data.model.LineItem
import com.example.data.model.PaymentEntry
import com.example.data.model.Project
import com.example.data.model.ProjectStatus
import com.example.data.model.Quotation
import com.example.data.model.QuotationStatus
import com.example.data.model.ServiceCatalogDefaults
import com.example.data.model.ServiceCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.util.FormatUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AgencyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user write." }
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> observeCollection(
        uid: String,
        subcollection: String,
        orderByField: String = "createdAt",
        descending: Boolean = true,
        mapper: (List<T>) -> List<T> = { it },
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val activeUid = requireUserId(uid)
        val path = "users/$activeUid/$subcollection"
        emitAll(
            userDoc(activeUid).collection(subcollection)
                .orderBy(orderByField, if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    mapper(snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfile(uid: String): Flow<AgencyProfile?> = flow {
        val activeUid = requireUserId(uid)
        val path = "users/$activeUid"
        emitAll(
            userDoc(activeUid).snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AgencyProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == activeUid) {
                        handleFirestoreError(ex, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeServices(uid: String): Flow<List<AgencyService>> =
        observeCollection(uid, "services", orderByField = "sortOrder", descending = false, clazz = AgencyService::class.java)

    fun observeLeads(uid: String): Flow<List<Lead>> =
        observeCollection(uid, "leads", clazz = Lead::class.java)

    fun observeClients(uid: String): Flow<List<Client>> =
        observeCollection(uid, "clients", clazz = Client::class.java)

    fun observeProjects(uid: String): Flow<List<Project>> =
        observeCollection(uid, "projects", clazz = Project::class.java)

    fun observeTasks(uid: String): Flow<List<AgencyTask>> =
        observeCollection(uid, "tasks", clazz = AgencyTask::class.java)

    fun observeInvoices(uid: String): Flow<List<Invoice>> =
        observeCollection(uid, "invoices", clazz = Invoice::class.java)

    fun observeQuotations(uid: String): Flow<List<Quotation>> =
        observeCollection(uid, "quotations", clazz = Quotation::class.java)

    fun observeEmployees(uid: String): Flow<List<Employee>> =
        observeCollection(uid, "employees", clazz = Employee::class.java)

    fun observeActivities(uid: String): Flow<List<ActivityLog>> =
        observeCollection(uid, "activities", clazz = ActivityLog::class.java)

    suspend fun ensureWorkspaceInitialized(uid: String): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId(uid)
            val user = auth.currentUser
            val docRef = userDoc(activeUid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val displayName = user?.displayName?.take(100)?.takeIf { it.isNotBlank() } ?: "Rajeev Kumar"
                val email = user?.email?.take(120) ?: ""
                val profilePayload = mapOf(
                    "userId" to activeUid,
                    "displayName" to displayName,
                    "email" to email,
                    "businessName" to "Rajeev Digital",
                    "tagline" to "WhatsApp Marketing Expert | Grow Your Business Digitally",
                    "whatsappNumber" to "+91 8307229361",
                    "instagramHandle" to "@rajeev.digital.ai",
                    "currency" to "INR",
                    "dateFormat" to "dd MMM yyyy",
                    "themeMode" to "DARK_NAVY",
                    "notificationsEnabled" to true,
                    "seededDefaults" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(profilePayload).await()
            }

            val servicesSnap = docRef.collection("services").limit(1).get().await()
            if (servicesSnap.isEmpty) {
                val batch = db.batch()
                ServiceCatalogDefaults.defaultServices.forEachIndexed { idx, seed ->
                    val srvRef = docRef.collection("services").document(seed.id)
                    val payload = mapOf(
                        "userId" to activeUid,
                        "name" to seed.name.take(100),
                        "category" to seed.category.name,
                        "price" to seed.price,
                        "fixedPrice" to seed.fixedPrice,
                        "billingType" to seed.billingType.name,
                        "description" to seed.description.take(1000),
                        "active" to true,
                        "sortOrder" to (idx + 1).toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(srvRef, payload)
                }
                val adminEmpRef = docRef.collection("employees").document("owner_admin")
                val empSnap = adminEmpRef.get().await()
                if (!empSnap.exists()) {
                    batch.set(
                        adminEmpRef,
                        mapOf(
                            "userId" to activeUid,
                            "name" to (user?.displayName?.take(100)?.takeIf { it.isNotBlank() } ?: "Rajeev"),
                            "email" to (user?.email?.take(120) ?: ""),
                            "phone" to "+91 8307229361",
                            "designation" to "Founder & Agency Admin",
                            "role" to EmployeeRole.ADMIN.name,
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.update(
                    docRef,
                    mapOf("seededDefaults" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        uid: String,
        displayName: String,
        businessName: String,
        tagline: String,
        whatsappNumber: String,
        instagramHandle: String,
        dateFormat: String,
        themeMode: String,
        notificationsEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId(uid)
            require(displayName.trim().isNotEmpty()) { "Display name is required" }
            require(businessName.trim().isNotEmpty()) { "Business name is required" }
            userDoc(activeUid).update(
                mapOf(
                    "displayName" to displayName.trim().take(100),
                    "businessName" to businessName.trim().take(100),
                    "tagline" to tagline.trim().take(150),
                    "whatsappNumber" to whatsappNumber.trim().take(25),
                    "instagramHandle" to instagramHandle.trim().take(60),
                    "dateFormat" to dateFormat.takeIf { it in listOf("dd MMM yyyy", "dd/MM/yyyy", "yyyy-MM-dd") }.orEmpty().ifEmpty { "dd MMM yyyy" },
                    "themeMode" to themeMode.takeIf { it in listOf("DARK_NAVY", "MIDNIGHT", "SYSTEM") }.orEmpty().ifEmpty { "DARK_NAVY" },
                    "notificationsEnabled" to notificationsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- SERVICES CRUD ---
    suspend fun saveService(
        uid: String,
        serviceId: String?,
        name: String,
        category: ServiceCategory,
        price: Double,
        fixedPrice: Boolean,
        billingType: BillingType,
        description: String,
        active: Boolean,
        sortOrder: Long = 100L
    ): Result<Unit> {
        var path = "users/$uid/services"
        return try {
            val activeUid = requireUserId(uid)
            require(name.trim().isNotEmpty()) { "Service name is required" }
            val normalizedPrice = if (fixedPrice && billingType != BillingType.CUSTOM_QUOTE) price.coerceAtLeast(0.0) else 0.0
            val col = userDoc(activeUid).collection("services")
            if (serviceId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "name" to name.trim().take(100),
                        "category" to category.name,
                        "price" to normalizedPrice,
                        "fixedPrice" to fixedPrice,
                        "billingType" to billingType.name,
                        "description" to description.trim().take(1000),
                        "active" to active,
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Added service: ${name.trim()}", category.label, ActivityCategory.SERVICE, docRef.id)
            } else {
                val docRef = col.document(serviceId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(100),
                        "category" to category.name,
                        "price" to normalizedPrice,
                        "fixedPrice" to fixedPrice,
                        "billingType" to billingType.name,
                        "description" to description.trim().take(1000),
                        "active" to active,
                        "sortOrder" to sortOrder.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Updated service: ${name.trim()}", category.label, ActivityCategory.SERVICE, serviceId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleServiceActive(uid: String, serviceId: String, active: Boolean): Result<Unit> {
        val path = "users/$uid/services/$serviceId"
        return try {
            val activeUid = requireUserId(uid)
            userDoc(activeUid).collection("services").document(serviceId)
                .update(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(uid: String, collection: String, docId: String): Result<Unit> {
        val path = "users/$uid/$collection/$docId"
        return try {
            val activeUid = requireUserId(uid)
            require(docId.isNotBlank()) { "Document ID cannot be blank" }
            userDoc(activeUid).collection(collection).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- LEADS CRUD ---
    suspend fun saveLead(
        uid: String,
        leadId: String?,
        name: String,
        phone: String,
        email: String,
        businessName: String,
        source: String,
        interestedServices: List<String>,
        status: LeadStatus,
        notes: String,
        followUpDateMillis: Long?
    ): Result<Unit> {
        var path = "users/$uid/leads"
        return try {
            val activeUid = requireUserId(uid)
            require(name.trim().isNotEmpty()) { "Lead name is required" }
            val boundedServices = interestedServices.take(25).map { it.take(100) }
            val col = userDoc(activeUid).collection("leads")
            if (leadId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                val payload = mapOf(
                    "userId" to activeUid,
                    "name" to name.trim().take(100),
                    "phone" to phone.trim().take(30),
                    "email" to email.trim().take(120),
                    "businessName" to businessName.trim().take(120),
                    "source" to source.trim().take(60),
                    "interestedServices" to boundedServices,
                    "status" to status.name,
                    "notes" to notes.trim().take(2000),
                    "followUpDateMillis" to followUpDateMillis?.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                logActivity(activeUid, "New lead: ${name.trim()}", "${source.ifBlank { "Direct" }} • ${status.label}", ActivityCategory.LEAD, docRef.id)
            } else {
                val docRef = col.document(leadId)
                path = docRef.path
                val payload = mapOf(
                    "name" to name.trim().take(100),
                    "phone" to phone.trim().take(30),
                    "email" to email.trim().take(120),
                    "businessName" to businessName.trim().take(120),
                    "source" to source.trim().take(60),
                    "interestedServices" to boundedServices,
                    "status" to status.name,
                    "notes" to notes.trim().take(2000),
                    "followUpDateMillis" to followUpDateMillis?.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(payload).await()
                logActivity(activeUid, "Updated lead: ${name.trim()}", "Status: ${status.label}", ActivityCategory.LEAD, leadId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun convertLeadToClient(uid: String, lead: Lead): Result<String> {
        var path = "users/$uid/clients"
        return try {
            val activeUid = requireUserId(uid)
            val clientRef = userDoc(activeUid).collection("clients").document()
            path = clientRef.path
            val leadRef = userDoc(activeUid).collection("leads").document(lead.id)

            val batch = db.batch()
            batch.set(
                clientRef,
                mapOf(
                    "userId" to activeUid,
                    "name" to lead.name.take(100),
                    "businessName" to lead.businessName.take(120),
                    "phone" to lead.phone.take(30),
                    "email" to lead.email.take(120),
                    "selectedServices" to lead.interestedServices.take(25),
                    "notes" to lead.notes.take(3000),
                    "leadId" to lead.id.take(128),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                leadRef,
                mapOf(
                    "status" to LeadStatus.CONVERTED.name,
                    "convertedClientId" to clientRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            logActivity(activeUid, "Lead converted to client: ${lead.name}", lead.businessName.ifBlank { lead.phone }, ActivityCategory.CLIENT, clientRef.id)
            Result.success(clientRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- CLIENTS CRUD ---
    suspend fun saveClient(
        uid: String,
        clientId: String?,
        name: String,
        businessName: String,
        phone: String,
        email: String,
        selectedServices: List<String>,
        notes: String
    ): Result<Unit> {
        var path = "users/$uid/clients"
        return try {
            val activeUid = requireUserId(uid)
            require(name.trim().isNotEmpty()) { "Client name is required" }
            val boundedServices = selectedServices.take(25).map { it.take(100) }
            val col = userDoc(activeUid).collection("clients")
            if (clientId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "name" to name.trim().take(100),
                        "businessName" to businessName.trim().take(120),
                        "phone" to phone.trim().take(30),
                        "email" to email.trim().take(120),
                        "selectedServices" to boundedServices,
                        "notes" to notes.trim().take(3000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "New client: ${name.trim()}", businessName.trim(), ActivityCategory.CLIENT, docRef.id)
            } else {
                val docRef = col.document(clientId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(100),
                        "businessName" to businessName.trim().take(120),
                        "phone" to phone.trim().take(30),
                        "email" to email.trim().take(120),
                        "selectedServices" to boundedServices,
                        "notes" to notes.trim().take(3000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Updated client: ${name.trim()}", businessName.trim(), ActivityCategory.CLIENT, clientId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PROJECTS CRUD ---
    suspend fun saveProject(
        uid: String,
        projectId: String?,
        name: String,
        description: String,
        clientId: String,
        clientName: String,
        startDateMillis: Long,
        deadlineMillis: Long,
        budget: Double,
        selectedServices: List<String>,
        progress: Int,
        status: ProjectStatus,
        assignedEmployeeId: String,
        assignedEmployeeName: String,
        notes: String
    ): Result<Unit> {
        var path = "users/$uid/projects"
        return try {
            val activeUid = requireUserId(uid)
            require(name.trim().isNotEmpty()) { "Project name is required" }
            val boundedProgress = if (status == ProjectStatus.COMPLETED) 100L else progress.coerceIn(0, 100).toLong()
            val boundedServices = selectedServices.take(25).map { it.take(100) }
            val col = userDoc(activeUid).collection("projects")
            if (projectId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "name" to name.trim().take(120),
                        "description" to description.trim().take(2000),
                        "clientId" to clientId.take(128),
                        "clientName" to clientName.trim().take(120),
                        "startDateMillis" to startDateMillis.coerceAtLeast(0L),
                        "deadlineMillis" to deadlineMillis.coerceAtLeast(0L),
                        "budget" to budget.coerceAtLeast(0.0),
                        "selectedServices" to boundedServices,
                        "progress" to boundedProgress,
                        "status" to status.name,
                        "assignedEmployeeId" to assignedEmployeeId.take(128),
                        "assignedEmployeeName" to assignedEmployeeName.take(100),
                        "notes" to notes.trim().take(2000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "New project: ${name.trim()}", "Client: ${clientName.ifBlank { "Agency" }}", ActivityCategory.PROJECT, docRef.id)
            } else {
                val docRef = col.document(projectId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(120),
                        "description" to description.trim().take(2000),
                        "clientId" to clientId.take(128),
                        "clientName" to clientName.trim().take(120),
                        "startDateMillis" to startDateMillis.coerceAtLeast(0L),
                        "deadlineMillis" to deadlineMillis.coerceAtLeast(0L),
                        "budget" to budget.coerceAtLeast(0.0),
                        "selectedServices" to boundedServices,
                        "progress" to boundedProgress,
                        "status" to status.name,
                        "assignedEmployeeId" to assignedEmployeeId.take(128),
                        "assignedEmployeeName" to assignedEmployeeName.take(100),
                        "notes" to notes.trim().take(2000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Updated project: ${name.trim()}", "${status.label} • $boundedProgress%", ActivityCategory.PROJECT, projectId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- TASKS CRUD ---
    suspend fun saveTask(
        uid: String,
        taskId: String?,
        title: String,
        description: String,
        projectId: String,
        projectName: String,
        priority: TaskPriority,
        status: TaskStatus,
        dueDateMillis: Long,
        assignedEmployeeId: String,
        assignedEmployeeName: String
    ): Result<Unit> {
        var path = "users/$uid/tasks"
        return try {
            val activeUid = requireUserId(uid)
            require(title.trim().isNotEmpty()) { "Task title is required" }
            val col = userDoc(activeUid).collection("tasks")
            if (taskId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "title" to title.trim().take(150),
                        "description" to description.trim().take(1500),
                        "projectId" to projectId.take(128),
                        "projectName" to projectName.take(120),
                        "priority" to priority.name,
                        "status" to status.name,
                        "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
                        "assignedEmployeeId" to assignedEmployeeId.take(128),
                        "assignedEmployeeName" to assignedEmployeeName.take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "New task: ${title.trim()}", "${priority.label} priority", ActivityCategory.TASK, docRef.id)
            } else {
                val docRef = col.document(taskId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "title" to title.trim().take(150),
                        "description" to description.trim().take(1500),
                        "projectId" to projectId.take(128),
                        "projectName" to projectName.take(120),
                        "priority" to priority.name,
                        "status" to status.name,
                        "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
                        "assignedEmployeeId" to assignedEmployeeId.take(128),
                        "assignedEmployeeName" to assignedEmployeeName.take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(uid: String, taskId: String, status: TaskStatus): Result<Unit> {
        val path = "users/$uid/tasks/$taskId"
        return try {
            val activeUid = requireUserId(uid)
            userDoc(activeUid).collection("tasks").document(taskId)
                .update(mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- INVOICES CRUD ---
    suspend fun saveInvoice(
        uid: String,
        invoiceId: String?,
        invoiceNumber: String,
        clientId: String,
        clientName: String,
        clientPhone: String,
        clientBusiness: String,
        items: List<LineItem>,
        discountAmount: Double,
        taxPercent: Double,
        paidAmount: Double,
        dueDateMillis: Long,
        paymentHistory: List<PaymentEntry>,
        notes: String
    ): Result<Unit> {
        var path = "users/$uid/invoices"
        return try {
            val activeUid = requireUserId(uid)
            require(invoiceNumber.trim().isNotEmpty()) { "Invoice number is required" }
            require(clientName.trim().isNotEmpty()) { "Client name is required" }
            require(items.isNotEmpty()) { "At least one service item is required" }

            val boundedItems = items.take(20)
            val subtotal = boundedItems.sumOf { it.lineTotal }
            val safeDiscount = discountAmount.coerceIn(0.0, subtotal)
            val taxable = (subtotal - safeDiscount).coerceAtLeast(0.0)
            val safeTaxPercent = taxPercent.coerceIn(0.0, 100.0)
            val taxAmount = taxable * (safeTaxPercent / 100.0)
            val totalAmount = taxable + taxAmount
            val safePaid = paidAmount.coerceIn(0.0, totalAmount)
            val status = when {
                totalAmount <= 0.0 || safePaid >= totalAmount - 0.01 -> InvoiceStatus.PAID
                safePaid > 0.0 -> InvoiceStatus.PARTIALLY_PAID
                else -> InvoiceStatus.UNPAID
            }
            val itemMaps = boundedItems.map { it.toMap() }
            val paymentMaps = paymentHistory.take(30).map { it.toMap() }

            val col = userDoc(activeUid).collection("invoices")
            if (invoiceId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "invoiceNumber" to invoiceNumber.trim().take(40),
                        "clientId" to clientId.take(128),
                        "clientName" to clientName.trim().take(120),
                        "clientPhone" to clientPhone.trim().take(30),
                        "clientBusiness" to clientBusiness.trim().take(120),
                        "items" to itemMaps,
                        "subtotal" to subtotal,
                        "discountAmount" to safeDiscount,
                        "taxPercent" to safeTaxPercent,
                        "taxAmount" to taxAmount,
                        "totalAmount" to totalAmount,
                        "paidAmount" to safePaid,
                        "status" to status.name,
                        "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
                        "paymentHistory" to paymentMaps,
                        "notes" to notes.trim().take(1500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Invoice ${invoiceNumber.trim()} created", "${clientName.trim()} • ${FormatUtils.formatInr(totalAmount)}", ActivityCategory.INVOICE, docRef.id)
            } else {
                val docRef = col.document(invoiceId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "invoiceNumber" to invoiceNumber.trim().take(40),
                        "clientId" to clientId.take(128),
                        "clientName" to clientName.trim().take(120),
                        "clientPhone" to clientPhone.trim().take(30),
                        "clientBusiness" to clientBusiness.trim().take(120),
                        "items" to itemMaps,
                        "subtotal" to subtotal,
                        "discountAmount" to safeDiscount,
                        "taxPercent" to safeTaxPercent,
                        "taxAmount" to taxAmount,
                        "totalAmount" to totalAmount,
                        "paidAmount" to safePaid,
                        "status" to status.name,
                        "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
                        "paymentHistory" to paymentMaps,
                        "notes" to notes.trim().take(1500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Invoice ${invoiceNumber.trim()} updated", "Status: ${status.label}", ActivityCategory.INVOICE, invoiceId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordInvoicePayment(
        uid: String,
        invoice: Invoice,
        paymentAmount: Double,
        method: String,
        note: String
    ): Result<Unit> {
        val path = "users/$uid/invoices/${invoice.id}"
        return try {
            val activeUid = requireUserId(uid)
            require(paymentAmount > 0.0) { "Payment amount must be greater than zero" }
            val newPaid = (invoice.paidAmount + paymentAmount).coerceAtMost(invoice.totalAmount)
            val newStatus = if (newPaid >= invoice.totalAmount - 0.01) InvoiceStatus.PAID else InvoiceStatus.PARTIALLY_PAID
            val updatedHistory = (invoice.paymentHistory + PaymentEntry(
                amount = paymentAmount,
                method = method.ifBlank { "UPI" },
                note = note.trim(),
                paidAtMillis = System.currentTimeMillis()
            )).takeLast(30)

            userDoc(activeUid).collection("invoices").document(invoice.id).update(
                mapOf(
                    "paidAmount" to newPaid,
                    "status" to newStatus.name,
                    "paymentHistory" to updatedHistory.map { it.toMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(activeUid, "Payment received: ${FormatUtils.formatInr(paymentAmount)}", "${invoice.invoiceNumber} • ${invoice.clientName}", ActivityCategory.INVOICE, invoice.id)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- QUOTATIONS CRUD ---
    suspend fun saveQuotation(
        uid: String,
        quotationId: String?,
        quotationNumber: String,
        clientId: String,
        clientName: String,
        clientPhone: String,
        clientBusiness: String,
        items: List<LineItem>,
        discountAmount: Double,
        status: QuotationStatus,
        validUntilMillis: Long,
        notes: String
    ): Result<Unit> {
        var path = "users/$uid/quotations"
        return try {
            val activeUid = requireUserId(uid)
            require(quotationNumber.trim().isNotEmpty()) { "Quotation number is required" }
            require(clientName.trim().isNotEmpty()) { "Client name is required" }
            require(items.isNotEmpty()) { "At least one service is required" }

            val boundedItems = items.take(20)
            val subtotal = boundedItems.sumOf { it.lineTotal }
            val safeDiscount = discountAmount.coerceIn(0.0, subtotal)
            val totalAmount = (subtotal - safeDiscount).coerceAtLeast(0.0)
            val itemMaps = boundedItems.map { it.toMap() }

            val col = userDoc(activeUid).collection("quotations")
            if (quotationId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "quotationNumber" to quotationNumber.trim().take(40),
                        "clientId" to clientId.take(128),
                        "clientName" to clientName.trim().take(120),
                        "clientPhone" to clientPhone.trim().take(30),
                        "clientBusiness" to clientBusiness.trim().take(120),
                        "items" to itemMaps,
                        "subtotal" to subtotal,
                        "discountAmount" to safeDiscount,
                        "totalAmount" to totalAmount,
                        "status" to status.name,
                        "validUntilMillis" to validUntilMillis.coerceAtLeast(0L),
                        "notes" to notes.trim().take(1500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Quotation ${quotationNumber.trim()} created", "${clientName.trim()} • ${FormatUtils.formatInr(totalAmount)}", ActivityCategory.QUOTATION, docRef.id)
            } else {
                val docRef = col.document(quotationId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "quotationNumber" to quotationNumber.trim().take(40),
                        "clientId" to clientId.take(128),
                        "clientName" to clientName.trim().take(120),
                        "clientPhone" to clientPhone.trim().take(30),
                        "clientBusiness" to clientBusiness.trim().take(120),
                        "items" to itemMaps,
                        "subtotal" to subtotal,
                        "discountAmount" to safeDiscount,
                        "totalAmount" to totalAmount,
                        "status" to status.name,
                        "validUntilMillis" to validUntilMillis.coerceAtLeast(0L),
                        "notes" to notes.trim().take(1500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun convertQuotationToProjectAndInvoice(uid: String, quotation: Quotation): Result<Unit> {
        val path = "users/$uid/quotations/${quotation.id}"
        return try {
            val activeUid = requireUserId(uid)
            val now = System.currentTimeMillis()
            val projectRef = userDoc(activeUid).collection("projects").document()
            val invoiceRef = userDoc(activeUid).collection("invoices").document()
            val quoteRef = userDoc(activeUid).collection("quotations").document(quotation.id)

            val serviceNames = quotation.items.map { it.serviceName }.distinct().take(25)
            val invoiceNumber = "RD-INV-${(1000..9999).random()}"

            val batch = db.batch()
            batch.set(
                projectRef,
                mapOf(
                    "userId" to activeUid,
                    "name" to "${quotation.clientBusiness.ifBlank { quotation.clientName }} - Campaign".take(120),
                    "description" to "Created from Quotation ${quotation.quotationNumber}: ${serviceNames.joinToString(", ")}".take(2000),
                    "clientId" to quotation.clientId.take(128),
                    "clientName" to quotation.clientName.take(120),
                    "startDateMillis" to now,
                    "deadlineMillis" to (now + 30L * 86_400_000L),
                    "budget" to quotation.totalAmount,
                    "selectedServices" to serviceNames,
                    "progress" to 10L,
                    "status" to ProjectStatus.IN_PROGRESS.name,
                    "assignedEmployeeId" to "",
                    "assignedEmployeeName" to "",
                    "notes" to quotation.notes.take(2000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                invoiceRef,
                mapOf(
                    "userId" to activeUid,
                    "invoiceNumber" to invoiceNumber,
                    "clientId" to quotation.clientId.take(128),
                    "clientName" to quotation.clientName.take(120),
                    "clientPhone" to quotation.clientPhone.take(30),
                    "clientBusiness" to quotation.clientBusiness.take(120),
                    "items" to quotation.items.take(20).map { it.toMap() },
                    "subtotal" to quotation.subtotal,
                    "discountAmount" to quotation.discountAmount,
                    "taxPercent" to 0.0,
                    "taxAmount" to 0.0,
                    "totalAmount" to quotation.totalAmount,
                    "paidAmount" to 0.0,
                    "status" to InvoiceStatus.UNPAID.name,
                    "dueDateMillis" to (now + 7L * 86_400_000L),
                    "paymentHistory" to emptyList<Map<String, Any>>(),
                    "notes" to "Converted from Quotation ${quotation.quotationNumber}".take(1500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                quoteRef,
                mapOf(
                    "status" to QuotationStatus.ACCEPTED.name,
                    "convertedProjectId" to projectRef.id,
                    "convertedInvoiceId" to invoiceRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            logActivity(
                activeUid,
                "Quotation ${quotation.quotationNumber} accepted",
                "Generated Project & Invoice $invoiceNumber",
                ActivityCategory.QUOTATION,
                quotation.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- EMPLOYEES CRUD ---
    suspend fun saveEmployee(
        uid: String,
        employeeId: String?,
        name: String,
        email: String,
        phone: String,
        designation: String,
        role: EmployeeRole,
        active: Boolean
    ): Result<Unit> {
        var path = "users/$uid/employees"
        return try {
            val activeUid = requireUserId(uid)
            require(name.trim().isNotEmpty()) { "Team member name is required" }
            val col = userDoc(activeUid).collection("employees")
            if (employeeId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to activeUid,
                        "name" to name.trim().take(100),
                        "email" to email.trim().take(120),
                        "phone" to phone.trim().take(30),
                        "designation" to designation.trim().take(80),
                        "role" to role.name,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(activeUid, "Team member added: ${name.trim()}", "${role.label} • ${designation.trim()}", ActivityCategory.TEAM, docRef.id)
            } else {
                val docRef = col.document(employeeId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(100),
                        "email" to email.trim().take(120),
                        "phone" to phone.trim().take(30),
                        "designation" to designation.trim().take(80),
                        "role" to role.name,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun logActivity(
        uid: String,
        title: String,
        detail: String,
        category: ActivityCategory,
        relatedId: String
    ) {
        try {
            userDoc(uid).collection("activities").document().set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(150),
                    "detail" to detail.take(400),
                    "category" to category.name,
                    "relatedId" to relatedId.take(128),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Activity feed logging should never fail the primary business write
        }
    }
}
