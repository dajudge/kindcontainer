package com.dajudge.kindcontainer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Logging is a user-visible diagnostic contract of this library. Dependency updates must not
 * silently replace the SLF4J provider, drop log events, duplicate them, or lose throwable output.
 */
public class LoggingTest {
    @Test
    public void logging_works() {
        final PrintStream out = System.out;
        try {
            final ByteArrayOutputStream temp = new ByteArrayOutputStream();
            System.setOut(new PrintStream(new BufferedOutputStream(temp)));

            assertEquals(
                    "ch.qos.logback.classic.LoggerContext",
                    LoggerFactory.getILoggerFactory().getClass().getName(),
                    "SLF4J must be bound to Logback"
            );

            final Logger logger = LoggerFactory.getLogger(LoggingTest.class);
            final String infoMessage = "logging-contract-info-7f27a4d0";
            final String errorMessage = "logging-contract-error-cd132e43";
            final String exceptionMessage = "logging-contract-exception-8410b8d2";

            logger.info(infoMessage);
            logger.error(errorMessage, new IllegalStateException(exceptionMessage));
            System.out.flush();

            final String output = new String(temp.toByteArray(), UTF_8);

            assertEquals(1, occurrences(output, infoMessage), "INFO event must be emitted exactly once");
            assertEquals(1, occurrences(output, errorMessage), "ERROR event must be emitted exactly once");
            assertEquals(1, occurrences(output, exceptionMessage), "Throwable message must be emitted exactly once");

            assertTrue(output.contains("INFO  "), "INFO level must be present");
            assertTrue(output.contains("LoggingTest - " + infoMessage),
                    "INFO formatting must preserve logger identity and message");
            assertTrue(output.contains("ERROR "), "ERROR level must be present");
            assertTrue(output.contains("LoggingTest - " + errorMessage),
                    "ERROR formatting must preserve logger identity and message");
            assertTrue(output.contains("java.lang.IllegalStateException: " + exceptionMessage),
                    "Throwable type and message must be present");
            assertTrue(output.contains("at com.dajudge.kindcontainer.LoggingTest.logging_works"),
                    "Throwable stack trace must be present");
        } finally {
            System.setOut(out);
        }
    }

    private static int occurrences(final String haystack, final String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = haystack.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
