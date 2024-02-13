package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;
import org.testcontainers.utility.MountableFile;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Helm3Test {

    @TestFactory
    public Stream<DynamicTest> can_install_something() {
        return allContainers(this::assertCanInstallSomething);
    }

    private void assertCanInstallSomething(final KubernetesTestPackage<? extends KubernetesContainer<?>> testPkg) {
        runWithK8s(configureContainer(testPkg.newContainer()), k8s -> runWithClient(k8s, client -> {
            assertFalse(client.apps().deployments().inNamespace("hello").list().getItems().isEmpty());
        }));
    }

    private KubernetesContainer<?> configureContainer(KubernetesContainer<?> container) {
        return container.withHelm3(helm -> {
            helm.copyFileToContainer(MountableFile.forClasspathResource("hello-values.yaml"), "/apps/values.yaml");
            helm.repo.add.run("examples", "https://helm.github.io/examples");
            helm.repo.update.run();
            helm.install
                    .namespace("hello")
                    .createNamespace()
                    .values("/apps/values.yaml")
                    .run("hello", "examples/hello-world");
        });
    }
}
