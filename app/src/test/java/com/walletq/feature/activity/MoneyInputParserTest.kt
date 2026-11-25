package com.walletq.feature.activity

import com.walletq.core.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyInputParserTest {

    @Test
    fun parseAmount_validInrAndUsd_returnsExpectedMinorUnits() {
        val inr = MoneyInputParser.parseAmount("125.50", Currency.INR)
        assertTrue(inr is MoneyInputParser.ParseResult.Success)
        assertEquals(12550L, (inr as MoneyInputParser.ParseResult.Success).value.minorUnits)

        val usd = MoneyInputParser.parseAmount("100", Currency.USD)
        assertTrue(usd is MoneyInputParser.ParseResult.Success)
        assertEquals(10000L, (usd as MoneyInputParser.ParseResult.Success).value.minorUnits)
    }

    @Test
    fun parseAmount_jpyZeroDecimals_rejectsFractionalInput() {
        val valid = MoneyInputParser.parseAmount("1500", Currency.JPY)
        assertTrue(valid is MoneyInputParser.ParseResult.Success)
        assertEquals(1500L, (valid as MoneyInputParser.ParseResult.Success).value.minorUnits)

        val invalid = MoneyInputParser.parseAmount("1500.5", Currency.JPY)
        assertTrue(invalid is MoneyInputParser.ParseResult.Error)
        assertEquals("JPY does not support decimal places.", (invalid as MoneyInputParser.ParseResult.Error).message)
    }

    @Test
    fun parseAmount_exceedingCurrencyPrecision_returnsError() {
        val result = MoneyInputParser.parseAmount("10.123", Currency.INR)
        assertTrue(result is MoneyInputParser.ParseResult.Error)
        assertEquals("INR supports at most 2 decimal places.", (result as MoneyInputParser.ParseResult.Error).message)
    }

    @Test
    fun parseAmount_zeroNegativeOrBlank_returnsError() {
        assertTrue(MoneyInputParser.parseAmount("", Currency.INR) is MoneyInputParser.ParseResult.Error)
        assertTrue(MoneyInputParser.parseAmount("0", Currency.INR) is MoneyInputParser.ParseResult.Error)
        assertTrue(MoneyInputParser.parseAmount("0.00", Currency.INR) is MoneyInputParser.ParseResult.Error)
        assertTrue(MoneyInputParser.parseAmount("-50", Currency.INR) is MoneyInputParser.ParseResult.Error)
        assertTrue(MoneyInputParser.parseAmount("abc", Currency.INR) is MoneyInputParser.ParseResult.Error)
    }

    @Test
    fun parseExchangeRate_validAndInvalidCases() {
        val valid = MoneyInputParser.parseExchangeRate("83.50")
        assertTrue(valid is MoneyInputParser.ParseResult.Success)
        assertEquals(0, BigDecimal("83.50").compareTo((valid as MoneyInputParser.ParseResult.Success).value))

        assertTrue(MoneyInputParser.parseExchangeRate("") is MoneyInputParser.ParseResult.Error)
        assertTrue(MoneyInputParser.parseExchangeRate("0") is MoneyInputParser.ParseResult.Error)
        assertTrue(MoneyInputParser.parseExchangeRate("-1.2") is MoneyInputParser.ParseResult.Error)
    }

    @Test
    fun computeDestinationPreview_crossCurrency_usesBankersRounding() {
        // $10.00 USD -> JPY @ 155.45 => 1554.5 => HALF_EVEN -> 1554
        val preview = MoneyInputParser.computeDestinationPreview(
            sourceAmountInput = "10.00",
            sourceCurrency = Currency.USD,
            destinationCurrency = Currency.JPY,
            exchangeRateInput = "155.45"
        )
        assertEquals(1554L, preview?.minorUnits)
        assertEquals(Currency.JPY, preview?.currency)

        // Missing rate returns null
        val missingRate = MoneyInputParser.computeDestinationPreview(
            sourceAmountInput = "10.00",
            sourceCurrency = Currency.USD,
            destinationCurrency = Currency.JPY,
            exchangeRateInput = ""
        )
        assertNull(missingRate)
    }
}
