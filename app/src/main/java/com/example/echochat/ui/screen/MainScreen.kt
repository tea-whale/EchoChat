package com.example.echochat.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object ChatList : Screen("chat_list", "聊天", Icons.AutoMirrored.Filled.Chat)
    object Contacts : Screen("contacts", "联系人", Icons.Default.Person)
    object Moments : Screen("moments", "朋友圈", Icons.Default.Public)
    object Me : Screen("me", "我的", Icons.Default.Settings)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.ChatList,
        Screen.Contacts,
        Screen.Moments,
        Screen.Me
    )

    val showBottomBar = currentRoute in bottomBarScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ChatList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onChatClick = { agentId, groupId, conversationId ->
                        navController.navigate("chat_detail?agentId=$agentId&groupId=$groupId&conversationId=$conversationId")
                    }
                )
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onAgentClick = { agentId ->
                        navController.navigate("chat_detail?agentId=$agentId")
                    }
                )
            }
            composable(Screen.Moments.route) {
                MomentsScreen()
            }
            composable(Screen.Me.route) {
                MeScreen()
            }
            composable(
                route = "chat_detail?agentId={agentId}&groupId={groupId}&conversationId={conversationId}",
                arguments = listOf(
                    navArgument("agentId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("groupId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("conversationId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val agentId = backStackEntry.arguments?.getString("agentId")?.toLongOrNull()
                val groupId = backStackEntry.arguments?.getString("groupId")?.toLongOrNull()
                val conversationId = backStackEntry.arguments?.getString("conversationId")?.toLongOrNull()
                ChatDetailScreen(
                    agentId = agentId,
                    groupId = groupId,
                    conversationId = conversationId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
