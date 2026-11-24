package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgencyService
import com.example.data.model.AgencyTask
import com.example.data.model.Client
import com.example.data.model.Employee
import com.example.data.model.EmployeeRole
import com.example.data.model.Project
import com.example.data.model.ProjectStatus
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.ui.components.AgencyDatePickerDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.WhatsAppActionChip
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FormatUtils
import com.example.util.WhatsAppHelper

@Composable
fun WorkScreen(viewModel: AgencyViewModel) {
    val context = LocalContext.current
    val projectsUi by viewModel.projectsState.collectAsStateWithLifecycle()
    val tasksUi by viewModel.tasksState.collectAsStateWithLifecycle()
    val clientsUi by viewModel.clientsState.collectAsStateWithLifecycle()
    val servicesUi by viewModel.servicesState.collectAsStateWithLifecycle()
    val employeesUi by viewModel.employeesState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN
    val clients = (clientsUi as? UiState.Success)?.data.orEmpty()
    val services = (servicesUi as? UiState.Success)?.data.orEmpty().filter { it.active }
    val employees = (employeesUi as? UiState.Success)?.data.orEmpty().filter { it.active }
    val projects = (projectsUi as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Projects, 1 = Tasks
    var searchQuery by remember { mutableStateOf("") }
    var projectFilter by remember { mutableStateOf<ProjectStatus?>(null) }
    var taskFilter by remember { mutableStateOf<TaskStatus?>(null) }

    var editingProject by remember { mutableStateOf<Project?>(null) }
    var showProjectEditor by remember { mutableStateOf(false) }
    var deletingProject by remember { mutableStateOf<Project?>(null) }

    var editingTask by remember { mutableStateOf<AgencyTask?>(null) }
    var showTaskEditor by remember { mutableStateOf(false) }
    var deletingTask by remember { mutableStateOf<AgencyTask?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingProject = null
                        showProjectEditor = true
                    } else {
                        editingTask = null
                        showTaskEditor = true
                    }
                },
                containerColor = ElectricPurple,
                contentColor = Color.White,
                modifier = Modifier.testTag("work_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Add Project" else "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("work_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                title = "Projects & Tasks",
                subtitle = "Delivery progress, deadlines & team assignments"
            )
            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Projects") }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Tasks") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SearchInputBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (selectedTab == 0) "Search projects or clients..." else "Search tasks or assignees...",
                testTag = "work_search_input"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = projectFilter == null, onClick = { projectFilter = null }, label = { Text("All") })
                    }
                    items(ProjectStatus.entries) { st ->
                        FilterChip(selected = projectFilter == st, onClick = { projectFilter = st }, label = { Text(st.label) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = projectsUi) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrightGold) }
                    is UiState.Error -> EmptyStateCard(Icons.Default.FolderSpecial, "Error loading projects", state.message)
                    is UiState.Success -> {
                        val filtered = state.data.filter { p ->
                            (projectFilter == null || p.statusEnum() == projectFilter) &&
                                (searchQuery.isBlank() || p.name.contains(searchQuery, true) || p.clientName.contains(searchQuery, true))
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.FolderSpecial,
                                title = "No Projects Found",
                                description = "Create a project or convert an accepted quotation.",
                                actionLabel = "New Project",
                                onAction = { editingProject = null; showProjectEditor = true }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { project ->
                                    val clientPhone = clients.firstOrNull {
                                        it.id == project.clientId || it.name.equals(project.clientName, true)
                                    }?.phone.orEmpty()
                                    ProjectCard(
                                        project = project,
                                        dateFormat = prefs.dateFormat,
                                        isAdmin = isAdmin,
                                        onWhatsApp = { WhatsAppHelper.sendProjectUpdate(context, project, clientPhone) },
                                        onEdit = { editingProject = project; showProjectEditor = true },
                                        onDelete = { deletingProject = project }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = taskFilter == null, onClick = { taskFilter = null }, label = { Text("All") })
                    }
                    items(TaskStatus.entries) { st ->
                        FilterChip(selected = taskFilter == st, onClick = { taskFilter = st }, label = { Text(st.label) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = tasksUi) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrightGold) }
                    is UiState.Error -> EmptyStateCard(Icons.Default.AssignmentTurnedIn, "Error loading tasks", state.message)
                    is UiState.Success -> {
                        val filtered = state.data.filter { t ->
                            (taskFilter == null || t.statusEnum() == taskFilter) &&
                                (searchQuery.isBlank() || t.title.contains(searchQuery, true) || t.assignedEmployeeName.contains(searchQuery, true))
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.AssignmentTurnedIn,
                                title = "No Tasks Found",
                                description = "Tap + to assign tasks with priorities and due dates.",
                                actionLabel = "Add Task",
                                onAction = { editingTask = null; showTaskEditor = true }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        dateFormat = prefs.dateFormat,
                                        isAdmin = isAdmin,
                                        onToggleDone = { viewModel.toggleTaskCompletion(task) },
                                        onEdit = { editingTask = task; showTaskEditor = true },
                                        onDelete = { deletingTask = task }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProjectEditor) {
        ProjectEditorSheet(
            initial = editingProject,
            clients = clients,
            services = services,
            employees = employees,
            dateFormat = prefs.dateFormat,
            onDismiss = { showProjectEditor = false },
            onSave = { name, desc, cid, cname, start, deadline, budget, svcs, progress, status, eid, ename, notes ->
                viewModel.saveProject(editingProject?.id, name, desc, cid, cname, start, deadline, budget, svcs, progress, status, eid, ename, notes)
                showProjectEditor = false
            }
        )
    }

    if (showTaskEditor) {
        TaskEditorSheet(
            initial = editingTask,
            projects = projects,
            employees = employees,
            dateFormat = prefs.dateFormat,
            onDismiss = { showTaskEditor = false },
            onSave = { title, desc, pid, pname, priority, status, due, eid, ename ->
                viewModel.saveTask(editingTask?.id, title, desc, pid, pname, priority, status, due, eid, ename)
                showTaskEditor = false
            }
        )
    }

    deletingProject?.let { p ->
        ConfirmDeleteDialog(
            title = "Delete Project",
            message = "Delete project \"${p.name}\"?",
            onConfirm = { viewModel.deleteProject(p.id); deletingProject = null },
            onDismiss = { deletingProject = null }
        )
    }

    deletingTask?.let { t ->
        ConfirmDeleteDialog(
            title = "Delete Task",
            message = "Delete task \"${t.title}\"?",
            onConfirm = { viewModel.deleteTask(t.id); deletingTask = null },
            onDismiss = { deletingTask = null }
        )
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    dateFormat: String,
    isAdmin: Boolean,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (project.statusEnum()) {
        ProjectStatus.PENDING -> WarningAmber
        ProjectStatus.IN_PROGRESS -> InfoSky
        ProjectStatus.COMPLETED -> SuccessEmerald
        ProjectStatus.ON_HOLD -> ErrorCoral
    }

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${project.clientName.ifBlank { "Agency" }} • Budget: ${FormatUtils.formatInr(project.budget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrightGold
                )
            }
            StatusPill(project.statusEnum().label, statusColor)
        }

        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (project.progress / 100f).coerceIn(0f, 1f) },
            color = statusColor,
            trackColor = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth().height(7.dp)
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${project.progress}% complete", style = MaterialTheme.typography.labelSmall, color = statusColor)
            Text("Deadline: ${FormatUtils.formatDate(project.deadlineMillis, dateFormat)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (project.assignedEmployeeName.isNotBlank()) {
            Text("Assignee: ${project.assignedEmployeeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            WhatsAppActionChip(label = "Send Update", onClick = onWhatsApp)
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit project", tint = BrightPurple) }
                if (isAdmin) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete project", tint = ErrorCoral) }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: AgencyTask,
    dateFormat: String,
    isAdmin: Boolean,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.statusEnum() == TaskStatus.COMPLETED
    val priorityColor = when (task.priorityEnum()) {
        TaskPriority.LOW -> SuccessEmerald
        TaskPriority.MEDIUM -> BrightPurple
        TaskPriority.HIGH -> WarningAmber
        TaskPriority.URGENT -> ErrorCoral
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isDone, onCheckedChange = { onToggleDone() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${task.projectName.ifBlank { "General" }} • Due ${FormatUtils.formatDate(task.dueDateMillis, dateFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.assignedEmployeeName.isNotBlank()) {
                    Text("Assigned: ${task.assignedEmployeeName}", style = MaterialTheme.typography.labelSmall, color = InfoSky)
                }
            }
            StatusPill(task.priorityEnum().label, priorityColor)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit task", tint = BrightPurple) }
            if (isAdmin) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete task", tint = ErrorCoral) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectEditorSheet(
    initial: Project?,
    clients: List<Client>,
    services: List<AgencyService>,
    employees: List<Employee>,
    dateFormat: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long, Long, Double, List<String>, Int, ProjectStatus, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var clientId by remember { mutableStateOf(initial?.clientId ?: "") }
    var clientName by remember { mutableStateOf(initial?.clientName ?: "") }
    var startMillis by remember { mutableStateOf(initial?.startDateMillis ?: System.currentTimeMillis()) }
    var deadlineMillis by remember { mutableStateOf(initial?.deadlineMillis ?: (System.currentTimeMillis() + 14L * 86_400_000L)) }
    var budgetText by remember { mutableStateOf(if ((initial?.budget ?: 0.0) > 0) initial?.budget?.toLong().toString() else "") }
    var selectedServices by remember { mutableStateOf(initial?.selectedServices ?: emptyList()) }
    var progress by remember { mutableFloatStateOf((initial?.progress ?: 0L).toFloat()) }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: ProjectStatus.IN_PROGRESS) }
    var empId by remember { mutableStateOf(initial?.assignedEmployeeId ?: "") }
    var empName by remember { mutableStateOf(initial?.assignedEmployeeName ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingDeadline by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Create Project" else "Edit Project", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Project Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (clients.isNotEmpty()) {
                Text("Select Client", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clients) { c ->
                        FilterChip(
                            selected = clientId == c.id,
                            onClick = { clientId = c.id; clientName = c.name },
                            label = { Text(c.name) }
                        )
                    }
                }
            }
            OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { pickingStart = true }) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Start: ${FormatUtils.formatDate(startMillis, dateFormat)}")
                }
                TextButton(onClick = { pickingDeadline = true }) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Due: ${FormatUtils.formatDate(deadlineMillis, dateFormat)}")
                }
            }

            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Budget (INR ₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Status", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProjectStatus.entries) { st ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }

            Text("Progress: ${progress.toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)

            if (employees.isNotEmpty()) {
                Text("Assign Team Member", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(employees) { emp ->
                        FilterChip(
                            selected = empId == emp.id,
                            onClick = { empId = emp.id; empName = emp.name },
                            label = { Text(emp.name) }
                        )
                    }
                }
            }

            Text("Services", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                services.take(15).forEach { srv ->
                    val chosen = srv.name in selectedServices
                    FilterChip(
                        selected = chosen,
                        onClick = { selectedServices = if (chosen) selectedServices - srv.name else (selectedServices + srv.name).take(25) },
                        label = { Text(srv.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Project Updates & Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (name.trim().isEmpty()) { error = "Project name is required."; return@Button }
                    onSave(
                        name.trim(), description, clientId, clientName.trim(),
                        startMillis, deadlineMillis, budgetText.toDoubleOrNull() ?: 0.0,
                        selectedServices, progress.toInt(), status, empId, empName, notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Project") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingStart) {
        AgencyDatePickerDialog(startMillis, { startMillis = it }, { pickingStart = false })
    }
    if (pickingDeadline) {
        AgencyDatePickerDialog(deadlineMillis, { deadlineMillis = it }, { pickingDeadline = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    initial: AgencyTask?,
    projects: List<Project>,
    employees: List<Employee>,
    dateFormat: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, TaskPriority, TaskStatus, Long, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var projectId by remember { mutableStateOf(initial?.projectId ?: "") }
    var projectName by remember { mutableStateOf(initial?.projectName ?: "") }
    var priority by remember { mutableStateOf(initial?.priorityEnum() ?: TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: TaskStatus.TODO) }
    var dueDateMillis by remember { mutableStateOf(initial?.dueDateMillis ?: (System.currentTimeMillis() + 2L * 86_400_000L)) }
    var empId by remember { mutableStateOf(initial?.assignedEmployeeId ?: "") }
    var empName by remember { mutableStateOf(initial?.assignedEmployeeName ?: "") }
    var pickingDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Create Task" else "Edit Task", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = title, onValueChange = { title = it; error = null }, label = { Text("Task Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (projects.isNotEmpty()) {
                Text("Project", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(projects) { p ->
                        FilterChip(
                            selected = projectId == p.id,
                            onClick = { projectId = p.id; projectName = p.name },
                            label = { Text(p.name) }
                        )
                    }
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskPriority.entries) { pr ->
                    FilterChip(selected = priority == pr, onClick = { priority = pr }, label = { Text(pr.label) })
                }
            }

            Text("Status", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskStatus.entries) { st ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Due: ${FormatUtils.formatDate(dueDateMillis, dateFormat)}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { pickingDate = true }) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Change Date")
                }
            }

            if (employees.isNotEmpty()) {
                Text("Assign Employee", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(employees) { emp ->
                        FilterChip(
                            selected = empId == emp.id,
                            onClick = { empId = emp.id; empName = emp.name },
                            label = { Text(emp.name) }
                        )
                    }
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Task Details") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (title.trim().isEmpty()) { error = "Task title is required."; return@Button }
                    onSave(title.trim(), description, projectId, projectName, priority, status, dueDateMillis, empId, empName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Task") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingDate) {
        AgencyDatePickerDialog(dueDateMillis, { dueDateMillis = it }, { pickingDate = false })
    }
}
