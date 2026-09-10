package com.dajudge.kindcontainer;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Logging is a user-visible diagnostic contract of this library. Dependency updates must not
 * silently replace the SLF4J provider, drop log events, duplicate them, or lose throwable output.
 */
public class LoggingTest {
    @Test
    public void logging_works() {
        assertEquals(
                LoggerContext.class,
                LoggerFactory.getILoggerFactory().getClass(),
                "SLF4J must be bound to Logback"
        );

        final String infoMessage = "logging-contract-info-7f27a4d0";
        final String errorMessage = "logging-contract-error-cd132e43";
        final String exceptionMessage = "logging-contract-exception-8410b8d2";

        final List<ILoggingEvent> events = captureLogs(() -> {
            final Logger logger = LoggerFactory.getLogger(LoggingTest.class);
            logger.info(infoMessage);
            logger.error(errorMessage, new IllegalStateException(exceptionMessage));
        });

        assertEquals(1, occurrences(events, infoMessage), "INFO event must be emitted exactly once");
        assertEquals(1, occurrences(events, errorMessage), "ERROR event must be emitted exactly once");
        assertEquals(1, occurrences(events, exceptionMessage), "Throwable message must be emitted exactly once");
    }

    @Test
    public void testcontainers_logging_works() {
        final List<ILoggingEvent> events = captureLogs(() -> {
            try (GenericContainer<?> container = new GenericContainer<>("alpine:3.20")
                    .withCommand("sh", "-c", "echo testcontainers-logging-smoke")) {
                container.start();
            }
        });

        assertTrue(events.stream().anyMatch(event -> event.getFormattedMessage().contains("alpine:3.20")),
                "Testcontainers lifecycle logs must identify the container image");
    }

    private static List<ILoggingEvent> captureLogs(final Runnable operation) {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            operation.run();
            return List.copyOf(appender.list);
        } finally {
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }

    private static long occurrences(final List<ILoggingEvent> events, final String message) {
        return events.stream()
                .filter(event -> event.getFormattedMessage().contains(message)
                        || (event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().contains(message)))
                .count();
    }
}
