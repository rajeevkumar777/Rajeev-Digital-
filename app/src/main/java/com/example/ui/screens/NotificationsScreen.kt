package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AgencyViewModel
import com.example.util.FormatUtils
import com.example.util.NotificationHelper
import com.example.util.ReminderWorker

@Composable
fun NotificationsScreen(viewModel: AgencyViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    var permissionGranted by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            NotificationHelper.ensureChannel(context)
            ReminderWorker.schedulePeriodic(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notifications_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Reminders & Alerts",
                subtitle = "Deadlines, pending tasks, due invoices & lead follow-ups"
            )
        }

        if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = WarningAmber)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Enable Android Notifications", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Allow notification permission so WorkManager can alert you about upcoming deadlines and due invoices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Notification Permission")
                    }
                }
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Background Digest Worker", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (permissionGranted) "Active • Checks every 12h" else "Permission needed for system alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionGranted) SuccessEmerald else WarningAmber
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            ReminderWorker.runNow(context)
                            if (permissionGranted && reminders.isNotEmpty()) {
                                NotificationHelper.showReminderNotification(
                                    context = context,
                                    notificationId = 1002,
                                    title = "Rajeev Digital • ${reminders.size} Active Reminder(s)",
                                    body = reminders.take(2).joinToString(" | ") { it.title }
                                )
                            }
                        },
                        modifier = Modifier.testTag("run_reminders_now_btn")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Check Now")
                    }
                }
            }
        }

        if (reminders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "No Upcoming Alerts",
                    description = "Everything within the next 7 days is clear. Project deadlines, tasks, due invoices, and lead follow-ups appear here."
                )
            }
        } else {
            items(reminders, key = { it.id }) { item ->
                val badgeColor = when (item.category) {
                    "Invoice" -> WarningAmber
                    "Lead" -> BrightGold
                    "Task" -> BrightPurple
                    else -> InfoSky
                }
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill(item.category, badgeColor)
                                if (item.overdue) {
                                    StatusPill("Overdue", ErrorCoral)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = FormatUtils.formatDate(item.dueMillis, prefs.dateFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.overdue) ErrorCoral else BrightGold
                        )
                    }
                }
            }
        }
    }
}
