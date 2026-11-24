package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AgencyService
import com.example.data.model.BillingType
import com.example.data.model.EmployeeRole
import com.example.data.model.ServiceCategory
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: AgencyViewModel) {
    val servicesUi by viewModel.servicesState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    var showInactive by remember { mutableStateOf(true) }
    var editingService by remember { mutableStateOf<AgencyService?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingService by remember { mutableStateOf<AgencyService?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        editingService = null
                        showEditor = true
                    },
                    containerColor = ElectricPurple,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_service_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Service")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("services_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                title = "Services Catalogue",
                subtitle = "Confirmed INR packages & Custom Quote offerings"
            )
            Spacer(modifier = Modifier.height(10.dp))

            SearchInputBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search services (e.g. Instagram, Shopify, AI)...",
                testTag = "services_search_input"
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Categories") }
                    )
                }
                items(ServiceCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showInactive) "Showing Active & Inactive" else "Active Only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = showInactive,
                    onCheckedChange = { showInactive = it }
                )
            }

            when (val state = servicesUi) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrightGold)
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        icon = Icons.Default.DesignServices,
                        title = "Could not load services",
                        description = state.message
                    )
                }
                is UiState.Success -> {
                    val filtered = remember(state.data, searchQuery, selectedCategory, showInactive) {
                        state.data.filter { s ->
                            (selectedCategory == null || s.categoryEnum() == selectedCategory) &&
                                (showInactive || s.active) &&
                                (searchQuery.isBlank() ||
                                    s.name.contains(searchQuery, ignoreCase = true) ||
                                    s.description.contains(searchQuery, ignoreCase = true) ||
                                    s.categoryEnum().label.contains(searchQuery, ignoreCase = true))
                        }
                    }

                    if (filtered.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.DesignServices,
                            title = "No matching services",
                            description = "Adjust your category filter or search query."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { service ->
                                ServiceCard(
                                    service = service,
                                    isAdmin = isAdmin,
                                    onToggleActive = { viewModel.toggleServiceActive(service) },
                                    onEdit = {
                                        editingService = service
                                        showEditor = true
                                    },
                                    onDelete = { deletingService = service }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ServiceEditorSheet(
            initial = editingService,
            onDismiss = { showEditor = false },
            onSave = { name, cat, price, fixed, billing, desc, active ->
                viewModel.saveService(
                    serviceId = editingService?.id,
                    name = name,
                    category = cat,
                    price = price,
                    fixedPrice = fixed,
                    billingType = billing,
                    description = desc,
                    active = active
                )
                showEditor = false
            }
        )
    }

    deletingService?.let { srv ->
        ConfirmDeleteDialog(
            title = "Delete Service",
            message = "Remove \"${srv.name}\" from the agency catalogue?",
            onConfirm = {
                viewModel.deleteService(srv.id)
                deletingService = null
            },
            onDismiss = { deletingService = null }
        )
    }
}

@Composable
private fun ServiceCard(
    service: AgencyService,
    isAdmin: Boolean,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priceText = FormatUtils.formatServicePrice(service)
    val isCustomQuote = priceText == "Custom Quote"

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(text = service.categoryEnum().label, color = BrightPurple)
                    if (!service.active) {
                        StatusPill(text = "Inactive", color = ErrorCoral)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (service.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                StatusPill(
                    text = priceText,
                    color = if (isCustomQuote) Color(0xFF94A3B8) else BrightGold
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = service.active,
                            onCheckedChange = { onToggleActive() }
                        )
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit service", tint = BrightPurple)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete service", tint = ErrorCoral)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceEditorSheet(
    initial: AgencyService?,
    onDismiss: () -> Unit,
    onSave: (String, ServiceCategory, Double, Boolean, BillingType, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.categoryEnum() ?: ServiceCategory.DIGITAL_MARKETING) }
    var fixedPrice by remember { mutableStateOf(initial?.fixedPrice ?: false) }
    var priceText by remember { mutableStateOf(if ((initial?.price ?: 0.0) > 0) initial?.price?.toLong().toString() else "") }
    var billingType by remember { mutableStateOf(initial?.billingEnum() ?: BillingType.CUSTOM_QUOTE) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initial == null) "Add New Service" else "Edit Service",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Service Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ServiceCategory.entries) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fixed Price (off = Custom Quote)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = fixedPrice,
                    onCheckedChange = {
                        fixedPrice = it
                        if (!it) billingType = BillingType.CUSTOM_QUOTE
                        else if (billingType == BillingType.CUSTOM_QUOTE) billingType = BillingType.ONE_TIME
                    }
                )
            }

            if (fixedPrice) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Price (INR ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Billing Type", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(BillingType.MONTHLY, BillingType.ONE_TIME, BillingType.PER_SESSION)) { bt ->
                        FilterChip(
                            selected = billingType == bt,
                            onClick = { billingType = bt },
                            label = { Text(bt.label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active in Catalogue", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = active, onCheckedChange = { active = it })
            }

            if (error != null) {
                Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "Service name cannot be empty."
                        return@Button
                    }
                    val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
                    if (fixedPrice && parsedPrice <= 0.0) {
                        error = "Enter a valid positive INR price or switch to Custom Quote."
                        return@Button
                    }
                    onSave(trimmed, category, parsedPrice, fixedPrice, billingType, description, active)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save Service")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
