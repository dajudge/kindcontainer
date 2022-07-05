package com.dajudge.kindcontainer;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Helm3Test {

    @TestFactory
    public Stream<DynamicTest> can_install_something() {
        return allContainers(k8s -> runWithK8s(configureContainer(k8s), this::assertCanInstallSomething));
    }

    private void assertCanInstallSomething(final KubernetesContainer<?> k8s) {
        runWithClient(k8s, client -> {
            assertFalse(client.apps().deployments().inNamespace("kubernetes-replicator").list().getItems().isEmpty());
        });
    }

    private KubernetesContainer<?> configureContainer(KubernetesContainer<?> container) {
        return container.withHelm3(helm -> {
            helm.repo.add.run("mittwald", "https://helm.mittwald.de");
            helm.repo.update.run();
            helm.install
                    .namespace("kubernetes-replicator")
                    .createNamespace()
                    .run("kubernetes-replicator", "mittwald/kubernetes-replicator");
        });
    }
}
