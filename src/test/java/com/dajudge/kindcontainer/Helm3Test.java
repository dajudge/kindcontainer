package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.utility.MountableFile;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Helm3Test {

    private static final String NAMESPACE = "hello";

    @TestFactory
    public Stream<DynamicTest> can_install_something() {
        return allContainers(this::assertCanInstallSomething);
    }

    @TestFactory
    public Stream<DynamicTest> assertInvalidVersionFails() {
        return allContainers(this::assertInvalidVersionFails);
    }

    private void assertCanInstallSomething(final KubernetesTestPackage<? extends KubernetesContainer<?>> testPkg) {
        assertCanInstallVersion(testPkg, "0.1.0");
    }

    private void assertInvalidVersionFails(final KubernetesTestPackage<? extends KubernetesContainer<?>> testPkg) {
        assertThrows(RuntimeException.class, () -> assertCanInstallVersion(testPkg, "42.42.42"));
    }

    private void assertCanInstallVersion(
            final KubernetesTestPackage<? extends KubernetesContainer<?>> testPkg,
            final String version
    ) {
        runWithK8s(configureContainer(testPkg.newContainer(), version), k8s -> runWithClient(k8s, client -> {
            assertFalse(client.apps().deployments().inNamespace(NAMESPACE).list().getItems().isEmpty());
        }));
    }

    private KubernetesContainer<?> configureContainer(final KubernetesContainer<?> container, final String version) {
        return container.withHelm3(helm -> {
            helm.copyFileToContainer(MountableFile.forClasspathResource("hello-values.yaml"), "/apps/values.yaml");
            helm.repo.add.run("examples", "https://helm.github.io/examples");
            helm.repo.update.run();
            helm.install
                    .namespace(NAMESPACE)
                    .createNamespace()
                    .values("/apps/values.yaml")
                    .version(version)
                    .run("hello", "examples/hello-world");
        });
    }
}
