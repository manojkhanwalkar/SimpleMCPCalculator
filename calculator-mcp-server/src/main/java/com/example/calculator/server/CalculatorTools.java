package com.example.calculator.server;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The calculator capabilities exposed as MCP tools. An exception thrown here is returned to the
 * client as a tool result with {@code isError=true} rather than breaking the connection.
 */
@Component
public class CalculatorTools {

    /** 20! is the largest factorial that fits in a {@code long}. */
    static final int MAX_FACTORIAL = 20;

    /** Largest whole number a {@code double} represents exactly (2^53). */
    static final long MAX_EXACT_WHOLE_NUMBER = 1L << 53;

    @McpTool(name = "add", description = "Add two numbers: a + b")
    public double add(@McpToolParam(description = "First operand") double a,
                      @McpToolParam(description = "Second operand") double b) {
        return a + b;
    }

    @McpTool(name = "subtract", description = "Subtract the second number from the first: a - b")
    public double subtract(@McpToolParam(description = "Number to subtract from") double a,
                           @McpToolParam(description = "Number to subtract") double b) {
        return a - b;
    }

    @McpTool(name = "multiply", description = "Multiply two numbers: a * b")
    public double multiply(@McpToolParam(description = "First factor") double a,
                           @McpToolParam(description = "Second factor") double b) {
        return a * b;
    }

    @McpTool(name = "divide", description = "Divide the first number by the second: a / b. Fails if b is 0")
    public double divide(@McpToolParam(description = "Dividend") double a,
                         @McpToolParam(description = "Divisor (must not be 0)") double b) {
        requireNonZero(b);
        return a / b;
    }

    @McpTool(name = "remainder", description = "Remainder of dividing the first number by the second: a % b. Fails if b is 0")
    public double remainder(@McpToolParam(description = "Dividend") double a,
                            @McpToolParam(description = "Divisor (must not be 0)") double b) {
        requireNonZero(b);
        return a % b;
    }

    @McpTool(name = "exponent", description = "Raise the first number to the power of the second: a ^ b")
    public double exponent(@McpToolParam(description = "Base") double a,
                           @McpToolParam(description = "Exponent") double b) {
        return Math.pow(a, b);
    }

    @McpTool(name = "factorial", description = "Factorial of a whole number: n!. Fails unless 0 <= n <= " + MAX_FACTORIAL)
    public long factorial(@McpToolParam(description = "Whole number from 0 to " + MAX_FACTORIAL) double n) {
        long value = requireWholeNumber(n);
        if (value < 0 || value > MAX_FACTORIAL) {
            throw new IllegalArgumentException("Factorial is only defined here for 0 to " + MAX_FACTORIAL);
        }
        long result = 1;
        for (long i = 2; i <= value; i++) {
            result *= i;
        }
        return result;
    }

    @McpTool(name = "isPrime", description = "Whether a whole number is prime. Returns true or false")
    public boolean isPrime(@McpToolParam(description = "Whole number to test") double n) {
        long value = requireWholeNumber(n);
        if (value < 2) {
            return false;
        }
        if (value % 2 == 0) {
            return value == 2;
        }
        for (long i = 3; i <= value / i; i += 2) {
            if (value % i == 0) {
                return false;
            }
        }
        return true;
    }

    @McpTool(name = "parity", description = "Whether a whole number is even or odd. Returns \"even\" or \"odd\"")
    public String parity(@McpToolParam(description = "Whole number to test") double n) {
        return requireWholeNumber(n) % 2 == 0 ? "even" : "odd";
    }

    private static void requireNonZero(double divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
    }

    /**
     * Integer tools take a number so that 7.5 is rejected instead of silently truncated to 7.
     * Beyond 2^53 a double can't hold every whole number exactly, so those are rejected too.
     */
    private static long requireWholeNumber(double n) {
        if (n != Math.rint(n)) {
            throw new IllegalArgumentException("Not a whole number: " + n);
        }
        if (Math.abs(n) > MAX_EXACT_WHOLE_NUMBER) {
            throw new IllegalArgumentException("Whole number out of range (|n| <= 2^53): " + n);
        }
        return (long) n;
    }
}
