package com.example.scientific.server;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Chooses a log file only the current user can read, before Spring Boot starts logging.
 * Stdout is the MCP channel, so the server logs to a file instead.
 */
final class PrivateLogFile {

    static final String DIRECTORY = ".calculator-mcp";

    private PrivateLogFile() {
    }

    /**
     * Points {@code logging.file.name} at {@code ~/.calculator-mcp/<fileName>}, created owner-only so
     * other local users cannot read or pre-create it. If the home directory is not writable (e.g. a
     * container user with HOME=/), a fresh private temp directory is used instead, so startup never
     * fails over logging. An explicit {@code --logging.file.name=...} or -D property still wins.
     */
    static void configure(String fileName, String[] args) {
        configure(fileName, args, Path.of(System.getProperty("user.home")),
                () -> Files.createTempDirectory("calculator-mcp-"));
    }

    static void configure(String fileName, String[] args, Path home, Callable<Path> tempDirectory) {
        if (System.getProperty("logging.file.name") != null
                || Arrays.stream(args).anyMatch(a -> a.startsWith("--logging.file.name="))) {
            return;
        }
        boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        Path file;
        try {
            file = createPrivate(home.resolve(DIRECTORY), fileName, posix);
        } catch (IOException | RuntimeException e) {
            try {
                file = createPrivate(tempDirectory.call(), fileName, posix);
            } catch (Exception ignored) {
                return; // keep the application.yml default
            }
        }
        System.setProperty("logging.file.name", file.toString());
    }

    static Path createPrivate(Path dir, String fileName, boolean posix) throws IOException {
        Path file = dir.resolve(fileName);
        if (!posix) {
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
