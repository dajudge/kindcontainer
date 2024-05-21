package com.dajudge.kindcontainer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApiServerTest {
    @Test
    public void configurableTimeout() {
        final AtomicBoolean containerCompleted = new AtomicBoolean();
        final AtomicReference<Exception> containerFailed = new AtomicReference<>();
        final ApiServerContainer<?> apiServer = new ApiServerContainer<>(latest(ApiServerContainerVersion.class).withImage("nginx"))
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint().withCmd())
                .withCommand("nginx")
                .withControlPlaneReadyTimeout(ofSeconds(1));
        final Thread containerThread = new Thread(() -> {
            try {
                apiServer.start();
                apiServer.stop();
            } catch (final Exception e) {
                containerFailed.set(e);
            } finally {
                containerCompleted.set(true);
            }
        });
        try {
            containerThread.start();

            await().timeout(5, MINUTES).until(containerCompleted::get);
            assertNotNull(containerFailed.get());
        } finally {
            apiServer.stop();
        }
    }

}
