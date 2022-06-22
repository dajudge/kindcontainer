package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.configure;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.containersWithKubelet;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigurableNodePortRangeTest {
    public static Stream<Supplier<KubernetesWithKubeletContainer<?>>> containers() {
        return configure(containersWithKubelet(), k8s -> k8s.withNodePortRange(20000, 20010));
    }

    @ParameterizedTest
    @MethodSource("containers")
    public void can_expose_in_valid_range(final Supplier<KubernetesWithKubeletContainer<?>> containerFactory) {
        createService(containerFactory, "valid-port-service-min", 20000);
        createService(containerFactory, "valid-port-service-max", 20010);
    }

    @ParameterizedTest
    @MethodSource("containers")
    public void cannot_expose_outside_of_valid_range(final Supplier<KubernetesWithKubeletContainer<?>> containerFactory) {
        assertThrows(KubernetesClientException.class, () -> {
            createService(containerFactory, "invalid-port-service", 20011);
        });
    }

    private void createService(
            final Supplier<KubernetesWithKubeletContainer<?>> containerFactory,
            final String name,
            final int nodePort
    ) {
        try (final KubernetesWithKubeletContainer<?> container = containerFactory.get()) {
            container.start();
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(container.getKubeconfig()))) {
                createService(client, name, nodePort);
            }
        }
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
