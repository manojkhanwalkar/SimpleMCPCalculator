package com.example.scientific.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScientificToolsTest {

    private static final double DELTA = 1e-12;

    private final ScientificTools tools = new ScientificTools();

    @Test
    void squareRoot() {
        assertEquals(0, tools.sqrt(0));
        assertEquals(1, tools.sqrt(1));
        assertEquals(12, tools.sqrt(144));
        assertEquals(1.5, tools.sqrt(2.25));
        assertEquals(Math.sqrt(2), tools.sqrt(2));
    }

    @Test
    void squareRootOfNegativeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> tools.sqrt(-1));
        assertThrows(IllegalArgumentException.class, () -> tools.sqrt(-0.001));
    }

    @Test
    void naturalLogarithm() {
        assertEquals(0, tools.ln(1));
        assertEquals(1, tools.ln(Math.E), DELTA);
        assertEquals(-1, tools.ln(1 / Math.E), DELTA);
    }

    @Test
    void base10Logarithm() {
        assertEquals(0, tools.log10(1));
        assertEquals(3, tools.log10(1000));
        assertEquals(-2, tools.log10(0.01));
    }

    @Test
    void logarithmInAnyBase() {
        assertEquals(10, tools.log(1024, 2), DELTA);
        assertEquals(4, tools.log(81, 3), DELTA);
        assertEquals(0.5, tools.log(5, 25), DELTA);
        assertEquals(-3, tools.log(8, 0.5), DELTA);
        assertEquals(Math.log(10) / Math.log(2), tools.log(10, 2));
    }

    @Test
    void logarithmOfExactPowerIsExact() {
        assertEquals(3, tools.log(1000, 10));
        assertEquals(15, tools.log(1e15, 10));
        assertEquals(5, tools.log(243, 3));
        assertEquals(3, tools.log(125, 5));
        assertEquals(0, tools.log(1, 7));
    }

    @Test
    void logarithmOfNonPositiveIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> tools.ln(0));
        assertThrows(IllegalArgumentException.class, () -> tools.ln(-5));
        assertThrows(IllegalArgumentException.class, () -> tools.log10(0));
        assertThrows(IllegalArgumentException.class, () -> tools.log(-8, 2));
    }

    @Test
    void invalidBaseIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> tools.log(8, 1));
        assertThrows(IllegalArgumentException.class, () -> tools.log(8, 0));
        assertThrows(IllegalArgumentException.class, () -> tools.log(8, -2));
    }

    @Test
    void nonFiniteInputIsRejected() {
        assertThrows(ArithmeticException.class, () -> tools.sqrt(Double.NaN));
        assertThrows(ArithmeticException.class, () -> tools.sqrt(Double.POSITIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> tools.ln(Double.POSITIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> tools.log(8, Double.NaN));
    }
}
