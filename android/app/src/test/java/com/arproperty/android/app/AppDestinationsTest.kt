package com.arproperty.android.app

import com.arproperty.android.app.navigation.BuildingDetailDestination
import com.arproperty.android.app.navigation.ComplexDetailDestination
import com.arproperty.android.app.navigation.LivabilityDestination
import com.arproperty.android.app.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationsTest {
    @Test
    fun topLevelRoutes_areStable() {
        assertEquals("ar", TopLevelDestination.Ar.route)
        assertEquals("map", TopLevelDestination.Map.route)
    }

    @Test
    fun detailRouteFactories_embedIds() {
        assertEquals("building/42", BuildingDetailDestination.createRoute(42))
        assertEquals("complex/10", ComplexDetailDestination.createRoute(10))
        assertEquals("livability/42", LivabilityDestination.createRoute(42))
    }

    @Test
    fun routeTemplates_keepRequiredArgs() {
        assertTrue(BuildingDetailDestination.route.contains(BuildingDetailDestination.buildingIdArg))
        assertTrue(ComplexDetailDestination.route.contains(ComplexDetailDestination.complexIdArg))
        assertTrue(LivabilityDestination.route.contains(LivabilityDestination.buildingIdArg))
    }
}
