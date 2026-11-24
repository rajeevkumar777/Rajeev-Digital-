package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ActivityLog
import com.example.data.model.EmployeeRole
import com.example.data.model.Invoice
import com.example.ui.components.BarDatum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.KpiStatCard
import com.example.ui.components.RevenueBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.WhatsAppActionChip
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SubtleGold
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.FormatUtils
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: AgencyViewModel,
    onNavigateServices: () -> Unit,
    onNavigateCrm: () -> Unit,
    onNavigateWork: () -> Unit,
    onNavigateBilling: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateNotifications: () -> Unit
) {
    val context = LocalContext.current
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val invoicesUi by viewModel.invoicesState.collectAsStateWithLifecycle()
    val activitiesUi by viewModel.activitiesState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()

    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN
    val profile = (profileUi as? UiState.Success)?.data
    val invoices = (invoicesUi as? UiState.Success)?.data.orEmpty()
    val activities = (activitiesUi as? UiState.Success)?.data.orEmpty()

    val monthlyBars = computeLastMonthsRevenue(invoices)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Hero Banner
        item {
            GlassCard(
                accentGradient = Brush.linearGradient(
                    listOf(
                        ElectricPurple.copy(alpha = 0.35f),
                        Color(0xFF10193C),
                        SubtleGold.copy(alpha = 0.14f)
                    )
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rajeev_digital_logo),
                        contentDescription = "Rajeev Digital",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.businessName ?: "Rajeev Digital",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BrightGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = profile?.tagline ?: "WhatsApp Marketing Expert | Grow Your Business Digitally",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftLavender,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onNavigateNotifications,
                        modifier = Modifier.testTag("dashboard_notifications_btn")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Notifications",
                                tint = if (reminders.isNotEmpty()) BrightGold else Color.White
                            )
                            if (reminders.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = ErrorCoral,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = reminders.size.coerceAtMost(9).toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = if (isAdmin) "Admin Workspace" else "Employee View",
                            color = if (isAdmin) BrightGold else InfoSky
                        )
                        if (isInitializing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = SubtleGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    WhatsAppActionChip(
                        label = "Agency Desk",
                        onClick = { WhatsAppHelper.openAgencyWhatsAppSupport(context) },
                        modifier = Modifier.testTag("dashboard_whatsapp_btn")
                    )
                }
            }
        }

        // Quick Actions Strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    QuickActionTile("Services", Icons.Default.DesignServices, BrightPurple, onNavigateServices, "qa_services")
                }
                item {
                    QuickActionTile("Leads & CRM", Icons.Default.Groups, WhatsAppGreen, onNavigateCrm, "qa_crm")
                }
                item {
                    QuickActionTile("Projects", Icons.Default.WorkOutline, InfoSky, onNavigateWork, "qa_work")
                }
                item {
                    QuickActionTile("Quotations", Icons.Default.RequestQuote, SubtleGold, onNavigateBilling, "qa_quotes")
                }
                if (isAdmin) {
                    item {
                        QuickActionTile("Reports", Icons.Default.Analytics, SuccessEmerald, onNavigateReports, "qa_reports")
                    }
                }
            }
        }

        // Core KPIs Grid (2x4 or responsive pairs)
        item {
            SectionHeader(
                title = "Agency Pulse",
                subtitle = "Live metrics from Cloud Firestore"
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Total Leads",
                        value = metrics.totalLeads.toString(),
                        subtitle = "Pipeline CRM",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = BrightPurple,
                        onClick = onNavigateCrm,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Total Clients",
                        value = metrics.totalClients.toString(),
                        subtitle = "Active Accounts",
                        icon = Icons.Default.Groups,
                        accentColor = WhatsAppGreen,
                        onClick = onNavigateCrm,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Active Projects",
                        value = metrics.activeProjects.toString(),
                        subtitle = "In Delivery",
                        icon = Icons.Default.FolderSpecial,
                        accentColor = InfoSky,
                        onClick = onNavigateWork,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Completed",
                        value = metrics.completedProjects.toString(),
                        subtitle = "Projects Shipped",
                        icon = Icons.Default.CheckCircle,
                        accentColor = SuccessEmerald,
                        onClick = onNavigateWork,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiStatCard(
                            title = "Total Revenue",
                            value = FormatUtils.formatCompactInr(metrics.totalRevenue),
                            subtitle = FormatUtils.formatInr(metrics.totalRevenue),
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = BrightGold,
                            onClick = onNavigateBilling,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "Pending Dues",
                            value = FormatUtils.formatCompactInr(metrics.pendingPayments),
                            subtitle = FormatUtils.formatInr(metrics.pendingPayments),
                            icon = Icons.Default.PendingActions,
                            accentColor = WarningAmber,
                            onClick = onNavigateBilling,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAdmin) {
                        KpiStatCard(
                            title = "Monthly Revenue",
                            value = FormatUtils.formatCompactInr(metrics.monthlyRevenue),
                            subtitle = "This Month",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            accentColor = SubtleGold,
                            onClick = onNavigateReports,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    KpiStatCard(
                        title = "Pending Tasks",
                        value = metrics.pendingTasksCount.toString(),
                        subtitle = "Action Items",
                        icon = Icons.Default.AssignmentTurnedIn,
                        accentColor = ErrorCoral,
                        onClick = onNavigateWork,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Revenue Chart (Admin only)
        if (isAdmin) {
            item {
                GlassCard {
                    SectionHeader(
                        title = "Monthly Revenue",
                        subtitle = "Paid invoice collections (INR)",
                        actionLabel = "Full Reports",
                        onActionClick = onNavigateReports
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RevenueBarChart(data = monthlyBars)
                }
            }
        }

        // Upcoming Deadlines
        item {
            SectionHeader(
                title = "Upcoming Deadlines",
                subtitle = "${metrics.upcomingDeadlines.size} active schedule(s)",
                actionLabel = "All Work",
                onActionClick = onNavigateWork
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (metrics.upcomingDeadlines.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Schedule,
                    title = "No Upcoming Project Deadlines",
                    description = "Create a project or convert an accepted quotation to track milestones."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.upcomingDeadlines.forEach { project ->
                        GlassCard(onClick = onNavigateWork) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${project.clientName.ifBlank { "Internal" }} • Due ${FormatUtils.formatDate(project.deadlineMillis, prefs.dateFormat)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = "${project.progress}%",
                                    color = if (project.deadlineMillis < System.currentTimeMillis()) ErrorCoral else InfoSky
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending Tasks Summary
        item {
            SectionHeader(
                title = "Pending Tasks",
                subtitle = "${metrics.upcomingTasks.size} priority task(s)",
                actionLabel = "Manage",
                onActionClick = onNavigateWork
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (metrics.upcomingTasks.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.AssignmentTurnedIn,
                    title = "All Caught Up",
                    description = "No pending tasks right now. Add tasks from the Work hub."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.upcomingTasks.forEach { task ->
                        GlassCard(onClick = onNavigateWork) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Due ${FormatUtils.formatDate(task.dueDateMillis, prefs.dateFormat)} • ${task.assignedEmployeeName.ifBlank { "Unassigned" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = task.priorityEnum().label,
                                    color = when (task.priorityEnum().name) {
                                        "URGENT" -> ErrorCoral
                                        "HIGH" -> WarningAmber
                                        "MEDIUM" -> BrightPurple
                                        else -> SuccessEmerald
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activities
        item {
            SectionHeader(
                title = "Recent Activities",
                subtitle = "Agency audit trail"
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activities.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Workspace Ready",
                    description = "Add leads, quotations, projects, or invoices and your live activity feed will appear here."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activities.take(8).forEach { act ->
                        ActivityRow(act)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.38f)),
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActivityRow(activity: ActivityLog) {
    GlassCard(contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(text = activity.categoryEnum().label, color = BrightPurple)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (activity.detail.isNotBlank()) {
                    Text(
                        text = activity.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = FormatUtils.formatTimestampWithTime(activity.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun computeLastMonthsRevenue(invoices: List<Invoice>): List<BarDatum> {
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val cal = Calendar.getInstance()
    val buckets = mutableListOf<Triple<Int, Int, String>>()
    for (offset in 5 downTo 0) {
        val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
        buckets.add(Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), monthFormat.format(c.time)))
    }
    return buckets.map { (yr, mo, label) ->
        val sum = invoices.sumOf { inv ->
            val paymentSum = inv.paymentHistory.filter { entry ->
                val c = Calendar.getInstance().apply { timeInMillis = entry.paidAtMillis }
                c.get(Calendar.YEAR) == yr && c.get(Calendar.MONTH) == mo
            }.sumOf { it.amount }
            if (paymentSum > 0) paymentSum
            else {
                val created = inv.createdAt?.toDate()?.time ?: 0L
                val c = Calendar.getInstance().apply { timeInMillis = created }
                if (c.get(Calendar.YEAR) == yr && c.get(Calendar.MONTH) == mo) inv.paidAmount else 0.0
            }
        }
        BarDatum(label = label, value = sum)
    }
}
