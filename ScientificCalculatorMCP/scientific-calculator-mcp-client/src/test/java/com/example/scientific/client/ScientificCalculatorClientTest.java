package com.example.scientific.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.Test;

/** Unit tests against a mocked MCP client; the end-to-end test covers the real server. */
class ScientificCalculatorClientTest {

    private final McpSyncClient mcp = mock(McpSyncClient.class);
    private final ScientificCalculatorClient calculator = new ScientificCalculatorClient(mcp);

    @Test
    void listsToolNames() {
        when(mcp.listTools()).thenReturn(new ListToolsResult(
                List.of(tool("sqrt"), tool("log")), null));
        assertEquals(List.of("sqrt", "log"), calculator.listOperations());
    }

    @Test
    void oneOperandToolsSendX() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(new TextContent("12.0")), false, null, null));

        assertEquals("12.0", calculator.calculate("sqrt", 144));
        verify(mcp).callTool(new CallToolRequest("sqrt", Map.of("x", 144.0)));
    }

    @Test
    void logSendsXAndBase() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(new TextContent("10.0")), null, null, null));

        assertEquals("10.0", calculator.calculate("log", 1024, 2));
        verify(mcp).callTool(new CallToolRequest("log", Map.of("x", 1024.0, "base", 2.0)));
    }

    @Test
    void nonTextContentIsIgnoredAndTextPartsAreJoined() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(new TextContent("a"),
                new ImageContent(null, "AAAA", "image/png"), new TextContent("b")), false, null, null));
        assertEquals("a\nb", calculator.calculate("sqrt", 1));
    }

    @Test
    void toolErrorKeepsOnlyTheFirstLine() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(
                List.of(new TextContent("Square root of a negative number\nagain")), true, null, null));
        assertEquals("Error: Square root of a negative number", calculator.calculate("sqrt", -4));
    }

    @Test
    void failedRequestBecomesAnErrorString() {
        when(mcp.callTool(any())).thenThrow(new IllegalStateException("Unknown tool: cube_root\nmore detail"));
        assertEquals("Error: Unknown tool: cube_root", calculator.calculate("cube_root", 8));
    }

    @Test
    void failedRequestWithoutAMessageNamesTheException() {
        when(mcp.callTool(any())).thenThrow(new IllegalStateException());
        assertEquals("Error: IllegalStateException", calculator.calculate("sqrt", 1));
    }

    private static Tool tool(String name) {
        return Tool.builder().name(name).inputSchema(Map.of("type", "object")).build();
    }
}
