package com.walletq.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun money_storesExactMinorUnits() {
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

    @Test(expected = ArithmeticException::class)
    fun money_unaryMinus_overflowsOnLongMinValue() {
        val minValueMoney = Money(Long.MIN_VALUE, Currency.INR)
        val unused = -minValueMoney
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
        val factor = BigDecimal("1.055")
        val result = base * factor
        assertEquals(10550L, result.minorUnits)
    }

    @Test
    fun money_divisionByLong_bankersRounding() {
        // 100.00 INR divided among 3 members: 100.00 / 3 = 33.33
        val total = Money(10000L, Currency.INR)
        val split = total / 3L
        assertEquals(3333L, split.minorUnits)
    }

    @Test(expected = IllegalArgumentException::class)
    fun money_divisionByZero_throwsException() {
        val total = Money(10000L, Currency.INR)
        val unused = total / 0L
    }

    @Test
    fun money_ratioDivision_returnsPreciseBigDecimal() {
        val spent = Money(5000L, Currency.INR) // 50.00
        val budget = Money(20000L, Currency.INR) // 200.00
        val ratio = spent / budget
        assertEquals(0, BigDecimal("0.25000000").compareTo(ratio))
    }

    @Test
    fun money_convertTo_handlesDifferentDecimals_usdToJpy() {
        // 10.00 USD with exchange rate 155.45 JPY per USD => 1554.50 JPY
        // Banker's Rounding (HALF_EVEN) rounds .5 to the nearest even integer: 1554.5 -> 1554 JPY (4 is even)
        val usd = Money(1000L, Currency.USD)
        val rate = BigDecimal("155.45")
        val jpy = usd.convertTo(Currency.JPY, rate)
        assertEquals(Currency.JPY, jpy.currency)
        assertEquals(1554L, jpy.minorUnits) // JPY has 0 decimals; 1554 is even
        assertEquals("¥ 1,554", jpy.format())
    }

    @Test
    fun money_convertTo_handlesDifferentDecimals_jpyToUsd() {
        // 1554 JPY with rate 0.006435 USD per JPY => 1554 * 0.006435 = 10.0000 -> 10.00 USD (2 decimals)
        val jpy = Money(1554L, Currency.JPY)
        val rate = BigDecimal("0.006435")
        val usd = jpy.convertTo(Currency.USD, rate)
        assertEquals(Currency.USD, usd.currency)
        assertEquals(1000L, usd.minorUnits)
        assertEquals("$ 10.00", usd.format())
    }

    @Test(expected = IllegalArgumentException::class)
    fun money_convertTo_zeroOrNegativeRate_throwsException() {
        val inr = Money(1000L, Currency.INR)
        inr.convertTo(Currency.USD, BigDecimal.ZERO)
    }

    @Test
    fun money_handlesCurrenciesWithZeroDecimals_JPY() {
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
