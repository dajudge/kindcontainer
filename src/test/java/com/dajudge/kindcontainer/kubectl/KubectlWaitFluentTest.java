package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.ApiServerContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentConditionBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class KubectlWaitFluentTest {
    @Container
    public final ApiServerContainer<?> k8s = new ApiServerContainer<>();

    private final String namespace = UUID.randomUUID().toString();
    private NamespacedKubernetesClient client;

    @BeforeEach
    public void before() {
        client = new KubernetesClientBuilder().withConfig(fromKubeconfig(k8s.getKubeconfig())).build().inNamespace(namespace);
        client.namespaces().create(new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build());
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void waits_for_condition() throws InterruptedException {
        // Given
        final String name = createMinimalDeployment().getMetadata().getName();
        final WaitForDeploymentThread waitThread = new WaitForDeploymentThread(name);
        waitThread.start();
        waitThread.blockHereUntilWaiting();

        // When
        markDeploymentAsTested(name);

        // Then
        waitThread.blockHereUntilFinished();
        waitThread.assertWaitedSuccessfully();
    }

    @Test
    public void aborts_after_timeout() throws InterruptedException {
        // Given
        final String name = createMinimalDeployment().getMetadata().getName();
        final WaitForDeploymentThread waitThread = new WaitForDeploymentThread(name, "1s");
        final long start = System.currentTimeMillis();
        waitThread.start();
        waitThread.blockHereUntilWaiting();

        // Then
        waitThread.blockHereUntilFinished();
        waitThread.assertWaitTimedOut();
        final long end = System.currentTimeMillis();
        assertTrue((end - start) > 1000);
        assertTrue((end - start) < 5000);
    }

    private void markDeploymentAsTested(final String name) {
        client.apps().deployments().withName(name).editStatus(d -> {
            d.getStatus().setConditions(singletonList(new DeploymentConditionBuilder()
                    .withType("Tested")
                    .withStatus("True")
                    .build()));
            return d;
        });
    }

    private Deployment createMinimalDeployment() {
        final HashMap<String, String> matchLabels = new HashMap<String, String>() {{
            put("a", "b");
        }};
        return client.apps().deployments().create(new DeploymentBuilder()
                .withNewMetadata()
                .withName(UUID.randomUUID().toString())
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .withMatchLabels(matchLabels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(matchLabels)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("lolcats")
                .withImage("lolcats")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build());
    }

    private class WaitForDeploymentThread extends Thread {
        private final String name;
        private final String timeout;
        private final CountDownLatch waiting;
        private final AtomicReference<Exception> waitFailed;

        public WaitForDeploymentThread(final String name) {
            this(name, null);
        }

        public WaitForDeploymentThread(final String name, final String timeout) {
            this.name = name;
            this.timeout = timeout;
            waiting = new CountDownLatch(1);
            waitFailed = new AtomicReference<>();
        }

        @Override
        public void run() {
            waiting.countDown();
            try {
                k8s.kubectl().wait
                        .namespace(namespace)
                        .timeout(timeout)
                        .forCondition("Tested")
                        .run("deployment", name);
            } catch (final IOException | ExecutionException | InterruptedException e) {
                waitFailed.set(e);
            }
        }

        public void blockHereUntilWaiting() throws InterruptedException {
            assertTrue(waiting.await(5, SECONDS));
        }

        public void blockHereUntilFinished() throws InterruptedException {
            join(60000);
        }

        public void assertWaitedSuccessfully() {
            if (waitFailed.get() != null) {
                throw new AssertionError("Wait failed", waitFailed.get());
            }
        }

        public void assertWaitTimedOut() {
            assertNotNull(waitFailed.get());
            assertTrue(waitFailed.get() instanceof ExecutionException);
        }
    }
}
