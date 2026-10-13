package com.dajudge.kindcontainer.client.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestBuilderTest {

    @Test
    void timed_out_request_can_be_retried() throws Exception {
        final AtomicInteger connections = new AtomicInteger();
        final AtomicLong firstFailureMillis = new AtomicLong();
        final AtomicReference<String> lastResult = new AtomicReference<>("not attempted");
        final CountDownLatch firstRequestFailed = new CountDownLatch(1);
        final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();

        try (final ServerSocket server = new ServerSocket(0)) {
            final Future<?> serverTask = serverExecutor.submit(() -> {
                serveStalledThenSuccessfulRequest(server, connections, firstRequestFailed);
                return null;
            });

            final TinyHttpClient client = TinyHttpClient.newHttpClient().build();
            await("HTTP request is retried after the first response stalls")
                    .atMost(4, TimeUnit.SECONDS)
                    .pollInterval(25, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        final long started = System.nanoTime();
                        try (final Response response = client.request()
                                .url("http://localhost:" + server.getLocalPort())
                                .connectTimeout(Duration.ofMillis(200))
                                .readTimeout(Duration.ofMillis(200))
                                .execute()) {
                            lastResult.set("HTTP " + response.code());
                            return response.code() == 200;
                        } catch (final IOException e) {
                            firstFailureMillis.compareAndSet(
                                    0,
                                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                            );
                            lastResult.set(e.getClass().getSimpleName() + ": " + e.getMessage());
                            firstRequestFailed.countDown();
                            return false;
                        }
                    });

            assertEquals("HTTP 200", lastResult.get());
            assertEquals(2, connections.get());
            assertTrue(firstFailureMillis.get() < 1_000,
                    "the stalled request should fail quickly enough to permit a retry");
            serverTask.get(1, TimeUnit.SECONDS);
        } finally {
            firstRequestFailed.countDown();
            serverExecutor.shutdownNow();
        }
    }

    private static void serveStalledThenSuccessfulRequest(
            final ServerSocket server,
            final AtomicInteger connections,
            final CountDownLatch firstRequestFailed
    ) {
        try (final Socket ignored = server.accept()) {
            connections.incrementAndGet();
            firstRequestFailed.await(2, TimeUnit.SECONDS);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        try (final Socket socket = server.accept()) {
            connections.incrementAndGet();
            socket.getOutputStream().write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
