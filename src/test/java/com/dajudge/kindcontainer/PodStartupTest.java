package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.Pod;
import org.junit.Test;

import java.util.Objects;

import static com.dajudge.kindcontainer.StaticContainers.apiServer;
import static com.dajudge.kindcontainer.StaticContainers.kind;
import static com.dajudge.kindcontainer.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest {
    @Test
    public void can_start_pod_in_kind() {
        final String namespace = kind().createNamespace();
        final Pod pod = runWithClient(kind(), client -> {
            return createSimplePod(client, namespace);
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> runWithClient(kind(), client -> {
                    return isRunning(client, pod);
                }));
    }

    @Test
    public void can_create_pod_in_apiserver() {
        final String namespace = apiServer().createNamespace();
        final Pod pod = runWithClient(apiServer(), client -> {
            return createSimplePod(client, namespace);
        });
        await("testpod")
                .until(() -> runWithClient(apiServer(), client -> {
                    return client.pods()
                            .inNamespace(namespace)
                            .withName(pod.getMetadata().getName())
                            .get();
                }), Objects::nonNull);
    }
}
