package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.Pod;
import org.junit.Test;

import static com.dajudge.kindcontainer.TestUtils.createSimplePod;
import static com.dajudge.kindcontainer.TestUtils.isRunning;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest extends BaseKindContainerTest {
    @Test
    public void can_start_pod() {
        final Pod pod = K8S.withClient(client -> {
            return createSimplePod(client, namespace);
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> K8S.withClient(client -> {
                    return isRunning(client, pod);
                }));
    }
}
