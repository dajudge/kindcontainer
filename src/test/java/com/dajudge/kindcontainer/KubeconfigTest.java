package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubeconfigTest {

    @TestFactory
    public Stream<DynamicTest> with_kubeconfig_gets_executed() {
        return allContainers(k8s -> {
            runWithK8s(configureContainer(k8s), this::assertWithKubeconfigGetsExecuted);
        });
    }

    private void assertWithKubeconfigGetsExecuted(KubernetesContainer<?> k8s) {
        runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("default").configMaps().withName("test").get());
        });
    }

    private static KubernetesContainer<?> configureContainer(final KubernetesContainer<?> container) {
        return container.withKubeconfig(kubeconfig -> {
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(kubeconfig))) {
                client.inNamespace("default").configMaps().create(new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName("test")
                        .endMetadata()
                        .build());
            }
        });
    }
}
