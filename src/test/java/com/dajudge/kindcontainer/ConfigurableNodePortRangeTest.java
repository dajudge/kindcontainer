package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.HashMap;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kubeletContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigurableNodePortRangeTest {

    private static KubernetesWithKubeletContainer<?> configureContainer(final KubernetesWithKubeletContainer<?> k8s) {
        return k8s.withNodePortRange(20000, 20010);
    }

    @TestFactory
    public Stream<DynamicTest> can_expose_in_valid_range() {
        return kubeletContainers(container -> {
            runWithK8s(configureContainer(container), k8s -> {
                runWithClient(configureContainer(container), client -> {
                    createService(client, "valid-port-service-min", 20000);
                    createService(client, "valid-port-service-max", 20010);
                });
            });
        });
    }

    @TestFactory
    public Stream<DynamicTest> cannot_expose_outside_of_valid_range() {
        return kubeletContainers(container -> {
            runWithK8s(configureContainer(container), k8s -> assertThrows(KubernetesClientException.class, () -> {
                try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(container.getKubeconfig()))) {
                    createService(client, "invalid-port-service", 20011);
                }
            }));
        });
    }

    private void createService(
            final DefaultKubernetesClient client,
            final String name,
            final int nodePort
    ) {
        client.services().create(new ServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace("default")
                .endMetadata()
                .withSpec(new ServiceSpecBuilder()
                        .withSelector(new HashMap<String, String>() {{
                            put("app", "doesNotExist");
                        }})
                        .withType("NodePort")
                        .withPorts(new ServicePortBuilder()
                                .withName("port")
                                .withPort(80)
                                .withNodePort(nodePort)
                                .build())
                        .build())
                .build());
    }
}
