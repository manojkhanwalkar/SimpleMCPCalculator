package com.example.calculator.router;

import static com.example.calculator.router.FakeServers.server;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.Test;

/** Unit tests against mocked servers; the end-to-end test covers the real ones. */
class CalculatorRouterTest {

    private final McpSyncClient basic = server("basic", Map.of("add", List.of("a", "b"), "parity", List.of("n")));
    private final McpSyncClient scientific = server("scientific", Map.of("log", List.of("x", "base")));
    private final CalculatorRouter router = new CalculatorRouter(List.of(basic, scientific));

    @Test
    void routesAreSortedByNameAndRememberServerAndParameters() {
        assertEquals(List.of("add", "log", "parity"), List.copyOf(router.routes().keySet()));
        assertEquals(new CalculatorRouter.Route(scientific, "scientific", List.of("x", "base")), router.routes().get("log"));
    }

    @Test
    void operandsAreMatchedToParametersInOrder() {
        assertTrue(router.calculate("log", List.of(8.0, 2.0)).startsWith("log{"));
        verify(scientific).callTool(new CallToolRequest("log", Map.of("x", 8.0, "base", 2.0)));
        verify(basic, never()).callTool(any());
    }

    @Test
    void toolErrorKeepsOnlyTheFirstLine() {
        assertEquals("Error: Negative operand", router.calculate("parity", List.of(-1.0)));
    }

    @Test
    void unknownOperationAndWrongOperandCountNeverReachAServer() {
        assertEquals("Error: Unknown operation: cube_root", router.calculate("cube_root", List.of(8.0)));
        assertEquals("Error: add takes 2 operand(s): a, b", router.calculate("add", List.of(1.0)));
        verify(basic, never()).callTool(any());
        verify(scientific, never()).callTool(any());
    }

    @Test
    void failedRequestBecomesAnErrorString() {
        doThrow(new IllegalStateException("Connection closed\ndetail")).when(basic).callTool(any());
        assertEquals("Error: Connection closed", router.calculate("add", List.of(1.0, 2.0)));
    }

    @Test
    void failedRequestWithoutAMessageNamesTheException() {
        doThrow(new IllegalStateException()).when(basic).callTool(any());
        assertEquals("Error: IllegalStateException", router.calculate("add", List.of(1.0, 2.0)));
    }

    @Test
    void toolWithoutRequiredParametersTakesNoOperands() {
        McpSyncClient odd = mock(McpSyncClient.class);
        when(odd.getServerInfo()).thenReturn(new Implementation("odd", "1"));
        when(odd.listTools()).thenReturn(new ListToolsResult(List.of(
                Tool.builder().name("noSchema").build(),
                Tool.builder().name("noRequired").inputSchema(Map.of("type", "object")).build()), null));

        CalculatorRouter oddRouter = new CalculatorRouter(List.of(odd));

        assertEquals(List.of(), oddRouter.routes().get("noSchema").parameters());
        assertEquals(List.of(), oddRouter.routes().get("noRequired").parameters());
    }

    @Test
    void sameToolOnTwoServersIsRejected() {
        McpSyncClient twin = server("twin", Map.of("add", List.of("a", "b")));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new CalculatorRouter(List.of(basic, twin)));
        assertTrue(e.getMessage().contains("'add' is offered by both basic and twin"), e.getMessage());
    }
}
