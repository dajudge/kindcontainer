package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.Pod;
import org.junit.Test;

import static com.dajudge.kindcontainer.TestUtils.createSimplePod;
import static com.dajudge.kindcontainer.TestUtils.isRunning;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest {
    @Test
    public void can_start_pod() {
        final Pod pod = StaticContainers.kind().runWithClient(client -> {
            return createSimplePod(client, TestUtils.createNewNamespace(client));
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> StaticContainers.kind().runWithClient(client -> {
                    return isRunning(client, pod);
                }));
    }
}
