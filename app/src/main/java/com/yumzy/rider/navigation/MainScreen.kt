package com.yumzy.rider.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Moped
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yumzy.rider.main.MyDeliveriesScreen
import com.yumzy.rider.main.NewOrdersScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object NewOrders : Screen("new_orders", "New Orders", Icons.Default.List)
    data object MyDeliveries : Screen("my_deliveries", "My Deliveries", Icons.Default.Moped)
}

@Composable
fun MainScreen(
    onAcceptOrder: (orderId: String) -> Unit,
    onUpdateOrderStatus: (orderId: String, newStatus: String) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.NewOrders,
        Screen.MyDeliveries,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.NewOrders.route, Modifier.padding(innerPadding)) {
            composable(Screen.NewOrders.route) {
                NewOrdersScreen(onAcceptOrder = onAcceptOrder)
            }
            composable(Screen.MyDeliveries.route) {
                MyDeliveriesScreen(onUpdateOrderStatus = onUpdateOrderStatus)
            }
        }
    }
}