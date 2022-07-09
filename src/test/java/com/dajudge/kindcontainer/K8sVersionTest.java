package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.allContainers;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class K8sVersionTest {

    @TestFactory
    public Stream<DynamicTest> can_start() {
        return allContainers(this::assertStartsCorrectVersion);
    }

    private void assertStartsCorrectVersion(KubernetesTestPackage<? extends KubernetesContainer<?>> testPkg) {
        final KubernetesContainer<?> k8s = testPkg.newContainer();
        final KubernetesVersionDescriptor version = testPkg.version();
        try {
            k8s.start();
            runWithClient(k8s, client -> {
                final String gitVersion = client.getKubernetesVersion().getGitVersion();  // e.g. v1.21.9+k3s1
                final String k8sVersion = gitVersion.replaceAll("\\+.*", ""); // e.g. v1.21.9
                assertEquals(version.getKubernetesVersion(), k8sVersion);
            });
        } catch (final Exception e) {
            throw new AssertionError("Failed to launch kubernetes with version " + version.getKubernetesVersion(), e);
        } finally {
            k8s.stop();
        }
    }

}
