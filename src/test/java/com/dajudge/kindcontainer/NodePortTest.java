package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.TestUtils;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.CONTAINERS_WITH_KUBELET;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class NodePortTest {

    @ParameterizedTest
    @MethodSource(CONTAINERS_WITH_KUBELET)
    public void exposes_node_port(final Supplier<KubernetesWithKubeletContainer<?>> factory) {
        runWithK8s(factory.get().withExposedPorts(30000), this::assertExposesNodePort);
    }

    private void assertExposesNodePort(KubernetesWithKubeletContainer<?> k8s) {
        final Pod pod = runWithClient(k8s, client -> {
            return createSimplePod(client, createNewNamespace(client));
        });
        runWithClient(k8s, client -> {
            client.services().create(new ServiceBuilder()
                    .withNewMetadata()
                    .withName("nginx")
                    .withNamespace(pod.getMetadata().getNamespace())
                    .endMetadata()
                    .withNewSpec()
                    .withType("NodePort")
                    .withSelector(new HashMap<String, String>() {{
                        put("app", "nginx");
                    }})
                    .withPorts(new ServicePortBuilder()
                            .withNodePort(30000)
                            .withPort(80)
                            .withTargetPort(new IntOrString(80))
                            .withProtocol("TCP")
                            .build())
                    .endSpec()
                    .build());
            await("testpod")
                    .timeout(ofSeconds(300))
                    .until(TestUtils.http("http://localhost:" + k8s.getMappedPort(30000)));

        });
    }

}
