package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.CONTAINERS_WITH_KUBELET;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest {
    @ParameterizedTest
    @MethodSource(CONTAINERS_WITH_KUBELET)
    public void can_start_pod(final Supplier<KubernetesWithKubeletContainer<?>> factory) {
        runWithK8s(factory.get(), (Consumer<KubernetesWithKubeletContainer<?>>) this::assertPodCanStart);
    }

    private void assertPodCanStart(final KubernetesWithKubeletContainer<?> k8s) {
        final Pod pod = runWithClient(k8s, client -> {
            return createSimplePod(client, createNewNamespace(client));
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> runWithClient(k8s, client -> {
                    return isRunning(client, pod);
                }));
    }
}
