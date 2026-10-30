package com.example.calculator.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Sends each operation to whichever connected MCP server offers a tool of that name. The routing
 * table is built once from every server's {@code tools/list}, so a tool added to either server is
 * picked up without changing this class. Operands are positional: they are matched, in order, to
 * the tool's required parameters ({@code a, b} for add, {@code n} for factorial, {@code x, base}
 * for log, ...).
 */
public class CalculatorRouter {

    /** Where an operation is sent and which parameters it takes, in order. */
    public record Route(McpSyncClient client, String server, List<String> parameters) {
    }

    private final SortedMap<String, Route> routes = new TreeMap<>();

    /** @throws IllegalStateException if two servers offer a tool with the same name */
    public CalculatorRouter(List<McpSyncClient> clients) {
        for (McpSyncClient client : clients) {
            String server = client.getServerInfo().name();
            for (Tool tool : client.listTools().tools()) {
                Route existing = routes.putIfAbsent(tool.name(), new Route(client, server, requiredParameters(tool)));
                if (existing != null) {
                    throw new IllegalStateException("Tool '" + tool.name() + "' is offered by both "
                            + existing.server() + " and " + server + "; can't tell which to call");
                }
            }
        }
    }

    /** The input schema's "required" list, which Spring AI writes in method parameter order. */
    private static List<String> requiredParameters(Tool tool) {
        if (tool.inputSchema() != null && tool.inputSchema().get("required") instanceof List<?> required) {
            return required.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /** Every operation offered by any server, by name, with the server it is routed to. */
    public SortedMap<String, Route> routes() {
        return routes;
    }

    /**
     * Calls {@code operation} on the server that offers it and returns the text result, or
     * {@code "Error: <message>"} if the operation is unknown, the operand count is wrong, or the
     * server reports an error. Spring AI repeats the exception message on several lines in error
     * results, so only the first line is kept.
     */
    public String calculate(String operation, List<Double> operands) {
        Route route = routes.get(operation);
        if (route == null) {
            return "Error: Unknown operation: " + operation;
        }
        if (operands.size() != route.parameters().size()) {
            return "Error: " + operation + " takes " + route.parameters().size() + " operand(s): "
                    + String.join(", ", route.parameters());
        }
        Map<String, Object> arguments = new HashMap<>();
        for (int i = 0; i < operands.size(); i++) {
            arguments.put(route.parameters().get(i), operands.get(i));
        }
        CallToolResult result;
        try {
            result = route.client().callTool(new CallToolRequest(operation, arguments));
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
