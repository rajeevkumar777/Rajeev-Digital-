package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AgencyRepository
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
import com.example.data.model.ServiceCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.preferences.LocalPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DashboardMetrics(
    val totalLeads: Int = 0,
    val totalClients: Int = 0,
    val activeProjects: Int = 0,
    val completedProjects: Int = 0,
    val totalRevenue: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val pendingTasksCount: Int = 0,
    val upcomingDeadlines: List<Project> = emptyList(),
    val upcomingTasks: List<AgencyTask> = emptyList()
)

data class ReminderItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val dueMillis: Long,
    val overdue: Boolean
)

class AgencyViewModel(
    private val repository: AgencyRepository,
    private val prefsRepository: UserPreferencesRepository,
    val currentUserId: String
) : ViewModel() {

    private val _actionMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val actionMessage = _actionMessage.asSharedFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    val localPrefs: StateFlow<LocalPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LocalPreferences())

    val profileState: StateFlow<UiState<AgencyProfile?>> = repository.observeProfile(currentUserId)
        .asUiState("profile")

    val servicesState: StateFlow<UiState<List<AgencyService>>> = repository.observeServices(currentUserId)
        .asUiState("services")

    val leadsState: StateFlow<UiState<List<Lead>>> = repository.observeLeads(currentUserId)
        .asUiState("leads")

    val clientsState: StateFlow<UiState<List<Client>>> = repository.observeClients(currentUserId)
        .asUiState("clients")

    val projectsState: StateFlow<UiState<List<Project>>> = repository.observeProjects(currentUserId)
        .asUiState("projects")

    val tasksState: StateFlow<UiState<List<AgencyTask>>> = repository.observeTasks(currentUserId)
        .asUiState("tasks")

    val invoicesState: StateFlow<UiState<List<Invoice>>> = repository.observeInvoices(currentUserId)
        .asUiState("invoices")

    val quotationsState: StateFlow<UiState<List<Quotation>>> = repository.observeQuotations(currentUserId)
        .asUiState("quotations")

    val employeesState: StateFlow<UiState<List<Employee>>> = repository.observeEmployees(currentUserId)
        .asUiState("employees")

    val activitiesState: StateFlow<UiState<List<ActivityLog>>> = repository.observeActivities(currentUserId)
        .asUiState("activities")

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        leadsState,
        clientsState,
        projectsState,
        tasksState,
        invoicesState
    ) { leadsUi, clientsUi, projectsUi, tasksUi, invoicesUi ->
        val leads = (leadsUi as? UiState.Success)?.data.orEmpty()
        val clients = (clientsUi as? UiState.Success)?.data.orEmpty()
        val projects = (projectsUi as? UiState.Success)?.data.orEmpty()
        val tasks = (tasksUi as? UiState.Success)?.data.orEmpty()
        val invoices = (invoicesUi as? UiState.Success)?.data.orEmpty()

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val totalRevenue = invoices.sumOf { it.paidAmount }
        val pendingPayments = invoices.sumOf { it.balanceDue }
        val monthlyRevenue = invoices.sumOf { inv ->
            val paymentInMonth = inv.paymentHistory.filter { entry ->
                val c = Calendar.getInstance().apply { timeInMillis = entry.paidAtMillis }
                c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
            }.sumOf { it.amount }
            if (paymentInMonth > 0) paymentInMonth
            else {
                val created = inv.createdAt?.toDate()?.time ?: 0L
                val c = Calendar.getInstance().apply { timeInMillis = created }
                if (c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear) inv.paidAmount else 0.0
            }
        }

        val upcomingProjects = projects
            .filter { it.statusEnum() != ProjectStatus.COMPLETED && it.deadlineMillis > 0L }
            .sortedBy { it.deadlineMillis }
            .take(5)

        val pendingTasks = tasks.filter { it.statusEnum() != TaskStatus.COMPLETED }
        val upcomingTasks = pendingTasks.sortedBy { it.dueDateMillis }.take(5)

        DashboardMetrics(
            totalLeads = leads.size,
            totalClients = clients.size,
            activeProjects = projects.count { it.statusEnum() == ProjectStatus.IN_PROGRESS || it.statusEnum() == ProjectStatus.PENDING },
            completedProjects = projects.count { it.statusEnum() == ProjectStatus.COMPLETED },
            totalRevenue = totalRevenue,
            pendingPayments = pendingPayments,
            monthlyRevenue = monthlyRevenue,
            pendingTasksCount = pendingTasks.size,
            upcomingDeadlines = upcomingProjects,
            upcomingTasks = upcomingTasks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardMetrics())

    val reminders: StateFlow<List<ReminderItem>> = combine(
        projectsState,
        tasksState,
        invoicesState,
        leadsState
    ) { projectsUi, tasksUi, invoicesUi, leadsUi ->
        val now = System.currentTimeMillis()
        val window = now + TimeUnit.DAYS.toMillis(7)
        val items = mutableListOf<ReminderItem>()

        (projectsUi as? UiState.Success)?.data.orEmpty()
            .filter { it.statusEnum() != ProjectStatus.COMPLETED && it.deadlineMillis in 1..window }
            .forEach { p ->
                items.add(
                    ReminderItem(
                        id = "proj_${p.id}",
                        title = "Project Deadline: ${p.name}",
                        subtitle = "${p.clientName.ifBlank { "Client" }} • ${p.progress}% done",
                        category = "Project",
                        dueMillis = p.deadlineMillis,
                        overdue = p.deadlineMillis < now
                    )
                )
            }

        (tasksUi as? UiState.Success)?.data.orEmpty()
            .filter { it.statusEnum() != TaskStatus.COMPLETED && it.dueDateMillis in 1..window }
            .forEach { t ->
                items.add(
                    ReminderItem(
                        id = "task_${t.id}",
                        title = "Task: ${t.title}",
                        subtitle = "${t.priorityEnum().label} priority • ${t.assignedEmployeeName.ifBlank { "Unassigned" }}",
                        category = "Task",
                        dueMillis = t.dueDateMillis,
                        overdue = t.dueDateMillis < now
                    )
                )
            }

        (invoicesUi as? UiState.Success)?.data.orEmpty()
            .filter { it.statusEnum() != InvoiceStatus.PAID && it.dueDateMillis in 1..window }
            .forEach { inv ->
                items.add(
                    ReminderItem(
                        id = "inv_${inv.id}",
                        title = "Invoice Due: ${inv.invoiceNumber}",
                        subtitle = "${inv.clientName} • Balance ₹${inv.balanceDue.toLong()}",
                        category = "Invoice",
                        dueMillis = inv.dueDateMillis,
                        overdue = inv.dueDateMillis < now
                    )
                )
            }

        (leadsUi as? UiState.Success)?.data.orEmpty()
            .filter {
                val s = it.statusEnum()
                s != LeadStatus.CONVERTED && s != LeadStatus.LOST && (it.followUpDateMillis ?: 0L) in 1..window
            }
            .forEach { lead ->
                val due = lead.followUpDateMillis ?: now
                items.add(
                    ReminderItem(
                        id = "lead_${lead.id}",
                        title = "Follow-up: ${lead.name}",
                        subtitle = "${lead.businessName.ifBlank { lead.phone }} • ${lead.statusEnum().label}",
                        category = "Lead",
                        dueMillis = due,
                        overdue = due < now
                    )
                )
            }

        items.sortedBy { it.dueMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    init {
        viewModelScope.launch {
            _isInitializing.value = true
            val res = repository.ensureWorkspaceInitialized(currentUserId)
            res.onFailure {
                _actionMessage.tryEmit("Workspace sync issue: ${it.localizedMessage ?: "Check connection"}")
            }
            _isInitializing.value = false
        }
    }

    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private fun launchResult(successMessage: String, block: suspend () -> Result<Any>) {
        viewModelScope.launch {
            val res = block()
            res.onSuccess {
                if (successMessage.isNotBlank()) _actionMessage.tryEmit(successMessage)
            }.onFailure { err ->
                _actionMessage.tryEmit(err.localizedMessage ?: "Operation failed")
            }
        }
    }

    // --- Preferences & Profile ---
    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun setDateFormat(format: String) {
        viewModelScope.launch { prefsRepository.setDateFormat(format) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setNotificationsEnabled(enabled) }
    }

    fun setActiveRole(role: EmployeeRole) {
        viewModelScope.launch {
            prefsRepository.setActiveRole(role)
            _actionMessage.tryEmit("Switched workspace access to ${role.label} mode")
        }
    }

    fun updateProfile(
        displayName: String,
        businessName: String,
        tagline: String,
        whatsappNumber: String,
        instagramHandle: String,
        dateFormat: String,
        themeMode: String,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            prefsRepository.setDateFormat(dateFormat)
            prefsRepository.setThemeMode(themeMode)
            prefsRepository.setNotificationsEnabled(notificationsEnabled)
            val res = repository.updateProfile(
                currentUserId, displayName, businessName, tagline,
                whatsappNumber, instagramHandle, dateFormat, themeMode, notificationsEnabled
            )
            res.onSuccess { _actionMessage.tryEmit("Profile & settings saved") }
                .onFailure { _actionMessage.tryEmit(it.localizedMessage ?: "Failed to save settings") }
        }
    }

    // --- Services ---
    fun saveService(
        serviceId: String?,
        name: String,
        category: ServiceCategory,
        price: Double,
        fixedPrice: Boolean,
        billingType: BillingType,
        description: String,
        active: Boolean
    ) = launchResult(if (serviceId == null) "Service added" else "Service updated") {
        repository.saveService(currentUserId, serviceId, name, category, price, fixedPrice, billingType, description, active)
    }

    fun toggleServiceActive(service: AgencyService) = launchResult("") {
        repository.toggleServiceActive(currentUserId, service.id, !service.active)
    }

    fun deleteService(serviceId: String) = launchResult("Service removed") {
        repository.deleteDocument(currentUserId, "services", serviceId)
    }

    // --- Leads ---
    fun saveLead(
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
    ) = launchResult(if (leadId == null) "Lead added" else "Lead updated") {
        repository.saveLead(currentUserId, leadId, name, phone, email, businessName, source, interestedServices, status, notes, followUpDateMillis)
    }

    fun convertLeadToClient(lead: Lead) = launchResult("Lead converted to client!") {
        repository.convertLeadToClient(currentUserId, lead)
    }

    fun deleteLead(leadId: String) = launchResult("Lead deleted") {
        repository.deleteDocument(currentUserId, "leads", leadId)
    }

    // --- Clients ---
    fun saveClient(
        clientId: String?,
        name: String,
        businessName: String,
        phone: String,
        email: String,
        selectedServices: List<String>,
        notes: String
    ) = launchResult(if (clientId == null) "Client created" else "Client updated") {
        repository.saveClient(currentUserId, clientId, name, businessName, phone, email, selectedServices, notes)
    }

    fun deleteClient(clientId: String) = launchResult("Client deleted") {
        repository.deleteDocument(currentUserId, "clients", clientId)
    }

    // --- Projects ---
    fun saveProject(
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
    ) = launchResult(if (projectId == null) "Project created" else "Project updated") {
        repository.saveProject(
            currentUserId, projectId, name, description, clientId, clientName,
            startDateMillis, deadlineMillis, budget, selectedServices, progress, status,
            assignedEmployeeId, assignedEmployeeName, notes
        )
    }

    fun deleteProject(projectId: String) = launchResult("Project deleted") {
        repository.deleteDocument(currentUserId, "projects", projectId)
    }

    // --- Tasks ---
    fun saveTask(
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
    ) = launchResult(if (taskId == null) "Task created" else "Task updated") {
        repository.saveTask(currentUserId, taskId, title, description, projectId, projectName, priority, status, dueDateMillis, assignedEmployeeId, assignedEmployeeName)
    }

    fun toggleTaskCompletion(task: AgencyTask) = launchResult("") {
        val next = if (task.statusEnum() == TaskStatus.COMPLETED) TaskStatus.TODO else TaskStatus.COMPLETED
        repository.updateTaskStatus(currentUserId, task.id, next)
    }

    fun deleteTask(taskId: String) = launchResult("Task deleted") {
        repository.deleteDocument(currentUserId, "tasks", taskId)
    }

    // --- Invoices ---
    fun saveInvoice(
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
    ) = launchResult(if (invoiceId == null) "Invoice created" else "Invoice updated") {
        repository.saveInvoice(
            currentUserId, invoiceId, invoiceNumber, clientId, clientName, clientPhone,
            clientBusiness, items, discountAmount, taxPercent, paidAmount, dueDateMillis, paymentHistory, notes
        )
    }

    fun recordInvoicePayment(invoice: Invoice, amount: Double, method: String, note: String) =
        launchResult("Payment recorded") {
            repository.recordInvoicePayment(currentUserId, invoice, amount, method, note)
        }

    fun deleteInvoice(invoiceId: String) = launchResult("Invoice deleted") {
        repository.deleteDocument(currentUserId, "invoices", invoiceId)
    }

    // --- Quotations ---
    fun saveQuotation(
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
    ) = launchResult(if (quotationId == null) "Quotation created" else "Quotation updated") {
        repository.saveQuotation(
            currentUserId, quotationId, quotationNumber, clientId, clientName, clientPhone,
            clientBusiness, items, discountAmount, status, validUntilMillis, notes
        )
    }

    fun convertQuotation(quotation: Quotation) = launchResult("Converted to Project & Invoice!") {
        repository.convertQuotationToProjectAndInvoice(currentUserId, quotation)
    }

    fun deleteQuotation(quotationId: String) = launchResult("Quotation deleted") {
        repository.deleteDocument(currentUserId, "quotations", quotationId)
    }

    // --- Employees ---
    fun saveEmployee(
        employeeId: String?,
        name: String,
        email: String,
        phone: String,
        designation: String,
        role: EmployeeRole,
        active: Boolean
    ) = launchResult(if (employeeId == null) "Team member added" else "Team member updated") {
        repository.saveEmployee(currentUserId, employeeId, name, email, phone, designation, role, active)
    }

    fun deleteEmployee(employeeId: String) = launchResult("Team member removed") {
        repository.deleteDocument(currentUserId, "employees", employeeId)
    }

    private companion object {
        const val TAG = "AgencyVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
