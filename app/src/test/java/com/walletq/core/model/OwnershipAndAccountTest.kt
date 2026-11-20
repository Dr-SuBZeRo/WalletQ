package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class OwnershipAndAccountTest {

    @Test
    fun ownershipType_supportsPersonalAndHousehold() {
        val types = OwnershipType.entries.map { it.name }
        assertTrue(types.contains("PERSONAL"))
        assertTrue(types.contains("HOUSEHOLD"))
    }

    @Test
    fun accountType_containsStandardFinancialCategories() {
        val types = AccountType.entries.map { it.name }
        val expected = listOf("CASH", "BANK", "SAVINGS", "CREDIT_CARD", "INVESTMENT", "OTHER")
        assertTrue(types.containsAll(expected))
    }

    @Test
    fun householdRole_containsStandardRoles() {
        val roles = HouseholdRole.entries.map { it.name }
        val expected = listOf("OWNER", "ADMIN", "MEMBER", "VIEWER")
        assertTrue(roles.containsAll(expected))
    }

    @Test
    fun household_initialization_preservesValues() {
        val household = Household(
            id = "hh-1",
            name = "Verma Family",
            description = "Main household budget",
            createdAtEpochMs = 1700000000000L
        )
        assertEquals("hh-1", household.id)
        assertEquals("Verma Family", household.name)
        assertEquals("Main household budget", household.description)
        assertEquals(1700000000000L, household.createdAtEpochMs)
    }

    @Test
    fun householdMember_supportsMultipleMemberships() {
        val m1 = HouseholdMember(
            householdId = "hh-1",
            profileId = "profile-1",
            role = HouseholdRole.OWNER,
            joinedAtEpochMs = 1700000000000L
        )
        val m2 = HouseholdMember(
            householdId = "hh-2",
            profileId = "profile-1", // Profile belongs to multiple households
            role = HouseholdRole.MEMBER,
            joinedAtEpochMs = 1700000000000L
        )
        assertEquals("hh-1", m1.householdId)
        assertEquals("hh-2", m2.householdId)
        assertEquals("profile-1", m1.profileId)
        assertEquals("profile-1", m2.profileId)
    }

    @Test
    fun personalAccount_initialization_preservesValues() {
        val account = Account(
            id = "acc-personal-1",
            profileId = "profile-1",
            householdId = null,
            ownershipType = OwnershipType.PERSONAL,
            name = "My Personal Cash",
            type = AccountType.CASH,
            currency = Currency.INR,
            initialBalanceMinorUnits = 50000L,
            currentBalanceMinorUnits = 50000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        assertEquals("acc-personal-1", account.id)
        assertNull(account.householdId)
        assertEquals(OwnershipType.PERSONAL, account.ownershipType)
    }

    @Test
    fun householdAccount_initialization_linksToHousehold() {
        val account = Account(
            id = "acc-hh-1",
            profileId = "profile-1",
            householdId = "hh-1",
            ownershipType = OwnershipType.HOUSEHOLD,
            name = "Joint Savings",
            type = AccountType.SAVINGS,
            currency = Currency.INR,
            initialBalanceMinorUnits = 500000L,
            currentBalanceMinorUnits = 500000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        assertEquals("acc-hh-1", account.id)
        assertEquals("hh-1", account.householdId)
        assertEquals(OwnershipType.HOUSEHOLD, account.ownershipType)
    }

    @Test
    fun exchangeRate_initialization_preservesValues() {
        val rate = ExchangeRate(
            id = "rate-usd-inr",
            baseCurrencyCode = "USD",
            targetCurrencyCode = "INR",
            rate = BigDecimal("84.25000000"),
            timestampEpochMs = 1700000000000L
        )
        assertEquals("rate-usd-inr", rate.id)
        assertEquals("USD", rate.baseCurrencyCode)
        assertEquals("INR", rate.targetCurrencyCode)
        assertEquals(BigDecimal("84.25000000"), rate.rate)
    }
}
