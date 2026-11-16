package com.example.calculator.client;

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
class CalculatorClientTest {

    private final McpSyncClient mcp = mock(McpSyncClient.class);
    private final CalculatorClient calculator = new CalculatorClient(mcp);

    @Test
    void listsToolNames() {
        when(mcp.listTools()).thenReturn(new ListToolsResult(
                List.of(tool("add"), tool("parity")), null));
        assertEquals(List.of("add", "parity"), calculator.listOperations());
    }

    @Test
    void sendsNamedOperandsAndReturnsTheTextResult() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(new TextContent("12.0")), false, null, null));

        assertEquals("12.0", calculator.calculate("add", 7, 5));
        verify(mcp).callTool(new CallToolRequest("add", Map.of("a", 7.0, "b", 5.0)));
    }

    @Test
    void oneOperandToolsSendN() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(new TextContent("odd")), null, null, null));

        assertEquals("odd", calculator.calculate("parity", 17));
        verify(mcp).callTool(new CallToolRequest("parity", Map.of("n", 17.0)));
    }

    @Test
    void nonTextContentIsIgnoredAndTextPartsAreJoined() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(new TextContent("a"),
                new ImageContent(null, "AAAA", "image/png"), new TextContent("b")), false, null, null));
        assertEquals("a\nb", calculator.calculate("add", 1, 2));
    }

    @Test
    void toolErrorKeepsOnlyTheFirstLine() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(
                List.of(new TextContent("Division by zero\nDivision by zero\n")), true, null, null));
        assertEquals("Error: Division by zero", calculator.calculate("divide", 1, 0));
    }

    @Test
    void emptyToolErrorStillReadsAsAnError() {
        when(mcp.callTool(any())).thenReturn(new CallToolResult(List.of(), true, null, null));
        assertEquals("Error: ", calculator.calculate("divide", 1, 0));
    }

    @Test
    void failedRequestBecomesAnErrorString() {
        when(mcp.callTool(any())).thenThrow(new IllegalStateException("Unknown tool: cube_root\nmore detail"));
        assertEquals("Error: Unknown tool: cube_root", calculator.calculate("cube_root", 8));
    }

    @Test
    void failedRequestWithoutAMessageNamesTheException() {
        when(mcp.callTool(any())).thenThrow(new IllegalStateException());
        assertEquals("Error: IllegalStateException", calculator.calculate("add", 1, 2));
    }

    private static Tool tool(String name) {
        return Tool.builder().name(name).inputSchema(Map.of("type", "object")).build();
    }
}
