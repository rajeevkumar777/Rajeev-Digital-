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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgencyService
import com.example.data.model.Client
import com.example.data.model.EmployeeRole
import com.example.data.model.Invoice
import com.example.data.model.Lead
import com.example.data.model.LeadStatus
import com.example.data.model.Project
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
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FormatUtils
import com.example.util.WhatsAppHelper

@Composable
fun CrmScreen(viewModel: AgencyViewModel) {
    val context = LocalContext.current
    val leadsUi by viewModel.leadsState.collectAsStateWithLifecycle()
    val clientsUi by viewModel.clientsState.collectAsStateWithLifecycle()
    val servicesUi by viewModel.servicesState.collectAsStateWithLifecycle()
    val projectsUi by viewModel.projectsState.collectAsStateWithLifecycle()
    val invoicesUi by viewModel.invoicesState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN
    val services = (servicesUi as? UiState.Success)?.data.orEmpty().filter { it.active }
    val projects = (projectsUi as? UiState.Success)?.data.orEmpty()
    val invoices = (invoicesUi as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Leads, 1 = Clients
    var searchQuery by remember { mutableStateOf("") }
    var leadFilter by remember { mutableStateOf<LeadStatus?>(null) }

    var editingLead by remember { mutableStateOf<Lead?>(null) }
    var showLeadEditor by remember { mutableStateOf(false) }
    var deletingLead by remember { mutableStateOf<Lead?>(null) }

    var editingClient by remember { mutableStateOf<Client?>(null) }
    var showClientEditor by remember { mutableStateOf(false) }
    var detailClient by remember { mutableStateOf<Client?>(null) }
    var deletingClient by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingLead = null
                        showLeadEditor = true
                    } else {
                        editingClient = null
                        showClientEditor = true
                    }
                },
                containerColor = ElectricPurple,
                contentColor = Color.White,
                modifier = Modifier.testTag("crm_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Add Lead" else "Add Client")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("crm_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                title = "Leads & Clients CRM",
                subtitle = "WhatsApp follow-ups, conversions & account history"
            )
            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text("Leads")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text("Clients")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SearchInputBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (selectedTab == 0) "Search leads by name, business, phone..." else "Search clients...",
                testTag = "crm_search_input"
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = leadFilter == null,
                            onClick = { leadFilter = null },
                            label = { Text("All") }
                        )
                    }
                    items(LeadStatus.entries) { st ->
                        FilterChip(
                            selected = leadFilter == st,
                            onClick = { leadFilter = st },
                            label = { Text(st.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = leadsUi) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrightGold)
                    }
                    is UiState.Error -> EmptyStateCard(Icons.Default.PersonAdd, "Unable to load leads", state.message)
                    is UiState.Success -> {
                        val filtered = state.data.filter { lead ->
                            (leadFilter == null || lead.statusEnum() == leadFilter) &&
                                (searchQuery.isBlank() ||
                                    lead.name.contains(searchQuery, true) ||
                                    lead.businessName.contains(searchQuery, true) ||
                                    lead.phone.contains(searchQuery, true))
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.PersonAdd,
                                title = "No Leads Found",
                                description = "Tap + to add your first agency lead and start WhatsApp follow-ups.",
                                actionLabel = "Add Lead",
                                onAction = { editingLead = null; showLeadEditor = true }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { lead ->
                                    LeadCard(
                                        lead = lead,
                                        dateFormat = prefs.dateFormat,
                                        isAdmin = isAdmin,
                                        onWhatsApp = { WhatsAppHelper.sendLeadFollowUp(context, lead) },
                                        onConvert = { viewModel.convertLeadToClient(lead) },
                                        onEdit = { editingLead = lead; showLeadEditor = true },
                                        onDelete = { deletingLead = lead }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when (val state = clientsUi) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrightGold)
                    }
                    is UiState.Error -> EmptyStateCard(Icons.Default.Groups, "Unable to load clients", state.message)
                    is UiState.Success -> {
                        val filtered = state.data.filter { client ->
                            searchQuery.isBlank() ||
                                client.name.contains(searchQuery, true) ||
                                client.businessName.contains(searchQuery, true) ||
                                client.phone.contains(searchQuery, true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.Groups,
                                title = "No Clients Yet",
                                description = "Convert a lead or tap + to add a client profile.",
                                actionLabel = "Add Client",
                                onAction = { editingClient = null; showClientEditor = true }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { client ->
                                    ClientCard(
                                        client = client,
                                        projectCount = projects.count { it.clientId == client.id || it.clientName.equals(client.name, true) },
                                        isAdmin = isAdmin,
                                        onClick = { detailClient = client },
                                        onWhatsApp = { WhatsAppHelper.sendClientMessage(context, client) },
                                        onEdit = { editingClient = client; showClientEditor = true },
                                        onDelete = { deletingClient = client }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLeadEditor) {
        LeadEditorSheet(
            initial = editingLead,
            availableServices = services,
            dateFormat = prefs.dateFormat,
            onDismiss = { showLeadEditor = false },
            onSave = { name, phone, email, biz, source, svcs, status, notes, followUp ->
                viewModel.saveLead(editingLead?.id, name, phone, email, biz, source, svcs, status, notes, followUp)
                showLeadEditor = false
            }
        )
    }

    if (showClientEditor) {
        ClientEditorSheet(
            initial = editingClient,
            availableServices = services,
            onDismiss = { showClientEditor = false },
            onSave = { name, biz, phone, email, svcs, notes ->
                viewModel.saveClient(editingClient?.id, name, biz, phone, email, svcs, notes)
                showClientEditor = false
            }
        )
    }

    detailClient?.let { client ->
        ClientDetailSheet(
            client = client,
            projects = projects.filter { it.clientId == client.id || it.clientName.equals(client.name, true) },
            invoices = invoices.filter { it.clientId == client.id || it.clientName.equals(client.name, true) },
            dateFormat = prefs.dateFormat,
            onWhatsApp = { WhatsAppHelper.sendClientMessage(context, client) },
            onDismiss = { detailClient = null }
        )
    }

    deletingLead?.let { lead ->
        ConfirmDeleteDialog(
            title = "Delete Lead",
            message = "Delete lead \"${lead.name}\"?",
            onConfirm = { viewModel.deleteLead(lead.id); deletingLead = null },
            onDismiss = { deletingLead = null }
        )
    }

    deletingClient?.let { client ->
        ConfirmDeleteDialog(
            title = "Delete Client",
            message = "Delete client \"${client.name}\"?",
            onConfirm = { viewModel.deleteClient(client.id); deletingClient = null },
            onDismiss = { deletingClient = null }
        )
    }
}

@Composable
private fun LeadCard(
    lead: Lead,
    dateFormat: String,
    isAdmin: Boolean,
    onWhatsApp: () -> Unit,
    onConvert: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (lead.statusEnum()) {
        LeadStatus.NEW -> InfoSky
        LeadStatus.CONTACTED -> BrightPurple
        LeadStatus.INTERESTED -> BrightGold
        LeadStatus.FOLLOW_UP -> WarningAmber
        LeadStatus.CONVERTED -> SuccessEmerald
        LeadStatus.LOST -> ErrorCoral
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(lead.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (lead.businessName.isNotBlank()) {
                    Text(lead.businessName, style = MaterialTheme.typography.bodySmall, color = BrightGold)
                }
                Text("${lead.phone} • ${lead.source.ifBlank { "Direct" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(text = lead.statusEnum().label, color = statusColor)
        }

        if (lead.interestedServices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Interested: ${lead.interestedServices.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (lead.followUpDateMillis != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Follow-up: ${FormatUtils.formatDate(lead.followUpDateMillis, dateFormat)}",
                style = MaterialTheme.typography.labelSmall,
                color = WarningAmber
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                WhatsAppActionChip(label = "Follow-up", onClick = onWhatsApp)
                if (lead.statusEnum() != LeadStatus.CONVERTED) {
                    OutlinedButton(onClick = onConvert, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Convert to client")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Convert", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit lead", tint = BrightPurple)
                }
                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete lead", tint = ErrorCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientCard(
    client: Client,
    projectCount: Int,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (client.businessName.isNotBlank()) {
                    Text(client.businessName, style = MaterialTheme.typography.bodySmall, color = BrightGold)
                }
                Text(
                    text = "${client.phone}${if (client.email.isNotBlank()) " • ${client.email}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(text = "$projectCount project(s)", color = InfoSky)
        }

        if (client.selectedServices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Services: ${client.selectedServices.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WhatsAppActionChip(label = "WhatsApp", onClick = onWhatsApp)
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit client", tint = BrightPurple)
                }
                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete client", tint = ErrorCoral)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LeadEditorSheet(
    initial: Lead?,
    availableServices: List<AgencyService>,
    dateFormat: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<String>, LeadStatus, String, Long?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var business by remember { mutableStateOf(initial?.businessName ?: "") }
    var source by remember { mutableStateOf(initial?.source ?: "Instagram") }
    var selectedServices by remember { mutableStateOf(initial?.interestedServices ?: emptyList()) }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: LeadStatus.NEW) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var followUpMillis by remember { mutableStateOf(initial?.followUpDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val sources = listOf("Instagram", "WhatsApp", "Referral", "Meta Ads", "Google", "Website", "Other")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Add Lead" else "Edit Lead", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Lead Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone / WhatsApp *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = business, onValueChange = { business = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Lead Source", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources) { src ->
                    FilterChip(selected = source == src, onClick = { source = src }, label = { Text(src) })
                }
            }

            Text("Lead Status", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LeadStatus.entries) { st ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Follow-up: ${followUpMillis?.let { FormatUtils.formatDate(it, dateFormat) } ?: "Not scheduled"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Pick Date")
                }
            }

            Text("Interested Services", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                availableServices.take(18).forEach { srv ->
                    val chosen = srv.name in selectedServices
                    FilterChip(
                        selected = chosen,
                        onClick = {
                            selectedServices = if (chosen) selectedServices - srv.name else (selectedServices + srv.name).take(25)
                        },
                        label = { Text(srv.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (name.trim().isEmpty()) { error = "Lead name is required."; return@Button }
                    if (phone.trim().isEmpty()) { error = "Phone number is required."; return@Button }
                    onSave(name.trim(), phone.trim(), email.trim(), business.trim(), source, selectedServices, status, notes, followUpMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Lead")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        AgencyDatePickerDialog(
            initialMillis = followUpMillis ?: System.currentTimeMillis(),
            onDateSelected = { followUpMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ClientEditorSheet(
    initial: Client?,
    availableServices: List<AgencyService>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, List<String>, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var business by remember { mutableStateOf(initial?.businessName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var selectedServices by remember { mutableStateOf(initial?.selectedServices ?: emptyList()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Add Client" else "Edit Client", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = business, onValueChange = { business = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone / WhatsApp *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Selected Services", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                availableServices.take(18).forEach { srv ->
                    val chosen = srv.name in selectedServices
                    FilterChip(
                        selected = chosen,
                        onClick = {
                            selectedServices = if (chosen) selectedServices - srv.name else (selectedServices + srv.name).take(25)
                        },
                        label = { Text(srv.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes & Communication Log") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (name.trim().isEmpty()) { error = "Client name is required."; return@Button }
                    if (phone.trim().isEmpty()) { error = "Phone number is required."; return@Button }
                    onSave(name.trim(), business.trim(), phone.trim(), email.trim(), selectedServices, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Client")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailSheet(
    client: Client,
    projects: List<Project>,
    invoices: List<Invoice>,
    dateFormat: String,
    onWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, style = MaterialTheme.typography.headlineMedium, color = BrightGold)
                    if (client.businessName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, null, tint = BrightPurple)
                            Spacer(Modifier.width(6.dp))
                            Text(client.businessName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                WhatsAppActionChip(label = "Chat", onClick = onWhatsApp)
            }

            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, null, tint = WhatsAppGreen)
                    Spacer(Modifier.width(8.dp))
                    Text(client.phone.ifBlank { "No phone" }, style = MaterialTheme.typography.bodyMedium)
                }
                if (client.email.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = InfoSky)
                        Spacer(Modifier.width(8.dp))
                        Text(client.email, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (client.selectedServices.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Services: ${client.selectedServices.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = BrightGold)
                }
                if (client.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Notes & Communication:", style = MaterialTheme.typography.labelLarge)
                    Text(client.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("Project History (${projects.size})", style = MaterialTheme.typography.titleMedium)
            if (projects.isEmpty()) {
                Text("No projects linked yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                projects.forEach { p ->
                    GlassCard(contentPadding = PaddingValues(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            StatusPill("${p.statusEnum().label} • ${p.progress}%", InfoSky)
                        }
                        Text("Deadline: ${FormatUtils.formatDate(p.deadlineMillis, dateFormat)} • Budget: ${FormatUtils.formatInr(p.budget)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("Payment History (${invoices.size})", style = MaterialTheme.typography.titleMedium)
            if (invoices.isEmpty()) {
                Text("No invoices yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                invoices.forEach { inv ->
                    GlassCard(contentPadding = PaddingValues(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(inv.invoiceNumber, style = MaterialTheme.typography.titleSmall)
                            StatusPill(inv.statusEnum().label, SuccessEmerald)
                        }
                        Text("Total: ${FormatUtils.formatInr(inv.totalAmount)} • Paid: ${FormatUtils.formatInr(inv.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
