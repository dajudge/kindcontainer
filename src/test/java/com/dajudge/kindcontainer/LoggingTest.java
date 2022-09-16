package com.dajudge.kindcontainer;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Testcontainers uses the 1.x API of slf4j and newer versions of logback are based on
 * slf4j 2.x, so updates to logback silently break logging for tests. This test ensures
 * that the logback version is compatible with the slf4j version used by testcontainers.
 */
public class LoggingTest {
    @Test
    public void logging_works() {
        final PrintStream out = System.out;
        try {
            final ByteArrayOutputStream temp = new ByteArrayOutputStream();
            System.setOut(new PrintStream(new BufferedOutputStream(temp)));
            LoggerFactory.getLogger(LoggingTest.class).info("Hello, world!");
            System.out.flush();
            assertTrue(new String(temp.toByteArray(), UTF_8).contains("Hello, world!"));
        } finally {
            System.setOut(out);
        }
    }
}
