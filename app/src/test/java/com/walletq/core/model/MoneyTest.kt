package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun money_storesExactMinorUnits() {
        // 100.50 INR = 10050 paise
        val money = Money(minorUnits = 10050L, currency = Currency.INR)
        assertEquals(10050L, money.minorUnits)
        assertEquals(Currency.INR, money.currency)
    }

    @Test
    fun money_zero_isZero() {
        val zeroInr = Money.zero(Currency.INR)
        assertTrue(zeroInr.isZero())
        assertEquals(0L, zeroInr.minorUnits)
    }

    @Test
    fun money_addition_sameCurrency_succeeds() {
        val m1 = Money(1500L, Currency.INR) // 15.00
        val m2 = Money(2500L, Currency.INR) // 25.00
        val result = m1 + m2
        assertEquals(4000L, result.minorUnits)
        assertEquals(Currency.INR, result.currency)
    }

    @Test
    fun money_addition_differentCurrencies_throwsException() {
        val inr = Money(1000L, Currency.INR)
        val usd = Money(1000L, Currency.USD)
        try {
            val unused = inr + usd
            fail("Expected IllegalArgumentException when adding different currencies without explicit conversion")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("different currencies") == true)
        }
    }

    @Test
    fun money_subtraction_sameCurrency_succeeds() {
        val m1 = Money(5000L, Currency.INR) // 50.00
        val m2 = Money(1850L, Currency.INR) // 18.50
        val result = m1 - m2
        assertEquals(3150L, result.minorUnits)
    }

    @Test
    fun money_subtraction_differentCurrencies_throwsException() {
        val inr = Money(5000L, Currency.INR)
        val eur = Money(2000L, Currency.EUR)
        try {
            val unused = inr - eur
            fail("Expected IllegalArgumentException when subtracting different currencies")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("different currencies") == true)
        }
    }

    @Test
    fun money_unaryMinus_negatesAmount() {
        val positive = Money(1200L, Currency.INR)
        val negative = -positive
        assertEquals(-1200L, negative.minorUnits)
        assertTrue(negative.isNegative())
    }

    @Test
    fun money_multiplicationByLong_succeeds() {
        val unitPrice = Money(250L, Currency.INR) // 2.50
        val total = unitPrice * 4L
        assertEquals(1000L, total.minorUnits) // 10.00
    }

    @Test
    fun money_multiplicationByBigDecimal_bankersRounding() {
        val base = Money(10000L, Currency.USD) // 100.00 USD
        // 100.00 * 1.055 = 105.50
        val factor = BigDecimal("1.055")
        val result = base * factor
        assertEquals(10550L, result.minorUnits)
    }

    @Test
    fun money_handlesCurrenciesWithZeroDecimals_JPY() {
        // JPY has 0 decimals
        val yen = Money.fromMajorUnits(5000L, Currency.JPY)
        assertEquals(5000L, yen.minorUnits)
        assertEquals(BigDecimal("5000"), yen.toBigDecimal())
        assertEquals("¥ 5,000", yen.format())
    }

    @Test
    fun money_formatting_inr_includesSymbolAndDecimals() {
        val inr = Money(123456L, Currency.INR) // 1,234.56
        assertEquals("₹ 1,234.56", inr.format(includeSymbol = true))
        assertEquals("1,234.56", inr.format(includeSymbol = false))
    }

    @Test
    fun money_fromBigDecimal_preservesPrecision() {
        val dec = BigDecimal("99.99")
        val money = Money.fromBigDecimal(dec, Currency.USD)
        assertEquals(9999L, money.minorUnits)
        assertEquals(dec, money.toBigDecimal())
    }

    @Test
    fun money_comparison_sameCurrency_ordersCorrectly() {
        val smaller = Money(1000L, Currency.EUR)
        val larger = Money(2000L, Currency.EUR)
        assertTrue(smaller < larger)
        assertTrue(larger > smaller)
        assertFalse(smaller == larger)
    }
}
