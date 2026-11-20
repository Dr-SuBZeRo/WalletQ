package com.walletq.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDestinationsTest {

    @Test
    fun topLevelDestinations_containsExactlyFivePrimaryDestinations() {
        val destinations = TopLevelDestination.entries
        assertEquals(5, destinations.size)
    }

    @Test
    fun topLevelDestinations_haveUniqueRoutes() {
        val routes = TopLevelDestination.entries.map { it.route }
        val uniqueRoutes = routes.toSet()
        assertEquals(routes.size, uniqueRoutes.size)
    }

    @Test
    fun topLevelDestinations_matchSpecifiedNames() {
        val titles = TopLevelDestination.entries.map { it.title }
        val expected = listOf("Home", "Activity", "Accounts", "Reports", "More")
        assertEquals(expected, titles)
    }

    @Test
    fun startDestination_isHome() {
        assertEquals(TopLevelDestination.HOME, TopLevelDestination.START_DESTINATION)
        assertEquals("home", TopLevelDestination.START_DESTINATION.route)
    }
}
