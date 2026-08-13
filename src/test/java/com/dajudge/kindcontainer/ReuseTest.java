package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.reusableContainers;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class ReuseTest {

    @TestFactory
    public Stream<DynamicTest> does_not_reapply_postAvailabilityExecutions() {
        return reusableContainers().map(testPkg -> dynamicTest(
                testPkg.toString(),
                () -> assertDoesNotReapplyPostAvailabilityExecutions(testPkg::newContainer)
        ));
    }

    private void assertDoesNotReapplyPostAvailabilityExecutions(final Supplier<KubernetesContainer<?>> factory) {
        try (final KubernetesContainer<?> orig = applyConfig(factory.get())) {
            orig.start();
            try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(fromKubeconfig(orig.getKubeconfig())).build()) {
                assertNotNull(client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").get());
                client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").delete();
            }
            final KubernetesContainer<?> copy = applyConfig(factory.get());
            copy.start();
            try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(fromKubeconfig(copy.getKubeconfig())).build()) {
                assertNotNull(client.namespaces().withName("my-namespace").get());
                assertNull(client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").get());
            }
        }
    }

    public KubernetesContainer<?> applyConfig(final KubernetesContainer<?> k8s) {
        return k8s.withReuse(true)
                .withKubectl(kubectl -> kubectl.apply.fileFromClasspath("manifests/serviceaccount1.yaml").run());
    }
}
