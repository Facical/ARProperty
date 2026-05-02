package com.arproperty.android.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface AppDestination {
    val route: String
}

enum class TopLevelDestination(
    override val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) : AppDestination {
    Ar(
        route = "ar",
        label = "AR",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
    ),
    Map(
        route = "map",
        label = "Map",
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
    ),
}

data object BuildingDetailDestination : AppDestination {
    const val buildingIdArg = "buildingId"
    override val route = "building/{$buildingIdArg}"

    fun createRoute(buildingId: Int): String = "building/$buildingId"
}

data object ComplexDetailDestination : AppDestination {
    const val complexIdArg = "complexId"
    override val route = "complex/{$complexIdArg}"

    fun createRoute(complexId: Int): String = "complex/$complexId"
}

data object LivabilityDestination : AppDestination {
    const val buildingIdArg = "buildingId"
    override val route = "livability/{$buildingIdArg}"

    fun createRoute(buildingId: Int): String = "livability/$buildingId"
}
