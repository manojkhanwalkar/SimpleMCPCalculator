package com.example.calculator.router;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/** Mocked MCP servers for the router's unit tests. */
final class FakeServers {

    private FakeServers() {
    }

    /**
     * A server named {@code name} offering each tool in {@code tools} with the given required
     * parameters. A call answers {@code "<tool>(<arguments>)"}, or a tool error when an operand is negative.
     */
    static McpSyncClient server(String name, Map<String, List<String>> tools) {
        McpSyncClient client = mock(McpSyncClient.class);
        when(client.getServerInfo()).thenReturn(new Implementation(name, "1.0.0"));
        List<Tool> list = new ArrayList<>();
        tools.forEach((tool, required) -> list.add(Tool.builder().name(tool)
                .inputSchema(Map.of("type", "object", "required", required)).build()));
        when(client.listTools()).thenReturn(new ListToolsResult(list, null));
        when(client.callTool(any())).thenAnswer(call -> {
            CallToolRequest request = call.getArgument(0);
            boolean negative = request.arguments().values().stream().anyMatch(v -> ((Double) v) < 0);
            String text = negative ? "Negative operand\nNegative operand" : request.name() + new LinkedHashMap<>(request.arguments());
            return new CallToolResult(List.of(new TextContent(text)), negative, null, null);
        });
        return client;
    }
}
