package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
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
        return reusableContainers()
                .map(factory -> dynamicTest(
                        factory.toString(),
                        () -> assertDoesNotReapplyPostAvailabilityExecutions(factory)
                ));
    }

    private void assertDoesNotReapplyPostAvailabilityExecutions(final Supplier<KubernetesContainer<?>> factory) {
        try (final KubernetesContainer<?> orig = applyConfig(factory.get())) {
            orig.start();
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(orig.getKubeconfig()))) {
                assertNotNull(client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").get());
                client.serviceAccounts().inNamespace("my-namespace").withName("my-service-account").delete();
            }
            final KubernetesContainer<?> copy = applyConfig(factory.get());
            copy.start();
            try (final DefaultKubernetesClient client = new DefaultKubernetesClient(fromKubeconfig(copy.getKubeconfig()))) {
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
