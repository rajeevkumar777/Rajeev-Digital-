package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.firebase.AgencyRepository
import com.example.data.model.EmployeeRole
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.BrandSplashScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.CrmScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkScreen
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BrightPurple
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RajeevDigitalTheme
import com.example.ui.viewmodel.AgencyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable data object RouteDashboard
@Serializable data object RouteCrm
@Serializable data object RouteWork
@Serializable data object RouteBilling
@Serializable data object RouteMore
@Serializable data object RouteServices
@Serializable data object RouteReports
@Serializable data object RouteNotifications
@Serializable data object RouteSettings

private data class TopLevelTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelTabs = listOf(
    TopLevelTab(RouteDashboard, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    TopLevelTab(RouteCrm, "CRM", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_crm"),
    TopLevelTab(RouteWork, "Work", Icons.Filled.WorkOutline, Icons.Outlined.WorkOutline, "nav_work"),
    TopLevelTab(RouteBilling, "Billing", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_billing"),
    TopLevelTab(RouteMore, "More", Icons.Filled.GridView, Icons.Outlined.GridView, "nav_more")
)

@Composable
fun RajeevDigitalApp() {
    val context = LocalContext.current
    val prefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val localPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.LocalPreferences()
    )

    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    RajeevDigitalTheme(themeMode = localPrefs.themeMode) {
        AnimatedContent(
            targetState = when {
                showSplash -> "splash"
                currentUser == null -> "auth"
                else -> "main"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "root_gate"
        ) { stage ->
            when (stage) {
                "splash" -> BrandSplashScreen(onFinished = { showSplash = false })
                "auth" -> AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
                else -> {
                    val uid = currentUser?.uid.orEmpty()
                    if (uid.isNotBlank()) {
                        key(uid) {
                            AuthenticatedAgencyShell(currentUserId = uid)
                        }
                    } else {
                        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAgencyShell(currentUserId: String) {
    val viewModel: AgencyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AgencyViewModel(
                    repository = AgencyRepository(db),
                    prefsRepository = UserPreferencesRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.actionMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isSecondaryRoute = currentDest?.let { dest ->
        dest.hasRoute<RouteServices>() ||
            dest.hasRoute<RouteReports>() ||
            dest.hasRoute<RouteNotifications>() ||
            dest.hasRoute<RouteSettings>()
    } ?: false

    val secondaryTitle = when {
        currentDest?.hasRoute<RouteServices>() == true -> "Services Catalogue"
        currentDest?.hasRoute<RouteReports>() == true -> "Reports & Analytics"
        currentDest?.hasRoute<RouteNotifications>() == true -> "Reminders & Alerts"
        currentDest?.hasRoute<RouteSettings>() == true -> "Settings & Team"
        else -> "Rajeev Digital"
    }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E1638), DeepNavy, MidnightBlack)
                )
            )
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isSecondaryRoute) {
                    TopAppBar(
                        title = { Text(secondaryTitle, style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = NavySurface.copy(alpha = 0.88f),
                            titleContentColor = BrightGold,
                            navigationIconContentColor = Color.White
                        ),
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            },
            bottomBar = {
                if (!useRail && !isSecondaryRoute) {
                    NavigationBar(
                        containerColor = NavySurface.copy(alpha = 0.94f),
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrightGold,
                                    selectedTextColor = BrightGold,
                                    indicatorColor = ElectricPurple.copy(alpha = 0.35f),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = NavySurface.copy(alpha = 0.92f)
                    ) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteDashboard,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 960.dp)
                    ) {
                        composable<RouteDashboard> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateServices = { navController.navigate(RouteServices) },
                                onNavigateCrm = { navigateTopLevel(RouteCrm) },
                                onNavigateWork = { navigateTopLevel(RouteWork) },
                                onNavigateBilling = { navigateTopLevel(RouteBilling) },
                                onNavigateReports = {
                                    if (prefs.activeRole == EmployeeRole.ADMIN) navController.navigate(RouteReports)
                                },
                                onNavigateNotifications = { navController.navigate(RouteNotifications) }
                            )
                        }
                        composable<RouteCrm> { CrmScreen(viewModel = viewModel) }
                        composable<RouteWork> { WorkScreen(viewModel = viewModel) }
                        composable<RouteBilling> { BillingScreen(viewModel = viewModel) }
                        composable<RouteMore> {
                            MoreScreen(
                                viewModel = viewModel,
                                onOpenServices = { navController.navigate(RouteServices) },
                                onOpenReports = { navController.navigate(RouteReports) },
                                onOpenNotifications = { navController.navigate(RouteNotifications) },
                                onOpenSettings = { navController.navigate(RouteSettings) }
                            )
                        }
                        composable<RouteServices> { ServicesScreen(viewModel = viewModel) }
                        composable<RouteReports> { ReportsScreen(viewModel = viewModel) }
                        composable<RouteNotifications> { NotificationsScreen(viewModel = viewModel) }
                        composable<RouteSettings> { SettingsScreen(viewModel = viewModel) }
                    }
                }
            }
        }
    }
}
