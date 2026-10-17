package com.example.scientific.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Launches the real server jar over stdio. Runs as part of {@code ./mvnw verify} from the project
 * root, where the server module is packaged first; skipped if the jar hasn't been built.
 */
class ScientificCalculatorClientEndToEndTest {

    private static final Path SERVER_JAR = Path.of("../scientific-calculator-mcp-server/target/scientific-calculator-mcp-server.jar");

    private static McpSyncClient mcp;
    private static ScientificCalculatorClient calculator;

    @BeforeAll
    static void startServer() {
        assumeTrue(Files.exists(SERVER_JAR), "server jar not built: " + SERVER_JAR.toAbsolutePath());
        ServerParameters params = ServerParameters.builder("java").args("-jar", SERVER_JAR.toString()).build();
        mcp = McpClient.sync(new StdioClientTransport(params, McpJsonDefaults.getMapper())).build();
        mcp.initialize();
        calculator = new ScientificCalculatorClient(mcp);
    }

    @AfterAll
    static void stopServer() {
        if (mcp != null) {
            mcp.closeGracefully();
        }
    }

    @Test
    void serverAdvertisesAllOperations() {
        assertEquals(Set.of("sqrt", "ln", "log10", "log"), Set.copyOf(calculator.listOperations()));
    }

    @Test
    void toolsAreAnnotatedReadOnly() {
        mcp.listTools().tools().forEach(tool -> {
            assertEquals(true, tool.annotations().readOnlyHint(), tool.name());
            assertEquals(false, tool.annotations().destructiveHint(), tool.name());
        });
    }

    @Test
    void callsEachOperation() {
        assertEquals("12.0", calculator.calculate("sqrt", 144));
        assertEquals("0.0", calculator.calculate("ln", 1));
        assertEquals("3.0", calculator.calculate("log10", 1000));
        assertEquals("10.0", calculator.calculate("log", 1024, 2));
        assertEquals("3.0", calculator.calculate("log", 1000, 10));
    }

    @Test
    void invalidInputComesBackAsToolError() {
        assertEquals("Error: Square root of a negative number", calculator.calculate("sqrt", -4));
        assertEquals("Error: Logarithm is only defined for x > 0", calculator.calculate("ln", 0));
        assertEquals("Error: Logarithm base must be > 0 and not 1", calculator.calculate("log", 8, 1));
    }
}
