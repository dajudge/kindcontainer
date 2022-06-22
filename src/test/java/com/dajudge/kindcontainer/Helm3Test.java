package com.dajudge.kindcontainer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.ALL_CONTAINERS;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Helm3Test {

    @ParameterizedTest
    @MethodSource(ALL_CONTAINERS)
    public void can_install_something(final Supplier<KubernetesContainer<?>> factory) {
        try (final KubernetesContainer<?> k8s = createContainer(factory)) {
            k8s.start();
            runWithClient(k8s, client -> {
                assertFalse(client.apps().deployments().inNamespace("kubernetes-replicator").list().getItems().isEmpty());
            });
        }
    }

    private KubernetesContainer<?> createContainer(final Supplier<KubernetesContainer<?>> factory) {
        return factory.get().withHelm3(helm -> {
            helm.repo.add.run("mittwald", "https://helm.mittwald.de");
            helm.repo.update.run();
            helm.install
                    .namespace("kubernetes-replicator")
                    .createNamespace()
                    .run("kubernetes-replicator", "mittwald/kubernetes-replicator");
        });
    }
}
