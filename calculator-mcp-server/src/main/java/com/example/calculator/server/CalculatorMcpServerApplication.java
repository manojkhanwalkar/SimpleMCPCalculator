package com.example.calculator.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot MCP server that speaks MCP over stdio. Spring AI's MCP server starter finds the
 * {@code @McpTool} methods in {@link CalculatorTools} and registers them as MCP tools.
 * Stdout carries the protocol, so the banner and console logging are turned off in application.yml
 * and logs go to a private file (see {@link PrivateLogFile}).
 */
@SpringBootApplication
public class CalculatorMcpServerApplication {

    public static void main(String[] args) {
        PrivateLogFile.configure("calculator-mcp-server.log", args);
        SpringApplication.run(CalculatorMcpServerApplication.class, args);
    }
}
