package com.example.scientific.server;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;

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
        usePrivateLogFile("scientific-calculator-mcp-server.log", args);
        SpringApplication.run(ScientificCalculatorMcpServerApplication.class, args);
    }

    /**
     * Points {@code logging.file.name} at {@code ~/.calculator-mcp/<fileName>}, created owner-only so
     * other local users cannot read or pre-create it. If the home directory is not writable (e.g. a
     * container user with HOME=/), a fresh private temp directory is used instead, so startup never
     * fails over logging. An explicit {@code --logging.file.name=...} or -D property still wins.
     */
    static void usePrivateLogFile(String fileName, String[] args) {
        if (System.getProperty("logging.file.name") != null
                || Arrays.stream(args).anyMatch(a -> a.startsWith("--logging.file.name="))) {
            return;
        }
        Path file;
        try {
            file = createPrivate(Path.of(System.getProperty("user.home"), ".calculator-mcp"), fileName);
        } catch (IOException | RuntimeException e) {
            try {
                file = createPrivate(Files.createTempDirectory("calculator-mcp-"), fileName);
            } catch (IOException | RuntimeException ignored) {
                return; // keep the application.yml default
            }
        }
        System.setProperty("logging.file.name", file.toString());
    }

    private static Path createPrivate(Path dir, String fileName) throws IOException {
        Path file = dir.resolve(fileName);
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.createDirectories(dir);
            return Files.exists(file) ? file : Files.createFile(file);
        }
        if (!Files.isDirectory(dir)) {
            Files.createDirectories(dir, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        }
        // Also tightens a directory or log left world-readable by an older version.
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
        if (!Files.exists(file)) {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        if (!Files.isWritable(file)) {
            throw new IOException("not writable: " + file);
        }
        return file;
    }
}
