package com.smartsplit.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single money policy for SmartSplit: every amount has exactly two decimal places
 * and rounding, where unavoidable, uses HALF_EVEN (banker's rounding).
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private Money() {}

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public static boolean hasValidScale(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= SCALE;
    }
}
