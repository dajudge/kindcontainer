package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.Test;

import static com.dajudge.kindcontainer.util.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class PodStartupTest extends BaseFullContainersTest {
    public PodStartupTest(final KubernetesWithKubeletContainer<?> k8s) {
        super(k8s);
    }

    @Test
    public void can_start_pod() {
        final Pod pod = runWithClient(k8s, client -> {
            return createSimplePod(client, TestUtils.createNewNamespace(client));
        });
        await("testpod")
                .timeout(ofSeconds(300))
                .until(() -> runWithClient(k8s, client -> {
                    return isRunning(client, pod);
                }));
    }
}
