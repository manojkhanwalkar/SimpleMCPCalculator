package com.example.scientific.server;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The scientific calculator capabilities exposed as MCP tools. An exception thrown here is
 * returned to the client as a tool result with {@code isError=true} rather than breaking the
 * connection. Every tool is a pure function, so each is marked read-only, idempotent and closed-world.
 */
@Component
public class ScientificTools {

    @McpTool(name = "sqrt", description = "Square root of x. Fails if x is negative",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public double sqrt(@McpToolParam(description = "Number to take the square root of (must not be negative)") double x) {
        if (x < 0) {
            throw new IllegalArgumentException("Square root of a negative number");
        }
        // Adding 0.0 turns Math.sqrt(-0.0) = -0.0 into 0.0
        return requireFinite(Math.sqrt(x) + 0.0);
    }

    @McpTool(name = "ln", description = "Natural logarithm (base e) of x. Fails unless x > 0",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public double ln(@McpToolParam(description = "Number to take the logarithm of (must be > 0)") double x) {
        requirePositive(x);
        return requireFinite(Math.log(x));
    }

    @McpTool(name = "log10", description = "Base-10 logarithm of x. Fails unless x > 0",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public double log10(@McpToolParam(description = "Number to take the logarithm of (must be > 0)") double x) {
        requirePositive(x);
        return requireFinite(Math.log10(x));
    }

    @McpTool(name = "log", description = "Logarithm of x in the given base: log_base(x). Fails unless x > 0, base > 0 and base != 1",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public double log(@McpToolParam(description = "Number to take the logarithm of (must be > 0)") double x,
                      @McpToolParam(description = "Base of the logarithm (must be > 0 and not 1)") double base) {
        requirePositive(x);
        if (base <= 0 || base == 1) {
            throw new IllegalArgumentException("Logarithm base must be > 0 and not 1");
        }
        double result = requireFinite(Math.log(x) / Math.log(base));
        // ln(x) / ln(base) is a little off for exact powers (log 1000 base 10 = 2.9999999999999996),
        // so return the whole number when base raised to it gives x back exactly.
        double whole = Math.rint(result);
        return Math.abs(result - whole) < 1e-9 && Math.pow(base, whole) == x ? whole : result;
    }

    private static void requirePositive(double x) {
        if (x <= 0) {
            throw new IllegalArgumentException("Logarithm is only defined for x > 0");
        }
    }

    /**
     * NaN and infinite inputs (or results) would otherwise reach the client as "NaN" or "Infinity"
     * in a successful result.
     */
    private static double requireFinite(double result) {
        if (!Double.isFinite(result)) {
            throw new ArithmeticException(Double.isNaN(result) ? "Result is undefined" : "Result is too large");
        }
        return result;
    }
}
