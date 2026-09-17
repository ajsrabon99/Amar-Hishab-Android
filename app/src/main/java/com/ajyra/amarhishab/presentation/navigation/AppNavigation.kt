package com.ajyra.amarhishab.presentation.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ajyra.amarhishab.presentation.screens.AddTransactionScreen
import com.ajyra.amarhishab.presentation.screens.HomeScreen
import com.ajyra.amarhishab.presentation.screens.LoginScreen
import com.ajyra.amarhishab.presentation.screens.ProfileScreen
import com.ajyra.amarhishab.presentation.screens.RegisterScreen
import com.ajyra.amarhishab.presentation.screens.ReportsScreen
import com.ajyra.amarhishab.presentation.screens.TransactionsScreen
import com.ajyra.amarhishab.presentation.screens.TransferScreen
import com.ajyra.amarhishab.presentation.screens.VerifyEmailScreen
import com.ajyra.amarhishab.presentation.viewmodel.AddTransactionViewModel
import com.ajyra.amarhishab.presentation.viewmodel.AuthViewModel
import com.ajyra.amarhishab.presentation.viewmodel.DashboardViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ProfileViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ReportsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.TransactionsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.TransferViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ViewModelFactory
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val factory = remember { ViewModelFactory(context) }

    val resolvedAuthViewModel: AuthViewModel = authViewModel ?: viewModel(factory = factory)
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
    val addTransactionViewModel: AddTransactionViewModel = viewModel(factory = factory)
    val transferViewModel: TransferViewModel = viewModel(factory = factory)
    val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)

    val isAuthenticated by resolvedAuthViewModel.isAuthenticated.collectAsState()
    val isCheckingSession by resolvedAuthViewModel.isCheckingSession.collectAsState()
    val language by profileViewModel.language.collectAsState()
    val isBengali = language == "bn"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")?.substringBefore("?")

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Reports,
        Screen.Profile
    )

    val showBottomBar = isAuthenticated && currentRoute in bottomNavItems.map { it.route }
    val startDestination = if (isAuthenticated) Screen.Home.route else Screen.Login.route

    val authBaseRoutes = listOf("login", "register", "verify_email")

    LaunchedEffect(isAuthenticated, isCheckingSession) {
        if (!isCheckingSession) {
            if (!isAuthenticated && currentBaseRoute != null && currentBaseRoute !in authBaseRoutes) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else if (isAuthenticated && currentBaseRoute in authBaseRoutes) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    if (isCheckingSession) {
        // Startup Session Checking Splash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Official Amar Hishab Brand Logo
                Image(
                    painter = painterResource(id = R.drawable.amar_hishab_icon),
                    contentDescription = "Amar Hishab Logo",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isBengali) "আমার হিসাব" else "Amar Hishab",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = EmeraldPrimary,
                    strokeWidth = 3.dp
                )
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            modifier = Modifier.testTag("nav_item_${screen.route}"),
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = if (isBengali) screen.titleBn else screen.titleEn
                                    )
                                }
                            },
                            label = {
                                Text(if (isBengali) screen.titleBn else screen.titleEn)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    authViewModel = resolvedAuthViewModel,
                    isBengali = isBengali,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToVerifyEmail = { email ->
                        navController.navigate(Screen.VerifyEmail.createRoute(email))
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    authViewModel = resolvedAuthViewModel,
                    isBengali = isBengali,
                    onRegistrationSuccess = { email ->
                        navController.navigate(Screen.VerifyEmail.createRoute(email))
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.VerifyEmail.route,
                arguments = listOf(
                    navArgument("email") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email").orEmpty()
                VerifyEmailScreen(
                    initialEmail = email,
                    authViewModel = resolvedAuthViewModel,
                    isBengali = isBengali,
                    onVerificationSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    dashboardViewModel = dashboardViewModel,
                    isBengali = isBengali,
                    onNavigateToAddTransaction = { isExpense ->
                        navController.navigate(Screen.AddTransaction.createRoute(isExpense))
                    },
                    onNavigateToTransfer = {
                        navController.navigate(Screen.Transfer.route)
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    transactionsViewModel = transactionsViewModel,
                    isBengali = isBengali,
                    onNavigateToAddTransaction = { isExpense ->
                        navController.navigate(Screen.AddTransaction.createRoute(isExpense))
                    }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    reportsViewModel = reportsViewModel,
                    isBengali = isBengali
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    isBengali = isBengali,
                    onLanguageToggle = {
                        profileViewModel.setLanguage(if (isBengali) "en" else "bn")
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(navArgument("isExpense") { type = NavType.BoolType })
            ) { backStackEntry ->
                val isExpense = backStackEntry.arguments?.getBoolean("isExpense") ?: true
                AddTransactionScreen(
                    viewModel = addTransactionViewModel,
                    initialIsExpense = isExpense,
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Transfer.route) {
                TransferScreen(
                    viewModel = transferViewModel,
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
