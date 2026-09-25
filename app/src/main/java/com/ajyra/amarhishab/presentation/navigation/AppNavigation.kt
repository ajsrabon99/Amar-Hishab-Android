package com.ajyra.amarhishab.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ajyra.amarhishab.presentation.screens.AddTransactionScreen
import com.ajyra.amarhishab.presentation.screens.CustomCategoriesScreen
import com.ajyra.amarhishab.presentation.screens.HomeScreen
import com.ajyra.amarhishab.presentation.screens.ImportDataScreen
import com.ajyra.amarhishab.presentation.screens.NotificationScreen
import com.ajyra.amarhishab.presentation.screens.ProfileScreen
import com.ajyra.amarhishab.presentation.screens.ReportsScreen
import com.ajyra.amarhishab.presentation.screens.SavingsGoalsScreen
import com.ajyra.amarhishab.presentation.screens.SettingsScreen
import com.ajyra.amarhishab.presentation.screens.StatementScreen
import com.ajyra.amarhishab.presentation.screens.TransactionsScreen
import com.ajyra.amarhishab.presentation.screens.TransferScreen
import com.ajyra.amarhishab.presentation.viewmodel.AddTransactionViewModel
import com.ajyra.amarhishab.presentation.viewmodel.DashboardViewModel
import com.ajyra.amarhishab.presentation.viewmodel.NotificationViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ProfileViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ReportsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.SavingsGoalsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.SettingsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.TransactionsViewModel
import com.ajyra.amarhishab.presentation.viewmodel.TransferViewModel
import com.ajyra.amarhishab.presentation.viewmodel.ViewModelFactory

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    initialDestination: String? = null
) {
    val context = LocalContext.current
    val factory = remember { ViewModelFactory(context) }

    LaunchedEffect(initialDestination) {
        if (initialDestination != null && initialDestination != Screen.Home.route) {
            navController.navigate(initialDestination)
        }
    }

    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val transactionsViewModel: TransactionsViewModel = viewModel(factory = factory)
    val addTransactionViewModel: AddTransactionViewModel = viewModel(factory = factory)
    val transferViewModel: TransferViewModel = viewModel(factory = factory)
    val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val notificationViewModel: NotificationViewModel = viewModel(factory = factory)
    val savingsGoalsViewModel: SavingsGoalsViewModel = viewModel(factory = factory)

    val language by profileViewModel.language.collectAsState()
    val isBengali = language == "bn"
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Reports,
        Screen.Profile
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
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
                                Text(
                                    text = if (isBengali) screen.titleBn else screen.titleEn,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = com.ajyra.amarhishab.ui.theme.FintechPrimary,
                                selectedTextColor = com.ajyra.amarhishab.ui.theme.FintechPrimary,
                                indicatorColor = com.ajyra.amarhishab.ui.theme.FintechPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(220))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    dashboardViewModel = dashboardViewModel,
                    isBengali = isBengali,
                    unreadNotificationCount = unreadCount,
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToStatement = {
                        navController.navigate(Screen.Statement.route)
                    },
                    onNavigateToImportData = {
                        navController.navigate(Screen.ImportData.route)
                    },
                    onNavigateToCustomCategories = {
                        navController.navigate(Screen.CustomCategories.route)
                    },
                    onNavigateToSavingsGoals = {
                        navController.navigate(Screen.SavingsGoals.route)
                    },
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
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStatement = {
                        navController.navigate(Screen.Statement.route)
                    },
                    onNavigateToImportData = {
                        navController.navigate(Screen.ImportData.route)
                    },
                    onNavigateToCustomCategories = {
                        navController.navigate(Screen.CustomCategories.route)
                    }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationScreen(
                    viewModel = notificationViewModel,
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() }
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
            composable(Screen.Statement.route) {
                StatementScreen(
                    transactionsViewModel = transactionsViewModel,
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ImportData.route) {
                ImportDataScreen(
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() },
                    onImportSuccess = {
                        dashboardViewModel.refresh()
                    }
                )
            }
            composable(Screen.CustomCategories.route) {
                CustomCategoriesScreen(
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SavingsGoals.route) {
                SavingsGoalsScreen(
                    viewModel = savingsGoalsViewModel,
                    isBengali = isBengali,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
