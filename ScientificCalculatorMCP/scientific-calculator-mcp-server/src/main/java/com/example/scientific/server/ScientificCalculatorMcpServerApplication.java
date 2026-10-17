package com.example.scientific.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot MCP server that speaks MCP over stdio. Spring AI's MCP server starter finds the
 * {@code @McpTool} methods in {@link ScientificTools} and registers them as MCP tools.
 * Stdout carries the protocol, so the banner and console logging are turned off in application.yml.
 */
@SpringBootApplication
public class ScientificCalculatorMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScientificCalculatorMcpServerApplication.class, args);
    }
}
