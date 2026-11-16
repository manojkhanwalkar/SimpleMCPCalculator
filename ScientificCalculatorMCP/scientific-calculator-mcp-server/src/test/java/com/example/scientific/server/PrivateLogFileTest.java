package com.example.scientific.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrivateLogFileTest {

    private static final String PROPERTY = "logging.file.name";
    private static final String[] NO_ARGS = {};

    @TempDir
    Path home;

    @TempDir
    Path fallback;

    private String saved;

    @BeforeEach
    void clearProperty() {
        saved = System.getProperty(PROPERTY);
        System.clearProperty(PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (saved == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, saved);
        }
    }

    private Callable<Path> fallbackDir() {
        return () -> fallback;
    }

    @Test
    void createsOwnerOnlyLogUnderHome() throws IOException {
        PrivateLogFile.configure("server.log", NO_ARGS, home, fallbackDir());

        Path dir = home.resolve(".calculator-mcp");
        Path file = dir.resolve("server.log");
        assertEquals(file.toString(), System.getProperty(PROPERTY));
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    }

    @Test
    void tightensAWorldReadableDirectoryAndLog() throws IOException {
        Path dir = Files.createDirectory(home.resolve(".calculator-mcp"));
        Path file = Files.createFile(dir.resolve("server.log"));
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxrwxrwx"));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-rw-rw-"));

        PrivateLogFile.configure("server.log", NO_ARGS, home, fallbackDir());

        assertEquals(file.toString(), System.getProperty(PROPERTY));
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    }

    @Test
    void fallsBackToAPrivateTempDirectoryWhenHomeIsUnusable() throws IOException {
        Path notADirectory = Files.createFile(home.resolve("plain-file"));

        PrivateLogFile.configure("server.log", NO_ARGS, notADirectory, fallbackDir());

        assertEquals(fallback.resolve("server.log").toString(), System.getProperty(PROPERTY));
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(fallback.resolve("server.log"))));
    }

    @Test
    void keepsTheDefaultWhenNoDirectoryIsUsable() throws IOException {
        Path notADirectory = Files.createFile(home.resolve("plain-file"));

        PrivateLogFile.configure("server.log", NO_ARGS, notADirectory, () -> {
            throw new IOException("no temp dir");
        });

        assertNull(System.getProperty(PROPERTY));
    }

    @Test
    void defaultsToUserHomeAndTheSystemTempDirectory() throws IOException {
        String userHome = System.getProperty("user.home");
        System.setProperty("user.home", Files.createFile(home.resolve("plain-file")).toString());
        try {
            PrivateLogFile.configure("server.log", NO_ARGS);
        } finally {
            System.setProperty("user.home", userHome);
        }

        Path file = Path.of(System.getProperty(PROPERTY));
        try {
            assertEquals(Path.of(System.getProperty("java.io.tmpdir")).toRealPath(), file.getParent().getParent().toRealPath());
            assertTrue(file.getParent().getFileName().toString().startsWith("calculator-mcp-"), file.toString());
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(file.getParent());
        }
    }

    @Test
    void explicitSystemPropertyWins() {
        System.setProperty(PROPERTY, "/chosen/by/user.log");

        PrivateLogFile.configure("server.log", NO_ARGS, home, fallbackDir());

        assertEquals("/chosen/by/user.log", System.getProperty(PROPERTY));
        assertTrue(Files.notExists(home.resolve(".calculator-mcp")));
    }

    @Test
    void explicitCommandLineOptionWins() {
        PrivateLogFile.configure("server.log", new String[] {"--logging.file.name=/chosen.log"}, home, fallbackDir());

        assertNull(System.getProperty(PROPERTY));
        assertTrue(Files.notExists(home.resolve(".calculator-mcp")));
    }

    @Test
    void withoutPosixPermissionsTheDirectoryAndFileAreStillCreated() throws IOException {
        Path dir = home.resolve("plain");

        Path file = PrivateLogFile.createPrivate(dir, "server.log", false);

        assertEquals(dir.resolve("server.log"), file);
        assertTrue(Files.isRegularFile(file));
        assertEquals(file, PrivateLogFile.createPrivate(dir, "server.log", false), "existing file is reused");
    }
}
