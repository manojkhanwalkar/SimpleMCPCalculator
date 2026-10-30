package com.example.calculator.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Launches the real server jar over stdio. Runs as part of {@code ./mvnw verify} from the project
 * root, where the server module is packaged first; skipped if the jar hasn't been built.
 */
class CalculatorClientEndToEndTest {

    private static final Path SERVER_JAR = Path.of("../calculator-mcp-server/target/calculator-mcp-server.jar");

    private static McpSyncClient mcp;
    private static CalculatorClient calculator;

    @BeforeAll
    static void startServer() {
        assumeTrue(Files.exists(SERVER_JAR), "server jar not built: " + SERVER_JAR.toAbsolutePath());
        ServerParameters params = ServerParameters.builder("java").args("-jar", SERVER_JAR.toString(),
                // Keep test logs in target/ rather than the developer's home directory.
                "--logging.file.name=target/" + SERVER_JAR.getFileName() + ".log").build();
        mcp = McpClient.sync(new StdioClientTransport(params, McpJsonDefaults.getMapper())).build();
        mcp.initialize();
        calculator = new CalculatorClient(mcp);
    }

    @AfterAll
    static void stopServer() {
        if (mcp != null) {
            mcp.closeGracefully();
        }
    }

    @Test
    void serverAdvertisesAllOperations() {
        assertEquals(java.util.Set.of("add", "subtract", "multiply", "divide", "remainder",
                        "exponent", "factorial", "isPrime", "parity"),
                java.util.Set.copyOf(calculator.listOperations()));
    }

    @Test
    void callsEachOperation() {
        assertEquals("22.0", calculator.calculate("add", 17, 5));
        assertEquals("12.0", calculator.calculate("subtract", 17, 5));
        assertEquals("85.0", calculator.calculate("multiply", 17, 5));
        assertEquals("3.4", calculator.calculate("divide", 17, 5));
        assertEquals("2.0", calculator.calculate("remainder", 17, 5));
        assertEquals("1024.0", calculator.calculate("exponent", 2, 10));
        assertEquals("120", calculator.calculate("factorial", 5));
        assertEquals("true", calculator.calculate("isPrime", 17));
        assertEquals("false", calculator.calculate("isPrime", 15));
        assertEquals("odd", calculator.calculate("parity", 17));
        assertEquals("even", calculator.calculate("parity", 42));
    }

    @Test
    void divisionByZeroComesBackAsToolError() {
        assertEquals(true, calculator.calculate("divide", 1, 0).startsWith("Error:"));
    }

    @Test
    void overflowComesBackAsToolError() {
        assertEquals("Error: Result is too large", calculator.calculate("exponent", 10, 400));
    }

    @Test
    void fractionalInputToWholeNumberToolComesBackAsToolError() {
        assertEquals(true, calculator.calculate("factorial", 2.5).startsWith("Error:"));
        assertEquals(true, calculator.calculate("parity", 1.5).startsWith("Error:"));
    }

    @Test
    void unknownOperationComesBackAsErrorInsteadOfException() {
        assertEquals(true, calculator.calculate("cube_root", 8).startsWith("Error:"));
    }
}
