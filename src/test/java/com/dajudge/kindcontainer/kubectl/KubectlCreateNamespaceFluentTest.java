package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.KubernetesContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.APISERVER_CONTAINER;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class KubectlCreateNamespaceFluentTest {
    @ParameterizedTest
    @MethodSource(APISERVER_CONTAINER)
    public void creates_namespace(final Supplier<KubernetesContainer<?>> factory) {
        runWithK8s(createContainer(factory), k8s -> runWithClient(k8s, client -> {
            assertNotNull(client.namespaces().withName("my-namespace").get());
        }));
    }

    private KubernetesContainer<?> createContainer(Supplier<KubernetesContainer<?>> factory) {
        return factory.get().withKubectl(kubectl -> {
            kubectl.create.namespace.run("my-namespace");
        });
    }
}
