package com.example.calculator.router;

import static com.example.calculator.router.FakeServers.server;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/** Drives the command line front end against mocked servers. */
class CalculatorRouterCliTest {

    private final McpSyncClient basic = server("calculator-mcp-server", Map.of(
            "add", List.of("a", "b"), "divide", List.of("a", "b"), "factorial", List.of("n"), "parity", List.of("n")));
    private final McpSyncClient scientific = server("scientific-calculator-mcp-server", Map.of(
            "sqrt", List.of("x"), "log", List.of("x", "base")));
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
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private CalculatorRouterCli run(String... args) {
        CalculatorRouterCli cli = new CalculatorRouterCli(List.of(basic, scientific));
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
    void routedCallPrintsTheResult() {
        CalculatorRouterCli cli = run("--scientific.server.jar=x.jar", "sqrt", "144");

        assertEquals("sqrt{x=144.0}" + System.lineSeparator(), stdout());
        assertEquals(0, cli.getExitCode());
    }

    @Test
    void errorResultExitsWithOne() {
        CalculatorRouterCli cli = run("cube_root", "8");

        assertEquals("Error: Unknown operation: cube_root" + System.lineSeparator(), stdout());
        assertEquals(1, cli.getExitCode());
    }

    @Test
    void nonNumericOrNonFiniteOperandsPrintUsage() {
        for (String[] args : List.of(new String[] {"add", "one", "2"}, new String[] {"sqrt", "NaN"},
                new String[] {"sqrt", "-Infinity"})) {
            err.reset();
            CalculatorRouterCli cli = run(args);

            assertEquals(1, cli.getExitCode(), String.join(" ", args));
            String usage = stderr();
            assertTrue(usage.startsWith("Usage: java -jar calculator-router-client.jar [<operation> <operand>...]"), usage);
            assertTrue(usage.contains("  log        x, base      -> scientific-calculator-mcp-server"), usage);
            assertTrue(usage.contains("  add        a, b         -> calculator-mcp-server"), usage);
        }
        assertEquals("", stdout());
        verify(basic, never()).callTool(any());
        verify(scientific, never()).callTool(any());
    }

    @Test
    void noArgumentsListsOperationsAndRunsTheDemo() {
        CalculatorRouterCli cli = run();

        String demo = stdout();
        assertTrue(demo.startsWith("Operations:" + System.lineSeparator()), demo);
        assertTrue(demo.contains("  factorial  n            -> calculator-mcp-server"), demo);
        assertTrue(demo.contains("add        [17.0, 5.0]   = add{"), demo);
        assertTrue(demo.contains("log        [1024.0, 2.0] = log{"), demo);
        assertTrue(demo.contains("cube_root  [8.0]         = Error: Unknown operation: cube_root"), demo);
        assertEquals(0, cli.getExitCode());
    }
}
