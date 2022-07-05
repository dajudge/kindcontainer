package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.KubernetesContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.apiServerContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlCreateNamespaceFluentTest {
    @TestFactory
    public Stream<DynamicTest> creates_namespace() {
        return apiServerContainers(this::assertCreatesNamespace);
    }

    private void assertCreatesNamespace(final KubernetesContainer<?> container) {
        runWithK8s(createContainer(container), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.namespaces().withName("my-namespace").get());
        }));
    }

    private KubernetesContainer<?> createContainer(final KubernetesContainer<?> container) {
        return container.withKubectl(kubectl -> kubectl.create.namespace.run("my-namespace"));
    }
}
