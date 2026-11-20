package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyTest {

    @Test
    fun defaultReportingCurrency_isINR() {
        assertEquals("INR", Currency.DEFAULT_REPORTING_CURRENCY.code)
        assertEquals("₹", Currency.DEFAULT_REPORTING_CURRENCY.symbol)
        assertEquals(2, Currency.DEFAULT_REPORTING_CURRENCY.decimalPlaces)
    }

    @Test
    fun supportedCurrencies_containsRequiredInitialSet() {
        val codes = Currency.SUPPORTED_CURRENCIES.map { it.code }
        val requiredCodes = listOf("INR", "USD", "EUR", "GBP", "AED", "JPY")
        assertTrue(codes.containsAll(requiredCodes))
    }

    @Test
    fun currencyPrecision_jpyHasZeroDecimals() {
        val jpy = Currency.JPY
        assertEquals(0, jpy.decimalPlaces)
        assertEquals(1L, jpy.minorUnitFactor)
    }

    @Test
    fun currencyPrecision_twoDecimalCurrencies() {
        val twoDecimalCurrencies = listOf(Currency.INR, Currency.USD, Currency.EUR, Currency.GBP, Currency.AED)
        twoDecimalCurrencies.forEach { currency ->
            assertEquals("Expected 2 decimal places for ${currency.code}", 2, currency.decimalPlaces)
            assertEquals("Expected factor 100 for ${currency.code}", 100L, currency.minorUnitFactor)
        }
    }

    @Test
    fun findByCode_caseInsensitive_returnsCurrency() {
        val usdLower = Currency.findByCode("usd")
        assertNotNull(usdLower)
        assertEquals("USD", usdLower.code)
        assertEquals("$", usdLower.symbol)
    }

    @Test
    fun findByCode_unknownCurrency_returnsFallback() {
        val unknown = Currency.findByCode("XYZ")
        assertEquals("XYZ", unknown.code)
        assertEquals(2, unknown.decimalPlaces)
    }
}
