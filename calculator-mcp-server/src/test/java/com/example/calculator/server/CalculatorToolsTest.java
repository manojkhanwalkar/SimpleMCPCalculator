package com.example.calculator.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalculatorToolsTest {

    private final CalculatorTools tools = new CalculatorTools();

    @Test
    void basicArithmetic() {
        assertEquals(12, tools.add(7, 5));
        assertEquals(2, tools.subtract(7, 5));
        assertEquals(35, tools.multiply(7, 5));
        assertEquals(1.4, tools.divide(7, 5));
        assertEquals(2, tools.remainder(7, 5));
    }

    @Test
    void negativeAndFractionalOperands() {
        assertEquals(-2.5, tools.add(-5, 2.5));
        assertEquals(-1, tools.remainder(-7, 3));
        assertEquals(1.5, tools.remainder(7.5, 2));
    }

    @Test
    void divisionByZeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> tools.divide(1, 0));
        assertThrows(IllegalArgumentException.class, () -> tools.remainder(1, 0));
    }

    @Test
    void exponent() {
        assertEquals(1024, tools.exponent(2, 10));
        assertEquals(1, tools.exponent(7, 0));
        assertEquals(0.25, tools.exponent(2, -2));
        assertEquals(3, tools.exponent(9, 0.5));
    }

    @Test
    void nonFiniteResultsAreRejected() {
        assertThrows(ArithmeticException.class, () -> tools.exponent(10, 400));
        assertThrows(ArithmeticException.class, () -> tools.exponent(-8, 0.5));
        assertThrows(ArithmeticException.class, () -> tools.exponent(0, -1));
        assertThrows(ArithmeticException.class, () -> tools.multiply(1e308, 10));
        assertThrows(ArithmeticException.class, () -> tools.divide(1e308, 1e-10));
        assertThrows(ArithmeticException.class, () -> tools.add(Double.MAX_VALUE, Double.MAX_VALUE));
    }

    @Test
    void factorial() {
        assertEquals(1, tools.factorial(0));
        assertEquals(1, tools.factorial(1));
        assertEquals(120, tools.factorial(5));
        assertEquals(2_432_902_008_176_640_000L, tools.factorial(20));
    }

    @Test
    void factorialRejectsOutOfRangeAndFractionalInput() {
        assertThrows(IllegalArgumentException.class, () -> tools.factorial(-1));
        assertThrows(IllegalArgumentException.class, () -> tools.factorial(21));
        assertThrows(IllegalArgumentException.class, () -> tools.factorial(2.5));
    }

    @Test
    void isPrime() {
        for (int p : new int[] {2, 3, 5, 7, 11, 13, 97, 7919}) {
            assertTrue(tools.isPrime(p), p + " is prime");
        }
        for (int n : new int[] {-7, 0, 1, 4, 9, 15, 25, 91, 7917}) {
            assertFalse(tools.isPrime(n), n + " is not prime");
        }
        assertTrue(tools.isPrime(2_147_483_647));
        assertThrows(IllegalArgumentException.class, () -> tools.isPrime(7.5));
    }

    @Test
    void parity() {
        assertEquals("even", tools.parity(0));
        assertEquals("even", tools.parity(42));
        assertEquals("odd", tools.parity(17));
        assertEquals("odd", tools.parity(-3));
        assertEquals("even", tools.parity(-4));
        assertThrows(IllegalArgumentException.class, () -> tools.parity(1.5));
        assertThrows(IllegalArgumentException.class, () -> tools.parity(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> tools.parity(1e300));
    }
}
