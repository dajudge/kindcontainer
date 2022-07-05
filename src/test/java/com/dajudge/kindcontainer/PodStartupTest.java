package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kubeletContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest {
    @TestFactory
    public Stream<DynamicTest> can_start_pod() {
        return kubeletContainers(k8s -> runWithK8s(k8s, this::assertPodCanStart));
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
