package com.smartsplit.util;

import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;
import java.math.BigDecimal;

public class MoneyTest {

    @Test public void normalize_roundsToTwoDecimalPlaces() {
        BigDecimal result = Money.normalize(new BigDecimal("1.005"));
        Assertions.assertEquals(2, result.scale());
    }

    @Test public void normalize_halfEven_roundsDown() {
        // 1.235 → banker's rounding → 1.24 (5 after even digit 3 rounds up)
        // 1.245 → banker's rounding → 1.24 (5 after even digit 4 rounds down)
        BigDecimal r1 = Money.normalize(new BigDecimal("1.235"));
        BigDecimal r2 = Money.normalize(new BigDecimal("1.245"));
        Assertions.assertDecimalEquals(new BigDecimal("1.24"), r1);
        Assertions.assertDecimalEquals(new BigDecimal("1.24"), r2);
    }

    @Test public void normalize_zero_returnsZero() {
        Assertions.assertDecimalEquals(BigDecimal.ZERO, Money.normalize(BigDecimal.ZERO));
    }

    @Test public void hasValidScale_twoDecimals_true() {
        Assertions.assertTrue(Money.hasValidScale(new BigDecimal("10.50")));
    }

    @Test public void hasValidScale_threeDecimals_false() {
        Assertions.assertFalse(Money.hasValidScale(new BigDecimal("10.505")));
    }

    @Test public void hasValidScale_integer_true() {
        Assertions.assertTrue(Money.hasValidScale(new BigDecimal("100")));
    }

    @Test public void hasValidScale_oneDecimal_true() {
        Assertions.assertTrue(Money.hasValidScale(new BigDecimal("10.5")));
    }
}
