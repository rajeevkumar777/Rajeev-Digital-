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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.InvoiceStatus
import com.example.data.model.LineItem
import com.example.data.model.Quotation
import com.example.data.model.QuotationStatus
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
import com.example.util.PdfGenerator
import com.example.util.WhatsAppHelper

@Composable
fun BillingScreen(viewModel: AgencyViewModel) {
    val context = LocalContext.current
    val invoicesUi by viewModel.invoicesState.collectAsStateWithLifecycle()
    val quotationsUi by viewModel.quotationsState.collectAsStateWithLifecycle()
    val clientsUi by viewModel.clientsState.collectAsStateWithLifecycle()
    val servicesUi by viewModel.servicesState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN
    val clients = (clientsUi as? UiState.Success)?.data.orEmpty()
    val services = (servicesUi as? UiState.Success)?.data.orEmpty().filter { it.active }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Invoices, 1 = Quotations
    var searchQuery by remember { mutableStateOf("") }
    var invoiceFilter by remember { mutableStateOf<InvoiceStatus?>(null) }

    var editingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var showInvoiceEditor by remember { mutableStateOf(false) }
    var paymentTargetInvoice by remember { mutableStateOf<Invoice?>(null) }
    var deletingInvoice by remember { mutableStateOf<Invoice?>(null) }

    var editingQuotation by remember { mutableStateOf<Quotation?>(null) }
    var showQuotationEditor by remember { mutableStateOf(false) }
    var deletingQuotation by remember { mutableStateOf<Quotation?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (isAdmin || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            editingInvoice = null
                            showInvoiceEditor = true
                        } else {
                            editingQuotation = null
                            showQuotationEditor = true
                        }
                    },
                    containerColor = ElectricPurple,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("billing_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Create Invoice" else "Create Quotation")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("billing_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                title = "Invoices & Quotations",
                subtitle = "INR billing, PDF sharing & WhatsApp dispatch"
            )
            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Invoices") }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Quotations") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SearchInputBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (selectedTab == 0) "Search invoice # or client..." else "Search quotation # or client...",
                testTag = "billing_search_input"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = invoiceFilter == null, onClick = { invoiceFilter = null }, label = { Text("All") }) }
                    items(InvoiceStatus.entries) { st ->
                        FilterChip(selected = invoiceFilter == st, onClick = { invoiceFilter = st }, label = { Text(st.label) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = invoicesUi) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrightGold) }
                    is UiState.Error -> EmptyStateCard(Icons.AutoMirrored.Filled.ReceiptLong, "Unable to load invoices", state.message)
                    is UiState.Success -> {
                        val filtered = state.data.filter { inv ->
                            (invoiceFilter == null || inv.statusEnum() == invoiceFilter) &&
                                (searchQuery.isBlank() || inv.invoiceNumber.contains(searchQuery, true) || inv.clientName.contains(searchQuery, true))
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = "No Invoices Yet",
                                description = "Create a professional INR invoice or convert an accepted quotation.",
                                actionLabel = if (isAdmin) "Create Invoice" else null,
                                onAction = if (isAdmin) { { editingInvoice = null; showInvoiceEditor = true } } else null
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { inv ->
                                    InvoiceCard(
                                        invoice = inv,
                                        dateFormat = prefs.dateFormat,
                                        isAdmin = isAdmin,
                                        onRecordPayment = { paymentTargetInvoice = inv },
                                        onSharePdf = { PdfGenerator.shareInvoicePdf(context, inv) },
                                        onWhatsApp = { WhatsAppHelper.sendInvoice(context, inv) },
                                        onEdit = { editingInvoice = inv; showInvoiceEditor = true },
                                        onDelete = { deletingInvoice = inv }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when (val state = quotationsUi) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BrightGold) }
                    is UiState.Error -> EmptyStateCard(Icons.Default.RequestQuote, "Unable to load quotations", state.message)
                    is UiState.Success -> {
                        val filtered = state.data.filter { qt ->
                            searchQuery.isBlank() || qt.quotationNumber.contains(searchQuery, true) || qt.clientName.contains(searchQuery, true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Default.RequestQuote,
                                title = "No Quotations Yet",
                                description = "Generate a multi-service proposal with custom pricing and share on WhatsApp.",
                                actionLabel = "Create Quotation",
                                onAction = { editingQuotation = null; showQuotationEditor = true }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { qt ->
                                    QuotationCard(
                                        quotation = qt,
                                        dateFormat = prefs.dateFormat,
                                        isAdmin = isAdmin,
                                        onSharePdf = { PdfGenerator.shareQuotationPdf(context, qt) },
                                        onWhatsApp = { WhatsAppHelper.sendQuotation(context, qt) },
                                        onConvert = { viewModel.convertQuotation(qt) },
                                        onEdit = { editingQuotation = qt; showQuotationEditor = true },
                                        onDelete = { deletingQuotation = qt }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInvoiceEditor) {
        InvoiceEditorSheet(
            initial = editingInvoice,
            clients = clients,
            services = services,
            dateFormat = prefs.dateFormat,
            onDismiss = { showInvoiceEditor = false },
            onSave = { number, cid, cname, cphone, cbiz, items, discount, tax, paid, due, notes ->
                viewModel.saveInvoice(
                    invoiceId = editingInvoice?.id,
                    invoiceNumber = number,
                    clientId = cid,
                    clientName = cname,
                    clientPhone = cphone,
                    clientBusiness = cbiz,
                    items = items,
                    discountAmount = discount,
                    taxPercent = tax,
                    paidAmount = paid,
                    dueDateMillis = due,
                    paymentHistory = editingInvoice?.paymentHistory ?: emptyList(),
                    notes = notes
                )
                showInvoiceEditor = false
            }
        )
    }

    if (showQuotationEditor) {
        QuotationEditorSheet(
            initial = editingQuotation,
            clients = clients,
            services = services,
            dateFormat = prefs.dateFormat,
            onDismiss = { showQuotationEditor = false },
            onSave = { number, cid, cname, cphone, cbiz, items, discount, status, validUntil, notes ->
                viewModel.saveQuotation(
                    quotationId = editingQuotation?.id,
                    quotationNumber = number,
                    clientId = cid,
                    clientName = cname,
                    clientPhone = cphone,
                    clientBusiness = cbiz,
                    items = items,
                    discountAmount = discount,
                    status = status,
                    validUntilMillis = validUntil,
                    notes = notes
                )
                showQuotationEditor = false
            }
        )
    }

    paymentTargetInvoice?.let { inv ->
        RecordPaymentDialog(
            invoice = inv,
            onDismiss = { paymentTargetInvoice = null },
            onConfirm = { amount, method, note ->
                viewModel.recordInvoicePayment(inv, amount, method, note)
                paymentTargetInvoice = null
            }
        )
    }

    deletingInvoice?.let { inv ->
        ConfirmDeleteDialog(
            title = "Delete Invoice",
            message = "Delete invoice ${inv.invoiceNumber}?",
            onConfirm = { viewModel.deleteInvoice(inv.id); deletingInvoice = null },
            onDismiss = { deletingInvoice = null }
        )
    }

    deletingQuotation?.let { qt ->
        ConfirmDeleteDialog(
            title = "Delete Quotation",
            message = "Delete quotation ${qt.quotationNumber}?",
            onConfirm = { viewModel.deleteQuotation(qt.id); deletingQuotation = null },
            onDismiss = { deletingQuotation = null }
        )
    }
}

@Composable
private fun InvoiceCard(
    invoice: Invoice,
    dateFormat: String,
    isAdmin: Boolean,
    onRecordPayment: () -> Unit,
    onSharePdf: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (invoice.statusEnum()) {
        InvoiceStatus.PAID -> SuccessEmerald
        InvoiceStatus.PARTIALLY_PAID -> WarningAmber
        InvoiceStatus.UNPAID -> ErrorCoral
    }

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrightGold)
                Text("${invoice.clientName}${if (invoice.clientBusiness.isNotBlank()) " • ${invoice.clientBusiness}" else ""}", style = MaterialTheme.typography.bodyMedium)
                Text("Due: ${FormatUtils.formatDate(invoice.dueDateMillis, dateFormat)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(invoice.statusEnum().label, statusColor)
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total: ${FormatUtils.formatInr(invoice.totalAmount)}", style = MaterialTheme.typography.labelLarge)
            Text("Paid: ${FormatUtils.formatInr(invoice.paidAmount)}", style = MaterialTheme.typography.labelMedium, color = SuccessEmerald)
            Text("Due: ${FormatUtils.formatInr(invoice.balanceDue)}", style = MaterialTheme.typography.labelMedium, color = statusColor)
        }

        if (invoice.paymentHistory.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Last payment: ${FormatUtils.formatInr(invoice.paymentHistory.last().amount)} via ${invoice.paymentHistory.last().method}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { WhatsAppActionChip(label = "WhatsApp", onClick = onWhatsApp) }
            item {
                OutlinedButton(onClick = onSharePdf, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", tint = BrightGold)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isAdmin && invoice.statusEnum() != InvoiceStatus.PAID) {
                item {
                    OutlinedButton(onClick = onRecordPayment, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Payments, contentDescription = "Record Payment", tint = SuccessEmerald)
                        Spacer(Modifier.width(4.dp))
                        Text("Pay", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (isAdmin) {
                item { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit invoice", tint = BrightPurple) } }
                item { IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete invoice", tint = ErrorCoral) } }
            }
        }
    }
}

@Composable
private fun QuotationCard(
    quotation: Quotation,
    dateFormat: String,
    isAdmin: Boolean,
    onSharePdf: () -> Unit,
    onWhatsApp: () -> Unit,
    onConvert: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (quotation.statusEnum()) {
        QuotationStatus.DRAFT -> InfoSky
        QuotationStatus.SENT -> BrightPurple
        QuotationStatus.ACCEPTED -> SuccessEmerald
        QuotationStatus.DECLINED -> ErrorCoral
    }

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(quotation.quotationNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrightGold)
                Text("${quotation.clientName}${if (quotation.clientBusiness.isNotBlank()) " • ${quotation.clientBusiness}" else ""}", style = MaterialTheme.typography.bodyMedium)
                Text("${quotation.items.size} service(s) • Valid until ${FormatUtils.formatDate(quotation.validUntilMillis, dateFormat)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(quotation.statusEnum().label, statusColor)
        }

        Spacer(Modifier.height(8.dp))
        Text("Total Quote: ${FormatUtils.formatInr(quotation.totalAmount)}", style = MaterialTheme.typography.titleMedium, color = BrightGold)

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item { WhatsAppActionChip(label = "Share Quote", onClick = onWhatsApp) }
            item {
                OutlinedButton(onClick = onSharePdf, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Quote PDF", tint = BrightGold)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (quotation.convertedInvoiceId.isBlank()) {
                item {
                    Button(
                        onClick = onConvert,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Convert to Project & Invoice", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                item { StatusPill("Converted", SuccessEmerald) }
            }
            item { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit quotation", tint = BrightPurple) } }
            if (isAdmin) item { IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete quotation", tint = ErrorCoral) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceEditorSheet(
    initial: Invoice?,
    clients: List<Client>,
    services: List<AgencyService>,
    dateFormat: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<LineItem>, Double, Double, Double, Long, String) -> Unit
) {
    var number by remember { mutableStateOf(initial?.invoiceNumber ?: "RD-INV-${(1001..9999).random()}") }
    var clientId by remember { mutableStateOf(initial?.clientId ?: "") }
    var clientName by remember { mutableStateOf(initial?.clientName ?: "") }
    var clientPhone by remember { mutableStateOf(initial?.clientPhone ?: "") }
    var clientBusiness by remember { mutableStateOf(initial?.clientBusiness ?: "") }
    val lineItems = remember { mutableStateListOf<LineItem>().apply { addAll(initial?.items ?: emptyList()) } }
    var discountText by remember { mutableStateOf(if ((initial?.discountAmount ?: 0.0) > 0) initial?.discountAmount?.toLong().toString() else "0") }
    var taxText by remember { mutableStateOf(if ((initial?.taxPercent ?: 0.0) > 0) initial?.taxPercent.toString() else "0") }
    var paidText by remember { mutableStateOf(if ((initial?.paidAmount ?: 0.0) > 0) initial?.paidAmount?.toLong().toString() else "0") }
    var dueMillis by remember { mutableStateOf(initial?.dueDateMillis ?: (System.currentTimeMillis() + 7L * 86_400_000L)) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var customServiceName by remember { mutableStateOf("") }
    var customPriceText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val subtotal = lineItems.sumOf { it.lineTotal }
    val discount = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val taxPct = (taxText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val taxAmt = (subtotal - discount) * (taxPct / 100.0)
    val total = (subtotal - discount) + taxAmt

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Create Invoice" else "Edit Invoice", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Invoice Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (clients.isNotEmpty()) {
                Text("Quick Select Client", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clients) { c ->
                        FilterChip(
                            selected = clientId == c.id,
                            onClick = { clientId = c.id; clientName = c.name; clientPhone = c.phone; clientBusiness = c.businessName },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            OutlinedTextField(value = clientName, onValueChange = { clientName = it; error = null }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = clientBusiness, onValueChange = { clientBusiness = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = clientPhone, onValueChange = { clientPhone = it }, label = { Text("Client WhatsApp / Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text("Add Service from Catalogue", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(services.take(20)) { srv ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (lineItems.size < 20) {
                                lineItems.add(LineItem(serviceName = srv.name, description = srv.description.take(80), quantity = 1, unitPrice = if (srv.fixedPrice) srv.price else 0.0))
                            }
                        },
                        label = { Text("+ ${srv.name}") }
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = customServiceName, onValueChange = { customServiceName = it }, label = { Text("Custom Service") }, singleLine = true, modifier = Modifier.weight(1.4f))
                OutlinedTextField(value = customPriceText, onValueChange = { customPriceText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("₹ Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = {
                    if (customServiceName.isNotBlank() && lineItems.size < 20) {
                        lineItems.add(LineItem(serviceName = customServiceName.trim(), quantity = 1, unitPrice = customPriceText.toDoubleOrNull() ?: 0.0))
                        customServiceName = ""
                        customPriceText = ""
                    }
                }) { Text("Add") }
            }

            lineItems.forEachIndexed { idx, item ->
                GlassCard(contentPadding = PaddingValues(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.serviceName, style = MaterialTheme.typography.titleSmall)
                            Text("Qty ${item.quantity} × ${FormatUtils.formatInr(item.unitPrice)} = ${FormatUtils.formatInr(item.lineTotal)}", style = MaterialTheme.typography.bodySmall, color = BrightGold)
                        }
                        Row {
                            TextButton(onClick = { lineItems[idx] = item.copy(quantity = item.quantity + 1) }) { Text("+1") }
                            TextButton(onClick = { lineItems[idx] = item.copy(unitPrice = item.unitPrice + 1000) }) { Text("+1k") }
                            IconButton(onClick = { lineItems.removeAt(idx) }) { Icon(Icons.Default.Delete, "Remove", tint = ErrorCoral) }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = discountText, onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Discount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = taxText, onValueChange = { taxText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("GST / Tax %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = paidText, onValueChange = { paidText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Paid ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Due: ${FormatUtils.formatDate(dueMillis, dateFormat)}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Change Due Date")
                }
            }

            Text("Calculated Total: ${FormatUtils.formatInr(total)}", style = MaterialTheme.typography.titleLarge, color = BrightGold)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Invoice Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (number.trim().isEmpty() || clientName.trim().isEmpty()) { error = "Invoice number and client name are required."; return@Button }
                    if (lineItems.isEmpty()) { error = "Add at least one service item."; return@Button }
                    onSave(number.trim(), clientId, clientName.trim(), clientPhone.trim(), clientBusiness.trim(), lineItems.toList(), discount, taxPct, paidText.toDoubleOrNull() ?: 0.0, dueMillis, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Invoice") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        AgencyDatePickerDialog(dueMillis, { dueMillis = it }, { showDatePicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotationEditorSheet(
    initial: Quotation?,
    clients: List<Client>,
    services: List<AgencyService>,
    dateFormat: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<LineItem>, Double, QuotationStatus, Long, String) -> Unit
) {
    var number by remember { mutableStateOf(initial?.quotationNumber ?: "RD-QT-${(1001..9999).random()}") }
    var clientId by remember { mutableStateOf(initial?.clientId ?: "") }
    var clientName by remember { mutableStateOf(initial?.clientName ?: "") }
    var clientPhone by remember { mutableStateOf(initial?.clientPhone ?: "") }
    var clientBusiness by remember { mutableStateOf(initial?.clientBusiness ?: "") }
    val lineItems = remember { mutableStateListOf<LineItem>().apply { addAll(initial?.items ?: emptyList()) } }
    var discountText by remember { mutableStateOf(if ((initial?.discountAmount ?: 0.0) > 0) initial?.discountAmount?.toLong().toString() else "0") }
    var status by remember { mutableStateOf(initial?.statusEnum() ?: QuotationStatus.SENT) }
    var validMillis by remember { mutableStateOf(initial?.validUntilMillis ?: (System.currentTimeMillis() + 14L * 86_400_000L)) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var customServiceName by remember { mutableStateOf("") }
    var customPriceText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val subtotal = lineItems.sumOf { it.lineTotal }
    val discount = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val total = (subtotal - discount).coerceAtLeast(0.0)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Quotation Generator" else "Edit Quotation", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Quotation Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (clients.isNotEmpty()) {
                Text("Select Client", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clients) { c ->
                        FilterChip(
                            selected = clientId == c.id,
                            onClick = { clientId = c.id; clientName = c.name; clientPhone = c.phone; clientBusiness = c.businessName },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            OutlinedTextField(value = clientName, onValueChange = { clientName = it; error = null }, label = { Text("Client Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = clientBusiness, onValueChange = { clientBusiness = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = clientPhone, onValueChange = { clientPhone = it }, label = { Text("Client WhatsApp / Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Tap Services to Add to Quote", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(services.take(20)) { srv ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (lineItems.size < 20) {
                                lineItems.add(LineItem(serviceName = srv.name, description = srv.description.take(80), quantity = 1, unitPrice = if (srv.fixedPrice) srv.price else 5000.0))
                            }
                        },
                        label = { Text("+ ${srv.name}") }
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = customServiceName, onValueChange = { customServiceName = it }, label = { Text("Custom Item") }, singleLine = true, modifier = Modifier.weight(1.4f))
                OutlinedTextField(value = customPriceText, onValueChange = { customPriceText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("₹ Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = {
                    if (customServiceName.isNotBlank() && lineItems.size < 20) {
                        lineItems.add(LineItem(serviceName = customServiceName.trim(), quantity = 1, unitPrice = customPriceText.toDoubleOrNull() ?: 0.0))
                        customServiceName = ""
                        customPriceText = ""
                    }
                }) { Text("Add") }
            }

            lineItems.forEachIndexed { idx, item ->
                GlassCard(contentPadding = PaddingValues(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.serviceName, style = MaterialTheme.typography.titleSmall)
                            Text("Qty ${item.quantity} × ${FormatUtils.formatInr(item.unitPrice)} = ${FormatUtils.formatInr(item.lineTotal)}", style = MaterialTheme.typography.bodySmall, color = BrightGold)
                        }
                        Row {
                            TextButton(onClick = { lineItems[idx] = item.copy(unitPrice = item.unitPrice + 1000) }) { Text("+1k") }
                            IconButton(onClick = { lineItems.removeAt(idx) }) { Icon(Icons.Default.Delete, "Remove", tint = ErrorCoral) }
                        }
                    }
                }
            }

            OutlinedTextField(value = discountText, onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Discount (INR ₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Quotation Status", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuotationStatus.entries) { st ->
                    FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label) })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Valid Until: ${FormatUtils.formatDate(validMillis, dateFormat)}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Change Date")
                }
            }

            Text("Total Quote: ${FormatUtils.formatInr(total)}", style = MaterialTheme.typography.titleLarge, color = BrightGold)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Proposal Scope & Terms") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (number.trim().isEmpty() || clientName.trim().isEmpty()) { error = "Quotation number and client name are required."; return@Button }
                    if (lineItems.isEmpty()) { error = "Select at least one service."; return@Button }
                    onSave(number.trim(), clientId, clientName.trim(), clientPhone.trim(), clientBusiness.trim(), lineItems.toList(), discount, status, validMillis, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Quotation") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        AgencyDatePickerDialog(validMillis, { validMillis = it }, { showDatePicker = false })
    }
}

@Composable
private fun RecordPaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(invoice.balanceDue.toLong().toString()) }
    var method by remember { mutableStateOf("UPI") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment • ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Balance Due: ${FormatUtils.formatInr(invoice.balanceDue)}", color = BrightGold, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label = { Text("Amount (INR ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("UPI", "Bank Transfer", "Card", "Cash")) { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Reference / Note") }, singleLine = true)
                if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt <= 0.0) { error = "Enter an amount > 0"; return@Button }
                onConfirm(amt, method, note)
            }) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
