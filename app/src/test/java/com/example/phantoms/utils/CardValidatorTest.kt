package com.example.phantoms.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardValidatorTest {

    @Test
    fun `valid Visa card passes Luhn and prefix checks`() {
        // 16-digit Visa test number (common test number that passes Luhn)
        val visa = 4111111111111111L
        assertTrue(CardValidator.isValid(visa))
    }

    @Test
    fun `random invalid number fails validation`() {
        val invalid = 1234567890123456L
        assertFalse(CardValidator.isValid(invalid))
    }
}
