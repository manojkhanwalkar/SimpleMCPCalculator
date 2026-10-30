package com.example.scientific.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/** Thin wrapper that turns scientific calculator operations into MCP {@code tools/call} requests. */
public class ScientificCalculatorClient {

    private final McpSyncClient client;

    public ScientificCalculatorClient(McpSyncClient client) {
        this.client = client;
    }

    /** Names of the tools the server advertises. */
    public List<String> listOperations() {
        return client.listTools().tools().stream().map(Tool::name).toList();
    }

    /** Calls a one-operand tool ({@code sqrt}, {@code ln}, {@code log10}) with argument {@code x}. */
    public String calculate(String operation, double x) {
        return call(operation, Map.of("x", x));
    }

    /** Calls {@code log} with argument {@code x} and {@code base}. */
    public String calculate(String operation, double x, double base) {
        return call(operation, Map.of("x", x, "base", base));
    }

    /**
     * Returns the server's text result, or {@code "Error: <message>"} if the tool reported an error
     * or the request failed (e.g. an unknown tool name, a timeout or a dead server). Spring AI repeats the
     * exception message on several lines in error results, so only the first line is kept.
     */
    private String call(String operation, Map<String, Object> arguments) {
        CallToolResult result;
        try {
            result = client.callTool(new CallToolRequest(operation, arguments));
        } catch (RuntimeException e) {
            // McpError (e.g. unknown tool) and transport failures/timeouts alike become an error string.
            return "Error: " + firstLine(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        String text = result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(c -> ((TextContent) c).text())
                .collect(Collectors.joining("\n"));
        return Boolean.TRUE.equals(result.isError()) ? "Error: " + firstLine(text) : text;
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("");
    }
}
