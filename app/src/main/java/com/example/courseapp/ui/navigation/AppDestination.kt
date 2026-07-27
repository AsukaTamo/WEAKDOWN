package com.example.courseapp.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination {
    data object Schedule : AppDestination()
    data object Manage : AppDestination()
    data object Profile : AppDestination()
    data object WebImport : AppDestination()
    data object ImportPreview : AppDestination()
}

data class BottomNavItem(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
