package com.example.calculator.router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot MCP client connected to both calculator servers. Spring AI's MCP client starter
 * launches every server configured under {@code spring.ai.mcp.client.stdio.connections} (see
 * application.yml) and provides one initialized {@code McpSyncClient} per server, which
 * {@link CalculatorRouterCli} hands to a {@link CalculatorRouter}.
 */
@SpringBootApplication
public class CalculatorRouterApplication {

    public static void main(String[] args) {
        // Exit once the runner is done so the stdio connections (and the server processes) are closed.
        System.exit(SpringApplication.exit(SpringApplication.run(CalculatorRouterApplication.class, args)));
    }
}
