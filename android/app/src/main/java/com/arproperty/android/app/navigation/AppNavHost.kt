package com.arproperty.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.arproperty.android.feature.ar.ArRoute
import com.arproperty.android.feature.building.BuildingDetailRoute
import com.arproperty.android.feature.building.ComplexDetailRoute
import com.arproperty.android.feature.livability.LivabilityRoute
import com.arproperty.android.feature.map.MapRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Ar.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Ar.route) {
            ArRoute(
                onOpenBuilding = { navController.navigate(BuildingDetailDestination.createRoute(it)) },
                onOpenLivability = { navController.navigate(LivabilityDestination.createRoute(it)) },
                onOpenMap = { navController.navigate(TopLevelDestination.Map.route) },
            )
        }
        composable(TopLevelDestination.Map.route) {
            MapRoute(
                onOpenBuilding = { navController.navigate(BuildingDetailDestination.createRoute(it)) },
                onOpenLivability = { navController.navigate(LivabilityDestination.createRoute(it)) },
            )
        }
        composable(
            route = BuildingDetailDestination.route,
            arguments = listOf(
                navArgument(BuildingDetailDestination.buildingIdArg) {
                    type = NavType.IntType
                },
            ),
        ) { entry ->
            BuildingDetailRoute(
                buildingId = entry.arguments?.getInt(BuildingDetailDestination.buildingIdArg) ?: -1,
                onOpenComplex = { navController.navigate(ComplexDetailDestination.createRoute(it)) },
                onOpenLivability = { navController.navigate(LivabilityDestination.createRoute(it)) },
            )
        }
        composable(
            route = ComplexDetailDestination.route,
            arguments = listOf(
                navArgument(ComplexDetailDestination.complexIdArg) {
                    type = NavType.IntType
                },
            ),
        ) { entry ->
            ComplexDetailRoute(
                complexId = entry.arguments?.getInt(ComplexDetailDestination.complexIdArg) ?: -1,
            )
        }
        composable(
            route = LivabilityDestination.route,
            arguments = listOf(
                navArgument(LivabilityDestination.buildingIdArg) {
                    type = NavType.IntType
                },
            ),
        ) { entry ->
            LivabilityRoute(
                buildingId = entry.arguments?.getInt(LivabilityDestination.buildingIdArg) ?: -1,
            )
        }
    }
}
