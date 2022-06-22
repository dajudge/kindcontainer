package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.ALL_CONTAINERS;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubeconfigTest {

    @ParameterizedTest
    @MethodSource(ALL_CONTAINERS)
    public void with_kubeconfig_gets_executed(final Supplier<KubernetesContainer<?>> factory) {
        runWithK8s(createContainer(factory), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.inNamespace("default").configMaps().withName("test").get());
        }));
    }

    private static KubernetesContainer<?> createContainer(final Supplier<KubernetesContainer<?>> factory) {
        return factory.get().withKubeconfig(kubeconfig -> {
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
