package com.example.calculator.client;

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
class CalculatorCliTest {

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
        when(mcp.listTools()).thenReturn(new ListToolsResult(
                List.of(tool("add"), tool("factorial")), null));
        // Echo "<tool>" back, or report an error for divide/factorial of a fraction.
        when(mcp.callTool(any())).thenAnswer(call -> {
            CallToolRequest request = call.getArgument(0);
            boolean error = request.name().equals("divide") && Double.valueOf(0).equals(request.arguments().get("b"))
                    || Double.valueOf(2.5).equals(request.arguments().get("n"));
            return new CallToolResult(List.of(new TextContent(error ? "bad input" : "ok:" + request.name())), error, null, null);
        });
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private CalculatorCli run(String... args) {
        CalculatorCli cli = new CalculatorCli(List.of(mcp));
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
    void twoOperandCallPrintsTheResult() {
        CalculatorCli cli = run("add", "7", "5");

        assertEquals("ok:add" + System.lineSeparator(), stdout());
        assertEquals(0, cli.getExitCode());
        verify(mcp).callTool(new CallToolRequest("add", Map.of("a", 7.0, "b", 5.0)));
    }

    @Test
    void oneOperandCallPrintsTheResult() {
        CalculatorCli cli = run("factorial", "5");

        assertEquals("ok:factorial" + System.lineSeparator(), stdout());
        assertEquals(0, cli.getExitCode());
        verify(mcp).callTool(new CallToolRequest("factorial", Map.of("n", 5.0)));
    }

    @Test
    void serverErrorExitsWithOne() {
        CalculatorCli cli = run("divide", "1", "0");

        assertEquals("Error: bad input" + System.lineSeparator(), stdout());
        assertEquals(1, cli.getExitCode());
    }

    @Test
    void springOptionsAreIgnored() {
        CalculatorCli cli = run("--calculator.server.jar=x.jar", "add", "1", "2");

        assertEquals("ok:add" + System.lineSeparator(), stdout());
        assertEquals(0, cli.getExitCode());
    }

    @Test
    void badUsagePrintsHelpAndExitsWithOne() {
        for (String[] args : List.of(new String[] {"add", "seven", "5"}, new String[] {"add", "7", "five"}, new String[] {"add"},
                new String[] {"add", "1", "2", "3"}, new String[] {"factorial", "x"}, new String[] {"add", "NaN", "1"},
                new String[] {"add", "1", "Infinity"}, new String[] {"factorial", "1e400"})) {
            err.reset();
            CalculatorCli cli = run(args);

            assertEquals(1, cli.getExitCode(), String.join(" ", args));
            assertTrue(stderr().startsWith("Usage: java -jar calculator-mcp-client.jar"), stderr());
            assertTrue(stderr().contains("Operations: add, factorial"), stderr());
        }
        assertEquals("", stdout());
        verify(mcp, never()).callTool(any());
    }

    @Test
    void noArgumentsRunsTheDemo() {
        CalculatorCli cli = run();

        String demo = stdout();
        assertTrue(demo.startsWith("Tools offered by the server: add, factorial"), demo);
        for (String op : List.of("add", "subtract", "multiply", "divide", "remainder", "exponent", "factorial", "isPrime", "parity")) {
            assertTrue(demo.contains(op + " ") && demo.contains("ok:" + op), op);
        }
        assertTrue(demo.contains("divide    1, 0  = Error: bad input"), demo);
        assertTrue(demo.contains("factorial 2.5   = Error: bad input"), demo);
        assertEquals(0, cli.getExitCode());
    }

    private static Tool tool(String name) {
        return Tool.builder().name(name).inputSchema(Map.of("type", "object")).build();
    }
}
