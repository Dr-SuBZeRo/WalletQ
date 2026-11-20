package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun profile_initialization_preservesValues() {
        val profile = Profile(
            id = "profile-1",
            name = "Family Budget",
            type = OwnershipType.HOUSEHOLD,
            isDefault = false,
            createdAtEpochMs = 1700000000000L
        )
        assertEquals("profile-1", profile.id)
        assertEquals("Family Budget", profile.name)
        assertEquals(OwnershipType.HOUSEHOLD, profile.type)
        assertFalse(profile.isDefault)
    }

    @Test
    fun account_initialization_preservesValues() {
        val account = Account(
            id = "acc-1",
            profileId = "profile-1",
            name = "Main Savings",
            type = AccountType.SAVINGS,
            currency = Currency.INR,
            initialBalanceMinorUnits = 500000L,
            currentBalanceMinorUnits = 500000L,
            isArchived = false,
            createdAtEpochMs = 1700000000000L
        )
        assertEquals("acc-1", account.id)
        assertEquals(500000L, account.currentBalanceMinorUnits)
        assertEquals(Currency.INR, account.currency)
        assertFalse(account.isArchived)
    }
}
