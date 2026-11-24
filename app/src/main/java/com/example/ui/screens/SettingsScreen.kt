package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Employee
import com.example.data.model.EmployeeRole
import com.example.ui.auth.AuthHelper
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.AgencyViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SettingsScreen(viewModel: AgencyViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val employeesUi by viewModel.employeesState.collectAsStateWithLifecycle()
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data
    val employees = (employeesUi as? UiState.Success)?.data.orEmpty()
    val isAdmin = prefs.activeRole == EmployeeRole.ADMIN

    var displayName by remember { mutableStateOf(profile?.displayName ?: Firebase.auth.currentUser?.displayName ?: "Rajeev Kumar") }
    var businessName by remember { mutableStateOf(profile?.businessName ?: "Rajeev Digital") }
    var tagline by remember { mutableStateOf(profile?.tagline ?: "WhatsApp Marketing Expert | Grow Your Business Digitally") }
    var whatsapp by remember { mutableStateOf(profile?.whatsappNumber ?: "+91 8307229361") }
    var instagram by remember { mutableStateOf(profile?.instagramHandle ?: "@rajeev.digital.ai") }

    LaunchedEffect(profile) {
        if (profile != null) {
            displayName = profile.displayName
            businessName = profile.businessName
            tagline = profile.tagline
            whatsapp = profile.whatsappNumber
            instagram = profile.instagramHandle
        }
    }

    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var showEmployeeEditor by remember { mutableStateOf(false) }
    var deletingEmployee by remember { mutableStateOf<Employee?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Settings & Profile",
                subtitle = "Agency branding, role access, team roster & security"
            )
        }

        // Workspace Role Switcher (Role-Based Access)
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupervisorAccount, null, tint = BrightGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Access Role Mode", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Admin has full financial, deletion, service pricing & team permissions. Employee mode focuses on assigned tasks, projects & leads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmployeeRole.entries.forEach { role ->
                        FilterChip(
                            selected = prefs.activeRole == role,
                            onClick = { viewModel.setActiveRole(role) },
                            label = { Text(role.label) }
                        )
                    }
                }
            }
        }

        // Business Profile Form
        item {
            GlassCard {
                SectionHeader(title = "Agency & Admin Profile", subtitle = Firebase.auth.currentUser?.email ?: "")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Admin Display Name") }, singleLine = true, enabled = isAdmin, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business Name") }, singleLine = true, enabled = isAdmin, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, enabled = isAdmin, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("Business WhatsApp") }, singleLine = true, enabled = isAdmin, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = instagram, onValueChange = { instagram = it }, label = { Text("Instagram Handle") }, singleLine = true, enabled = isAdmin, modifier = Modifier.fillMaxWidth())

                    if (isAdmin) {
                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    displayName = displayName,
                                    businessName = businessName,
                                    tagline = tagline,
                                    whatsappNumber = whatsapp,
                                    instagramHandle = instagram,
                                    dateFormat = prefs.dateFormat,
                                    themeMode = prefs.themeMode,
                                    notificationsEnabled = prefs.notificationsEnabled
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
                        ) { Text("Save Business Profile") }
                    }
                }
            }
        }

        // Theme, Currency & Date Settings
        item {
            GlassCard {
                SectionHeader(title = "Appearance, Currency & Locale")
                Spacer(Modifier.height(10.dp))
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("DARK_NAVY" to "Dark Navy", "MIDNIGHT" to "Midnight OLED", "SYSTEM" to "System")) { (key, label) ->
                        FilterChip(
                            selected = prefs.themeMode == key,
                            onClick = { viewModel.setThemeMode(key) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Date Format", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("dd MMM yyyy", "dd/MM/yyyy", "yyyy-MM-dd")) { fmt ->
                        FilterChip(
                            selected = prefs.dateFormat == fmt,
                            onClick = { viewModel.setDateFormat(fmt) },
                            label = { Text(fmt) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Currency", style = MaterialTheme.typography.bodyMedium)
                    StatusPill("INR (₹)", BrightGold)
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reminder Notifications", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            }
        }

        // Employee Management (Admin Only)
        if (isAdmin) {
            item {
                GlassCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Team & Employees (${employees.size})", style = MaterialTheme.typography.titleMedium)
                            Text("Assign tasks & projects to team members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { editingEmployee = null; showEmployeeEditor = true }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    employees.forEach { emp ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(emp.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${emp.designation.ifBlank { emp.roleEnum().label }} • ${emp.roleEnum().label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(if (emp.active) "Active" else "Inactive", if (emp.active) SuccessEmerald else ErrorCoral)
                            IconButton(onClick = { editingEmployee = emp; showEmployeeEditor = true }) {
                                Icon(Icons.Default.Edit, "Edit employee", tint = BrightPurple)
                            }
                            IconButton(onClick = { deletingEmployee = emp }) {
                                Icon(Icons.Default.Delete, "Delete employee", tint = ErrorCoral)
                            }
                        }
                    }
                }
            }
        }

        // Account Security & Logout
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = InfoSky)
                    Spacer(Modifier.width(8.dp))
                    Text("Security & Session", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Authenticated via Google Sign-In & Firebase Auth. Manage password, 2-step verification, and recovery options in your Google Account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/security"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Google Account Security")
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { AuthHelper.signOut(credentialManager, scope) },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    Spacer(Modifier.width(8.dp))
                    Text("Secure Logout")
                }
            }
        }
    }

    if (showEmployeeEditor) {
        EmployeeEditorSheet(
            initial = editingEmployee,
            onDismiss = { showEmployeeEditor = false },
            onSave = { name, email, phone, designation, role, active ->
                viewModel.saveEmployee(editingEmployee?.id, name, email, phone, designation, role, active)
                showEmployeeEditor = false
            }
        )
    }

    deletingEmployee?.let { emp ->
        ConfirmDeleteDialog(
            title = "Remove Team Member",
            message = "Remove ${emp.name} from Rajeev Digital?",
            onConfirm = { viewModel.deleteEmployee(emp.id); deletingEmployee = null },
            onDismiss = { deletingEmployee = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeEditorSheet(
    initial: Employee?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, EmployeeRole, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var designation by remember { mutableStateOf(initial?.designation ?: "") }
    var role by remember { mutableStateOf(initial?.roleEnum() ?: EmployeeRole.EMPLOYEE) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (initial == null) "Add Team Member" else "Edit Team Member", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("Full Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation (e.g. Video Editor)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone / WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Role", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmployeeRole.entries.forEach { r ->
                    FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active Team Member", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = active, onCheckedChange = { active = it })
            }

            if (error != null) Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    if (name.trim().isEmpty()) { error = "Name is required."; return@Button }
                    onSave(name.trim(), email.trim(), phone.trim(), designation.trim(), role, active)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Team Member") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
