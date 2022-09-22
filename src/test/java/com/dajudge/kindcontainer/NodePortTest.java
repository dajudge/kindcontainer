package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import com.dajudge.kindcontainer.util.TestUtils;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kubeletContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.*;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;

public class NodePortTest {

    private static final Logger LOG = LoggerFactory.getLogger(NodePortTest.class);

    @TestFactory
    public Stream<DynamicTest> exposes_node_port() {
        return kubeletContainers(this::assertExposesNodePort);
    }

    private void assertExposesNodePort(final KubernetesTestPackage<? extends KubernetesWithKubeletContainer<?>> testPkg) {
        runWithK8s(configureContainer(testPkg.newContainer()), k8s -> {
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
                try {
                    await("testpod answers on node port")
                            .timeout(1, TimeUnit.MINUTES)
                            .until(TestUtils.http("http://localhost:" + k8s.getMappedPort(30000)));
                } catch (final RuntimeException e) {
                    final Pod currentPodState = client.pods()
                            .inNamespace(pod.getMetadata().getNamespace())
                            .withName(pod.getMetadata().getName())
                            .get();
                    LOG.info("Test pod state: {}", currentPodState);
                    throw e;
                }
            });
        });
    }

    private KubernetesWithKubeletContainer<?> configureContainer(final KubernetesWithKubeletContainer<?> container) {
        return container.withExposedPorts(30000);
    }

}
