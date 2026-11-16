package com.example.scientific.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/** Drives the command line front end against a mocked MCP client. */
class ScientificCalculatorCliTest {

    private final McpSyncClient mcp = mock(McpSyncClient.class);
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void captureOutput() {
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        when(mcp.listTools()).thenReturn(new ListToolsResult(List.of(tool("sqrt"),
                tool("ln"), tool("log10"), tool("log")), null));
        // Echo "ok:<tool>" back, or report an error for a negative or zero x.
        when(mcp.callTool(any())).thenAnswer(call -> {
            CallToolRequest request = call.getArgument(0);
            boolean error = (Double) request.arguments().get("x") <= 0;
            return new CallToolResult(List.of(new TextContent(error ? "bad input" : "ok:" + request.name())), error, null, null);
        });
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private ScientificCalculatorCli run(String... args) {
        ScientificCalculatorCli cli = new ScientificCalculatorCli(List.of(mcp));
        cli.run(new DefaultApplicationArguments(args));
        return cli;
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void oneOperandCallPrintsTheResult() {
        ScientificCalculatorCli cli = run("sqrt", "144");

        assertEquals("ok:sqrt" + System.lineSeparator(), stdout());
        assertEquals(0, cli.getExitCode());
        verify(mcp).callTool(new CallToolRequest("sqrt", Map.of("x", 144.0)));
    }

    @Test
    void logTakesXAndBase() {
        ScientificCalculatorCli cli = run("--scientific.server.jar=x.jar", "log", "1024", "2");

        assertEquals("ok:log" + System.lineSeparator(), stdout());
        assertEquals(0, cli.getExitCode());
        verify(mcp).callTool(new CallToolRequest("log", Map.of("x", 1024.0, "base", 2.0)));
    }

    @Test
    void serverErrorExitsWithOne() {
        ScientificCalculatorCli cli = run("ln", "0");

        assertEquals("Error: bad input" + System.lineSeparator(), stdout());
        assertEquals(1, cli.getExitCode());
    }

    @Test
    void badUsagePrintsHelpAndExitsWithOne() {
        for (String[] args : List.of(new String[] {"cube_root", "8"}, new String[] {"sqrt"}, new String[] {"sqrt", "1", "2"},
                new String[] {"log", "8"}, new String[] {"sqrt", "x"}, new String[] {"sqrt", "NaN"},
                new String[] {"log", "8", "Infinity"})) {
            err.reset();
            ScientificCalculatorCli cli = run(args);

            assertEquals(1, cli.getExitCode(), String.join(" ", args));
            assertTrue(stderr().startsWith("Usage: java -jar scientific-calculator-mcp-client.jar"), stderr());
            assertTrue(stderr().contains("Operations: sqrt, ln, log10, log"), stderr());
        }
        assertEquals("", stdout());
        verify(mcp, never()).callTool(any());
    }

    @Test
    void noArgumentsRunsTheDemo() {
        ScientificCalculatorCli cli = run();

        String demo = stdout();
        assertTrue(demo.startsWith("Tools offered by the server: sqrt, ln, log10, log"), demo);
        for (String op : List.of("sqrt", "ln", "log10", "log")) {
            assertTrue(demo.contains("ok:" + op), op);
        }
        assertTrue(demo.contains("sqrt  -4      = Error: bad input"), demo);
        assertTrue(demo.contains("ln    0       = Error: bad input"), demo);
        assertEquals(0, cli.getExitCode());
    }

    private static Tool tool(String name) {
        return Tool.builder().name(name).inputSchema(Map.of("type", "object")).build();
    }
}
