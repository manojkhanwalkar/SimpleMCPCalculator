package com.example.calculator.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Launches both real server jars over stdio. Runs as part of {@code ./mvnw verify} from the
 * repository root, where both servers are packaged first; skipped if either jar hasn't been built.
 */
class CalculatorRouterEndToEndTest {

    private static final Path CALCULATOR_JAR = Path.of("../calculator-mcp-server/target/calculator-mcp-server.jar");
    private static final Path SCIENTIFIC_JAR = Path.of(
            "../ScientificCalculatorMCP/scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar");

    private static McpSyncClient calculatorServer;
    private static McpSyncClient scientificServer;
    private static CalculatorRouter router;

    @BeforeAll
    static void startServers() {
        assumeTrue(Files.exists(CALCULATOR_JAR), "server jar not built: " + CALCULATOR_JAR.toAbsolutePath());
        assumeTrue(Files.exists(SCIENTIFIC_JAR), "server jar not built: " + SCIENTIFIC_JAR.toAbsolutePath());
        calculatorServer = start(CALCULATOR_JAR);
        scientificServer = start(SCIENTIFIC_JAR);
        router = new CalculatorRouter(List.of(calculatorServer, scientificServer));
    }

    private static McpSyncClient start(Path jar) {
        ServerParameters params = ServerParameters.builder("java").args("-jar", jar.toString(),
                // Keep test logs in target/ rather than the developer's home directory.
                "--logging.file.name=target/" + jar.getFileName() + ".log").build();
        McpSyncClient client = McpClient.sync(new StdioClientTransport(params, McpJsonDefaults.getMapper())).build();
        client.initialize();
        return client;
    }

    @AfterAll
    static void stopServers() {
        for (McpSyncClient client : new McpSyncClient[] {calculatorServer, scientificServer}) {
            if (client != null) {
                client.closeGracefully();
            }
        }
    }

    @Test
    void offersEveryOperationOfBothServers() {
        assertEquals(Set.of("add", "subtract", "multiply", "divide", "remainder", "exponent", "factorial", "isPrime", "parity",
                        "sqrt", "ln", "log10", "log"),
                router.routes().keySet());
    }

    @Test
    void routesEachOperationToTheServerThatOffersIt() {
        for (String op : List.of("add", "subtract", "multiply", "divide", "remainder", "exponent", "factorial", "isPrime", "parity")) {
            assertEquals("calculator-mcp-server", router.routes().get(op).server(), op);
        }
        for (String op : List.of("sqrt", "ln", "log10", "log")) {
            assertEquals("scientific-calculator-mcp-server", router.routes().get(op).server(), op);
        }
    }

    @Test
    void learnsParameterNamesInOrderFromTheServers() {
        assertEquals(List.of("a", "b"), router.routes().get("subtract").parameters());
        assertEquals(List.of("n"), router.routes().get("factorial").parameters());
        assertEquals(List.of("x", "base"), router.routes().get("log").parameters());
    }

    @Test
    void callsOperationsOnBothServers() {
        assertEquals("12.0", router.calculate("subtract", List.of(17.0, 5.0)));
        assertEquals("120", router.calculate("factorial", List.of(5.0)));
        assertEquals("odd", router.calculate("parity", List.of(17.0)));
        assertEquals("12.0", router.calculate("sqrt", List.of(144.0)));
        assertEquals("10.0", router.calculate("log", List.of(1024.0, 2.0)));
    }

    @Test
    void serverErrorsComeBackAsErrors() {
        assertEquals("Error: Division by zero", router.calculate("divide", List.of(1.0, 0.0)));
        assertEquals("Error: Square root of a negative number", router.calculate("sqrt", List.of(-4.0)));
    }

    @Test
    void badCallsAreRejectedBeforeReachingAServer() {
        assertEquals("Error: Unknown operation: cube_root", router.calculate("cube_root", List.of(8.0)));
        assertEquals("Error: log takes 2 operand(s): x, base", router.calculate("log", List.of(8.0)));
        assertEquals("Error: sqrt takes 1 operand(s): x", router.calculate("sqrt", List.of(4.0, 2.0)));
    }

    @Test
    void sameToolOnTwoServersIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new CalculatorRouter(List.of(scientificServer, scientificServer)));
        assertTrue(e.getMessage().contains("offered by both"), e.getMessage());
    }
}
