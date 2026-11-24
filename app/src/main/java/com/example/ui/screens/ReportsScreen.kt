package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceStatus
import com.example.data.model.LeadStatus
import com.example.data.model.ProjectStatus
import com.example.ui.components.BarDatum
import com.example.ui.components.GlassCard
import com.example.ui.components.HorizontalBreakdownChart
import com.example.ui.components.KpiStatCard
import com.example.ui.components.RevenueBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class DateRangeFilter(val label: String, val days: Long?) {
    D7("7D", 7L),
    D30("30D", 30L),
    D90("90D", 90L),
    ALL("All Time", null)
}

@Composable
fun ReportsScreen(viewModel: AgencyViewModel) {
    val invoicesUi by viewModel.invoicesState.collectAsStateWithLifecycle()
    val leadsUi by viewModel.leadsState.collectAsStateWithLifecycle()
    val clientsUi by viewModel.clientsState.collectAsStateWithLifecycle()
    val projectsUi by viewModel.projectsState.collectAsStateWithLifecycle()

    var range by remember { mutableStateOf(DateRangeFilter.ALL) }
    val now = System.currentTimeMillis()
    val cutoff = range.days?.let { now - TimeUnit.DAYS.toMillis(it) } ?: 0L

    val invoices = (invoicesUi as? UiState.Success)?.data.orEmpty().filter {
        cutoff == 0L || (it.createdAt?.toDate()?.time ?: now) >= cutoff
    }
    val leads = (leadsUi as? UiState.Success)?.data.orEmpty().filter {
        cutoff == 0L || (it.createdAt?.toDate()?.time ?: now) >= cutoff
    }
    val clients = (clientsUi as? UiState.Success)?.data.orEmpty().filter {
        cutoff == 0L || (it.createdAt?.toDate()?.time ?: now) >= cutoff
    }
    val projects = (projectsUi as? UiState.Success)?.data.orEmpty().filter {
        cutoff == 0L || (it.createdAt?.toDate()?.time ?: now) >= cutoff
    }

    val totalCollected = invoices.sumOf { it.paidAmount }
    val totalOutstanding = invoices.sumOf { it.balanceDue }
    val conversionRate = if (leads.isEmpty()) 0 else (leads.count { it.statusEnum() == LeadStatus.CONVERTED } * 100 / leads.size)
    val projectCompletionRate = if (projects.isEmpty()) 0 else (projects.count { it.statusEnum() == ProjectStatus.COMPLETED } * 100 / projects.size)

    val serviceRevenueMap = linkedMapOf<String, Double>()
    invoices.forEach { inv ->
        inv.items.forEach { item ->
            serviceRevenueMap[item.serviceName] = (serviceRevenueMap[item.serviceName] ?: 0.0) + item.lineTotal
        }
    }
    val serviceBreakdown = serviceRevenueMap.entries.sortedByDescending { it.value }.take(6).map { it.key to it.value }

    val leadPipeline = LeadStatus.entries.map { st ->
        st.label to leads.count { it.statusEnum() == st }.toDouble()
    }.filter { it.second > 0.0 }

    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val cal = Calendar.getInstance()
    val monthlyBars = (5 downTo 0).map { offset ->
        val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
        val yr = c.get(Calendar.YEAR)
        val mo = c.get(Calendar.MONTH)
        val sum = invoices.sumOf { inv ->
            val paymentSum = inv.paymentHistory.filter { entry ->
                val ec = Calendar.getInstance().apply { timeInMillis = entry.paidAtMillis }
                ec.get(Calendar.YEAR) == yr && ec.get(Calendar.MONTH) == mo
            }.sumOf { it.amount }
            if (paymentSum > 0) paymentSum else {
                val created = inv.createdAt?.toDate()?.time ?: 0L
                val ic = Calendar.getInstance().apply { timeInMillis = created }
                if (ic.get(Calendar.YEAR) == yr && ic.get(Calendar.MONTH) == mo) inv.paidAmount else 0.0
            }
        }
        BarDatum(monthFormat.format(c.time), sum)
    }

    val outstandingInvoices = invoices.filter { it.statusEnum() != InvoiceStatus.PAID && it.balanceDue > 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Reports & Analytics",
                subtitle = "Revenue, conversions, service ROI & outstanding dues"
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DateRangeFilter.entries) { f ->
                    FilterChip(selected = range == f, onClick = { range = f }, label = { Text(f.label) })
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiStatCard(
                        title = "Collected Revenue",
                        value = FormatUtils.formatCompactInr(totalCollected),
                        subtitle = FormatUtils.formatInr(totalCollected),
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = BrightGold,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Outstanding Dues",
                        value = FormatUtils.formatCompactInr(totalOutstanding),
                        subtitle = "${outstandingInvoices.size} invoice(s)",
                        icon = Icons.Default.PendingActions,
                        accentColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiStatCard(
                        title = "Lead Conversion",
                        value = "$conversionRate%",
                        subtitle = "${leads.size} leads tracked",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = WhatsAppGreen,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Project Completion",
                        value = "$projectCompletionRate%",
                        subtitle = "${clients.size} clients active",
                        icon = Icons.Default.CheckCircle,
                        accentColor = InfoSky,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "Monthly Revenue Trend", subtitle = "Last 6 months (INR ₹)")
                Spacer(Modifier.height(10.dp))
                RevenueBarChart(data = monthlyBars)
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "Service-wise Revenue", subtitle = "Top billed services")
                Spacer(Modifier.height(10.dp))
                HorizontalBreakdownChart(items = serviceBreakdown)
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "Lead Pipeline & Client Growth", subtitle = "${clients.size} client(s) onboarded")
                Spacer(Modifier.height(10.dp))
                HorizontalBreakdownChart(
                    items = leadPipeline,
                    valueFormatter = { "${it.toInt()} lead(s)" },
                    barColors = listOf(InfoSky, BrightPurple, BrightGold, WarningAmber, WhatsAppGreen, ErrorCoral)
                )
            }
        }

        item {
            GlassCard {
                SectionHeader(title = "Outstanding Payments", subtitle = "Pending invoice balances")
                Spacer(Modifier.height(8.dp))
                if (outstandingInvoices.isEmpty()) {
                    Text("All invoices are fully settled!", style = MaterialTheme.typography.bodyMedium, color = SuccessEmerald)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        outstandingInvoices.forEach { inv ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("${inv.invoiceNumber} • ${inv.clientName}", style = MaterialTheme.typography.titleSmall)
                                    Text("Due ${FormatUtils.formatDate(inv.dueDateMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(FormatUtils.formatInr(inv.balanceDue), ErrorCoral)
                            }
                        }
                    }
                }
            }
        }
    }
}
