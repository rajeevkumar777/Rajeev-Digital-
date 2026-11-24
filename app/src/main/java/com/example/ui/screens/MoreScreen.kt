package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EmployeeRole
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AgencyViewModel
import com.example.util.WhatsAppHelper

@Composable
fun MoreScreen(
    viewModel: AgencyViewModel,
    onOpenServices: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("more_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Agency Hub",
                subtitle = "Services, Analytics, Reminders & Settings"
            )
        }

        item {
            HubRowCard(
                title = "Services & Pricing Catalogue",
                subtitle = "6 categories • Confirmed INR packages & Custom Quotes",
                icon = Icons.Default.DesignServices,
                tint = BrightPurple,
                onClick = onOpenServices,
                testTag = "hub_services_card"
            )
        }

        if (isAdmin) {
            item {
                HubRowCard(
                    title = "Reports & Analytics",
                    subtitle = "Revenue trends, service ROI, conversions & dues",
                    icon = Icons.Default.Analytics,
                    tint = SuccessEmerald,
                    onClick = onOpenReports,
                    testTag = "hub_reports_card"
                )
            }
        }

        item {
            HubRowCard(
                title = "Notifications & Reminders",
                subtitle = "${reminders.size} active deadline, task, invoice & follow-up alert(s)",
                icon = Icons.Default.NotificationsActive,
                tint = BrightGold,
                onClick = onOpenNotifications,
                testTag = "hub_notifications_card"
            )
        }

        item {
            HubRowCard(
                title = "Settings, Team & Security",
                subtitle = "Business profile, employees, role access, theme & logout",
                icon = Icons.Default.Settings,
                tint = InfoSky,
                onClick = onOpenSettings,
                testTag = "hub_settings_card"
            )
        }

        item {
            HubRowCard(
                title = "Rajeev Digital WhatsApp Desk",
                subtitle = "Direct WhatsApp shortcut (+91 8307229361) • @rajeev.digital.ai",
                icon = Icons.AutoMirrored.Filled.Chat,
                tint = WhatsAppGreen,
                onClick = { WhatsAppHelper.openAgencyWhatsAppSupport(context) },
                testTag = "hub_whatsapp_card"
            )
        }
    }
}

@Composable
private fun HubRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    GlassCard(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
